#!/usr/bin/env bash

set -euo pipefail

base_repo="${1:?base repository root is required}"
head_dir="${2:?head bundle directory is required}"
oasdiff_bin="${OASDIFF_BIN:-oasdiff}"

base_repo="$(cd "${base_repo}" && pwd)"
head_dir="$(cd "${head_dir}" && pwd)"
checked=0
while IFS= read -r base_spec; do
  head_spec="${head_dir}/$(basename "${base_spec}")"
  if [[ ! -f "${head_spec}" ]]; then
    echo "Provider contract was removed: $(basename "${base_spec}")" >&2
    exit 1
  fi
  "${oasdiff_bin}" breaking "${base_spec}" "${head_spec}" \
    --fail-on WARN \
    --format githubactions
  checked=$((checked + 1))
done < <(find "${base_repo}/api" -maxdepth 1 -type f \( -name '*.yaml' -o -name '*.yml' \) | sort)

if [[ "${checked}" -eq 0 ]]; then
  echo "No base provider contracts were available for compatibility checking." >&2
  exit 1
fi

echo "Verified ${checked} provider source contract(s) against the bundled pull-request head."
