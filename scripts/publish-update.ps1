# Production Release & Seamless Auto-Update Pipeline for KnowToMigrate
# Builds Windows binaries, updates checksums, publishes to website, deploys to Firebase, and pushes to GitHub.
param(
    [string]$TargetVersion = "",
    [string[]]$ReleaseNotes = @()
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Get-Item $PSScriptRoot).Parent.FullName
Set-Location $RepoRoot

$DotnetExe = "C:\Users\gokul\.dotnet\dotnet.exe"
$CscExe = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
$WixExe = "C:\Users\gokul\.dotnet\tools\wix.exe"

if (!(Test-Path $DotnetExe)) { Write-Error ".NET SDK executable not found at $DotnetExe" }
if (!(Test-Path $CscExe)) { Write-Error "CSC compiler not found at $CscExe" }
if (!(Test-Path $WixExe)) { Write-Error "WiX tool not found at $WixExe" }

function Write-Utf8NoBom {
    param([string]$Path, [string]$Content)
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
}

# 1. Determine Target Version
$versionJsonPath = Join-Path $RepoRoot "version.json"
$vConfig = Get-Content $versionJsonPath | ConvertFrom-Json
$currentVer = $vConfig.version

if ([string]::IsNullOrWhiteSpace($TargetVersion)) {
    $parts = $currentVer.Split('.')
    if ($parts.Length -ge 3) {
        $patch = [int]$parts[2] + 1
        $TargetVersion = "$($parts[0]).$($parts[1]).$patch"
    } else {
        $TargetVersion = "1.0.1"
    }
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "KNOWTOMIGRATE PRODUCTION RELEASE & AUTO-UPDATE PIPELINE" -ForegroundColor Cyan
Write-Host "Upgrading from v$currentVer -> v$TargetVersion" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# 2. Sync version across all project files
Write-Host "`n[1/7] Synchronizing version numbers to v$TargetVersion..." -ForegroundColor Green
& (Join-Path $RepoRoot "scripts\sync-version.ps1") -TargetVersion $TargetVersion

# 3. Publish Windows WPF Application (Self-contained single-file x64)
Write-Host "`n[2/7] Publishing Windows WPF Application (x64)..." -ForegroundColor Green
$publishDir = Join-Path $RepoRoot "releases\windows\publish"
if (!(Test-Path $publishDir)) { New-Item -ItemType Directory -Path $publishDir -Force | Out-Null }

$wpfProj = Join-Path $RepoRoot "apps\windows-wpf\KnowToMigrate.csproj"
& $DotnetExe publish $wpfProj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:EnableCompressionInSingleFile=true -o $publishDir
if ($LASTEXITCODE -ne 0) { Write-Error "WPF application publish failed with exit code $LASTEXITCODE" }

# 4. Compile Standalone Native C# Updater (55 KB zero-dependency WinForms)
Write-Host "`n[3/7] Compiling Native C# Updater..." -ForegroundColor Green
$appIcon = Join-Path $RepoRoot "apps\windows-wpf\Assets\KnowToMigrate.ico"
$updaterSrc = Join-Path $RepoRoot "apps\windows-updater\UpdaterMain.cs"
$updaterManifest = Join-Path $RepoRoot "apps\windows-updater\app.manifest"
$updaterOut = Join-Path $publishDir "KnowToMigrate.Updater.exe"
$logoPng = Join-Path $RepoRoot "apps\windows-wpf\Assets\logo.png"
& $CscExe /target:winexe /platform:x64 /optimize+ /win32manifest:$updaterManifest /win32icon:$appIcon /resource:"$logoPng,logo.png" /out:$updaterOut $updaterSrc
if ($LASTEXITCODE -ne 0) { Write-Error "Updater compilation failed with exit code $LASTEXITCODE" }

# 5. Build WiX MSI Package & Setup Bootstrapper
Write-Host "`n[4/7] Building WiX MSI Package & Setup Bootstrapper..." -ForegroundColor Green
$msiOut = Join-Path $RepoRoot "releases\windows\KnowToMigrate-$TargetVersion-x64.msi"
& $WixExe build (Join-Path $RepoRoot "Package.wxs") -o $msiOut
if ($LASTEXITCODE -ne 0) { Write-Error "WiX MSI build failed with exit code $LASTEXITCODE" }

$setupSrc = Join-Path $RepoRoot "apps\windows-setup\SetupBootstrapper.cs"
$setupManifest = Join-Path $RepoRoot "apps\windows-setup\app.manifest"
$setupOut = Join-Path $RepoRoot "releases\windows\KnowToMigrate-Setup.exe"
& $CscExe /target:winexe /platform:x64 /optimize+ /win32manifest:$setupManifest /win32icon:$appIcon /resource:"$msiOut,Payload.msi" /out:$setupOut $setupSrc
if ($LASTEXITCODE -ne 0) { Write-Error "Setup bootstrapper build failed with exit code $LASTEXITCODE" }

# Also keep standalone EXE in releases/windows/
$standaloneOut = Join-Path $RepoRoot "releases\windows\KnowToMigrate.exe"
Copy-Item -Path (Join-Path $publishDir "KnowToMigrate.exe") -Destination $standaloneOut -Force

# 6. Calculate Cryptographic SHA-256 and Byte Sizes
Write-Host "`n[5/7] Computing SHA-256 Hashes and Staging Website Downloads..." -ForegroundColor Green
$setupHash = (Get-FileHash -Path $setupOut -Algorithm SHA256).Hash.ToLowerInvariant()
$setupSize = (Get-Item $setupOut).Length

$msiHash = (Get-FileHash -Path $msiOut -Algorithm SHA256).Hash.ToLowerInvariant()
$msiSize = (Get-Item $msiOut).Length

$standaloneHash = (Get-FileHash -Path $standaloneOut -Algorithm SHA256).Hash.ToLowerInvariant()
$standaloneSize = (Get-Item $standaloneOut).Length

$webDownloadDir = Join-Path $RepoRoot "apps\website\public\download"
if (!(Test-Path $webDownloadDir)) { New-Item -ItemType Directory -Path $webDownloadDir -Force | Out-Null }

Copy-Item -Path $setupOut -Destination (Join-Path $webDownloadDir "KnowToMigrate-Setup.exe.bin") -Force
Copy-Item -Path $msiOut -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion-x64.msi.bin") -Force
Copy-Item -Path $standaloneOut -Destination (Join-Path $webDownloadDir "KnowToMigrate.exe.bin") -Force
Copy-Item -Path $standaloneOut -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion-x64.exe.bin") -Force

Write-Host "  Setup EXE:   $setupSize bytes | SHA256: $setupHash" -ForegroundColor Gray
Write-Host "  WiX MSI:     $msiSize bytes | SHA256: $msiHash" -ForegroundColor Gray
Write-Host "  Standalone:  $standaloneSize bytes | SHA256: $standaloneHash" -ForegroundColor Gray

# 6. Build and Stage Android APK
Write-Host "`nBuilding Android APK..." -ForegroundColor Green
$androidDir = Join-Path $RepoRoot "apps\android"
Push-Location $androidDir
& .\gradlew.bat assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Warning "assembleRelease failed, building assembleDebug instead..."
    & .\gradlew.bat assembleDebug
}
Pop-Location

$apkSrc = Join-Path $androidDir "app\build\outputs\apk\release\app-release.apk"
if (!(Test-Path $apkSrc)) {
    $apkSrc = Join-Path $androidDir "app\build\outputs\apk\debug\app-debug.apk"
}

$apkHash = ""
$apkSize = 0
if (Test-Path $apkSrc) {
    $apkHash = (Get-FileHash -Path $apkSrc -Algorithm SHA256).Hash.ToLowerInvariant()
    $apkSize = (Get-Item $apkSrc).Length
    Copy-Item -Path $apkSrc -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion.apk.bin") -Force
    Copy-Item -Path $apkSrc -Destination (Join-Path $webDownloadDir "KnowToMigrate.apk.bin") -Force
    Write-Host "  Android APK: $apkSize bytes | SHA256: $apkHash" -ForegroundColor Gray
}

# 7. Update update-manifest.json and Website DownloadPage
Write-Host "`n[6/7] Updating update-manifest.json and DownloadPage.tsx..." -ForegroundColor Green
$manifestPath = Join-Path $RepoRoot "apps\website\public\update-manifest.json"
$manifest = Get-Content $manifestPath | ConvertFrom-Json

$manifest.version = $TargetVersion
$manifest.publishedAt = [DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ")
$manifest.windows.version = $TargetVersion
$manifest.windows.size = $setupSize
$manifest.windows.sha256 = $setupHash
$manifest.windows.msi.filename = "KnowToMigrate-$TargetVersion-x64.msi"
$manifest.windows.msi.url = "https://knowtomigrate.web.app/download/KnowToMigrate-$TargetVersion-x64.msi.bin"
$manifest.windows.msi.size = $msiSize
$manifest.windows.msi.sha256 = $msiHash
$manifest.windows.standalone.size = $standaloneSize
$manifest.windows.standalone.sha256 = $standaloneHash

if ($apkSize -gt 0) {
    if (!$manifest.android) {
        $manifest | Add-Member -MemberType NoteProperty -Name "android" -Value (@{})
    }
    $vParts = $TargetVersion.Split('.')
    $manifest.android.versionCode = [int]$vParts[0] * 10000 + [int]$vParts[1] * 100 + [int]$vParts[2]
    $manifest.android.versionName = $TargetVersion
    $manifest.android.size = $apkSize
    $manifest.android.sha256 = $apkHash
    $manifest.android.url = "https://knowtomigrate.web.app/download/KnowToMigrate-$TargetVersion.apk.bin"
    $manifest.android.filename = "KnowToMigrate-$TargetVersion.apk"
}

if ($ReleaseNotes.Length -gt 0) {
    $manifest.releaseNotes = $ReleaseNotes
} else {
    $manifest.releaseNotes = @(
        "Fixed file verification and byte alignment in Pluto transfer engine",
        "Independent Android APK Self-Updater with UTF-8 BOM sanitation and SHA-256 verification",
        "Liquid Glass navigation bar and responsive non-clipping action button",
        "Persistent Migration Ledger with cryptographic audit log, export, and quick folder reveal",
        "Universal Web Share with live QR code pairing and seamless multi-device browser transfers",
        "Windows Security Center real-time socket telemetry and Pluto Auto diagnostics",
        "Liquid Glass AMOLED custom CheckBox unique UI upgrade"
    )
}

Write-Utf8NoBom -Path $manifestPath -Content ($manifest | ConvertTo-Json -Depth 6)

# Sync Android stable update manifest
$androidStablePath = Join-Path $RepoRoot "apps\website\public\updates\android\stable.json"
if (Test-Path $androidStablePath) {
    $as = Get-Content $androidStablePath | ConvertFrom-Json
    $as.versionName = $TargetVersion
    $parts = $TargetVersion.Split('.')
    if ($parts.Length -ge 3) {
        $as.versionCode = [int]$parts[0] * 10000 + [int]$parts[1] * 100 + [int]$parts[2]
    }
    $as.releaseDate = (Get-Date).ToString("yyyy-MM-dd")
    $as.title = "KnowToMigrate $TargetVersion"
    $as.releaseNotes = $manifest.releaseNotes
    if ($apkSize -gt 0) {
        $as.sizeBytes = $apkSize
        $as.sha256 = $apkHash
        $as.apkUrl = "https://knowtomigrate.web.app/download/KnowToMigrate-$TargetVersion.apk.bin"
    }
    Write-Utf8NoBom -Path $androidStablePath -Content ($as | ConvertTo-Json -Depth 6)
    Write-Host "  Updated $androidStablePath (UTF-8 without BOM)" -ForegroundColor Gray
}

# Update DownloadPage.tsx with latest version, hashes, sizes, and release date
$downloadPagePath = Join-Path $RepoRoot "apps\website\src\pages\DownloadPage.tsx"
if (Test-Path $downloadPagePath) {
    $todayFormatted = (Get-Date).ToString("dd MMMM yyyy", [System.Globalization.CultureInfo]::InvariantCulture)
    $dpContent = Get-Content $downloadPagePath -Raw
    $dpContent = [regex]::Replace($dpContent, "export const RELEASE_DATE = '[^']*'", "export const RELEASE_DATE = '$todayFormatted'")
    $dpContent = [regex]::Replace($dpContent, "const RELEASE_DATE = '[^']*'", "const RELEASE_DATE = '$todayFormatted'")
    $dpContent = [regex]::Replace($dpContent, "v\d+\.\d+\.\d+", "v$TargetVersion")
    $dpContent = [regex]::Replace($dpContent, "version:\s*'\d+\.\d+\.\d+'", "version: '$TargetVersion'")
    $dpContent = [regex]::Replace($dpContent, "KnowToMigrate-\d+\.\d+\.\d+-x64\.msi", "KnowToMigrate-$TargetVersion-x64.msi")
    Set-Content -Path $downloadPagePath -Value $dpContent -NoNewline
    Write-Host "  Updated $downloadPagePath with release date ($todayFormatted) and v$TargetVersion" -ForegroundColor Gray
}

# 8. Build Website & Deploy to Firebase
Write-Host "`n[7/7] Building Website and Deploying to Firebase Hosting..." -ForegroundColor Green
Set-Location (Join-Path $RepoRoot "apps\website")
npm run build
if ($LASTEXITCODE -ne 0) { Write-Error "Website build failed with exit code $LASTEXITCODE" }

firebase deploy --only hosting
if ($LASTEXITCODE -ne 0) { Write-Error "Firebase deployment failed with exit code $LASTEXITCODE" }

Set-Location $RepoRoot

# 9. Commit & Push to GitHub
Write-Host "`n[Done] Committing and pushing release v$TargetVersion to GitHub..." -ForegroundColor Green
git add -A
git commit -m "release: KnowToMigrate v$TargetVersion - Universal Clipboard, Smart Duplicates, Migration Target Picker"
git push origin main

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "RELEASE v$TargetVersion SUCCESSFULLY PUBLISHED AND DEPLOYED!" -ForegroundColor Green
Write-Host "Any KnowToMigrate instance checking for updates will now detect v$TargetVersion." -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
