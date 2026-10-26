# Issue: Website deploy and Android APK build fail on push; Windows build stuck

## What the checks show
| Check | Result | Root cause (verified) |
|---|---|---|
| Build Android Release APK | Failed after 15 s | `apps/android/gradlew` is not in the repository. Only `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` are committed. `chmod +x gradlew` and `./gradlew` fail at once. Fixed: `gradlew` launcher added. |
| Deploy Website to Firebase | Failed after 20 s | Three causes. (1) The job deploys on every push to `main`. A site deploy replaces the whole hosted site, so it must only run when the release binaries exist. (2) The 1.0.6 Windows binaries committed under `apps/website/public/download/` were not CI output: `KnowToMigrate-1.0.6-x64.exe` is 198 KB and `KnowToMigrate-Setup.exe` is 807 KB, while a self-contained single-file build is tens of MB. Any download would have been broken. (3) `FIREBASE_SERVICE_ACCOUNT` must exist as a repository secret. I cannot read secrets, so the new workflow checks for it and stops with a clear message. |
| Build Windows MSI & EXE | In progress at capture | Not yet finished when the screenshot was taken. The new publish step fails early if `KnowToMigrate.exe` is under 20 MB, so a bad single-file publish is caught before packaging. |

## What changed
- `apps/android/gradlew`: added (executable).
- `.github/workflows/release.yml` (new): runs on `v*` tags. Builds the signed APK and the Windows binaries, generates the website data from those exact files, builds and deploys the website, then publishes the GitHub Release. Nothing deploys from a branch push.
- `.github/workflows/website-ci.yml` (new): type-checks and builds the website on every change, and fails on any hard-coded release version. It never deploys.
- `.github/workflows/build-android.yml`, `build-windows.yml`: build and test only. Release creation moved to `release.yml`, which removes the duplicate releases.
- `.github/workflows/deploy-website.yml`: deleted.
- `apps/website/public/download/`: 12 committed binaries removed from git. CI now produces them.
- `scripts/release/site-release.mjs`: `--literals` mode added, so CI can check for hard-coded versions without binaries.

## What you must do once
1. Add repository secrets (Settings > Secrets and variables > Actions):
   - `FIREBASE_SERVICE_ACCOUNT`: the service-account JSON for project `knowtomigrate`.
   - `KTM_KEYSTORE_BASE64`: `base64 -w0 knowtomigrate-release.jks`.
   - `KTM_KEYSTORE_PASSWORD`, `KTM_KEY_ALIAS`, `KTM_KEY_PASSWORD`.
2. Record the executable bit in git: `git update-index --chmod=+x apps/android/gradlew`.
3. Remove the old binaries from history (they are still in git even though the tree no longer has them): `git filter-repo --path apps/website/public/download --invert-paths`, then force-push. Rotate the keystore password too, because `KnowToMigrate2026!` is in history.
4. Tag a release: `git tag v1.0.6 && git push origin v1.0.6`.

## Not verified here
- The GitHub Actions runs themselves. This environment has no Android SDK, .NET, or Gradle distribution, and no access to GitHub secrets.
- The Rust build of `ktm_ffi.dll`. `cargo` is not installed here, so the DLL is still committed. The release should build it from `core/` in CI, then remove the committed copy.
