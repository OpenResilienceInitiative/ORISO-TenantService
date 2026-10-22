#!/usr/bin/env python3
"""Current-source compilation and affected unit checks before any native fixture."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import time
import xml.etree.ElementTree as ET

HOST_ROOT = Path(__file__).resolve().parents[2]

BASES = {
    "tenant": "c0da86b50ee6537facf4af9b90eb799f5e975063",
    "cts": "53c1ab3752245e7df371a4476739de4cfeda2645",
    "user": "408d8da9ab7ce54e87af7f57e9499dbb2d3ed424",
}
FLOORS = {
    "tenant": {
        "com.vi.tenantservice.api.controller.AccountPolicyPurposeAuthorizationIT": 3,
        "com.vi.tenantservice.config.security.CreationContextKeyTest": 2,
        "com.vi.tenantservice.config.security.TenantBootstrapAuthorityTest": 2,
        "com.vi.tenantservice.config.security.WizardPolicyV2ContextTest": 35,
        "com.vi.tenantservice.api.service.consultingtype.ConsultingTypeBootstrapRoutingTest": 4,
        "com.vi.tenantservice.api.facade.TenantServiceFacadeTest": 66,
        "com.vi.tenantservice.api.service.TenantBootstrapCreationIT": 6,
    },
    "cts": {
        "de.caritas.cob.consultingtypeservice.config.security.CreationContextKeyTest": 2,
        "de.caritas.cob.consultingtypeservice.config.security.TenantBootstrapV2ContextTest": 14,
    },
    "user": {
        "de.caritas.cob.userservice.api.service.accountpolicy.CheckedAccountPolicyReadsTest": 7,
        "de.caritas.cob.userservice.api.service.ConsultantImportServiceTest": 12,
        "de.caritas.cob.userservice.api.service.accountinvite.CounsellorInviteProvisioningServiceTest": 26,
        "de.caritas.cob.userservice.api.admin.service.consultant.create.CreateConsultantSagaTest": 41,
    },
}
COMPILED = {
    "tenant": [
        "target/test-classes/com/vi/tenantservice/api/controller/BootstrapV2CallerReceiverIT.class",
        "target/test-classes/com/vi/tenantservice/api/service/TenantBootstrapCreationIT.class",
        "target/classes/com/vi/tenantservice/api/service/TenantBootstrapCreation.class",
        "target/test-classes/com/vi/tenantservice/config/security/WizardPolicyV2ContextTest.class",
    ],
    "cts": [
        "target/test-classes/de/caritas/cob/consultingtypeservice/api/controller/BootstrapV2ReceiverFixture.class",
        "target/test-classes/de/caritas/cob/consultingtypeservice/config/security/TenantBootstrapV2ContextTest.class",
        "target/classes/de/caritas/cob/consultingtypeservice/config/security/TenantCreationContext.class",
        "target/test-classes/de/caritas/cob/consultingtypeservice/api/controller/ScenarioCMailNativeServer.class",
    ],
    "user": [
        "target/classes/de/caritas/cob/userservice/api/service/accountpolicy/CheckedAccountPolicyReads.class",
        "target/test-classes/de/caritas/cob/userservice/api/service/accountpolicy/CheckedAccountPolicyReadsTest.class",
    ],
}


def git(root, *args):
    return subprocess.check_output(["git", *args], cwd=root).decode().strip()


def snapshot(root):
    names = subprocess.check_output(["git", "ls-files", "-z"], cwd=root).decode().split("\0")
    return {
        name: hashlib.sha256(
            b"symlink\0" + os.readlink(root / name).encode()
            if (root / name).is_symlink() else (root / name).read_bytes()
        ).hexdigest()
        for name in sorted(filter(None, names))
    }


def host_snapshot():
    return {
        "commit": git(HOST_ROOT, "rev-parse", "HEAD"),
        "inputs": {
            name: hashlib.sha256((HOST_ROOT / name).read_bytes()).hexdigest()
            for name in (
                "scripts/ci/scenario-c-context-preflight.py",
                ".github/workflows/scenario-c-context-preflight.yml",
            )
        },
    }


def execute(argv, root, destination, seconds):
    with destination.open("w") as output:
        child = subprocess.Popen(argv, cwd=root, stdout=output, stderr=subprocess.STDOUT,
                                 start_new_session=True)
        try:
            return child.wait(timeout=seconds)
        except subprocess.TimeoutExpired:
            os.killpg(child.pid, signal.SIGTERM)
            try:
                child.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(child.pid, signal.SIGKILL)
                child.wait()
            return 124


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("role", choices=BASES)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--expected", required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    args = parser.parse_args()
    root, evidence = args.source.resolve(), args.evidence.resolve()
    evidence.mkdir(parents=True, exist_ok=False)
    if not re.fullmatch(r"[0-9a-f]{40}", args.expected):
        raise SystemExit("An immutable source commit is required")
    if git(root, "rev-parse", "HEAD") != args.expected or git(root, "status", "--porcelain"):
        raise SystemExit("Expected exact clean source checkout")
    subprocess.run(["git", "merge-base", "--is-ancestor", BASES[args.role], "HEAD"],
                   cwd=root, check=True)
    before = snapshot(root)
    host_before = host_snapshot()
    if host_before["commit"] != os.environ.get("GITHUB_SHA"):
        raise SystemExit("The host workflow revision must match the actual checkout")
    report = {
        "role": args.role, "commit": args.expected, "devBase": BASES[args.role],
        "expectedSuites": FLOORS[args.role], "sourceBefore": before,
        "nativeResourcesStarted": False, "accepted": False,
        "scope": "compilation and affected units only; no native HTTP or full Scenario C claim",
        "hostBefore": host_before,
    }
    try:
        changed = git(root, "diff", "--name-only", BASES[args.role], "HEAD", "--", "*.java").splitlines()
        scoped = [str(root / name) for name in changed if (root / name).is_file()]
        if not scoped:
            raise RuntimeError("Missing changed Java source inventory")
        common = ["./mvnw", "-B", "-ntp", "-q", "-Dmaven.wagon.http.retryHandler.count=3",
                  "-DspotlessFiles=" + "|".join(re.escape(name) for name in scoped)]
        if any((root / name).exists() for name in COMPILED[args.role]):
            raise RuntimeError("A fresh job must not start with compiled fixture classes")
        for attempt in range(1, 4):
            tool_status = execute(["./mvnw", "--version"], root,
                                  evidence / ("toolchain-" + str(attempt) + ".log"), 120)
            if tool_status == 0:
                break
        report["toolchainExit"] = tool_status
        if tool_status:
            return 1
        version = (evidence / ("toolchain-" + str(attempt) + ".log")).read_text()
        if "Java version: 21.0.12" not in version:
            raise RuntimeError("Unexpected Java runtime")
        report["compileExit"] = execute([*common, "test-compile"], root,
                                        evidence / "compile.log", 600)
        if report["compileExit"]:
            report["classification"] = "COMPILE_OR_FORMAT_SETUP_FAILURE_NO_PRODUCT_RED"
            return 1
        compiled = {}
        for name in COMPILED[args.role]:
            data = (root / name).read_bytes()
            if not data.startswith(b"\xca\xfe\xba\xbe"):
                raise RuntimeError("Missing actual compiled class")
            compiled[name] = hashlib.sha256(data).hexdigest()
        report["compiledClasses"] = compiled
        if not FLOORS[args.role]:
            report["classification"] = "COMPILE_ONLY_PASS"
            report["accepted"] = True
            return 0
        old_reports = root / "target/surefire-reports"
        if old_reports.exists():
            shutil.move(str(old_reports), str(evidence / "preexisting-reports"))
        started = time.time()
        report["unitExit"] = execute(
            [*common, "-Dtest=" + ",".join(FLOORS[args.role]), "-DfailIfNoTests=true", "test"],
            root, evidence / "unit.log", 600,
        )
        suites, failed = {}, []
        for path in sorted(old_reports.glob("TEST-*.xml")):
            if path.stat().st_mtime < started:
                raise RuntimeError("Stale unit report cannot count as proof")
            suite = ET.parse(path).getroot()
            name = suite.get("name")
            if name in suites:
                raise RuntimeError("Duplicate unit suite")
            counts = {k: int(suite.get(k, "0")) for k in ("tests", "failures", "errors", "skipped")}
            cases = suite.findall("testcase")
            if len(cases) != counts["tests"]:
                raise RuntimeError("Incomplete unit testcase inventory")
            for key, tag in (("failures", "failure"), ("errors", "error"), ("skipped", "skipped")):
                if sum(bool(case.findall(tag)) for case in cases) != counts[key]:
                    raise RuntimeError("Inconsistent unit outcome inventory")
            suites[name] = counts
            failed.extend(name + "#" + case.get("name", "") for case in cases
                          if case.find("failure") is not None or case.find("error") is not None)
        if old_reports.exists():
            shutil.copytree(old_reports, evidence / "surefire-reports")
        report["suites"], report["failedCases"] = suites, failed
        inventory = set(suites) == set(FLOORS[args.role]) and all(
            suites.get(name, {}).get("tests", 0) >= floor
            for name, floor in FLOORS[args.role].items()
        )
        totals = {k: sum(s[k] for s in suites.values()) for k in ("tests", "failures", "errors", "skipped")}
        report["totals"] = totals
        report["accepted"] = inventory and report["unitExit"] == 0 and not any(
            totals[k] for k in ("failures", "errors", "skipped")
        )
        report["classification"] = "PASS" if report["accepted"] else (
            "TEST_ASSERTION_FAILURE" if inventory and totals["failures"]
            and not totals["errors"] and not totals["skipped"] else "INCOMPLETE_OR_SETUP_FAILURE"
        )
        return not report["accepted"]
    except Exception as failure:
        report["classification"] = "PREFLIGHT_HARNESS_FAILURE_NO_PRODUCT_RED"
        report["exceptionType"] = type(failure).__name__
        raise
    finally:
        report["sourceAfter"] = snapshot(root)
        report["hostAfter"] = host_snapshot()
        report["sourceStable"] = (
            report["sourceAfter"] == before and report["hostAfter"] == host_before
        )
        if not report["sourceStable"]:
            report["accepted"] = False
            report["classification"] = "SOURCE_CHANGED_NOT_ACCEPTED"
        (evidence / "receipt.json").write_text(json.dumps(report, indent=2) + "\n")
        print(json.dumps({k: v for k, v in report.items() if k not in ("sourceBefore", "sourceAfter")}, sort_keys=True))
        if not report["sourceStable"]:
            raise SystemExit(1)


if __name__ == "__main__":
    raise SystemExit(main())
