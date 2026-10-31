#!/usr/bin/env python3
"""Bind the CTX2 policy slice to immutable source and complete native receipts."""
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
spec = importlib.util.spec_from_file_location('policy_source_support', HOST / 'scripts/ci/scenario-c-context-preflight.py')
support = importlib.util.module_from_spec(spec)
spec.loader.exec_module(support)
CASES = {
    'actualHeldRepositoryOriginAndActualBrokerReachTheRealNarrowProjection',
    'unheldRevokedChangedAndForeignTransactionOriginsNeverQueryTenant',
    'aDifferentLiveTransactionCannotBorrowTheHeldInvitationCapability',
    'independentlySignedPurposeTargetAndOriginMutationsFailBeforeTheActualPolicyQuery',
    'actualConfiguredCsvLoopIssuesThePrivateImportOriginForTheRealHttpRead',
}


def complete(native, groups, exit_code):
    counts = native.get('counts', {})
    cases = native.get('cases', [])
    run = native.get('runId', '')
    cleanup = native.get('cleanup', [])
    expected = {('network', 'oriso-policy-' + run[:12]), ('container', 'oriso-policy-kc-' + run[:12])}
    containers = [row for row in cleanup if row.get('kind') == 'container']
    server = native.get('processCleanup', {})
    classes = native.get('compiledClasses', {})
    return bool(
        exit_code == 0 and native.get('state') == 'PASS' and native.get('sourceUnchanged') is True
        and native.get('observed', {}).get('platformRuntimeRead') is True
        and re.fullmatch('[a-f0-9]{32}', run)
        and counts == {'tests': 5, 'failures': 0, 'errors': 0, 'skipped': 0}
        and len(cases) == 5 and {c.get('name', '').removesuffix('()') for c in cases} == CASES
        and all(c.get('outcome') == 'pass' for c in cases)
        and len(cleanup) == 2 and {(r.get('kind'), r.get('name')) for r in cleanup} == expected
        and all(r.get('confirmedAbsent') is True for r in cleanup)
        and len(containers) == 1 and isinstance(containers[0].get('runtime'), dict)
        and containers[0]['runtime'].get('oomKilled') is False
        and type(containers[0]['runtime'].get('restartCount')) is int
        and containers[0]['runtime']['restartCount'] == 0
        and len(groups) == 5 and all(g.get('confirmedAbsent') is True for g in groups)
        and server.get('confirmedAbsent') is True
        and all(type(g.get('processGroup')) is int and g['processGroup'] > 0 for g in [*groups, server])
        and len({g['processGroup'] for g in [*groups, server]}) == 6
        and set(classes) == {'tenant', 'user'} and all(
            re.fullmatch('[a-f0-9]{64}', value.get('classpathSHA256', ''))
            and value.get('fixture') and all(re.fullmatch('[a-f0-9]{64}', digest)
                                           for digest in value['fixture'].values())
            for value in classes.values()))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--user', type=Path, required=True)
    parser.add_argument('--user-ref', required=True)
    parser.add_argument('--helm', type=Path, required=True)
    parser.add_argument('--helm-ref', required=True)
    parser.add_argument('--evidence', type=Path, required=True)
    args = parser.parse_args()
    if os.environ.get('CI') != 'true' or os.environ.get('RUNNER_OS') != 'Linux':
        raise SystemExit('Native CTX2 execution requires the selected fresh hosted Linux job')
    roots = {'tenant': HOST, 'user': args.user.resolve(), 'helm': args.helm.resolve()}
    refs = {'tenant': os.environ.get('GITHUB_SHA'), 'user': args.user_ref, 'helm': args.helm_ref}
    for role, root in roots.items():
        if not refs[role] or not re.fullmatch('[a-f0-9]{40}', refs[role]):
            raise SystemExit('Every native input must have a full immutable Git commit')
        if support.git(root, 'rev-parse', 'HEAD') != refs[role] or support.git(root, 'status', '--porcelain'):
            raise SystemExit('Every native source checkout must be exact and clean')
    evidence = args.evidence.resolve()
    evidence.mkdir(parents=True, exist_ok=False)
    before = {role: support.snapshot(root) for role, root in roots.items()}
    report = {'state': 'STARTED_NOT_ACCEPTED', 'refs': refs, 'sourceBefore': before,
              'scope': 'CTX2 held invitation and configured import native policy slice only; no full C claim'}
    java = Path(os.environ['JAVA_HOME'])
    version = subprocess.check_output([str(java / 'bin/java'), '--version'], text=True)
    if '21.0.12' not in version:
        raise SystemExit('The selected Java toolchain must match the exact CTX source preflight')
    report['javaVersion'] = version.strip()
    try:
        log = evidence / 'private-runner.log'
        with os.fdopen(os.open(log, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'w') as output:
            child = subprocess.Popen(
                [sys.executable, str(roots['user'] / 'scripts/test-account-policy-context.py'),
                 '--tenant', str(HOST), '--user', str(roots['user']), '--helm', str(roots['helm']),
                 '--java-home', str(java), '--evidence', str(evidence / 'native')],
                stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
            try:
                report['runnerExit'] = child.wait(timeout=1500)
            except subprocess.TimeoutExpired:
                child.send_signal(signal.SIGINT)
                try:
                    child.wait(timeout=60)
                except subprocess.TimeoutExpired:
                    os.killpg(child.pid, signal.SIGKILL)
                    child.wait()
                report['state'] = 'DEADLINE_FAILURE_NO_ACCEPTANCE'
                return 1
        receipt = evidence / 'native/receipt.json'
        if not receipt.is_file():
            report['state'] = 'MISSING_NATIVE_RECEIPT_NO_ACCEPTANCE'
            # Only source frames/type are exported; private provider messages stay private.
            source_spec = importlib.util.spec_from_file_location('policy_early_failure', HOST / 'scripts/ci/scenario-c-context-native.py')
            early = importlib.util.module_from_spec(source_spec)
            source_spec.loader.exec_module(early)
            report['safeEarlyFailure'] = early.safe_early_failure(log)
            return 1
        native = json.loads(receipt.read_text())
        report['nativeReceipt'] = native
        process_file = evidence / 'native/process-cleanup.jsonl'
        groups = [json.loads(line) for line in process_file.read_text().splitlines()] if process_file.is_file() else []
        report['state'] = 'PASS' if complete(native, groups, report['runnerExit']) else 'INCOMPLETE_OR_FAILED_NATIVE_PROOF'
        return 0 if report['state'] == 'PASS' else 1
    finally:
        after = {role: support.snapshot(root) for role, root in roots.items()}
        report['sourceAfter'], report['sourceStable'] = after, after == before
        if after != before:
            report['state'] = 'SOURCE_CHANGED_NO_ACCEPTANCE'
        (evidence / 'source-receipt.json').write_text(json.dumps(report, indent=2, sort_keys=True) + '\n')
        print(json.dumps({'state': report['state'], 'sourceStable': after == before, 'refs': refs,
                          'counts': report.get('nativeReceipt', {}).get('counts')}))
        if after != before:
            raise RuntimeError('Source changed during native CTX2 proof')


if __name__ == '__main__':
    raise SystemExit(main())
