#!/usr/bin/env python3
"""Bind a CTX1 native run to exact Git inputs and fail closed on incomplete proof."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import sys

HOST = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location(
    "context_preflight_support", HOST / "scripts/ci/scenario-c-context-preflight.py")
support = importlib.util.module_from_spec(spec)
spec.loader.exec_module(support)


def verify_v1(roots):
    contract = json.loads((HOST / "scripts/ci/scenario-c-bootstrap-v1-inputs.json").read_text())
    return all(hashlib.sha256((roots[role] / name).read_bytes()).hexdigest() == digest
               for role, inputs in contract.items() for name, digest in inputs.items())


def known_container_runtime(cleanup):
    containers = [row for row in cleanup if row.get("kind") == "container"]
    return len(containers) == 2 and all(
        isinstance(row.get("runtime"), dict) and row["runtime"].get("oomKilled") is False
        and type(row["runtime"].get("restartCount")) is int
        and row["runtime"]["restartCount"] == 0 for row in containers)


def safe_early_failure(log):
    """Uncaught pre-fixture failures retain code frames/type, never provider messages/values."""
    result = {"frames": []}
    for line in log.read_text(errors="replace").splitlines():
        frame = re.fullmatch(
            r'\s*File "[^"\n]*[/\\]([A-Za-z0-9_.-]+\.py)", line (\d+), in ([A-Za-z0-9_<>]+)', line)
        if frame:
            result["frames"].append({"file": frame[1], "line": int(frame[2]), "function": frame[3]})
        failure = re.match(r"^([A-Za-z_][A-Za-z0-9_.]*(?:Error|Exception)):", line)
        if failure:
            result["failureType"] = failure[1]
        if line == "ModuleNotFoundError: No module named 'yaml'":
            result["missingDeclaredDependency"] = "PyYAML"
    result["frames"] = result["frames"][-8:]
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--receiver", type=Path, required=True)
    parser.add_argument("--receiver-ref", required=True)
    parser.add_argument("--helm", type=Path, required=True)
    parser.add_argument("--helm-ref", required=True)
    parser.add_argument("--expect", choices=("red", "green"), required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    args = parser.parse_args()
    if os.environ.get("CI") != "true" or os.environ.get("RUNNER_OS") != "Linux":
        raise SystemExit("Native CTX execution is restricted to the selected fresh Linux CI runner")
    roots = {"tenant": HOST, "cts": args.receiver.resolve(), "helm": args.helm.resolve()}
    refs = {"tenant": os.environ.get("GITHUB_SHA"), "cts": args.receiver_ref,
            "helm": args.helm_ref}
    for role, root in roots.items():
        if not refs[role] or not re.fullmatch(r"[a-f0-9]{40}", refs[role]):
            raise SystemExit("Every selected source must be an immutable full Git commit")
        if support.git(root, "rev-parse", "HEAD") != refs[role] or support.git(root, "status", "--porcelain"):
            raise SystemExit("Every native source checkout must be exact and clean")
    if args.expect == "red" and not verify_v1(roots):
        raise SystemExit("All six original v1 bootstrap production files are required for RED")
    evidence = args.evidence.resolve()
    evidence.mkdir(parents=True, exist_ok=False)
    before = {role: support.snapshot(root) for role, root in roots.items()}
    report = {"state": "STARTED_NOT_ACCEPTED", "refs": refs, "expect": args.expect,
              "sourceBefore": before, "scope": "CTX1 native slice only; not whole Scenario C"}
    java_home = Path(os.environ["JAVA_HOME"])
    version = subprocess.check_output([str(java_home / "bin/java"), "--version"], text=True)
    if "21.0.12" not in version:
        raise SystemExit("The selected native Java runtime must match the verified toolchain")
    report["javaVersion"] = version.strip()
    try:
        log = evidence / "sanitized-runner.log"
        with os.fdopen(os.open(log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), "w") as output:
            child = subprocess.Popen(
                [sys.executable, str(HOST / "scripts/test-bootstrap-context-roundtrip.py"),
                 "--tenant", str(HOST), "--receiver", str(roots["cts"]), "--helm", str(roots["helm"]),
                 "--java-home", str(java_home), "--expect", args.expect, "--expected-tests", "1",
                 "--evidence", str(evidence / "native")], cwd=HOST, stdout=output,
                stderr=subprocess.STDOUT, start_new_session=True,
            )
            try:
                report["runnerExit"] = child.wait(timeout=1500)
            except subprocess.TimeoutExpired:
                # The Python owner receives SIGINT and runs its exact-name cleanup first.
                child.send_signal(signal.SIGINT)
                try:
                    child.wait(timeout=90)
                except subprocess.TimeoutExpired:
                    os.killpg(child.pid, signal.SIGKILL)
                    child.wait()
                report["state"] = "DEADLINE_FAILURE_NO_ACCEPTANCE"
                return 1
        receipt_path = evidence / "native/receipt.json"
        if not receipt_path.is_file():
            report["state"] = "MISSING_NATIVE_RECEIPT_NO_ACCEPTANCE"
            report["safeEarlyFailure"] = safe_early_failure(log)
            return 1
        native = json.loads(receipt_path.read_text())
        report["nativeReceipt"] = native
        wanted = "BEHAVIORAL_RED" if args.expect == "red" else "PASS"
        counts = native.get("counts", {})
        expected = (counts.get("tests") == 1 and not counts.get("errors", 1)
                    and not counts.get("skipped", 1))
        if args.expect == "red":
            expected = (expected and counts.get("failures") == 1
                        and native.get("observed", {}).get("receiverStatus") == 403
                        and native.get("observed", {}).get("allocationStatus") == "RESERVED"
                        and native.get("receiverBefore") == native.get("receiverAfter")
                        and set(native.get("receiverBefore", {})) == {"claims", "counter", "types"})
        else:
            expected = expected and counts.get("failures") == 0
        cleanup = native.get("cleanup", [])
        run_id = native.get("runId", "")
        expected_resources = {("network", "oriso-context-" + run_id[:12]),
                              ("container", "oriso-context-kc-" + run_id[:12]),
                              ("container", "oriso-context-mongo-" + run_id[:12])}
        process_path = evidence / "native/process-cleanup.jsonl"
        groups = [json.loads(line) for line in process_path.read_text().splitlines()] if process_path.is_file() else []
        groups_complete = (len(groups) == 3 and len({g.get("processGroup") for g in groups}) == 3
                           and all(g.get("confirmedAbsent") is True for g in groups)
                           and native.get("processCleanup", {}).get("confirmedAbsent") is True)
        if (report["runnerExit"] or native.get("state") != wanted or not expected
                or not native.get("sourceUnchanged") or len(cleanup) != 3
                or {(row.get("kind"), row.get("name")) for row in cleanup} != expected_resources
                or not all(row.get("confirmedAbsent") is True for row in cleanup) or not groups_complete
                or not known_container_runtime(cleanup)):
            report["state"] = "INCOMPLETE_OR_FAILED_NATIVE_PROOF"
            return 1
        report["state"] = wanted
        return 0
    finally:
        after = {role: support.snapshot(root) for role, root in roots.items()}
        report["sourceAfter"] = after
        report["sourceStable"] = after == before
        if after != before:
            report["state"] = "SOURCE_CHANGED_NO_ACCEPTANCE"
        (evidence / "source-receipt.json").write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
        print(json.dumps({"state": report["state"], "sourceStable": after == before,
                          "refs": refs, "counts": report.get("nativeReceipt", {}).get("counts")}))
        if after != before:
            raise RuntimeError("Source changed during native proof; acceptance refused")


if __name__ == "__main__":
    raise SystemExit(main())
