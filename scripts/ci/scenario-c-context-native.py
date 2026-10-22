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


def verify_v1(roots, baseline="original"):
    original = json.loads((HOST / "scripts/ci/scenario-c-bootstrap-v1-inputs.json").read_text())
    if set(original) != {"tenant", "cts"} or any(len(inputs) != 3 for inputs in original.values()):
        return False
    contract = original
    if baseline in ("routed", "compensated-routed", "compensated-routed-policy"):
        contract = json.loads(
            (HOST / "scripts/ci/scenario-c-bootstrap-routed-v1-inputs.json").read_text())
        if set(contract) != set(original) or any(
                set(contract[role]) != set(original[role]) for role in original):
            return False
        changed = {(role, name) for role in original for name in original[role]
                   if contract[role][name] != original[role][name]}
        if changed != {("tenant", "src/main/java/com/vi/tenantservice/api/service/"
                        "consultingtype/ConsultingTypeService.java")}:
            return False
        if baseline in ("compensated-routed", "compensated-routed-policy"):
            compensated = json.loads(
                (HOST / "scripts/ci/scenario-c-bootstrap-compensated-routed-v1-inputs.json").read_text())
            if set(compensated) != set(contract) or any(
                    set(compensated[role]) != set(contract[role]) for role in contract):
                return False
            changed = {(role, name) for role in contract for name in contract[role]
                       if compensated[role][name] != contract[role][name]}
            if changed != {("tenant", "src/main/java/com/vi/tenantservice/api/facade/"
                            "TenantServiceFacade.java")}:
                return False
            contract = compensated
            if baseline == "compensated-routed-policy":
                policy = json.loads(
                    (HOST / "scripts/ci/scenario-c-bootstrap-compensated-routed-policy-v1-inputs.json").read_text())
                if set(policy) != set(contract) or any(
                        set(policy[role]) != set(contract[role]) for role in contract):
                    return False
                changed = {(role, name) for role in contract for name in contract[role]
                           if policy[role][name] != contract[role][name]}
                if changed != {("tenant", "src/main/java/com/vi/tenantservice/api/facade/"
                                "TenantServiceFacade.java")}:
                    return False
                contract = policy
    elif baseline != "original":
        return False
    return all(hashlib.sha256((roots[role] / name).read_bytes()).hexdigest() == digest
               for role, inputs in contract.items() for name, digest in inputs.items())


def known_container_runtime(cleanup):
    containers = [row for row in cleanup if row.get("kind") == "container"]
    return len(containers) == 2 and all(
        isinstance(row.get("runtime"), dict) and row["runtime"].get("oomKilled") is False
        and type(row["runtime"].get("restartCount")) is int
        and row["runtime"]["restartCount"] == 0 for row in containers)


def known_process_cleanup(groups, server):
    owned = [*groups, server]
    return (len(groups) == 3 and all(
        item.get("confirmedAbsent") is True
        and type(item.get("processGroup")) is int and item["processGroup"] > 0
        for item in owned)
        and len({item["processGroup"] for item in owned}) == 4)


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


NATIVE_CASES = (
    "ownedTenantCreationUsesTheSharedV2ProofAtTheMaterializedDtoSendSeam",
    "aLaterTenantFailureRetiresOnlyItsIssuedAttemptAndReusesTheExactReservation",
)


def exact_native_inventory(counts, cases, number):
    if type(number) is not int or number not in (1, 2):
        return False
    if not isinstance(counts, dict) or not isinstance(cases, list):
        return False
    if set(counts) != {"tests", "failures", "errors", "skipped"} or any(
            type(value) is not int or value < 0 for value in counts.values()):
        return False
    if counts["tests"] != number or counts["errors"] or counts["skipped"] or len(cases) != number:
        return False
    if any(not isinstance(case, dict) or set(case) != {"name", "outcome"}
           or not isinstance(case["name"], str) for case in cases):
        return False
    names = [case["name"].removesuffix("()") for case in cases]
    outcomes = [case.get("outcome") for case in cases]
    return (len(set(names)) == number and set(names) == set(NATIVE_CASES[:number])
            and all(outcome in ("pass", "failure") for outcome in outcomes)
            and outcomes.count("failure") == counts["failures"])


def paired_success(observed):
    """Count only a complete native storage fence, never an authorization-only refusal."""
    def matches(stage, numbers, flags):
        return (isinstance(stage, dict) and stage.get("state") == "ACK"
                and all(type(stage.get(key)) is int and stage[key] == value
                        for key, value in numbers.items())
                and all(stage.get(key) is True for key in flags))
    if not isinstance(observed, dict):
        return False
    pair = observed.get("paired")
    status = {"tenantFailureStatus": 400, "createStatus": 200, "compensationStatus": 204,
              "delayedReplyStatus": 409, "newTenantStatus": 200, "oldReplayStatus": 409}
    stages = {"beforeDelayedRelease", "terminalRace", "staleWrites", "newAttempt"}
    flags = {"allocationRestored", "allocationRestoredThenReassigned"}
    if (not isinstance(pair, dict) or set(pair) != set(status) | stages | flags
            or any(type(pair[key]) is not int or pair[key] != value for key, value in status.items())
            or any(pair[key] is not True for key in flags)):
        return False
    reached = {"primaryInsertReached", "delayedInsertReached", "primaryInsertCompleted",
               "staleCaptured", "counterNeverRewound"}
    if not matches(pair["beforeDelayedRelease"],
                   {"actualInsertCalls": 1, "liveTypes": 0, "terminalTypes": 1},
                   reached | {"counterUnchangedSinceCapture"}):
        return False
    retired = reached | {"delayedInsertDuplicate", "counterUnchangedSinceCapture"}
    if not matches(pair["terminalRace"],
                   {"actualInsertCalls": 2, "liveTypes": 0, "terminalTypes": 1}, retired):
        return False
    writes = {"conditionalWriteAttempts": 2, "conditionalWritesBlocked": 2,
              "unconditionalSaveAttempts": 0, "staleUpdateRefusals": 2, "retiredReadChecks": 20}
    retired |= {"awareConditionalWriteAttempted", "awareConditionalWriteBlocked",
                "unawareConditionalWriteAttempted", "unawareConditionalWriteBlocked", "retiredReadsHidden"}
    return (matches(pair["staleWrites"],
                    dict(writes, actualInsertCalls=2, liveTypes=0, terminalTypes=1), retired)
            and matches(pair["newAttempt"],
                        dict(writes, actualInsertCalls=3, liveTypes=1, terminalTypes=1),
                        retired - {"counterUnchangedSinceCapture"} | {"ordinaryPreserved",
                                   "newAttemptDistinct", "newResultDistinct",
                                   "counterAdvancedExactlyOnce"}))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--receiver", type=Path, required=True)
    parser.add_argument("--receiver-ref", required=True)
    parser.add_argument("--helm", type=Path, required=True)
    parser.add_argument("--helm-ref", required=True)
    parser.add_argument("--expect", choices=("red", "green"), required=True)
    parser.add_argument("--v1-baseline", choices=("original", "routed", "compensated-routed", "compensated-routed-policy"),
                        default="original")
    parser.add_argument("--expected-tests", type=int, choices=(1, 2), default=1)
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
    if args.expect == "red" and not verify_v1(roots, args.v1_baseline):
        raise SystemExit("All six exact selected v1 baseline files are required for RED")
    evidence = args.evidence.resolve()
    evidence.mkdir(parents=True, exist_ok=False)
    before = {role: support.snapshot(root) for role, root in roots.items()}
    report = {"state": "STARTED_NOT_ACCEPTED", "refs": refs, "expect": args.expect,
              "v1Baseline": args.v1_baseline, "expectedTests": args.expected_tests,
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
                 "--java-home", str(java_home), "--expect", args.expect, "--expected-tests", str(args.expected_tests),
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
        expected = exact_native_inventory(counts, native.get("cases", []), args.expected_tests)
        if args.expect == "red":
            expected = (expected and counts.get("failures") == 1
                        and native.get("observed", {}).get("receiverStatus") == 403
                        and native.get("observed", {}).get("allocationStatus") == "RESERVED"
                        and native.get("receiverBefore") == native.get("receiverAfter")
                        and set(native.get("receiverBefore", {})) == {"claims", "counter", "types"})
        else:
            expected = expected and counts.get("failures") == 0
            if args.expected_tests == 2:
                expected = expected and paired_success(native.get("observed"))
        cleanup = native.get("cleanup", [])
        run_id = native.get("runId", "")
        expected_resources = {("network", "oriso-context-" + run_id[:12]),
                              ("container", "oriso-context-kc-" + run_id[:12]),
                              ("container", "oriso-context-mongo-" + run_id[:12])}
        process_path = evidence / "native/process-cleanup.jsonl"
        groups = [json.loads(line) for line in process_path.read_text().splitlines()] if process_path.is_file() else []
        groups_complete = known_process_cleanup(groups, native.get("processCleanup", {}))
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
