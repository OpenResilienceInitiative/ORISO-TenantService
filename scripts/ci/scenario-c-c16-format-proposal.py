#!/usr/bin/env python3
"""Failure-only mechanical formatting, in an exact owned disposable Git worktree."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import time
import uuid

HOST = Path(__file__).resolve().parents[2]
FILES = {
    "tenant": [
        "src/main/java/com/vi/tenantservice/api/facade/TenantServiceFacade.java",
        "src/main/java/com/vi/tenantservice/api/service/TenantService.java",
        "src/main/java/com/vi/tenantservice/api/service/TenantBootstrapCreation.java",
        "src/main/java/com/vi/tenantservice/api/service/consultingtype/ConsultingTypeService.java",
        "src/main/java/com/vi/tenantservice/config/security/TenantCreationContext.java",
        "src/test/java/com/vi/tenantservice/api/service/TenantBootstrapCreationIT.java",
        "src/test/java/com/vi/tenantservice/api/controller/BootstrapV2CallerReceiverIT.java",
        "src/test/java/com/vi/tenantservice/config/security/TaskServiceIdentityTest.java",
        "src/test/java/com/vi/tenantservice/config/security/CreationContextKeyTest.java",
    ],
    "cts": [
        "src/main/java/de/caritas/cob/consultingtypeservice/config/security/TenantCreationContext.java",
        "src/main/java/de/caritas/cob/consultingtypeservice/api/service/TenantBootstrapService.java",
        "src/main/java/de/caritas/cob/consultingtypeservice/api/model/TenantBootstrapClaim.java",
        "src/main/java/de/caritas/cob/consultingtypeservice/api/consultingtypes/ConsultingTypeValidator.java",
        "src/main/java/de/caritas/cob/consultingtypeservice/api/controller/ConsultingTypeController.java",
        "src/main/java/de/caritas/cob/consultingtypeservice/api/model/ConsultingTypeEntity.java",
        "src/main/java/de/caritas/cob/consultingtypeservice/api/consultingtypes/ConsultingTypeGroupRepositoryImpl.java",
        "src/main/java/de/caritas/cob/consultingtypeservice/api/consultingtypes/ConsultingTypeMongoRepositoryService.java",
        "src/main/java/de/caritas/cob/consultingtypeservice/api/consultingtypes/ConsultingTypeMongoTenantAwareRepositoryService.java",
        "src/main/java/de/caritas/cob/consultingtypeservice/api/consultingtypes/ConsultingTypeRepository.java",
        "src/main/java/de/caritas/cob/consultingtypeservice/api/consultingtypes/ConsultingTypeTenantAwareRepository.java",
        "src/test/java/de/caritas/cob/consultingtypeservice/config/security/TenantBootstrapV2ContextTest.java",
        "src/test/java/de/caritas/cob/consultingtypeservice/api/controller/BootstrapV2ReceiverFixture.java",
        "src/test/java/de/caritas/cob/consultingtypeservice/config/security/TaskServiceIdentityTest.java",
        "src/test/java/de/caritas/cob/consultingtypeservice/config/security/CreationContextKeyTest.java",
    ],
}


def owned_delta(role, changed, untracked):
    return set(changed).issubset(FILES[role]) and not untracked


def eligible(receipt, log):
    return (receipt.get("sourceStable") is True
            and receipt.get("nativeResourcesStarted") is False
            and receipt.get("accepted") is False
            and type(receipt.get("compileExit")) is int
            and receipt["compileExit"] != 0
            and receipt.get("classification") == "COMPILE_OR_FORMAT_SETUP_FAILURE_NO_PRODUCT_RED"
            and "The following files had format violations:" in log
            and "Run 'mvn spotless:apply'" in log)


def finish_receipt(receipt, now, overall):
    receipt["totalDeadlineRetained"] = now <= overall
    if not receipt["totalDeadlineRetained"]:
        receipt["state"] = "DEADLINE_FAILURE_NO_ACCEPTANCE"
    elif not receipt["originalSourcesStable"] or not receipt["scratchConfirmedAbsent"]:
        receipt["state"] = "SOURCE_OR_CLEANUP_FAILURE_NO_ACCEPTANCE"
    return receipt["state"] == "MECHANICAL_PROPOSAL_ONLY"


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("role", choices=FILES)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--source-evidence", type=Path, required=True)
    args = parser.parse_args()
    if os.environ.get("CI") != "true" or os.environ.get("RUNNER_OS") != "Linux":
        raise SystemExit("Fresh hosted Linux formatter proposal only")
    if not (args.source_evidence / "receipt.json").is_file():
        return 0
    prior = json.loads((args.source_evidence / "receipt.json").read_text())
    if not eligible(prior, (args.source_evidence / "compile.log").read_text()):
        return 0
    source = module("c16_format_source", HOST / "scripts/ci/scenario-c-context-preflight.py")
    support = module("c16_format_support", HOST / "scripts/test-bootstrap-context-roundtrip.py")
    root = args.source.resolve()
    ref = prior["commit"]
    if (prior["role"] != args.role or source.git(root, "rev-parse", "HEAD") != ref
            or source.git(root, "status", "--porcelain")
            or source.git(HOST, "rev-parse", "HEAD") != os.environ["GITHUB_SHA"]
            or source.git(HOST, "status", "--porcelain")):
        raise RuntimeError("Exact immutable clean originals required")
    before, host_before = source.snapshot(root), source.snapshot(HOST)
    if before != prior["sourceBefore"] or before != prior["sourceAfter"]:
        raise RuntimeError("Completed failed gate source changed")
    evidence = args.source_evidence.resolve() / "format-proposal"
    evidence.mkdir(exist_ok=False)
    lease = support.configure_memory(2896, evidence)
    scratch = Path(os.environ["RUNNER_TEMP"]) / ("c16-format-" + args.role + "-" + uuid.uuid4().hex)
    paths = FILES[args.role]
    receipt = {"state": "STARTED_NO_FEATURE_CREDIT", "ref": ref, "role": args.role,
               "budgetMiB": 2896, "sourceBefore": before, "hostBefore": host_before,
               "allowedPaths": paths, "scratchConfirmedAbsent": False}
    overall = time.monotonic() + 600
    try:
        lease.check(starting=True)
        source.git(root, "worktree", "add", "--detach", str(scratch), ref)
        if source.git(scratch, "rev-parse", "HEAD") != ref:
            raise RuntimeError("Scratch source differs from immutable ref")
        with tempfile.TemporaryDirectory(prefix="private-c16-format-") as private:
            os.chmod(private, 0o700)
            env = {**os.environ,
                   "MAVEN_OPTS": "-Xmx512m -XX:MaxMetaspaceSize=192m -XX:ActiveProcessorCount=2",
                   "JAVA_TOOL_OPTIONS": "-Xmx512m -XX:MaxMetaspaceSize=192m -XX:ActiveProcessorCount=2"}
            result = support.bounded(["./mvnw", "-B", "-ntp", "-q",
                "-DspotlessFiles=" + "|".join(re.escape(str(scratch / path)) for path in paths),
                "spotless:apply"], scratch, env, Path(private) / "formatter.log",
                420, overall)
            if result:
                raise RuntimeError("Owned scoped formatter failed")
        changed = source.git(scratch, "diff", "--name-only").splitlines()
        if not owned_delta(args.role, changed,
                           source.git(scratch, "ls-files", "--others", "--exclude-standard")):
            raise RuntimeError("Formatter changed a path beyond owned allowlist")
        patch = subprocess.check_output(["git", "diff", "--", *paths], cwd=scratch)
        groups = [json.loads(line) for line in (evidence / "process-cleanup.jsonl").read_text().splitlines()]
        if (len(groups) != 1 or type(groups[0].get("processGroup")) is not int
                or groups[0]["processGroup"] <= 0 or groups[0].get("confirmedAbsent") is not True):
            raise RuntimeError("Owned formatter process group must be known absent")
        lease.check()
        (evidence / "mechanical.patch").write_bytes(patch)
        receipt.update(state="MECHANICAL_PROPOSAL_ONLY", changedPaths=changed,
                       patchSHA256=hashlib.sha256(patch).hexdigest(), processCleanup=groups)
    finally:
        if scratch.exists():
            original_common = (root / source.git(root, "rev-parse", "--git-common-dir")).resolve()
            scratch_common = (scratch / source.git(scratch, "rev-parse", "--git-common-dir")).resolve()
            if (scratch.parent != Path(os.environ["RUNNER_TEMP"])
                    or not scratch.name.startswith("c16-format-" + args.role + "-")
                    or source.git(scratch, "rev-parse", "HEAD") != ref
                    or original_common != scratch_common):
                raise RuntimeError("Scratch exact ownership unavailable; removal refused")
            source.git(root, "worktree", "remove", "--force", str(scratch))
        receipt["scratchConfirmedAbsent"] = not scratch.exists()
        receipt["sourceAfter"], receipt["hostAfter"] = source.snapshot(root), source.snapshot(HOST)
        receipt["originalSourcesStable"] = before == receipt["sourceAfter"] and host_before == receipt["hostAfter"]
        accepted = finish_receipt(receipt, time.monotonic(), overall)
        (evidence / "receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
        if not accepted:
            raise RuntimeError("Mechanical proposal lacks exact source/cleanup proof")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
