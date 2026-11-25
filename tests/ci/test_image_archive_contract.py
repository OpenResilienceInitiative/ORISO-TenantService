import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


ROOT = Path(__file__).resolve().parents[2]
MANIFEST = "sha256:" + "1" * 64


def test_workflows_build_once_and_scan_before_registry_login_and_upload():
    action = (ROOT / ".github/actions/docker-build-push/action.yml").read_text()
    assert action.count("uses: docker/build-push-action@v7") == 1
    assert "platforms: linux/amd64" in action
    assert "push: false" in action
    assert "outputs: type=oci," in action
    assert "run: bash scripts/ci/scan-image-archive.sh" in action
    assert "run: bash scripts/ci/publish-image-archive.sh" in action
    assert action.index("scan-image-archive.sh") < action.index("docker/login-action")
    assert action.index("docker/login-action") < action.index("publish-image-archive.sh")
    assert action.count("if: ${{ inputs.push_to_ghcr == 'true' }}") == 2
    assert "EXPECTED_ARCHIVE_SHA256: ${{ steps.scan.outputs.archive_sha256 }}" in action

    workflows = ROOT / ".github/workflows"
    for name in ("ci-main.yml", "release-image.yml"):
        workflow = (workflows / name).read_text()
        assert workflow.count("uses: ./.github/actions/docker-build-push") == 1
        assert "push_to_ghcr: true" in workflow
    validation = (workflows / "ci-pull-request.yml").read_text().split("  contract-tests:")[0]
    assert "push_to_ghcr: false" in validation
    assert "contents: read" in validation
    assert "persist-credentials: false" in validation
    assert "github_token:" not in validation


def test_publishers_attest_the_verified_digest_before_release():
    action = (ROOT / ".github/actions/docker-build-push/action.yml").read_text()
    assert "value: ${{ steps.publish.outputs.digest }}" in action

    workflows = ROOT / ".github/workflows"
    for name in ("ci-main.yml", "release-image.yml"):
        publish = (workflows / name).read_text()
        assert "id-token: write" in publish
        assert "attestations: write" in publish
        assert publish.count("subject-digest:") == 1
        step = publish.index("Sign and publish the image provenance attestation")
        assert publish.index("id: image") < publish.index("push_to_ghcr: true") < step
        attestation = publish[step:]
        assert re.search(r"uses: actions/attest@[a-f0-9]{40}(?:\s|$)", attestation)
        assert "subject-name: ${{ env.REGISTRY }}/${{ env.ORG }}/${{ env.IMAGE_NAME }}" in attestation
        assert "push-to-registry: true" in attestation
        assert "create-storage-record: false" in attestation
        if name == "ci-main.yml":
            build, sign_job = publish.split("\n  attest-dev:\n", maxsplit=1)
            assert "id-token: write" not in build
            assert "attestations: write" not in build
            assert "digest: ${{ steps.image.outputs.digest }}" in build
            assert "if: github.ref == 'refs/heads/dev'" in sign_job
            assert "needs: build" in sign_job
            assert sign_job.index("docker/login-action") < sign_job.index("actions/attest@")
            assert "subject-digest: ${{ needs.build.outputs.digest }}" in attestation
        else:
            assert "subject-digest: ${{ steps.image.outputs.digest }}" in attestation
            assert step < publish.index("Create and push release tag")
            assert publish.index("Create and push release tag") < publish.index("Create GitHub Release")

    for name in ("ci-pull-request.yml", "ci-feature-branch.yml"):
        validation = (workflows / name).read_text()
        assert "actions/attest@" not in validation
        assert "id-token: write" not in validation
        assert "attestations: write" not in validation


def test_scanned_amd64_archive_is_bound_to_publication(tmp_path):
    archive = tmp_path / "tenant.tar"
    archive.write_bytes(b"synthetic archive")
    index = tmp_path / "index.json"
    index.write_text(json.dumps({"manifests": [
        {"platform": {"os": "linux", "architecture": "amd64"}, "digest": MANIFEST},
        {
            "platform": {"os": "unknown", "architecture": "unknown"},
            "annotations": {"vnd.docker.reference.type": "attestation-manifest"},
        },
    ]}))
    index_digest = "sha256:" + hashlib.sha256(index.read_bytes()).hexdigest()
    receipt = tmp_path / "receipt"
    copied = tmp_path / "copies"
    env = {
        **os.environ,
        "PATH": f"{tmp_path}{os.pathsep}{os.environ['PATH']}",
        "GITHUB_OUTPUT": str(receipt),
        "IMAGE_TAGS": "example.invalid/tenant:fixture",
        "TEST_INDEX": str(index),
        "TEST_INDEX_DIGEST": index_digest,
        "TEST_MANIFEST": MANIFEST,
        "TEST_COPIES": str(copied),
    }
    tools = {
        "skopeo": r"""#!/usr/bin/env bash
set -euo pipefail
case "$1" in
  inspect) [[ "$2" == --raw && "$3" == oci-archive:* ]]; cat "$TEST_INDEX" ;;
  manifest-digest) checksum=$(sha256sum "$2"); printf 'sha256:%s\n' "${checksum%% *}" ;;
  --override-os)
    [[ "$2" == linux && "$3" == --override-arch && "$4" == amd64 &&
       "$5" == copy && "$6" == --preserve-digests && "$7" == --digestfile ]]
    printf '%s\n' "$TEST_MANIFEST" > "$8"
    ;;
  copy)
    [[ "$2" == --all && "$3" == --preserve-digests && "$4" == --authfile &&
       "$6" == --digestfile && "$8" == oci-archive:* && "$9" == docker://example.invalid/* ]]
    printf '%s\n' "$TEST_INDEX_DIGEST" > "$7"
    printf '%s\n' "$9" >> "$TEST_COPIES"
    ;;
  *) exit 2 ;;
esac
""",
        "trivy": r"""#!/usr/bin/env bash
set -euo pipefail
[[ "$1" == image && "$2" == --input && "$4" == --platform && "$5" == linux/amd64 &&
   "${*:6}" == '--format table --exit-code 1 --ignore-unfixed --vuln-type os,library --severity CRITICAL,HIGH' ]]
[[ "${TEST_FAIL_SCAN:-}" != 1 ]]
""",
    }
    for name, content in tools.items():
        tool = tmp_path / name
        tool.write_text(content)
        tool.chmod(0o700)

    def run(script, *args, **overrides):
        return subprocess.run(
            ["bash", ROOT / "scripts/ci" / script, archive, *args],
            env={**env, **overrides},
            capture_output=True,
            text=True,
            timeout=10,
        )

    failed_scan = run("scan-image-archive.sh", "linux/amd64", index_digest, TEST_FAIL_SCAN="1")
    assert failed_scan.returncode != 0
    assert not receipt.exists()
    assert not copied.exists()
    scan = run("scan-image-archive.sh", "linux/amd64", index_digest)
    assert scan.returncode == 0, scan.stderr
    assert not copied.exists()
    checksum = receipt.read_text().strip().removeprefix("archive_sha256=")
    assert checksum == hashlib.sha256(archive.read_bytes()).hexdigest()

    publish = run("publish-image-archive.sh", index_digest, checksum)
    assert publish.returncode == 0, publish.stderr
    assert f"digest={index_digest}" in receipt.read_text().splitlines()
    assert copied.read_text().splitlines() == ["docker://example.invalid/tenant:fixture"]
    receipt.unlink()
    archive.write_bytes(b"different archive")
    assert run("publish-image-archive.sh", index_digest, checksum).returncode != 0
    assert not receipt.exists()
    assert copied.read_text().splitlines() == ["docker://example.invalid/tenant:fixture"]
