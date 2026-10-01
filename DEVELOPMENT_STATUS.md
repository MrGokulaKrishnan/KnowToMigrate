# KnowToMigrate — Development & Production Status

**PROJECT:** KnowToMigrate  
**CURRENT VERSION:** 1.0.0  
**LAST UPDATED:** 01 October 2026  
**STATUS:** PRODUCTION READY — ENTERPRISE AUTO-UPDATE SYSTEM FULLY IMPLEMENTED & VERIFIED  

---

## Project Audit & System Specification

| Field | Value |
| :--- | :--- |
| **PROJECT** | KnowToMigrate |
| **CURRENT VERSION** | 1.0.0 |
| **WINDOWS STACK** | C# / .NET 8.0 WPF (`apps/windows-wpf/KnowToMigrate.csproj`), single-file self-contained `win-x64`, custom WindowChrome, Pluto Engine v2 (TCP 54124, UDP 54123) |
| **INSTALLER** | WiX Toolset v4 Enterprise MSI (`Package.wxs`) + C# Setup Bootstrapper (`apps/windows-setup/SetupBootstrapper.cs`). Native MajorUpgrade configured via `UpgradeCode="C8B72D32-1594-4F77-8E82-7AC4E7D01A4B"` |
| **STANDALONE UPDATER** | `KnowToMigrate.Updater.exe` (55.5 KB, C# Native WinForms, zero-dependency, self-relocates to `%TEMP%`, enforces SHA-256 verification, manages parent PID exit, executes WiX MajorUpgrade / msiexec, with rollback guard) |
| **ANDROID STACK** | Kotlin + Jetpack Compose (Material3 + Custom Liquid Glass Design System), Compile SDK 35, Min SDK 26, Gradle 8.7 (`apps/android`), In-app Update Center in `SettingsScreen.kt` |
| **UPDATE PIPELINE** | Fully automated: Authoritative `version.json` + `scripts/sync-version.ps1`, HTTPS Update Manifest (`update-manifest.json`), `KtmUpdateService.cs` (background/manual checks, rate limiting, resumable HTTP download, live progress/speed/ETA, SHA-256 validation, active transfer protection), Windows Update Center UI in `MainWindow.xaml` |
| **USER DATA LOCATION** | Windows: `%LOCALAPPDATA%\KnowToMigrate\` (`settings.json`, `history.json`, `trusted_devices.json`, `window_state.json`, `logs\update.log`, `Updates\pending\`, `Updates\backup\`) — completely isolated from `Program Files` and preserved across all upgrades/reinstalls.<br>Android: Internal Storage (`context.filesDir`) & DataStore Preferences |
| **RELEASE ARTIFACT LOCATION** | Local: `c:\KnowToMigrate\releases\windows\` and `c:\KnowToMigrate\releases\android\`<br>Web: `https://knowtomigrate.web.app/download/` and `https://knowtomigrate.web.app/update-manifest.json` |
| **VERIFICATION STATUS** | 1. `KnowToMigrate.csproj`: Compiled & published cleanly (0 errors).<br>2. `KnowToMigrate.Updater.exe`: Compiled cleanly (55 KB native binary).<br>3. `Package.wxs`: Built `KnowToMigrate-1.0.0-x64.msi` (63.0 MB).<br>4. `KnowToMigrate-Setup.exe`: Built cleanly (63.1 MB).<br>5. Android Kotlin compilation: Passed cleanly (0 errors).<br>6. Firebase Hosting: Deployed live to `https://knowtomigrate.web.app`.<br>7. Security tests (`scripts/test-update-system.ps1`): Passed (corrupt SHA-256 rejected, user data preserved). |

---

## Architecture: Seamless In-Place Update Pipeline

```
KnowToMigrate (v1.0.0)
   │
   ├─► Background / Manual Check: GET https://knowtomigrate.web.app/update-manifest.json
   │
   ├─► Update Detected: Displays Update Center card with release notes & download size
   │
   ├─► [Update Now] selected:
   │      │
   │      ├─ Check if Pluto Engine is actively transferring (if active: prompt [Update After Transfer])
   │      │
   │      ├─ Resumable / secure download to %LOCALAPPDATA%\KnowToMigrate\Updates\pending\
   │      │
   │      ├─ Real-time download metrics: % progress, current MB/s, ETA
   │      │
   │      ├─ Cryptographic Verification:
   │      │    • Exact file size verification
   │      │    • SHA-256 hash comparison against manifest
   │      │    • Digital signature check (where applicable)
   │      │    • Anti-downgrade check
   │      │
   │      └─ Spawn KnowToMigrate.Updater.exe from %TEMP%
   │
   ├─► KnowToMigrate.exe terminates gracefully
   │
   ├─► KnowToMigrate.Updater.exe:
   │      │
   │      ├─ Waits for parent PID to exit
   │      ├─ Executes WiX MajorUpgrade: msiexec /i <package.msi> /qn (or Setup.exe)
   │      ├─ User data in %LOCALAPPDATA%\KnowToMigrate remains 100% untouched
   │      ├─ Restores previous version if install fails (rollback)
   │      └─ Launches updated KnowToMigrate.exe
   │
   └─► KnowToMigrate restarts:
          • Displays "Update Complete — KnowToMigrate v1.0.1"
          • All previous settings, history, and trusted devices preserved
          • Normal transfer functionality ready
```
