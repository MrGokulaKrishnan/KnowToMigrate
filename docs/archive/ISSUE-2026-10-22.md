# Website and download links keep showing the old version after a release

## Symptom
After a new version is published, the website does not reflect it:
- The hero text still reads "KnowToMigrate 1.0.5".
- Download buttons point at `/download/KnowToMigrate-1.0.5-x64.msi` and `/download/KnowToMigrate-1.0.5.apk`. The publish script deletes old versioned files, so these links fall through to the site's `index.html`.
- The Firebase rewrites only cover the 1.0.5 file names, so new versioned downloads are not routed correctly.

## Root cause
The version is recorded in five places, and the release script updates only some of them:

| Location | Problem |
|---|---|
| `apps/website/src/pages/HomePage.tsx` | Hero text hardcoded to 1.0.5. The release script never touches it. |
| `apps/website/src/pages/DownloadPage.tsx` | Default entries and download URLs hardcoded to 1.0.5. The script only rewrites `version:` fields, so URLs stay at 1.0.5. |
| `apps/website/src/utils/platform.ts` | Recommended APK name hardcoded to 1.0.0. |
| `firebase.json` | Rewrites and headers listed for 1.0.5 file names only. |
| `scripts/publish-update.ps1` | Patches manifests with regular expressions, then prunes the previous version's files. |

There is also no automated check, so a stale value can ship without anyone noticing.

## Fix
One generator, `scripts/release/site-release.mjs`, now writes every release reference from `version.json` and the `.bin` artifacts:
- `apps/website/public/update-manifest.json`
- `apps/website/public/updates/android/stable.json`
- `apps/website/src/generated/release.ts` (new; the website reads from this file)
- the `rewrites` and download `headers` in `firebase.json`

Changes:
1. The website imports `RELEASE` from the generated file. Hardcoded versions and fallbacks are removed. The page no longer fetches the manifest at runtime, so it cannot show a stale cached copy.
2. `publish-update.ps1` calls the generator instead of patching files with regular expressions.
3. `deploy-website.yml` runs `node scripts/release/site-release.mjs --check` before building. The check fails if generated files are stale or if a website source file contains a hardcoded release version.
4. Manifests and `/updates/**` are served with `Cache-Control: no-cache`, so apps and browsers pick up a new release immediately. Versioned binaries are served with `immutable` caching.
5. `version.json` release notes no longer claim AES-256-GCM encryption or secure package verification, which the code does not provide.

## How to release now
1. Bump `version.json` (and the native version fields, as `sync-version.ps1` already does).
2. Build the Windows and Android artifacts and copy them into `apps/website/public/download/` as `*.bin` files (`publish-update.ps1` does this).
3. Run `node scripts/release/site-release.mjs`.
4. Commit the generated files together with the binaries, then push. CI verifies that the generated data is current before deploying.

## Verification
- Running the generator on the current 1.0.5 artifacts reproduces the SHA-256 values already in the repo (`17dd0d0b…` for the Windows setup, `6eef8bfd…` for the APK). The artifact pipeline agrees with the published manifests.
- `node scripts/release/site-release.mjs --check` passes, and a second run produces byte-identical `firebase.json`.
- `tsc --noEmit` and `vite build` pass for the website.
- Simulated 1.0.6 release (copied artifacts, bumped `version.json`): all generated files, the Firebase rewrites, the APK URL, and `release.ts` move to 1.0.6, and `--check` passes.

## Follow-ups
- Stop committing installers. Host them on GitHub Releases and have the generator read from there.
- Sign the update manifest (see `docs/SECURITY_PLAN.md`); the `signature` field is still empty.
- Remove the unused `KnowToMigrate.exe.bin` and `KnowToMigrate.apk.bin` aliases once no link refers to them.
