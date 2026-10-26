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
Write-Host "`n[4/7] Building WiX MSI Package & Setup Bootstrapper (x64)..." -ForegroundColor Green
$msiOut = Join-Path $RepoRoot "releases\windows\KnowToMigrate-$TargetVersion-x64.msi"

& $WixExe build (Join-Path $RepoRoot "Package.wxs") -arch x64 -o $msiOut
if ($LASTEXITCODE -ne 0) { Write-Error "WiX MSI build failed with exit code $LASTEXITCODE" }

$msiSize = (Get-Item $msiOut).Length
if ($msiSize -lt 20MB) {
    Write-Error "MSI package is only $msiSize bytes. Expected self-contained 64-bit payload!"
}
Write-Host "  KnowToMigrate-$TargetVersion-x64.msi built: $([math]::Round($msiSize / 1MB, 2)) MB" -ForegroundColor Green

$setupSrc = Join-Path $RepoRoot "apps\windows-setup\SetupBootstrapper.cs"
$setupManifest = Join-Path $RepoRoot "apps\windows-setup\app.manifest"
$setupOut = Join-Path $RepoRoot "releases\windows\KnowToMigrate-Setup.exe"

& $CscExe /target:winexe /platform:x64 /optimize+ /win32manifest:$setupManifest /win32icon:$appIcon /resource:"$msiOut,Payload.msi" /resource:"$logoPng,logo.png" /out:$setupOut $setupSrc
if ($LASTEXITCODE -ne 0) { Write-Error "Setup bootstrapper build failed with exit code $LASTEXITCODE" }

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



# Prune older versioned binaries from web download dir and releases to prevent storage bloat

Get-ChildItem -Path $webDownloadDir -File -ErrorAction SilentlyContinue | Where-Object {

    $_.Name -match '^KnowToMigrate-\d+\.\d+\.\d+' -and

    $_.Name -notmatch [regex]::Escape($TargetVersion)

} | Remove-Item -Force



Get-ChildItem -Path (Join-Path $RepoRoot "releases\windows") -File -ErrorAction SilentlyContinue | Where-Object {

    $_.Name -match '^KnowToMigrate-\d+\.\d+\.\d+' -and

    $_.Name -notmatch [regex]::Escape($TargetVersion)

} | Remove-Item -Force



$androidRelDir = Join-Path $RepoRoot "releases\android"

if (Test-Path $androidRelDir) {

    Get-ChildItem -Path $androidRelDir -File -ErrorAction SilentlyContinue | Where-Object {

        $_.Name -match '^KnowToMigrate-\d+\.\d+\.\d+' -and

        $_.Name -notmatch [regex]::Escape($TargetVersion)

    } | Remove-Item -Force

}



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



# 6. Build and Stage Android APK

Write-Host "`nBuilding Android APK..." -ForegroundColor Green

$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

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

    Copy-Item -Path $apkSrc -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion.apk") -Force

    Copy-Item -Path $apkSrc -Destination (Join-Path $webDownloadDir "KnowToMigrate.apk") -Force

    Copy-Item -Path $apkSrc -Destination (Join-Path $webDownloadDir "KnowToMigrate-$TargetVersion.apk.bin") -Force

    Copy-Item -Path $apkSrc -Destination (Join-Path $webDownloadDir "KnowToMigrate.apk.bin") -Force

    Copy-Item -Path $apkSrc -Destination (Join-Path $releaseBinDir "KnowToMigrate-$TargetVersion.apk") -Force

    Copy-Item -Path $apkSrc -Destination (Join-Path $releaseBinDir "KnowToMigrate.apk") -Force

    Copy-Item -Path $apkSrc -Destination (Join-Path $releaseBinDir "KnowToMigrate-Debug.apk") -Force

    Write-Host "  Android APK: $apkSize bytes | SHA256: $apkHash" -ForegroundColor Gray

}



# 7. Regenerate every site/manifest reference from version.json and the .bin artifacts

Write-Host "`n[6/7] Generating release data (manifests, website data, Firebase routes)..." -ForegroundColor Green

node (Join-Path $RepoRoot "scripts\release\site-release.mjs")

if ($LASTEXITCODE -ne 0) { throw "site-release generation failed; nothing will be deployed" }





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

git commit -m "release: KnowToMigrate v$TargetVersion - Transit Auto Transfer Engine, Adaptive Streaming, Real-Time Intelligence"

git push origin main



Write-Host "`n==========================================================" -ForegroundColor Green

Write-Host "RELEASE v$TargetVersion SUCCESSFULLY PUBLISHED AND DEPLOYED!" -ForegroundColor Green

Write-Host "Any KnowToMigrate instance checking for updates will now detect v$TargetVersion." -ForegroundColor Green

Write-Host "==========================================================" -ForegroundColor Green

