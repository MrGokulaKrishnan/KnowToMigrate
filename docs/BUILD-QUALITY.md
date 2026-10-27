# Build quality: what was removed, what to change next

## Size and performance (measured)
| Item | Before | After | Note |
|---|---|---|---|
| Repository tree | about 556 MB | 6.0 MB | 12 release binaries removed from git (CI now builds them) |
| `apps/website/public/prototype.html` | 40 KB served publicly | moved to `design/` | Design prototype should not be on the public site |
| One-off scripts (4) | — | deleted | They rewrote source files from a hard-coded `c:\` path |
| Root status docs (6) | about 60 KB | `docs/archive/` | Overlapping, out of date |
| `apps/windows-wpf/ktm_ffi.dll` | 853 KB | removed | Retired in favor of native .NET 8 engine; P/Invoke bridge eliminated |

## Resolved: `ktm_ffi.dll`
Removed `ktm_ffi.dll` and `KtmNativeBridge.cs`. The Windows WPF application runs on pure native .NET 8 managed sockets and cryptographic services without external native DLL dependencies. The Rust workspace in `core/` remains preserved for future shared-core work targeting MSVC.

## Pipeline next steps
1. Add a test job that runs `dotnet test tests/KtmIntegrationTests` and the Android unit tests. Both exist but no workflow runs them.
2. Add `dotnet format --verify-no-changes` and Android lint, so style regressions fail in pull requests.
3. Sign the Windows MSI and EXE with `sign-windows.ps1` in the release job, using a secret certificate.
4. Generate build provenance with `actions/attest-build-provenance` and an SBOM for each release.
5. Add a Playwright smoke test that opens the download page and checks each download link returns a file, not HTML.
6. Replace the `Setup.exe` copy with a real bootstrapper built from `apps/windows-setup`, or drop the name. Today it is an identical copy of the portable EXE, which is misleading.
7. Gradle: regenerate the wrapper with `gradle wrapper --gradle-version 8.10.2` so the launcher matches Gradle's own script.

## Architecture next steps
- Put the protocol in one Rust crate that both apps call, as planned in the architecture upgrade document. Until then, the Kotlin, C#, and TypeScript protocols will keep drifting.
- Keep the website download data generated from `version.json` (already in place), and never commit the binaries.
- Move the relay, web receiver, and website to their own release cadence so one failing build cannot block the others.
