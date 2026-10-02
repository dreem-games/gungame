#!/usr/bin/env bash
set -euo pipefail

: "${GITHUB_REPOSITORY:?}" "${COMMIT_SHA:?}" "${REF_TYPE:?}" "${REF_NAME:?}" "${ASSET:?}" "${CHECKSUM:?}"
[[ "$COMMIT_SHA" =~ ^[0-9a-f]{40}$ ]]
test -f "$ASSET"
test -f "$CHECKSUM"

case "$REF_TYPE" in
    branch)
        release_tag="gungame-build-${COMMIT_SHA}"
        create_args=(--target "$COMMIT_SHA" --prerelease --title "GunGame build ${COMMIT_SHA}" --notes "Machine-readable deployment artifacts.")
        publish_args=(--latest=false)
        ;;
    tag)
        [[ "$REF_NAME" =~ ^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]
        release_tag="$REF_NAME"
        create_args=(--verify-tag --generate-notes --title "$REF_NAME")
        publish_args=()
        ;;
    *) exit 65 ;;
esac

# Черновик можно восстановить после сбоя загрузки. Публикация замораживает assets.
if is_draft="$(gh release view "$release_tag" --repo "$GITHUB_REPOSITORY" --json isDraft --jq '.isDraft')"; then
    case "$is_draft" in
        false)
            release_id="$(gh api "repos/$GITHUB_REPOSITORY/releases/tags/$release_tag" --jq '.id')"
            published_assets="$(gh api --paginate "repos/$GITHUB_REPOSITORY/releases/$release_id/assets?per_page=100" --jq '.[].name')"
            for file in "$ASSET" "$CHECKSUM"; do
                if ! grep -Fxq "$(basename "$file")" <<< "$published_assets"; then
                    echo "Published release $release_tag is incomplete; it cannot be repaired after becoming immutable" >&2
                    exit 65
                fi
            done
            echo "$release_tag is already published"
            exit 0
            ;;
        true) ;;
        *) echo "Unexpected release state: $is_draft" >&2; exit 65 ;;
    esac
else
    # При сетевой ошибке или отказе доступа create также завершится ошибкой.
    gh release create "$release_tag" --repo "$GITHUB_REPOSITORY" --draft "${create_args[@]}"
fi

gh release upload "$release_tag" "$ASSET" "$CHECKSUM" --repo "$GITHUB_REPOSITORY" --clobber
gh release edit "$release_tag" --repo "$GITHUB_REPOSITORY" --draft=false "${publish_args[@]}"
