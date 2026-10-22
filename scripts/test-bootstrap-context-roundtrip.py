#!/usr/bin/env python3
"""Explicit native gate: real Tenant caller -> real CTS receiver, isolated resources.

Requires an operator-granted native resource lease. No shared containers, source/targets,
tokens in argv or durable logs. A missing fixture or skipped case fails the gate.
"""
import argparse
import base64
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import time
import uuid
import xml.etree.ElementTree as XML


class FixtureError(Exception):
    pass


resource_spec = importlib.util.spec_from_file_location(
    "context_resource_support", Path(__file__).with_name("native_context_resources.py"))
resources = importlib.util.module_from_spec(resource_spec)
resource_spec.loader.exec_module(resources)
memory_lease = None


def configure_memory(budget_mib, evidence):
    global memory_lease
    memory_lease = resources.MemoryLease(budget_mib, evidence / "memory-observations.jsonl")
    return memory_lease


def source_manifest(root):
    names = subprocess.check_output(["git", "ls-files", "--cached", "--others", "--exclude-standard"],
                                    cwd=root, text=True).splitlines()
    return {name: hashlib.sha256((root / name).read_bytes()).hexdigest()
            for name in sorted(set(names)) if (root / name).is_file()
            and "__pycache__" not in Path(name).parts
            and (name.startswith(("src/", "scripts/", ".mvn/")) or name in ("pom.xml", "mvnw"))}


def free_guard():
    if memory_lease is None:
        raise FixtureError("A measured native memory lease must be configured before execution")
    try:
        return memory_lease.check()
    except resources.ResourceRefusal as failure:
        raise FixtureError(str(failure)) from None


def stop(process):
    if process is None:
        return None

    def group_alive():
        # Every caller created this exact process session with start_new_session=True.
        process.poll()
        try:
            os.killpg(process.pid, 0)
            return True
        except ProcessLookupError:
            return False

    if group_alive():
        try:
            os.killpg(process.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
        deadline = time.monotonic() + 20
        while group_alive() and time.monotonic() < deadline:
            time.sleep(0.1)
        if group_alive():
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            deadline = time.monotonic() + 10
            while group_alive() and time.monotonic() < deadline:
                time.sleep(0.1)
    if group_alive():
        raise FixtureError("Owned process group survived finite TERM/KILL cleanup")
    process.wait(timeout=1)
    return {"processGroup": process.pid, "confirmedAbsent": True}


def private_log(path):
    return os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w")


def bounded(command, cwd, env, log, seconds, overall):
    free_guard()
    with private_log(log) as output:
        process = subprocess.Popen(command, cwd=cwd, env=env, stdout=output,
                                   stderr=subprocess.STDOUT, start_new_session=True)
        try:
            deadline = min(time.monotonic() + seconds, overall)
            while process.poll() is None:
                if time.monotonic() >= deadline:
                    raise FixtureError("Owned compile/test process exceeded its finite lease")
                free_guard()
                time.sleep(2)
            return process.returncode
        finally:
            cleanup = stop(process)
            with (memory_lease.observations.parent / "process-cleanup.jsonl").open("a") as output:
                output.write(json.dumps(cleanup) + "\n")


def docker(*command):
    return subprocess.check_output(["docker", *command], text=True, timeout=30).strip()


def intended_counterfactual(tree, counts, result, receipt, tenant):
    if not result or counts != {"tests": 1, "failures": 1, "errors": 0, "skipped": 0}:
        return False
    cases = tree.findall("testcase")
    method = "ownedTenantCreationUsesTheSharedV2ProofAtTheMaterializedDtoSendSeam"
    if (len(cases) != 1 or cases[0].get("classname") !=
            "com.vi.tenantservice.api.controller.BootstrapV2CallerReceiverIT"
            or cases[0].get("name") not in (method, method + "()")):
        return False
    failures = cases[0].findall("failure")
    if len(failures) != 1 or cases[0].find("error") is not None or cases[0].find("skipped") is not None:
        return False
    source = tenant / "src/test/java/com/vi/tenantservice/api/controller/BootstrapV2CallerReceiverIT.java"
    lines = [i + 1 for i, line in enumerate(source.read_text().splitlines())
             if "assertThat(RECEIVER_STATUS.get()).isEqualTo(200);" in line]
    if len(lines) != 1:
        return False
    failure = failures[0]
    message = failure.get("message", "") + "\n" + (failure.text or "")
    if (not re.search(r"\bexpected:\s*200\s+but was:\s*403\b", message)
            or method + "(BootstrapV2CallerReceiverIT.java:" + str(lines[0]) + ")" not in message):
        return False
    observation = receipt.get("observed", {})
    before = receipt.get("receiverBefore", {})
    if (observation.get("receiverStatus") != 403
            or observation.get("receiverRequestObserved") is not True
            or observation.get("allocationStatus") != "RESERVED"
            or not 400 <= observation.get("tenantStatus", 0) <= 599
            or set(before) != {"claims", "counter", "types"}
            or before != receipt.get("receiverAfter")):
        return False
    return all(set(value) == {"count", "sha256"} and type(value["count"]) is int
               and value["count"] >= 0 and re.fullmatch(r"[a-f0-9]{64}", value["sha256"])
               for value in before.values())


def safe_java_frames(log):
    """Retain public application locations and exception types, excluding diagnostic values."""
    frames, exceptions = [], []
    for line in log.read_text(errors="replace").splitlines():
        frame = re.search(
            r"\bat ((?:com\.vi|de\.caritas)\.[A-Za-z0-9_.$<>]+)\(([A-Za-z0-9_$.-]+\.java):(\d+)\)", line)
        if frame:
            value = {"method": frame[1], "file": frame[2], "line": int(frame[3])}
            if value not in frames:
                frames.append(value)
        failure = re.search(
            r"\b((?:java|javax|jakarta|org|com|de|io)\.[A-Za-z0-9_.$]+(?:Exception|Error))(?=:|$)", line)
        if failure and failure[1] not in exceptions:
            exceptions.append(failure[1])
    return {"log": log.name, "exceptionTypes": exceptions[:8],
            "applicationFrames": frames[:8] + frames[8:][-8:]}



def safe_observation(value):
    """Only fixed labels and bounded booleans/counts may leave the private fixture."""
    def refuse():
        raise FixtureError("Native observation shape refused; no private values exported")
    required = {"receiverStatus", "tenantStatus", "receiverRequestObserved", "allocationStatus"}
    if not isinstance(value, dict) or not required.issubset(value) or set(value) - required - {"paired"}:
        refuse()
    def status(number):
        return type(number) is int and (number == -1 or 100 <= number <= 599)
    if (not status(value["receiverStatus"]) or not status(value["tenantStatus"])
            or type(value["receiverRequestObserved"]) is not bool
            or value["allocationStatus"] not in ("ASSIGNED", "RESERVED", "FREE", "EXPIRED", "CANCELLED")):
        refuse()
    if "paired" not in value:
        return value
    paired = value["paired"]
    status_fields = {"tenantFailureStatus", "createStatus", "compensationStatus", "delayedReplyStatus",
                     "newTenantStatus", "oldReplayStatus"}
    boolean_fields = {"allocationRestored", "allocationRestoredThenReassigned",
                      "compensationReplyLost", "sameCompensationAttemptAndPayload"}
    pair_counts = {"compensationAttempts"}
    stages = {"beforeDelayedRelease", "terminalRace", "staleWrites", "newAttempt"}
    if (not isinstance(paired, dict) or set(paired) - status_fields - boolean_fields - pair_counts - stages
            or not {"tenantFailureStatus", "createStatus", "compensationStatus", "allocationRestored",
                    "beforeDelayedRelease"}.issubset(paired)):
        refuse()
    for field in status_fields & set(paired):
        if not status(paired[field]): refuse()
    for field in pair_counts & set(paired):
        if type(paired[field]) is not int or not 0 <= paired[field] <= 2: refuse()
    for field in boolean_fields & set(paired):
        if type(paired[field]) is not bool: refuse()
    counts = {"sequence", "actualInsertCalls", "conditionalWriteAttempts", "conditionalWritesBlocked",
              "unconditionalSaveAttempts", "staleUpdateRefusals", "retiredReadChecks", "liveTypes", "terminalTypes"}
    booleans = {"primaryInsertReached", "delayedInsertReached", "primaryInsertCompleted",
                "delayedInsertDuplicate", "staleCaptured", "ordinaryPreserved", "newAttemptDistinct",
                "newResultDistinct", "counterUnchangedSinceCapture", "counterNeverRewound",
                "counterAdvancedExactlyOnce", "awareConditionalWriteAttempted", "awareConditionalWriteBlocked",
                "unawareConditionalWriteAttempted", "unawareConditionalWriteBlocked", "retiredReadsHidden",
                "terminalRetryUnchanged"}
    for field in stages & set(paired):
        stage = paired[field]
        if not isinstance(stage, dict) or set(stage) != counts | booleans | {"state"}:
            refuse()
        if stage["state"] not in ("ACK", "FIXTURE_FAILURE"):
            refuse()
        if any(type(stage[key]) is not int or not 0 <= stage[key] <= 10000 for key in counts):
            refuse()
        if any(type(stage[key]) is not bool for key in booleans):
            refuse()
    return value


def compiled_fixture_inventory(root, compiled, classpath):
    fixtures = {}
    for path in sorted(compiled.parent.glob(compiled.stem + "*.class")):
        data = path.read_bytes()
        if not data.startswith(b"\xca\xfe\xba\xbe"):
            raise FixtureError("Actual compiled fixture required")
        fixtures[str(path.relative_to(root))] = hashlib.sha256(data).hexdigest()
    if not fixtures:
        raise FixtureError("Complete compiled fixture inventory required")
    dependencies = []
    for item in classpath.read_text().strip().split(os.pathsep):
        path = Path(item)
        if not path.is_file() or not re.fullmatch(r"[A-Za-z0-9_.+\-]+\.jar", path.name):
            raise FixtureError("Present bounded classpath JAR name required")
        with path.open("rb") as stream:
            digest = hashlib.file_digest(stream, "sha256").hexdigest()
        dependencies.append({"jar": path.name, "sha256": digest, "bytes": path.stat().st_size})
    return {"fixture": fixtures, "classpathSHA256": hashlib.sha256(classpath.read_bytes()).hexdigest(),
            "dependencies": dependencies}


def owned_memory_snapshot(owned, name, proc_root=Path("/proc"),
                          cgroup_root=Path("/sys/fs/cgroup")):
    """Read only this labelled container's numeric memory counters before removal."""
    try:
        metadata = owned.inspect("container", name)
        if metadata is None or (metadata.get("Config", {}).get("Labels") or {}).get(
                owned.label_key) != owned.run_id:
            raise RuntimeError("Owned container required")
        pid, identifier = metadata.get("State", {}).get("Pid"), metadata.get("Id", "")
        if type(pid) is not int or pid <= 0 or not re.fullmatch("[a-f0-9]{64}", identifier):
            raise RuntimeError("Running owned container required")
        lines = (proc_root / str(pid) / "cgroup").read_text().splitlines()
        if len(lines) != 1 or not lines[0].startswith("0::/"):
            raise RuntimeError("Exact unified container cgroup required")
        group = Path(lines[0][3:])
        if group.name not in (identifier, "docker-" + identifier + ".scope"):
            raise RuntimeError("Container cgroup identity mismatch")
        directory = (cgroup_root / str(group).lstrip("/")).resolve()
        if not directory.is_relative_to(cgroup_root.resolve()):
            raise RuntimeError("Container cgroup escaped its root")
        values = {}
        for filename in ("current", "peak", "max", "events"):
            path = directory / ("memory." + filename)
            if path.stat().st_size > 4096:
                raise RuntimeError("Bounded numeric counters required")
            values[filename] = path.read_text()
        number = lambda value: int(value.strip()) if re.fullmatch(r"[0-9]+\s*", value) else None
        result = {"available": True, "currentBytes": number(values["current"]),
                  "peakBytes": number(values["peak"]), "limitBytes": number(values["max"])}
        if any(value is None for field, value in result.items() if field != "available"):
            raise RuntimeError("Finite numeric container limits required")
        events = {}
        for line in values["events"].splitlines():
            field, value = line.split()
            if field in events or number(value) is None:
                raise RuntimeError("Unique numeric memory events required")
            if field in ("low", "high", "max", "oom", "oom_kill", "oom_group_kill"):
                events[field] = number(value)
        if not {"high", "max", "oom", "oom_kill"}.issubset(events):
            raise RuntimeError("Required container events missing")
        result["events"] = events
        return result
    except Exception as error:
        # Never export paths, native representations or exception messages.
        return {"available": False, "failureType": type(error).__name__}



def main():
    parser = argparse.ArgumentParser()
    for name in ("tenant", "receiver", "helm", "java-home", "evidence"):
        parser.add_argument("--" + name, type=Path, required=True)
    parser.add_argument("--expect", choices=("red", "green"), required=True)
    parser.add_argument("--expected-tests", type=int, default=1)
    args = parser.parse_args()
    args.evidence = args.evidence.resolve()
    args.evidence.mkdir(parents=True, exist_ok=False)
    tenant, receiver, helm = (p.resolve() for p in (args.tenant, args.receiver, args.helm))
    for root in (tenant, receiver, helm):
        if not (root / "pom.xml").is_file() and root != helm:
            raise FixtureError("Explicit isolated source root unavailable")
    sys.path.insert(0, str(helm / "tests"))
    spec = importlib.util.spec_from_file_location("context_native_fixture", helm / "tests/scenario_c_receiver_fixture.py")
    fixture_module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(fixture_module)
    import _keycloak_container as shared
    before = {"tenant": source_manifest(tenant), "receiver": source_manifest(receiver),
              "helmFixture": {str(p.relative_to(helm)): hashlib.sha256(p.read_bytes()).hexdigest()
                              for p in (helm / "tests/scenario_c_receiver_fixture.py", helm / "tests/_keycloak_container.py")}}
    (args.evidence / "before-source.json").write_text(json.dumps(before, indent=2, sort_keys=True) + "\n")
    run_id = uuid.uuid4().hex
    label = "oriso-context-run-id=" + run_id
    network = "oriso-context-" + run_id[:12]
    containers = []
    process = None
    server_log = None
    temporary = None
    owned = resources.OwnedDocker("oriso-context-run-id", run_id)
    overall = time.monotonic() + 1200
    receipt = {"runId": run_id, "state": "RUNNING", "stage": "prestart", "image": shared.IMAGE,
               "budgetMiB": 3184, "sourceUnchanged": False, "cleanup": []}
    try:
        lease = configure_memory(3184, args.evidence)
        receipt["initialMemoryFreePercent"] = lease.check(starting=True)
        temporary = tempfile.TemporaryDirectory(prefix="oriso-context-")
        private = Path(temporary.name).resolve()
        os.chmod(private, 0o700)
        env = {**os.environ, "JAVA_HOME": str(args.java_home.resolve()),
               "PATH": str(args.java_home.resolve() / "bin") + os.pathsep + os.environ["PATH"],
               "MAVEN_OPTS": "-Xmx512m -XX:MaxMetaspaceSize=192m -XX:ActiveProcessorCount=2",
               "GIT_CONFIG_NOSYSTEM": "1"}
        bases = {"receiver": "53c1ab3752245e7df371a4476739de4cfeda2645",
                 "tenant": "c0da86b50ee6537facf4af9b90eb799f5e975063"}
        # All ordinary format/production/test compilation finishes before any container starts.
        receipt["compiledClasses"] = {}
        for key, root in (("receiver", receiver), ("tenant", tenant)):
            receipt["stage"] = key + "-compile"
            cp = private / (key + "-classpath")
            changed = subprocess.check_output(
                ["git", "diff", "--name-only", bases[key], "HEAD", "--", "*.java"],
                cwd=root, text=True).splitlines()
            scoped = [str(root / name) for name in changed if (root / name).is_file()]
            if not scoped:
                raise FixtureError("Changed Java inventory missing before native execution")
            command = ["./mvnw", "-B", "-ntp", "-q",
                       "-DspotlessFiles=" + "|".join(re.escape(name) for name in scoped),
                       "test-compile", "dependency:build-classpath", "-Dmdep.includeScope=test",
                       "-Dmdep.outputFile=" + str(cp)]
            if bounded(command, root, env, private / (key + "-compile.log"), 420, overall):
                raise FixtureError(key + " compile failed before behavioral HTTP execution")
            compiled = (root / "target/test-classes" /
                        ("de/caritas/cob/consultingtypeservice/api/controller/BootstrapV2ReceiverFixture.class"
                         if key == "receiver" else
                         "com/vi/tenantservice/api/controller/BootstrapV2CallerReceiverIT.class"))
            data = compiled.read_bytes()
            if not data.startswith(b"\xca\xfe\xba\xbe") or not cp.is_file():
                raise FixtureError("Fresh compiled native fixture/classpath is missing")
            receipt["compiledClasses"][key] = hashlib.sha256(data).hexdigest()
            receipt.setdefault("compiledFixtureClasses", {})[key] = compiled_fixture_inventory(root, compiled, cp)
        # Names are registered before the create call, even if Docker loses its response.
        receipt["stage"] = "network"
        owned.plan("network", network)
        docker("network", "create", "--label", label, network)
        kc = "oriso-context-kc-" + run_id[:12]
        mongo = "oriso-context-mongo-" + run_id[:12]
        for name, options in ((kc, ["--memory=768m", "--cpus=2", "-p", "127.0.0.1::8080",
                "-e", "JAVA_OPTS_APPEND=-Xmx384m -XX:ActiveProcessorCount=2",
                "-e", "KC_BOOTSTRAP_ADMIN_USERNAME=" + shared.INSTALLER[0],
                "-e", "KC_BOOTSTRAP_ADMIN_PASSWORD=" + shared.INSTALLER[1],
                shared.IMAGE, "start-dev", "--http-relative-path=/auth"]),
                (mongo, ["--memory=384m", "--cpus=1", "-p", "127.0.0.1::27017", "mongo:7.0.2",
                         "--wiredTigerCacheSizeGB", "0.25"])):
            free_guard()
            owned.plan("container", name)
            identifier = docker("run", "-d", "--name", name, "--network", network,
                                "--label", label, *options)
            containers.append(identifier)
        base = "http://" + docker("port", containers[0], "8080/tcp").splitlines()[0] + "/auth"
        receipt["stage"] = "keycloak-readiness"
        deadline = min(time.monotonic() + 120, overall)
        while True:
            free_guard()
            try:
                admin = shared.master_admin(base)
                break
            except Exception:
                if time.monotonic() >= deadline:
                    raise FixtureError("Owned Keycloak did not become ready") from None
                time.sleep(2)
        realm = "context-" + run_id[:12]
        receipt["stage"] = "native-actor-fixture"
        actors = fixture_module.seed_actors(base, realm, admin)
        fixture_module.validate_native_accounts(base, realm, admin, actors)
        for actor in actors:
            fixture_module.validate_token(fixture_module.issue(base, realm, actor), actor,
                                          base + "/realms/" + realm)
        fixture = private / "fixture.json"
        descriptor = os.open(fixture, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(descriptor, "w") as output:
            json.dump({"identityModel": "FIVE_ACTORS", "issuer": base + "/realms/" + realm,
                       "tasks": actors}, output)
        control = private / "paired-control"
        control.mkdir(mode=0o700)
        env.update({
               "ORISO_BOOTSTRAP_CONTROL_PATH": str(control),
               "ORISO_TASK_TOKEN_FIXTURE": str(fixture),
               "ORISO_BOOTSTRAP_SHARED_KEY": base64.b64encode(bytes([7]) * 32).decode(),
               "ORISO_BOOTSTRAP_RECEIVER_READY": str(private / "receiver-ready"),
               "ORISO_BOOTSTRAP_OBSERVED": str(private / "observed.json"),
               "ORISO_BOOTSTRAP_RECEIVER_BEFORE": str(private / "receiver-before.json"),
               "ORISO_BOOTSTRAP_RECEIVER_AFTER": str(private / "receiver-after.json"),
               "ORISO_BOOTSTRAP_MONGO_URI": "mongodb://" + docker("port", containers[1], "27017/tcp").splitlines()[0] + "/context_" + run_id})
        classpath = os.pathsep.join([str(receiver / "target/test-classes"), str(receiver / "target/classes"),
                                    (private / "receiver-classpath").read_text().strip()])
        server_log = private_log(private / "receiver.log")
        jvm = ["-Xmx384m", "-XX:MaxMetaspaceSize=128m", "-XX:ReservedCodeCacheSize=32m",
               "-XX:MaxDirectMemorySize=16m", "-XX:ActiveProcessorCount=2"]
        process = subprocess.Popen([str(args.java_home / "bin/java"), *jvm, "-cp", classpath,
            "de.caritas.cob.consultingtypeservice.api.controller.BootstrapV2ReceiverFixture"],
            cwd=receiver, env=env, stdin=subprocess.PIPE, stdout=server_log,
            stderr=subprocess.STDOUT, start_new_session=True)
        receipt["stage"] = "cts-startup"
        deadline = min(time.monotonic() + 120, overall)
        ready = Path(env["ORISO_BOOTSTRAP_RECEIVER_READY"])
        while not ready.is_file():
            free_guard()
            if process.poll() is not None or time.monotonic() >= deadline:
                raise FixtureError("CTS startup failed before behavioral HTTP execution")
            time.sleep(1)
        env["ORISO_CTS_BOOTSTRAP_RECEIVER_URL"] = ready.read_text().strip()
        env["MAVEN_OPTS"] = "-Xmx256m -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=16m -XX:ActiveProcessorCount=2"
        started = time.time()
        receipt["stage"] = "real-public-http-gate"
        result = bounded(["./mvnw", "-q", "-Pnative-permission-matrix",
            "-Dit.test=BootstrapV2CallerReceiverIT", "-DargLine=" + " ".join(jvm).replace("MaxMetaspaceSize=128m", "MaxMetaspaceSize=256m"),
            "failsafe:integration-test", "failsafe:verify"], tenant, env,
            private / "caller.log", 240, overall)
        report = tenant / "target/failsafe-reports/TEST-com.vi.tenantservice.api.controller.BootstrapV2CallerReceiverIT.xml"
        if not report.is_file() or report.stat().st_mtime < started:
            raise FixtureError("Fresh complete public HTTP execution XML missing")
        tree = XML.parse(report).getroot()
        counts = {k: int(tree.get(k, "0")) for k in ("tests", "failures", "errors", "skipped")}
        cases = tree.findall("testcase")
        if (counts["tests"] != args.expected_tests or counts["skipped"]
                or len(cases) != counts["tests"] or any(
                    sum(bool(case.findall(tag)) for case in cases) != counts[key]
                    for key, tag in (("failures", "failure"), ("errors", "error"), ("skipped", "skipped")))):
            raise FixtureError("Native fixture skipped or missed required cases")
        raw = report.read_bytes()
        if re.search(rb"eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+(?:\.[A-Za-z0-9_-]+)?", raw):
            raise FixtureError("Unsafe token material in native XML; original retained privately only")
        shutil.copy2(report, args.evidence / report.name)
        observed = Path(env["ORISO_BOOTSTRAP_OBSERVED"])
        receipt["counts"] = counts
        receipt["cases"] = [{"name": case.get("name"), "outcome": next((tag for tag in
            ("failure", "error", "skipped") if case.find(tag) is not None), "pass")} for case in cases]
        receipt["observed"] = safe_observation(json.loads(observed.read_text())) if observed.is_file() else {}
        process.stdin.close()
        process.stdin = None
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            raise FixtureError("Owned CTS did not finish its actual persistence snapshot") from None
        if process.returncode:
            raise FixtureError("Owned CTS exited before verified persistence snapshot")
        receipt["receiverBefore"] = json.loads(Path(env["ORISO_BOOTSTRAP_RECEIVER_BEFORE"]).read_text())
        receipt["receiverAfter"] = json.loads(Path(env["ORISO_BOOTSTRAP_RECEIVER_AFTER"]).read_text())
        after = {"tenant": source_manifest(tenant), "receiver": source_manifest(receiver),
                 "helmFixture": {str(p.relative_to(helm)): hashlib.sha256(p.read_bytes()).hexdigest()
                                 for p in (helm / "tests/scenario_c_receiver_fixture.py",
                                           helm / "tests/_keycloak_container.py")}}
        if after != before:
            raise FixtureError("Source changed during the native proof")
        receipt["sourceUnchanged"] = True
        free_guard()
        if args.expect == "red":
            if not intended_counterfactual(tree, counts, result, receipt, tenant):
                raise FixtureError("Expected real CTS HTTP403 behavioral RED was not established")
            receipt["state"] = "BEHAVIORAL_RED"
        elif result or any(counts[k] for k in ("failures", "errors", "skipped")):
            receipt["state"] = "EXECUTED_FAILURE"
            receipt["safeApplicationDiagnostics"] = [safe_java_frames(private / "caller.log")]
            return 1
        else:
            receipt["state"] = "PASS"
    except Exception as failure:
        receipt["state"] = "FIXTURE_FAILURE"
        receipt["failureType"] = type(failure).__name__
        if isinstance(failure, (FixtureError, resources.ResourceRefusal)):
            receipt["safeFailure"] = str(failure)
        if temporary:
            safe = []
            receipt["safeApplicationDiagnostics"] = []
            for log in Path(temporary.name).glob("*.log"):
                diagnostic = safe_java_frames(log)
                if diagnostic["exceptionTypes"] or diagnostic["applicationFrames"]:
                    receipt["safeApplicationDiagnostics"].append(diagnostic)
                for line in log.read_text(errors="replace").splitlines():
                    if ("[ERROR]" in line or "Caused by:" in line or "APPLICATION FAILED" in line) and "eyJ" not in line:
                        for actor in locals().get("actors", []):
                            line = line.replace(actor["secret"], "[redacted]")
                        safe.append({"log": log.name, "diagnostic": line[:500]})
            receipt["safeDiagnostics"] = safe[-18:]
        return 2
    finally:
        cleanup_errors = []
        try:
            if process is not None:
                if process.stdin:
                    process.stdin.close()
                receipt["processCleanup"] = stop(process)
            if server_log:
                server_log.close()
        except Exception as failure:
            cleanup_errors.append({"ownedProcess": type(failure).__name__})
        receipt["ownedMemoryBeforeCleanup"] = {
            name: owned_memory_snapshot(owned, name)
            for kind, name in owned.planned if kind == "container"
        }
        try:
            receipt["cleanup"] = owned.cleanup()
        except resources.CleanupRefusal as failure:
            cleanup_errors.extend(failure.errors)
            receipt["cleanup"] = failure.completed
        if temporary:
            try:
                temporary.cleanup()
            except Exception as failure:
                cleanup_errors.append({"privateDirectory": type(failure).__name__})
        if cleanup_errors:
            receipt["state"] = "CLEANUP_FAILURE_NO_ACCEPTANCE"
            receipt["cleanupErrors"] = cleanup_errors
        (args.evidence / "receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
        print(json.dumps({"state": receipt["state"], "counts": receipt.get("counts"),
                          "observed": receipt.get("observed"), "safeFailure": receipt.get("safeFailure")}))
        if cleanup_errors:
            raise FixtureError("Exact owned cleanup was not confirmed; native proof is refused")
    return 0


if __name__ == "__main__":
    sys.exit(main())
