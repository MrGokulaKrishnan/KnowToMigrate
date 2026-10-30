# Production Release & Seamless Auto-Update Pipeline for KnowToMigrate
# Builds Windows binaries, updates checksums, stages binaries to website, and deploys to Firebase.

param(
    [string]$TargetVersion = "",
    [string[]]$ReleaseNotes = @(),
    [switch]$DeployWebsite = $true,
    [switch]$PushToGit = $false
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Get-Item $PSScriptRoot).Parent.FullName
Set-Location $RepoRoot

$DotnetExe = "C:\Users\gokul\.dotnet\dotnet.exe"
$WixExe = "C:\Users\gokul\.dotnet\tools\wix.exe"

if (!(Test-Path $DotnetExe)) { Write-Error ".NET SDK executable not found at $DotnetExe" }
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
    $TargetVersion = $currentVer
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "KNOWTOMIGRATE PRODUCTION RELEASE & AUTO-UPDATE PIPELINE" -ForegroundColor Cyan
Write-Host "Packaging Version: v$TargetVersion" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# 2. Sync version across all project files
Write-Host "`n[1/7] Synchronizing version numbers to v$TargetVersion..." -ForegroundColor Green
& (Join-Path $RepoRoot "scripts\sync-version.ps1") -TargetVersion $TargetVersion

# 3. Publish Windows WPF Application (Self-contained single-file x64)
Write-Host "`n[2/7] Publishing Windows WPF Application (Self-contained single-file x64)..." -ForegroundColor Green
$publishDir = Join-Path $RepoRoot "releases\windows\publish"
if (Test-Path $publishDir) {
    Remove-Item -Path $publishDir -Recurse -Force
}
New-Item -ItemType Directory -Path $publishDir -Force | Out-Null

$wpfProj = Join-Path $RepoRoot "apps\windows-wpf\KnowToMigrate.csproj"
& $DotnetExe publish $wpfProj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeAllContentForSelfExtract=true -p:EnableCompressionInSingleFile=true -o $publishDir
if ($LASTEXITCODE -ne 0) { Write-Error "WPF application publish failed with exit code $LASTEXITCODE" }

$mainExePath = Join-Path $publishDir "KnowToMigrate.exe"
if (!(Test-Path $mainExePath)) {
    Write-Error "KnowToMigrate.exe was not produced in $publishDir"
}
$mainExeSize = (Get-Item $mainExePath).Length
if ($mainExeSize -lt 20MB) {
    Write-Error "KnowToMigrate.exe is only $mainExeSize bytes. Single-file self-contained build expected tens of MBs!"
}
Write-Host "  KnowToMigrate.exe successfully built: $([math]::Round($mainExeSize / 1MB, 2)) MB" -ForegroundColor Green

$ktmFfiDll = Join-Path $RepoRoot "core\target\release\ktm_ffi.dll"
if (Test-Path $ktmFfiDll) {
    Copy-Item $ktmFfiDll (Join-Path $publishDir "ktm_ffi.dll") -Force
}

# 4. Compile Standalone Native C# Updater (.NET 8 self-contained)
Write-Host "`n[3/7] Publishing Native C# Updater..." -ForegroundColor Green
$updaterProj = Join-Path $RepoRoot "apps\windows-updater\KnowToMigrate.Updater.csproj"
& $DotnetExe publish $updaterProj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:EnableCompressionInSingleFile=true -o $publishDir
if ($LASTEXITCODE -ne 0) { Write-Error "Updater compilation failed with exit code $LASTEXITCODE" }

# 5. Build WiX MSI Package & Setup Bootstrapper
Write-Host "`n[4/7] Building WiX MSI Package & Setup Bootstrapper (x64)..." -ForegroundColor Green
$msiOut = Join-Path $RepoRoot "releases\windows\KnowToMigrate-$TargetVersion-x64.msi"

& $WixExe build (Join-Path $RepoRoot "Package.wxs") -arch x64 -o $msiOut
if ($LASTEXITCODE -ne 0) { Write-Error "WiX MSI build failed with exit code $LASTEXITCODE" }

$msiSize = (Get-Item $msiOut).Length
if ($msiSize -lt 20MB) {
    Write-Error "MSI package is only $msiSize bytes. Expected self-contained 64-bit payload!"
}
Write-Host "  KnowToMigrate-$TargetVersion-x64.msi built: $([math]::Round($msiSize / 1MB, 2)) MB" -ForegroundColor Green

$setupProj = Join-Path $RepoRoot "apps\windows-setup\KnowToMigrate-Setup.csproj"
& $DotnetExe publish $setupProj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:EnableCompressionInSingleFile=true -o (Join-Path $RepoRoot "releases\windows")
if ($LASTEXITCODE -ne 0) { Write-Error "Setup bootstrapper build failed with exit code $LASTEXITCODE" }

$setupOut = Join-Path $RepoRoot "releases\windows\KnowToMigrate-Setup.exe"
$setupSize = (Get-Item $setupOut).Length
if ($setupSize -lt 20MB) {
    Write-Error "Setup installer is only $setupSize bytes. Expected self-contained MSI payload!"
}
Write-Host "  KnowToMigrate-Setup.exe built: $([math]::Round($setupSize / 1MB, 2)) MB" -ForegroundColor Green

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

Copy-Item -Path $setupOut -Destination (Join-Path $webDownloadDir "KnowToMigrate-Setup.exe") -Force
Copy-Item -Path $setupOut -Destination (Join-Path $webDownloadDir "KnowToMigrate-Setup.exe.bin") -Force
Copy-Item -Path $msiOut -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion-x64.msi") -Force
Copy-Item -Path $msiOut -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion-x64.msi.bin") -Force
Copy-Item -Path $standaloneOut -Destination (Join-Path $webDownloadDir "KnowToMigrate.exe") -Force
Copy-Item -Path $standaloneOut -Destination (Join-Path $webDownloadDir "KnowToMigrate.exe.bin") -Force
Copy-Item -Path $standaloneOut -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion-x64.exe") -Force
Copy-Item -Path $standaloneOut -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion-x64.exe.bin") -Force

$releaseBinDir = Join-Path $RepoRoot "release-binaries"
if (!(Test-Path $releaseBinDir)) { New-Item -ItemType Directory -Path $releaseBinDir -Force | Out-Null }
Copy-Item -Path $setupOut -Destination (Join-Path $releaseBinDir "KnowToMigrate-Setup.exe") -Force
Copy-Item -Path $msiOut -Destination (Join-Path $releaseBinDir "KnowToMigrate-$TargetVersion-x64.msi") -Force
Copy-Item -Path $standaloneOut -Destination (Join-Path $releaseBinDir "KnowToMigrate-Portable.exe") -Force
Copy-Item -Path $standaloneOut -Destination (Join-Path $releaseBinDir "KnowToMigrate-$TargetVersion-x64.exe") -Force

Write-Host "  Setup EXE:   $setupSize bytes | SHA256: $setupHash" -ForegroundColor Gray
Write-Host "  WiX MSI:     $msiSize bytes | SHA256: $msiHash" -ForegroundColor Gray
Write-Host "  Standalone:  $standaloneSize bytes | SHA256: $standaloneHash" -ForegroundColor Gray

# 7. Build and Stage Android APK
Write-Host "`nBuilding Android APK..." -ForegroundColor Green
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$androidDir = Join-Path $RepoRoot "apps\android"
Push-Location $androidDir
& .\gradlew.bat assembleDebug --no-daemon
Pop-Location

$apkSrc = Join-Path $androidDir "app\build\outputs\apk\debug\app-debug.apk"
$apkHash = ""
$apkSize = 0

if (Test-Path $apkSrc) {
    $apkHash = (Get-FileHash -Path $apkSrc -Algorithm SHA256).Hash.ToLowerInvariant()
    $apkSize = (Get-Item $apkSrc).Length

    Copy-Item -Path $apkSrc -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion.apk") -Force
    Copy-Item -Path $apkSrc -Destination (Join-Path $webDownloadDir "KnowToMigrate.apk") -Force
    Copy-Item -Path $apkSrc -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion.apk.bin") -Force
    Copy-Item -Path $apkSrc -Destination (Join-Path $webDownloadDir "KnowToMigrate.apk.bin") -Force
    Copy-Item -Path $apkSrc -Destination (Join-Path $releaseBinDir "KnowToMigrate-$TargetVersion.apk") -Force
    Copy-Item -Path $apkSrc -Destination (Join-Path $releaseBinDir "KnowToMigrate.apk") -Force
    Copy-Item -Path $apkSrc -Destination (Join-Path $releaseBinDir "KnowToMigrate-Debug.apk") -Force

    Write-Host "  Android APK: $apkSize bytes | SHA256: $apkHash" -ForegroundColor Gray
}

# 8. Regenerate every site/manifest reference from version.json and the .bin artifacts
Write-Host "`n[6/7] Generating release data (manifests, website data, Firebase routes)..." -ForegroundColor Green
node (Join-Path $RepoRoot "scripts\release\site-release.mjs")
if ($LASTEXITCODE -ne 0) { throw "site-release generation failed; nothing will be deployed" }

# 9. Build Website & Deploy to Firebase
Write-Host "`n[7/7] Building Website..." -ForegroundColor Green
Set-Location (Join-Path $RepoRoot "apps\website")
npm run build
if ($LASTEXITCODE -ne 0) { Write-Error "Website build failed with exit code $LASTEXITCODE" }

if ($DeployWebsite) {
    Write-Host "Deploying to Firebase Hosting..." -ForegroundColor Green
    firebase deploy --only hosting
    if ($LASTEXITCODE -ne 0) { Write-Error "Firebase deployment failed with exit code $LASTEXITCODE" }
}

Set-Location $RepoRoot

if ($PushToGit) {
    Write-Host "`nCommitting and pushing release v$TargetVersion to GitHub..." -ForegroundColor Green
    git add -A
    git commit -m "release: KnowToMigrate v$TargetVersion updated release binaries"
    git push origin main
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "RELEASE v$TargetVersion SUCCESSFULLY PACKAGED AND STAGED!" -ForegroundColor Green
Write-Host "Binaries updated with fresh timestamps in release-binaries/ and website." -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
