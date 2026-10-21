#!/usr/bin/env python3
"""Failure-only formatter proposal in separately owned immutable scratch worktrees."""
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
    'tenant': [
        'src/test/java/com/vi/tenantservice/api/controller/AccountPolicyReceiverFixture.java',
        'src/test/java/com/vi/tenantservice/api/controller/GroupedTaskServiceIdentityAuthorizationIT.java'],
    'user': ['src/test/java/de/caritas/cob/userservice/api/service/accountpolicy/CheckedAccountPolicyNativeGate.java'],
}


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-evidence', type=Path, required=True)
    parser.add_argument('--user', type=Path, required=True)
    parser.add_argument('--helm', type=Path, required=True)
    parser.add_argument('--evidence', type=Path, required=True)
    args = parser.parse_args()
    if os.environ.get('CI') != 'true' or os.environ.get('RUNNER_OS') != 'Linux':
        raise SystemExit('Hosted Linux formatter proposal only')
    prior = json.loads((args.source_evidence / 'source-receipt.json').read_text())
    native = prior.get('nativeReceipt', {})
    diagnostic = str(native.get('compileDiagnostics', []))
    if (not prior.get('sourceStable') or native.get('state') != 'FIXTURE_FAILURE'
            or not native.get('stage', '').endswith('-compile') or native.get('cleanup') != []
            or "Run 'mvn spotless:apply'" not in diagnostic):
        raise SystemExit('Only a confirmed formatter failure before any container may produce a proposal')
    source = module('policy_format_source', HOST / 'scripts/ci/scenario-c-context-preflight.py')
    support = module('policy_format_resources', HOST / 'scripts/test-bootstrap-context-roundtrip.py')
    roots = {'tenant': HOST, 'user': args.user.resolve(), 'helm': args.helm.resolve()}
    before = {role: source.snapshot(root) for role, root in roots.items()}
    if before != prior['sourceBefore']:
        raise SystemExit('Original immutable source no longer matches the completed failed gate')
    evidence = args.evidence.resolve()
    evidence.mkdir(parents=True, exist_ok=False)
    lease = support.configure_memory(2896, evidence)
    receipt = {'state': 'STARTED_NO_NATIVE_CREDIT', 'refs': prior['refs'], 'scratch': [], 'patches': {}}
    planned = []
    overall = time.monotonic() + 900
    try:
        lease.check(starting=True)
        for role in ('tenant', 'user'):
            root, ref = roots[role], prior['refs'][role]
            if source.git(root, 'rev-parse', 'HEAD') != ref or source.git(root, 'status', '--porcelain'):
                raise RuntimeError('An original checkout is not exact and clean')
            scratch = Path(os.environ['RUNNER_TEMP']) / ('policy-format-' + role + '-' + uuid.uuid4().hex)
            planned.append((root, scratch, ref))
            source.git(root, 'worktree', 'add', '--detach', str(scratch), ref)
            if source.git(scratch, 'rev-parse', 'HEAD') != ref:
                raise RuntimeError('Scratch immutable source mismatch')
            paths = FILES[role]
            with tempfile.TemporaryDirectory(prefix='private-format-') as private:
                os.chmod(private, 0o700)
                env = {**os.environ, 'MAVEN_OPTS': '-Xmx512m -XX:MaxMetaspaceSize=192m -XX:ActiveProcessorCount=2'}
                result = support.bounded(['./mvnw', '-B', '-ntp', '-q',
                    '-DspotlessFiles=' + '|'.join(re.escape(str(scratch / path)) for path in paths), 'spotless:apply'],
                    scratch, env, Path(private) / 'formatter.log', 420, overall)
                if result:
                    raise RuntimeError('Scoped scratch formatter failed')
            changed = source.git(scratch, 'diff', '--name-only').splitlines()
            if not set(changed).issubset(paths) or source.git(scratch, 'ls-files', '--others', '--exclude-standard'):
                raise RuntimeError('Formatter changed a path outside the exact owned fixture')
            patch = subprocess.check_output(['git', 'diff', '--', *paths], cwd=scratch)
            (evidence / (role + '.patch')).write_bytes(patch)
            receipt['patches'][role] = {'paths': paths, 'sha256': hashlib.sha256(patch).hexdigest(), 'bytes': len(patch)}
        receipt['state'] = 'MECHANICAL_FORMAT_PROPOSAL_ONLY'
    finally:
        for root, scratch, ref in reversed(planned):
            if scratch.exists():
                if (source.git(scratch, 'rev-parse', 'HEAD') != ref
                        or not scratch.name.startswith('policy-format-')
                        or scratch.parent != Path(os.environ['RUNNER_TEMP'])):
                    raise RuntimeError('Exact scratch ownership is unavailable; refusing removal')
                source.git(root, 'worktree', 'remove', '--force', str(scratch))
            if scratch.exists():
                raise RuntimeError('Owned scratch worktree survived cleanup')
            receipt['scratch'].append({'path': str(scratch), 'sourceRef': ref, 'confirmedAbsent': True})
        after = {role: source.snapshot(root) for role, root in roots.items()}
        receipt['originalSourcesStable'] = before == after
        if before != after:
            receipt['state'] = 'SOURCE_CHANGED_NO_ACCEPTANCE'
        (evidence / 'receipt.json').write_text(json.dumps(receipt, indent=2, sort_keys=True) + '\n')
        if before != after:
            raise RuntimeError('Original sources changed during scratch formatting')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
