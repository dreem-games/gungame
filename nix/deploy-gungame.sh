# shellcheck shell=bash

set -euo pipefail

flake_dir="${GUNGAME_NIXOS_FLAKE:?GUNGAME_NIXOS_FLAKE is required}"
configuration="${GUNGAME_NIXOS_CONFIGURATION:?GUNGAME_NIXOS_CONFIGURATION is required}"
health_url="${GUNGAME_HEALTH_URL:?GUNGAME_HEALTH_URL is required}"
lock_file="${flake_dir}/flake.lock"
lock_guard="/run/lock/gungame-deploy.lock"

if [[ "$(id -u)" -ne 0 ]]; then
    echo "gungame-deploy must run as root" >&2
    exit 77
fi

if [[ ! -f "${flake_dir}/flake.nix" || ! -f "${lock_file}" ]]; then
    echo "${flake_dir} must contain flake.nix and flake.lock" >&2
    exit 66
fi

exec 9>"${lock_guard}"
if ! flock -n 9; then
    echo "another GunGame deployment is already running" >&2
    exit 75
fi

staging_dir="$(mktemp -d /var/tmp/gungame-deploy.XXXXXX)"
cleanup() {
    rm -rf -- "${staging_dir}"
}
trap cleanup EXIT

cp -a "${flake_dir}/." "${staging_dir}/"
cd "${staging_dir}"

echo "Updating the pinned gungame input in a staging flake"
nix flake update gungame --flake "${staging_dir}"

echo "Evaluating the staged flake"
nix flake check "${staging_dir}" --no-build

echo "Building the staged NixOS generation"
nixos-rebuild build --flake "${staging_dir}#${configuration}"

echo "Activating the staged NixOS generation"
if ! nixos-rebuild switch --flake "${staging_dir}#${configuration}"; then
    echo "Activation failed, rolling back to the previous generation" >&2
    nixos-rebuild switch --rollback || true
    exit 1
fi

healthy=false
for _ in {1..10}; do
    if systemctl is-active --quiet gungame-server.service && \
        curl --silent --show-error --output /dev/null --max-time 2 "${health_url}"; then
        healthy=true
        break
    fi
    sleep 1
done

if [[ "${healthy}" != true ]]; then
    echo "GunGame service is not active after deployment, rolling back" >&2
    nixos-rebuild switch --rollback || true
    exit 1
fi

install -m 0644 "${staging_dir}/flake.lock" "${flake_dir}/flake.lock.next"
mv -f "${flake_dir}/flake.lock.next" "${lock_file}"

echo "GunGame deployment completed successfully"
