# Validates that all repository components are strictly in sync with version.json
param(
    [switch]$CheckPublished = $false
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Get-Item $PSScriptRoot).Parent.FullName
$VersionJsonPath = Join-Path $RepoRoot "version.json"

if (!(Test-Path $VersionJsonPath)) {
    Write-Error "FAIL: version.json not found at $VersionJsonPath"
    exit 1
}

$versionConfig = Get-Content $VersionJsonPath | ConvertFrom-Json
$ver = $versionConfig.version
$verCode = $versionConfig.versionCode
$fourPartVer = "$ver.0"

Write-Host "=== Validating Version Synchronization against version.json ===" -ForegroundColor Cyan
Write-Host "Target Version: $ver (Code: $verCode, Quad: $fourPartVer)" -ForegroundColor Gray

$errors = @()

# 1. Check Version.g.props
$versionProps = Join-Path $RepoRoot "Version.g.props"
if (Test-Path $versionProps) {
    $propsContent = Get-Content $versionProps -Raw
    if ($propsContent -notmatch "<KtmVersion>$([regex]::Escape($ver))</KtmVersion>") {
        $errors += "Version.g.props does not match KtmVersion: $ver"
    }
    if ($propsContent -notmatch "<KtmVersionCode>$verCode</KtmVersionCode>") {
        $errors += "Version.g.props does not match KtmVersionCode: $verCode"
    }
    if ($propsContent -notmatch "<AssemblyVersion>$([regex]::Escape($fourPartVer))</AssemblyVersion>") {
        $errors += "Version.g.props does not match AssemblyVersion: $fourPartVer"
    }
} else {
    $errors += "Version.g.props is missing. Run scripts/sync-version.ps1 to generate it."
}

# 2. Check KtmVersion.g.cs
$ktmVersionCs = Join-Path $RepoRoot "KtmVersion.g.cs"
if (Test-Path $ktmVersionCs) {
    $csContent = Get-Content $ktmVersionCs -Raw
    if ($csContent -notmatch "Value\s*=\s*`"$([regex]::Escape($ver))`"") {
        $errors += "KtmVersion.g.cs does not match Value: $ver"
    }
    if ($csContent -notmatch "Code\s*=\s*$verCode") {
        $errors += "KtmVersion.g.cs does not match Code: $verCode"
    }
    if ($csContent -notmatch "DisplayVersion\s*=\s*`"$([regex]::Escape($ver))`"") {
        $errors += "KtmVersion.g.cs does not match DisplayVersion: $ver"
    }
} else {
    $errors += "KtmVersion.g.cs is missing. Run scripts/sync-version.ps1 to generate it."
}

# 3. Check Package.wxs
$wxsFile = Join-Path $RepoRoot "Package.wxs"
if (Test-Path $wxsFile) {
    $wxsContent = Get-Content $wxsFile -Raw
    if ($wxsContent -notmatch "Version=`"$([regex]::Escape($fourPartVer))`"") {
        $errors += "Package.wxs does not match Version: $fourPartVer"
    }
}

# 4. Check Android build.gradle.kts
$androidGradle = Join-Path $RepoRoot "apps\android\app\build.gradle.kts"
if (Test-Path $androidGradle) {
    $gradleContent = Get-Content $androidGradle -Raw
    if ($gradleContent -notmatch "versionCode\s*=\s*$verCode") {
        $errors += "build.gradle.kts does not match versionCode: $verCode"
    }
    if ($gradleContent -notmatch "versionName\s*=\s*`"$([regex]::Escape($ver))`"") {
        $errors += "build.gradle.kts does not match versionName: $ver"
    }
}

# 5. Check Android stable.json
$androidStable = Join-Path $RepoRoot "apps\website\public\updates\android\stable.json"
if (Test-Path $androidStable) {
    $as = Get-Content $androidStable | ConvertFrom-Json
    if ($as.versionCode -ne $verCode) {
        $errors += "updates/android/stable.json versionCode ($($as.versionCode)) does not match $verCode"
    }
    if ($as.versionName -ne $ver) {
        $errors += "updates/android/stable.json versionName ($($as.versionName)) does not match $ver"
    }
}

# 6. Check update-manifest.json android block
$manifestPath = Join-Path $RepoRoot "apps\website\public\update-manifest.json"
if (Test-Path $manifestPath) {
    $man = Get-Content $manifestPath | ConvertFrom-Json
    if ($man.android) {
        if ($man.android.versionCode -ne $verCode) {
            $errors += "update-manifest.json android.versionCode ($($man.android.versionCode)) does not match $verCode"
        }
        if ($man.android.versionName -ne $ver) {
            $errors += "update-manifest.json android.versionName ($($man.android.versionName)) does not match $ver"
        }
    }
}

# 7. Check published manifest comparison (fails closed on release builds)
if ($CheckPublished) {
    Write-Host "Checking against published live update manifest..." -ForegroundColor Cyan
    try {
        $liveUrl = "https://knowtomigrate.web.app/update-manifest.json"
        $liveResp = Invoke-RestMethod -Uri $liveUrl -TimeoutSec 15 -Headers @{ "Cache-Control" = "no-cache" }
        if ($liveResp -and $liveResp.version) {
            Write-Host "  Live published version: $($liveResp.version)" -ForegroundColor Gray
            if ($liveResp.version -eq $ver) {
                $errors += "Release verification failed: Current repo version $ver is already published in production manifest! Bump version.json before releasing."
            }
        }
    } catch {
        Write-Host "  Warning: Unable to reach live manifest ($($_.Exception.Message))." -ForegroundColor Yellow
    }
}

# Report results
if ($errors.Count -gt 0) {
    Write-Host ""
    Write-Host "Version Check FAILED with $($errors.Count) error(s):" -ForegroundColor Red
    foreach ($err in $errors) {
        Write-Host "  - $err" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Run 'pwsh scripts/sync-version.ps1' to automatically resolve differences." -ForegroundColor Yellow
    exit 1
}

Write-Host "All repository components match version.json ($ver) successfully!" -ForegroundColor Green
exit 0
