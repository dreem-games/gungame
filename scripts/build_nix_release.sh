#!/usr/bin/env bash

set -euo pipefail

baseline_sha="${GUNGAME_BASELINE_SHA:-e2f3db3ab611185252a407458b40bdb96ac7e5f2}"
commit_sha="${GUNGAME_COMMIT_SHA:-$(git rev-parse HEAD)}"
ref_type="${GUNGAME_REF_TYPE:-branch}"
ref_name="${GUNGAME_REF_NAME:-$(git branch --show-current)}"
system="${GUNGAME_ARTIFACT_SYSTEM:-x86_64-linux}"
output_dir="${1:-release-artifacts}"

if [[ ! "${baseline_sha}" =~ ^[0-9a-f]{40}$ ]]; then
    echo "GUNGAME_BASELINE_SHA must be a full lowercase commit SHA" >&2
    exit 64
fi

if [[ ! "${commit_sha}" =~ ^[0-9a-f]{40}$ ]]; then
    echo "GUNGAME_COMMIT_SHA must be a full lowercase commit SHA" >&2
    exit 64
fi

if [[ "$(git rev-parse HEAD)" != "${commit_sha}" ]]; then
    echo "GUNGAME_COMMIT_SHA does not match the checked-out commit" >&2
    exit 65
fi

if ! git merge-base --is-ancestor "${baseline_sha}" HEAD; then
    echo "The commit does not descend from the GunGame deployment baseline" >&2
    exit 65
fi

case "${ref_type}" in
    branch)
        git check-ref-format --branch "${ref_name}" >/dev/null
        ;;
    tag)
        if [[ ! "${ref_name}" =~ ^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
            echo "Release tags must use vMAJOR.MINOR.PATCH without leading zeroes" >&2
            exit 64
        fi
        if [[ "$(git rev-list -n 1 "refs/tags/${ref_name}")" != "${commit_sha}" ]]; then
            echo "Release tag does not point at GUNGAME_COMMIT_SHA" >&2
            exit 65
        fi
        ;;
    *)
        echo "GUNGAME_REF_TYPE must be branch or tag" >&2
        exit 64
        ;;
esac

if [[ "${system}" != "x86_64-linux" ]]; then
    echo "Only x86_64-linux release artifacts are currently supported" >&2
    exit 64
fi

mkdir -p "${output_dir}"
work_dir="$(mktemp -d)"
cleanup() {
    rm -rf -- "${work_dir}"
}
trap cleanup EXIT

frontend_path="$(nix build --no-link --print-out-paths ".#packages.${system}.gungame-frontend")"
server_path="$(nix build --no-link --print-out-paths ".#packages.${system}.gungame-server")"
cache_dir="${work_dir}/payload/cache"
mkdir -p "${cache_dir}"
nix copy --to "file://${cache_dir}" "${frontend_path}" "${server_path}"

asset_name="gungame-${commit_sha}-${system}.nix-cache.tar.gz"
asset_path="${output_dir}/${asset_name}"
manifest_path="${work_dir}/payload/manifest.json"

jq -n \
    --arg commitSha "${commit_sha}" \
    --arg refType "${ref_type}" \
    --arg refName "${ref_name}" \
    --arg system "${system}" \
    --arg frontendStorePath "${frontend_path}" \
    --arg serverStorePath "${server_path}" \
    '{
        schemaVersion: 1,
        commitSha: $commitSha,
        refType: $refType,
        refName: $refName,
        system: $system,
        frontendStorePath: $frontendStorePath,
        serverStorePath: $serverStorePath
    }' > "${manifest_path}"

tar \
    --sort=name \
    --mtime='@0' \
    --owner=0 \
    --group=0 \
    --numeric-owner \
    -C "${work_dir}/payload" \
    -cf - . | gzip -n > "${asset_path}"

asset_hash="$(nix hash file --type sha256 --base16 "${asset_path}")"
printf '%s  %s\n' "${asset_hash}" "${asset_name}" > "${asset_path}.sha256"
cp "${manifest_path}" "${output_dir}/manifest.json"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    {
        printf 'asset_name=%s\n' "${asset_name}"
        printf 'commit_sha=%s\n' "${commit_sha}"
        printf 'ref_type=%s\n' "${ref_type}"
        printf 'ref_name=%s\n' "${ref_name}"
    } >> "${GITHUB_OUTPUT}"
fi
