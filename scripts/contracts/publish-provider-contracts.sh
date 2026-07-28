#!/usr/bin/env bash

set -euo pipefail

repo_root="${1:?repository root is required}"
output_dir="${2:?output directory is required}"
redocly_version="${REDOCLY_VERSION:?REDOCLY_VERSION is required}"

repo_root="$(cd "${repo_root}" && pwd)"
mkdir -p "${output_dir}"
output_dir="$(cd "${output_dir}" && pwd)"

run_redocly() {
  if [[ -n "${REDOCLY_BIN:-}" ]]; then
    "${REDOCLY_BIN}" "$@"
  else
    REDOCLY_TELEMETRY=off npx --yes "@redocly/cli@${redocly_version}" "$@"
  fi
}

spec_count=0
while IFS= read -r spec; do
  relative_spec="${spec#"${repo_root}/"}"
  output_spec="${output_dir}/$(basename "${spec}")"
  (
    cd "${repo_root}"
    run_redocly bundle "${relative_spec}" --output "${output_spec}"
  )
  spec_count=$((spec_count + 1))
done < <(find "${repo_root}/api" -maxdepth 1 -type f \( -name '*.yaml' -o -name '*.yml' \) | sort)

if [[ "${spec_count}" -eq 0 ]]; then
  echo "No provider-owned OpenAPI files found in ${repo_root}/api." >&2
  exit 1
fi

manifest="${output_dir}/SHA256SUMS"
: > "${manifest}"
while IFS= read -r bundle; do
  (
    cd "${output_dir}"
    shasum -a 256 "$(basename "${bundle}")"
  ) >> "${manifest}"
done < <(find "${output_dir}" -maxdepth 1 -type f \( -name '*.yaml' -o -name '*.yml' \) | sort)

git -C "${repo_root}" rev-parse HEAD > "${output_dir}/SOURCE_COMMIT"
echo "Published ${spec_count} bundled provider contract(s) from $(cat "${output_dir}/SOURCE_COMMIT")."
