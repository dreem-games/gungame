import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

const script = fileURLToPath(new URL('./publish_nix_release.sh', import.meta.url));
const sha = 'a'.repeat(40);

function publish(options = {}) {
    const root = mkdtempSync(join(tmpdir(), 'gungame-publish-test-'));
    try {
        // Поддельный gh запрещает изменение опубликованного immutable release.
        writeFileSync(
            join(root, 'gh'),
            `#!/usr/bin/env bash
set -eu
printf '%s\\n' "$*" >> "$MOCK_ROOT/calls"
case "$1 $2" in
    'release view')
        case "$MOCK_STATE" in
            absent|denied) exit 1 ;;
            draft) echo true ;;
            *) echo false ;;
        esac ;;
    'release create')
        test "$MOCK_STATE" != denied
        [[ " $* " == *' --draft '* ]] ;;
    'release upload')
        test "$MOCK_STATE" != published
        test "$MOCK_FAIL_UPLOAD" != yes ;;
    'release edit')
        test "$MOCK_STATE" != published ;;
    *)
        if [[ "$*" == *'/assets?'* ]]; then
            echo artifact.tar.gz
            if [[ "$MOCK_STATE" != incomplete ]]; then echo artifact.tar.gz.sha256; fi
        else
            echo 42
        fi ;;
esac
`,
            { mode: 0o755 }
        );
        const asset = join(root, 'artifact.tar.gz');
        writeFileSync(asset, 'archive');
        writeFileSync(`${asset}.sha256`, 'checksum');
        const result = spawnSync('bash', [script], {
            encoding: 'utf8',
            env: {
                ...process.env,
                PATH: `${root}:${process.env.PATH}`,
                MOCK_ROOT: root,
                MOCK_STATE: options.state ?? 'absent',
                MOCK_FAIL_UPLOAD: options.failUpload ? 'yes' : 'no',
                GITHUB_REPOSITORY: 'example/game',
                COMMIT_SHA: sha,
                REF_TYPE: options.refType ?? 'branch',
                REF_NAME: options.refName ?? 'feature/example',
                ASSET: asset,
                CHECKSUM: `${asset}.sha256`
            }
        });
        const calls = readFileSync(join(root, 'calls'), 'utf8').trim().split('\n');
        return { ...result, calls };
    } finally {
        rmSync(root, { recursive: true, force: true });
    }
}

test('branch assets are uploaded before publishing a dedicated SHA release', () => {
    const result = publish();
    assert.equal(result.status, 0, result.stderr);
    assert.deepEqual(
        result.calls.map((call) => call.split(' ').slice(0, 2).join(' ')),
        ['release view', 'release create', 'release upload', 'release edit']
    );
    assert.ok(result.calls.every((call) => call.includes(`gungame-build-${sha}`)));
    assert.match(result.calls[1], /--draft/);
    assert.match(result.calls[1], /--prerelease/);
    assert.match(result.calls[2], /artifact.tar.gz .*artifact.tar.gz.sha256/);
    assert.match(result.calls[3], /--draft=false/);
    assert.match(result.calls[3], /--latest=false/);
});

test('SemVer release uses the existing verified tag and uploads before publishing', () => {
    const result = publish({ refType: 'tag', refName: 'v1.2.3' });
    assert.equal(result.status, 0, result.stderr);
    assert.match(result.calls[1], /release create v1.2.3 .*--draft --verify-tag/);
    assert.doesNotMatch(result.calls[1], /--prerelease/);
    assert.match(result.calls[2], /^release upload v1.2.3 /);
    assert.match(result.calls[3], /^release edit v1.2.3 /);
    assert.doesNotMatch(result.calls[3], /--latest/);
});

test('failed upload leaves the release unpublished', () => {
    const result = publish({ failUpload: true });
    assert.notEqual(result.status, 0);
    assert.ok(!result.calls.some((call) => call.startsWith('release edit')));
});

test('retry resumes an existing draft', () => {
    const result = publish({ state: 'draft' });
    assert.equal(result.status, 0, result.stderr);
    assert.equal(result.calls.length, 3);
    assert.match(result.calls[1], /^release upload .*--clobber$/);
    assert.match(result.calls[2], /^release edit /);
});

test('retry of a complete published release performs no mutations', () => {
    const result = publish({ state: 'published' });
    assert.equal(result.status, 0, result.stderr);
    assert.ok(result.calls.every((call) => /^(release view|api) /.test(call)));
});

test('incomplete immutable release fails without attempting to upload', () => {
    const result = publish({ state: 'incomplete' });
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /is incomplete/);
    assert.ok(result.calls.every((call) => /^(release view|api) /.test(call)));
});

test('permission failure cannot publish or upload', () => {
    const result = publish({ state: 'denied' });
    assert.notEqual(result.status, 0);
    assert.equal(result.calls.length, 2);
});
