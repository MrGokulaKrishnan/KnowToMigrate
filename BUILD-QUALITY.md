# KnowToMigrate Build Quality & Version Management Standard

## 1. Single Source of Truth
All version numbering across the KnowToMigrate ecosystem is strictly driven from:
- `version.json` (root directory)

No project file, manifest, script, or source code file may hardcode a release version string independently.

### Automated Propagators:
- `Version.g.props` (MSBuild property group imported automatically via `Directory.Build.props`)
- `KtmVersion.g.cs` (C# constants `KtmVersion.Value`, `KtmVersion.Code`, `KtmVersion.DisplayVersion`)
- `Package.wxs` (WiX installer definition)
- `apps/android/app/build.gradle.kts` (`versionCode`, `versionName`)
- `apps/website/public/update-manifest.json` & `apps/website/public/update-manifest-dev.json`
- `apps/website/public/updates/android/stable.json`

## 2. Release & Build Rule
> **MANDATORY:** Bump `version.json` before every build.

Before tagging or deploying a release:
1. Update `version` and `versionCode` in `version.json`.
2. Run `pwsh scripts/sync-version.ps1` to update all downstream files.
3. Run `pwsh scripts/check-versions.ps1` to verify complete synchronization.

The CI pipeline enforces this rule:
- `build-windows.yml` and `build-android.yml` verify all project files match `version.json`.
- `release.yml` checks against the live production manifest (`https://knowtomigrate.web.app/update-manifest.json`) with `-CheckPublished` and fails if the version has not been bumped.

## 3. Fallback and Protocol Safety
- `KtmUpdateService.CurrentVersion` returns `"unknown"` if assembly metadata cannot be inspected, and will refuse to check for updates when version is unknown to prevent false downloads or regressions.
- `MinimumSupportedVersion` represents the protocol floor (transfer wire framing / encryption compatibility), not the installed client version.
- In Debug builds, local testing of update manifests is supported via `KTM_UPDATE_MANIFEST_URL` and `KTM_UPDATE_MANIFEST_FILE`. Production release builds strictly use the production manifest URL.
