# KnowToMigrate — Build, Packaging & Release Guide

This document contains the exact reproducible build commands, prerequisites, and verification steps for all KnowToMigrate production targets.

---

## 1. Windows Installer & Application Window Verification Matrix

| Verification Item | Status | Details / Implementation |
| :--- | :---: | :--- |
| **Installer Framework** | **PASS** | WiX Toolset v4 (v4.0.6), pure 64-bit (`x64;0`) MSI package |
| **Install Directory** | **PASS** | `C:\Program Files\KnowToMigrate\` (stable, standard 64-bit Program Files) |
| **EXE Executable** | **PASS** | `KnowToMigrate.exe` (72.2 MB, single-file bundle compressed, embedded icon, DPI-aware manifest) |
| **MSI Package** | **PASS** | `KnowToMigrate-1.0.0-x64.msi` (65.9 MB, perMachine, embedded CAB) |
| **Start Menu Shortcut** | **PASS** | `C:\ProgramData\Microsoft\Windows\Start Menu\Programs\KnowToMigrate.lnk` pointing to `C:\Program Files\KnowToMigrate\KnowToMigrate.exe` with `AppUserModel.ID = KnowToMigrate.App` |
| **Desktop Shortcut** | **PASS** | `C:\Users\Public\Desktop\KnowToMigrate.lnk` pointing to `C:\Program Files\KnowToMigrate\KnowToMigrate.exe` with `AppUserModel.ID = KnowToMigrate.App` |
| **Windows Search** | **PASS** | Non-advertised `.lnk` shortcut pointing directly to `[#KnowToMigrateEXE]` allows Windows Search Indexer to read the embedded icon from `KnowToMigrate.exe`, displaying "KnowToMigrate App" with master icon |
| **Application Icon** | **PASS** | Multi-resolution `.ico` (16, 24, 32, 48, 64, 128, 256) embedded in `KnowToMigrate.exe`, referencing approved master logo with 15% rounded corners |
| **Taskbar Icon** | **PASS** | Process registers `SetCurrentProcessExplicitAppUserModelID("KnowToMigrate.App")` on startup, linking taskbar instance to Start Menu shortcut |
| **Title Bar & Controls** | **PASS** | AMOLED black (`#070707`) 42px header with master brand badge, draggable zone, and native Minimize, Maximize/Restore toggle, and Close with red hover |
| **Resize & Move** | **PASS** | Native `WindowChrome` with 6px resize border on all 4 edges and corners; native double-click maximize/restore |
| **Taskbar WorkArea** | **PASS** | Maximizing respects Windows Taskbar completely, never extending underneath or obscuring taskbar |
| **Responsive Content** | **PASS** | Dynamic `ScrollViewer` container prevents clipping on 1280x720, 1366x768, and High-DPI (125%-200%) displays |
| **Installed Apps** | **PASS** | Registered in `HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall` as "KnowToMigrate" by "KnowToMigrate Team" with `ARPPRODUCTICON` |
| **Uninstall** | **PASS** | Standard MSI uninstallation cleanly removes binaries, Start Menu shortcut, Desktop shortcut, and registry entries |
| **Upgrade** | **PASS** | `MajorUpgrade` configured with `AllowSameVersionUpgrades="yes"` and `Schedule="afterInstallInitialize"`, preventing duplicate shortcuts |
| **Reboot Test** | **PASS** | Hardened `.lnk` targets and `App Paths` registry entries remain permanent across reboots |
| **Known Issues** | **None** | All previous Darwin advertised shortcut and window chrome issues resolved |

---

## 2. Windows Standalone Executable (.EXE) Build

Builds a self-contained, single-file compressed executable containing all runtime components and native FFI libraries:

```powershell
# Navigate to project root
cd c:\KnowToMigrate

# Publish compressed single-file Windows x64 binary
C:\Users\gokul\.dotnet\dotnet.exe publish apps\windows-wpf\KnowToMigrate.csproj `
    -c Release `
    -r win-x64 `
    --self-contained true `
    -p:PublishSingleFile=true `
    -p:EnableCompressionInSingleFile=true `
    -p:IncludeNativeLibrariesForSelfExtract=true `
    -o releases\windows\publish

# Copy to final releases folder
Copy-Item "releases\windows\publish\KnowToMigrate.exe" "releases\windows\KnowToMigrate-1.0.0-x64.exe" -Force

# Verify file hash and size
Get-FileHash "releases\windows\KnowToMigrate-1.0.0-x64.exe" -Algorithm SHA256
```

- **Output Artifact**: `releases\windows\KnowToMigrate-1.0.0-x64.exe` (72.2 MB)
- **SHA-256**: `CBA232E9796551A3977B9CEEA4B9AA37D55D921527A97A7DD055590867AEB993`

---

## 3. Windows Standalone MSI Installer (.MSI) Build

Builds a native 64-bit per-machine Windows Installer package with embedded CAB via WiX Toolset v4:

```powershell
# Navigate to project root
cd c:\KnowToMigrate

# Build native 64-bit MSI installer
wix build -arch x64 Package.wxs -o releases\windows\KnowToMigrate-1.0.0-x64.msi

# Clean up symbol databases
Remove-Item -Force "releases\windows\*.wixpdb" -ErrorAction SilentlyContinue

# Verify file hash and size
Get-FileHash "releases\windows\KnowToMigrate-1.0.0-x64.msi" -Algorithm SHA256
```

- **Output Artifact**: `releases\windows\KnowToMigrate-1.0.0-x64.msi` (65.9 MB)
- **SHA-256**: `35C92C4EE8570415F5CE20805A66E3947F35950E8A3B450192785ED8ECD3A37A`

---

## 4. Android APK Package (.APK) Build

Compiles native Rust core via CMake, processes Hilt dependency injection, compiles Jetpack Compose UI, and produces the signed production APK:

```powershell
# Set Java and Android SDK paths
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:ANDROID_HOME = "C:\Users\gokul\android-sdk"
cd apps\android

# Execute Gradle assemble
.\gradlew.bat :app:assembleRelease --quiet

# Verify file hash and size
Get-FileHash "app\build\outputs\apk\release\app-release.apk" -Algorithm SHA256
```

- **Output Artifact**: `apps\android\app\build\outputs\apk\release\app-release.apk` (11.36 MB)
- **SHA-256**: `602A399D1D090E2A39B05D1FF928EEAF99F0E0C0BBAAA23B583BC4D5B6EE43DD`

---

## 5. Website Build & Firebase Hosting Deployment

Builds the production Vite bundle and deploys live to Firebase Hosting:

```powershell
# Build website distribution
cd c:\KnowToMigrate\apps\website
npm run build

# Deploy to Firebase Hosting
cd c:\KnowToMigrate
npx firebase deploy --only hosting
```

- **Live URL**: [https://knowtomigrate.web.app](https://knowtomigrate.web.app)
- **Direct Downloads**:
  - `https://knowtomigrate.web.app/download/KnowToMigrate-1.0.0-x64.msi`
  - `https://knowtomigrate.web.app/download/KnowToMigrate-1.0.0-x64.exe`
  - `https://knowtomigrate.web.app/download/KnowToMigrate-1.0.0.apk`

---

## 6. Automated Diagnostic Transfer Suite

Validates file transfer integrity, folder hierarchies, and multi-transport capability selection (Wi-Fi LAN, Wi-Fi Direct, Bluetooth):

```powershell
cd c:\KnowToMigrate
C:\Users\gokul\.dotnet\dotnet.exe run --project tests\transfer-test\transfer-test.csproj
```
Expected output:
```text
>>> ALL 5 TESTS (TRANSFERS + MULTI-TRANSPORT CAPABILITY PIPELINE) PASSED SUCCESSFULLY! <<<
```
