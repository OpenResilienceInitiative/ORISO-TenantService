#!/usr/bin/env python3
"""Explicit local native gate: real Tenant caller -> real CTS receiver, isolated resources.

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


def source_manifest(root):
    names = subprocess.check_output(["git", "ls-files", "--cached", "--others", "--exclude-standard"],
                                    cwd=root, text=True).splitlines()
    return {name: hashlib.sha256((root / name).read_bytes()).hexdigest()
            for name in sorted(set(names)) if (root / name).is_file()
            and "__pycache__" not in Path(name).parts
            and (name.startswith(("src/", "scripts/", ".mvn/")) or name in ("pom.xml", "mvnw"))}


def free_guard():
    result = subprocess.run(["memory_pressure", "-Q"], capture_output=True, text=True, timeout=10)
    match = re.search(r"System-wide memory free percentage:\s*(\d+)%", result.stdout)
    if result.returncode or not match or int(match[1]) < 25:
        raise FixtureError("Native memory headroom is below the required25% or unavailable")
    return int(match[1])


def stop(process):
    if process is not None and process.poll() is None:
        os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait(timeout=10)


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
            stop(process)


def docker(*command):
    return subprocess.check_output(["docker", *command], text=True, timeout=30).strip()


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
    network_created = False
    overall = time.monotonic() + 1200
    receipt = {"runId": run_id, "state": "RUNNING", "stage": "network", "image": shared.IMAGE,
               "sourceUnchanged": False, "cleanup": [], "initialMemoryFreePercent": free_guard()}
    if receipt["initialMemoryFreePercent"] < 40:
        receipt["state"] = "PRESTART_REFUSAL_NO_NATIVE_EXECUTION"
        receipt["safeFailure"] = "Native start requires fresh40% free memory"
        (args.evidence / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
        print(json.dumps({"state": receipt["state"], "freshMemoryFreePercent": receipt["initialMemoryFreePercent"]}))
        return 2
    try:
        docker("network", "create", "--label", label, network)
        network_created = True
        temporary = tempfile.TemporaryDirectory(prefix="oriso-context-")
        private = Path(temporary.name).resolve()
        os.chmod(private, 0o700)
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
        env = {**os.environ, "JAVA_HOME": str(args.java_home.resolve()),
               "PATH": str(args.java_home.resolve() / "bin") + os.pathsep + os.environ["PATH"],
               "MAVEN_OPTS": "-Xmx512m -XX:MaxMetaspaceSize=192m -XX:ActiveProcessorCount=2",
               # Scoped to Maven children: avoid the measured JGit system-config discovery wait.
               # Spotless and all normal source checks still execute; no Git config is changed.
               "GIT_CONFIG_NOSYSTEM": "1",
               "ORISO_TASK_TOKEN_FIXTURE": str(fixture),
               "ORISO_BOOTSTRAP_SHARED_KEY": base64.b64encode(bytes([7]) * 32).decode(),
               "ORISO_BOOTSTRAP_RECEIVER_READY": str(private / "receiver-ready"),
               "ORISO_BOOTSTRAP_OBSERVED": str(private / "observed.json"),
               "ORISO_BOOTSTRAP_MONGO_URI": "mongodb://" + docker("port", containers[1], "27017/tcp").splitlines()[0] + "/context_" + run_id}
        for key, root in (("receiver", receiver), ("tenant", tenant)):
            receipt["stage"] = key + "-compile"
            cp = private / (key + "-classpath")
            status = bounded(["./mvnw", "-q", "-Pnative-permission-matrix", "test-compile",
                              "dependency:build-classpath", "-Dmdep.includeScope=test", "-Dmdep.outputFile=" + str(cp)],
                             root, env, private / (key + "-compile.log"), 420, overall)
            if status:
                raise FixtureError(key + " compile failed before behavioral HTTP execution")
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
        if counts["tests"] != args.expected_tests or counts["skipped"]:
            raise FixtureError("Native fixture skipped or missed required cases")
        raw = report.read_bytes()
        if re.search(rb"eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+", raw):
            raise FixtureError("Unsafe token material in native XML; original retained privately only")
        shutil.copy2(report, args.evidence / report.name)
        observed = Path(env["ORISO_BOOTSTRAP_OBSERVED"])
        receipt["observed"] = json.loads(observed.read_text()) if observed.is_file() else {}
        receipt["counts"] = counts
        after = {"tenant": source_manifest(tenant), "receiver": source_manifest(receiver),
                 "helmFixture": before["helmFixture"]}
        if after != before:
            raise FixtureError("Source changed during the native proof")
        receipt["sourceUnchanged"] = True
        if args.expect == "red":
            if (not result or counts["errors"] or not counts["failures"]
                    or receipt["observed"].get("receiverStatus") != 403
                    or not receipt["observed"].get("receiverRequestObserved")):
                raise FixtureError("Expected real CTS HTTP403 behavioral RED was not established")
            receipt["state"] = "BEHAVIORAL_RED"
        elif result or any(counts[k] for k in ("failures", "errors", "skipped")):
            receipt["state"] = "EXECUTED_FAILURE"
            return 1
        else:
            receipt["state"] = "PASS"
    except Exception as failure:
        receipt["state"] = "FIXTURE_FAILURE"
        receipt["failureType"] = type(failure).__name__
        if isinstance(failure, FixtureError):
            receipt["safeFailure"] = str(failure)
        if temporary:
            safe = []
            for log in Path(temporary.name).glob("*.log"):
                for line in log.read_text(errors="replace").splitlines():
                    if ("[ERROR]" in line or "Caused by:" in line or "APPLICATION FAILED" in line) and "eyJ" not in line:
                        for actor in locals().get("actors", []):
                            line = line.replace(actor["secret"], "[redacted]")
                        safe.append({"log": log.name, "diagnostic": line[:500]})
            receipt["safeDiagnostics"] = safe[-18:]
        return 2
    finally:
        if process is not None:
            if process.stdin:
                process.stdin.close()
            stop(process)
        if server_log:
            server_log.close()
        for identifier in reversed(containers):
            metadata = json.loads(docker("inspect", identifier))[0]
            if metadata["Config"]["Labels"].get("oriso-context-run-id") != run_id:
                raise FixtureError("Owned container label changed; refusing cleanup")
            volumes = [mount["Name"] for mount in metadata.get("Mounts", []) if mount["Type"] == "volume"]
            docker("rm", "-f", "-v", identifier)
            receipt["cleanup"].append({"containerId": identifier, "anonymousVolumes": volumes, "removed": True})
        if network_created:
            metadata = json.loads(docker("network", "inspect", network))[0]
            if metadata["Labels"].get("oriso-context-run-id") != run_id:
                raise FixtureError("Owned network label changed; refusing cleanup")
            docker("network", "rm", network)
            receipt["cleanup"].append({"network": network, "removed": True})
        if temporary:
            temporary.cleanup()
        (args.evidence / "receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
        print(json.dumps({"state": receipt["state"], "counts": receipt.get("counts"),
                          "observed": receipt.get("observed"), "safeFailure": receipt.get("safeFailure")}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
