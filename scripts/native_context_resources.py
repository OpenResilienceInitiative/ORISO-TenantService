"""Fail-closed resource leases and exact cleanup for disposable native CTX gates."""
import json
import math
import os
from pathlib import Path
import re
import subprocess
import sys
import time

MIB = 1024 * 1024


class ResourceRefusal(RuntimeError):
    pass


class CleanupRefusal(RuntimeError):
    def __init__(self, errors, completed):
        super().__init__("Exact owned resource cleanup was not confirmed")
        self.errors = errors
        self.completed = completed


def linux_memory(proc=Path("/proc"), cgroup_root=Path("/sys/fs/cgroup")):
    """Read host pressure and every applicable ancestor of the process's v2 cgroup."""
    try:
        fields = {}
        for line in (proc / "meminfo").read_text().splitlines():
            name, _, raw = line.partition(":")
            if name in ("MemTotal", "MemAvailable", "SwapTotal", "SwapFree"):
                match = re.fullmatch(r"\s*(\d+) kB\s*", raw)
                if not match:
                    raise ValueError("Malformed memory unit")
                fields[name] = int(match[1]) * 1024
        total, available = fields["MemTotal"], fields["MemAvailable"]
        swap = fields["SwapTotal"] - fields["SwapFree"]
        if not 0 < available <= total or not 0 <= swap <= fields["SwapTotal"]:
            raise ValueError("Invalid memory range")
        pressure = {}
        for line in (proc / "pressure/memory").read_text().splitlines():
            name, *values = line.split()
            pressure[name] = dict(value.split("=", 1) for value in values)
        full = float(pressure["full"]["avg10"])
        full_total = int(pressure["full"]["total"])
        if not math.isfinite(full) or full < 0 or full_total < 0:
            raise ValueError("Invalid PSI")
        if not (cgroup_root / "cgroup.controllers").is_file():
            raise ValueError("A verified unified cgroup is required")
        groups = [line[3:] for line in (proc / "self/cgroup").read_text().splitlines()
                  if line.startswith("0::")]
        if len(groups) != 1 or not groups[0].startswith("/") or ".." in Path(groups[0]).parts:
            raise ValueError("Ambiguous cgroup path")
        root = cgroup_root.resolve()
        group = (root / groups[0].lstrip("/")).resolve()
        group.relative_to(root)
        cgroups = []
        while True:
            memory_files = [group / name for name in ("memory.max", "memory.current", "memory.events")]
            if group == root and not any(path.exists() for path in memory_files):
                # Kernel root cgroups have no non-root memory limit/event files.
                # Accept this only for an explicitly verified mount of the hierarchy root.
                mounts = []
                for line in (proc / "self/mountinfo").read_text().splitlines():
                    left, right = line.split(" - ", 1)
                    parts = left.split()
                    if right.split()[0] == "cgroup2" and parts[4] == str(root):
                        mounts.append(parts[3])
                if mounts != ["/"]:
                    raise ValueError("An unbounded kernel root mount was not verified")
                cgroups.append({"path": "/", "limitBytes": None, "currentBytes": None,
                                "events": {}, "measurement": "verified-kernel-root-no-limit-files"})
                break
            maximum = (group / "memory.max").read_text().strip()
            limit = None if maximum == "max" else int(maximum)
            current = int((group / "memory.current").read_text().strip())
            events = dict(line.split() for line in (group / "memory.events").read_text().splitlines())
            events = {key: int(events[key]) for key in ("high", "max", "oom", "oom_kill")}
            if current < 0 or any(value < 0 for value in events.values()) or (
                    limit is not None and (limit <= 0 or current > limit)):
                raise ValueError("Invalid cgroup memory range")
            cgroups.append({"path": "/" + str(group.relative_to(root)),
                            "limitBytes": limit, "currentBytes": current, "events": events})
            if group == root:
                break
            group = group.parent
        return {"platform": "linux", "totalBytes": total, "availableBytes": available,
                "swapUsedBytes": swap, "fullPsiAvg10": full, "fullPsiTotal": full_total,
                "cgroups": cgroups}
    except (OSError, ValueError, KeyError, IndexError) as failure:
        raise ResourceRefusal("Required Linux memory/pressure/cgroup measurement is unavailable") from failure


def machine_memory():
    if sys.platform.startswith("linux"):
        return linux_memory()
    if sys.platform == "darwin":
        try:
            result = subprocess.run(["memory_pressure", "-Q"], capture_output=True,
                                    text=True, timeout=10, check=True)
            match = re.search(r"System-wide memory free percentage:\s*(\d+)%", result.stdout)
            total = int(subprocess.check_output(["sysctl", "-n", "hw.memsize"], timeout=10))
            pressure = int(subprocess.check_output(
                ["sysctl", "-n", "kern.memorystatus_vm_pressure_level"], timeout=10))
            swap = subprocess.check_output(["sysctl", "-n", "vm.swapusage"],
                                           text=True, timeout=10)
            used = re.search(r"used\s*=\s*([0-9.]+)M", swap)
            if not match or not used or pressure != 1:
                raise ValueError("Mac warning/critical memory pressure")
            return {"platform": "darwin", "totalBytes": total,
                    "availableBytes": total * int(match[1]) // 100,
                    "swapUsedBytes": int(float(used[1]) * MIB), "fullPsiAvg10": 0,
                    "cgroups": [], "nativePressureLevel": pressure}
        except (OSError, ValueError, subprocess.SubprocessError) as failure:
            raise ResourceRefusal("Native Mac memory hold remains in force") from failure
    raise ResourceRefusal("No reviewed native resource adapter for this platform")


def require_memory(sample, budget_mib, starting=False):
    """Fresh runners: no swap, no full PSI stalls/events; 40% start and 25% retained."""
    total, available = sample["totalBytes"], sample["availableBytes"]
    if budget_mib <= 0 or sample["swapUsedBytes"] or sample["fullPsiAvg10"] > 0:
        raise ResourceRefusal("Native start/monitor refuses swap or full memory pressure")
    reserve = math.ceil(total / 4)
    if available < reserve or (starting and (
            available * 100 < 40 * total or available < budget_mib * MIB + reserve)):
        raise ResourceRefusal("Native host headroom cannot retain the required reserve")
    for group in sample["cgroups"]:
        if any(group["events"].values()):
            raise ResourceRefusal("Native cgroup pressure/OOM events refuse the lease")
        limit = group["limitBytes"]
        if limit is not None:
            remaining = limit - group["currentBytes"]
            reserve = math.ceil(limit / 4)
            if remaining < reserve or (starting and (
                    remaining * 100 < 40 * limit or remaining < budget_mib * MIB + reserve)):
                raise ResourceRefusal("Native cgroup headroom cannot retain the required reserve")
    return available * 100 // total


class MemoryLease:
    def __init__(self, budget_mib, observations):
        self.budget_mib = budget_mib
        self.observations = Path(observations)
        self.last = None
        self.full_psi_total = None

    def check(self, starting=False):
        sample = machine_memory()
        self.last = sample
        with os.fdopen(os.open(self.observations, os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o600), "a") as f:
            f.write(json.dumps({"measuredAt": time.time(), "starting": starting,
                                "budgetMiB": self.budget_mib, **sample}, sort_keys=True) + "\n")
        percentage = require_memory(sample, self.budget_mib, starting)
        full_total = sample.get("fullPsiTotal")
        if self.full_psi_total is not None and full_total != self.full_psi_total:
            raise ResourceRefusal("Full memory stalls changed during the native lease")
        self.full_psi_total = full_total
        return percentage


def docker(*command):
    return subprocess.check_output(["docker", *command], text=True, timeout=30).strip()


def inspect_optional(kind, name):
    command = ["docker", *([kind] if kind in ("network", "volume") else []), "inspect", name]
    result = subprocess.run(command, capture_output=True, text=True, timeout=30)
    if not result.returncode:
        return json.loads(result.stdout)[0]
    missing = re.search(r"[Nn]o such (?:object|container|network|volume)", result.stderr)
    missing_network = kind == "network" and ("network " + name + " not found") in result.stderr
    if result.returncode == 1 and (missing or missing_network):
        return None
    raise CleanupRefusal([{"kind": kind, "name": name, "inspection": "unavailable"}], [])


class OwnedDocker:
    """Track names before create, including a create whose response was lost."""
    def __init__(self, label_key, run_id, command=docker, inspect=inspect_optional):
        self.label_key, self.run_id = label_key, run_id
        self.command, self.inspect = command, inspect
        self.planned = []

    def plan(self, kind, name):
        if kind not in ("network", "container") or (kind, name) in self.planned:
            raise ValueError("Unique exact owned network/container required")
        self.planned.append((kind, name))

    def cleanup(self):
        completed, errors = [], []
        for kind, name in reversed(self.planned):
            try:
                metadata = self.inspect(kind, name)
                volumes = []
                runtime = None
                if metadata is not None:
                    labels = metadata.get("Config", {}).get("Labels", {}) if kind == "container" else metadata.get("Labels", {})
                    if (labels or {}).get(self.label_key) != self.run_id:
                        raise ResourceRefusal("Owned resource label changed; refusing removal")
                    if kind == "container":
                        runtime = {"oomKilled": metadata.get("State", {}).get("OOMKilled"),
                                   "restartCount": metadata.get("RestartCount")}
                        volumes = [m["Name"] for m in metadata.get("Mounts", []) if m["Type"] == "volume"]
                        self.command("rm", "-f", "-v", name)
                        if any(self.inspect("volume", volume) is not None for volume in volumes):
                            raise ResourceRefusal("Owned anonymous volume survived cleanup")
                    else:
                        self.command("network", "rm", name)
                if self.inspect(kind, name) is not None:
                    raise ResourceRefusal("Exact owned resource survived cleanup")
                completed.append({"kind": kind, "name": name, "confirmedAbsent": True,
                                  "anonymousVolumesConfirmedAbsent": volumes, "runtime": runtime})
                if runtime is not None and (
                        runtime["oomKilled"] is not False or runtime["restartCount"] != 0):
                    errors.append({"kind": kind, "name": name,
                                   "failureType": "OwnedContainerOOMRestartOrUnknownState"})
            except Exception as failure:
                errors.append({"kind": kind, "name": name, "failureType": type(failure).__name__})
        if errors:
            raise CleanupRefusal(errors, completed)
        return completed
