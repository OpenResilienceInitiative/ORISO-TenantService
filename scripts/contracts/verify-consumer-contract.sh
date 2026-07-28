#!/usr/bin/env bash

set -euo pipefail

consumer_spec="${1:?consumer spec is required}"
provider_root="${2:?provider repository root is required}"
provider_spec="${3:?provider spec path is required}"
redocly_version="${REDOCLY_VERSION:?REDOCLY_VERSION is required}"
oasdiff_bin="${OASDIFF_BIN:-oasdiff}"

consumer_spec="$(cd "$(dirname "${consumer_spec}")" && pwd)/$(basename "${consumer_spec}")"
provider_root="$(cd "${provider_root}" && pwd)"
bundle_dir="$(mktemp -d "${TMPDIR:-/tmp}/oriso-provider-contract.XXXXXX")"
trap 'rm -rf "${bundle_dir}"' EXIT
provider_bundle="${bundle_dir}/$(basename "${provider_spec}")"

run_redocly() {
  if [[ -n "${REDOCLY_BIN:-}" ]]; then
    "${REDOCLY_BIN}" "$@"
  else
    REDOCLY_TELEMETRY=off npx --yes "@redocly/cli@${redocly_version}" "$@"
  fi
}

(
  cd "${provider_root}"
  run_redocly bundle "${provider_spec}" --output "${provider_bundle}"
)

"${oasdiff_bin}" breaking "${consumer_spec}" "${provider_bundle}" \
  --fail-on WARN \
  --format githubactions

provider_commit="$(git -C "${provider_root}" rev-parse HEAD)"
echo "Consumer contract $(basename "${consumer_spec}") is compatible with provider commit ${provider_commit}."
