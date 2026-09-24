# KnowToMigrate — Build, Packaging & Release Guide

This document contains the exact reproducible build commands, prerequisites, and verification steps for all KnowToMigrate production targets.

---

## 1. Prerequisites & Toolchains

| Toolchain | Minimum Version | Path / Verification Command |
| :--- | :--- | :--- |
| **.NET SDK** | 8.0.400+ | `C:\dotnet\dotnet.exe --version` |
| **WiX Toolset** | v4.0.0+ | `C:\Users\gokul\.dotnet\tools\wix.exe --version` |
| **Android SDK** | API 35 (Build-tools 35.0.0) | `C:\Users\gokul\android-sdk` |
| **Java JDK** | OpenJDK 17.0.12+ | `java -version` |
| **Node.js** | v20.0.0+ (LTS) | `node -v` |
| **Firebase CLI** | v13.0.0+ | `npx firebase-tools --version` |

---

## 2. Windows Standalone Executable (.EXE)

Builds a self-contained, single-file compressed executable containing all runtime components:

```powershell
# Navigate to project root
cd c:\KnowToMigrate

# Publish compressed single-file Windows x64 binary
C:\dotnet\dotnet.exe publish c:\KnowToMigrate\apps\windows-wpf\KnowToMigrate.csproj `
    -c Release `
    -r win-x64 `
    --self-contained true `
    -p:PublishSingleFile=true `
    -p:EnableCompressionInSingleFile=true `
    -p:IncludeNativeLibrariesForSelfExtract=true `
    -o c:\KnowToMigrate\releases\windows_build

# Copy to final releases folder
Copy-Item "c:\KnowToMigrate\releases\windows_build\KnowToMigrate.exe" "c:\KnowToMigrate\releases\windows\KnowToMigrate-1.0.0-x64.exe" -Force
Remove-Item -Recurse -Force "c:\KnowToMigrate\releases\windows_build"

# Verify file hash and size
Get-FileHash "c:\KnowToMigrate\releases\windows\KnowToMigrate-1.0.0-x64.exe" -Algorithm SHA256
```

- **Output Artifact**: `c:\KnowToMigrate\releases\windows\KnowToMigrate-1.0.0-x64.exe` (68.7 MB)
- **SHA-256**: `2B888126CE08F5C8F52BD7CAC775DA2C6FCA25475F1B29CA9B3609F367DAADE9`

---

## 3. Windows Standalone MSI Installer (.MSI)

Builds a per-machine Windows Installer package with embedded CAB via WiX Toolset v4:

```powershell
# Navigate to project root
cd c:\KnowToMigrate

# Build standalone MSI installer
C:\Users\gokul\.dotnet\tools\wix.exe build c:\KnowToMigrate\Package.wxs -o c:\KnowToMigrate\releases\windows\KnowToMigrate-1.0.0-x64.msi

# Clean up symbol databases
Remove-Item -Force "c:\KnowToMigrate\releases\windows\*.wixpdb" -ErrorAction SilentlyContinue

# Verify file hash and size
Get-FileHash "c:\KnowToMigrate\releases\windows\KnowToMigrate-1.0.0-x64.msi" -Algorithm SHA256
```

- **Output Artifact**: `c:\KnowToMigrate\releases\windows\KnowToMigrate-1.0.0-x64.msi` (62.7 MB)
- **SHA-256**: `28F8A7D4D41024762D88DB24DD27AA3F1A14A37E2B1B483AF30A033474557BDF`

---

## 4. Android APK Package (.APK)

Compiles native C++ libraries for `arm64-v8a` and `x86_64`, processes Hilt dependency injection, compiles Jetpack Compose UI, and produces the debug/release APK:

```powershell
# Navigate to Android directory
cd c:\KnowToMigrate\apps\android

# Execute Gradle assemble
.\gradlew.bat assembleDebug

# Copy output APK to distribution folder
Copy-Item "c:\KnowToMigrate\apps\android\app\build\outputs\apk\debug\app-debug.apk" "c:\KnowToMigrate\releases\android\KnowToMigrate-1.0.0.apk" -Force

# Verify file hash and size
Get-FileHash "c:\KnowToMigrate\releases\android\KnowToMigrate-1.0.0.apk" -Algorithm SHA256
```

- **Output Artifact**: `c:\KnowToMigrate\releases\android\KnowToMigrate-1.0.0.apk` (17.8 MB)
- **SHA-256**: `239D0A3FD506EAFE5A09ECC92F7E4815171B197C55A95B7BB73FC14A2FD1EADA`

---

## 5. Website Build & Firebase Hosting Deployment

Builds the production Vite bundle and deploys to Firebase Hosting with custom domain direct download redirects:

```powershell
# Build website distribution
cd c:\KnowToMigrate\apps\website
npm run build

# Deploy to Firebase Hosting
cd c:\KnowToMigrate
npx firebase-tools deploy --only hosting --non-interactive
```

- **Live URL**: [https://knowtomigrate.web.app](https://knowtomigrate.web.app)
- **Direct Downloads**:
  - `https://knowtomigrate.web.app/download/KnowToMigrate-1.0.0-x64.msi`
  - `https://knowtomigrate.web.app/download/KnowToMigrate-1.0.0-x64.exe`
  - `https://knowtomigrate.web.app/download/KnowToMigrate-1.0.0.apk`

---

## 6. Running the Automated Integration Stress Test Suite

Validates path traversal protection, 100 MB large file transfers, folder trees, and network interruption/resume:

```powershell
cd c:\KnowToMigrate\tests\KtmIntegrationTests
C:\dotnet\dotnet.exe run -c Release
```
Expected output:
```text
============================================================
ALL TESTS PASSED (4/4) - PRODUCTION READY!
============================================================
```
