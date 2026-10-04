# Synchronizes version numbers and timestamps across all KnowToMigrate components from version.json
param(
    [string]$TargetVersion = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Get-Item $PSScriptRoot).Parent.FullName
$VersionJsonPath = Join-Path $RepoRoot "version.json"

if (!(Test-Path $VersionJsonPath)) {
    Write-Error "version.json not found at $VersionJsonPath"
}

$versionConfig = Get-Content $VersionJsonPath | ConvertFrom-Json

if ($TargetVersion -ne "") {
    $versionConfig.version = $TargetVersion
    $parts = $TargetVersion.Split('.')
    if ($parts.Length -ge 3) {
        $versionConfig.versionCode = [int]$parts[0] * 10000 + [int]$parts[1] * 100 + [int]$parts[2]
    }
}

function Write-Utf8NoBom {
    param([string]$Path, [string]$Content)
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
}

# ALWAYS update publishedAt and updatedAt timestamps in version.json
$nowUtc = [DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ")
$versionConfig.publishedAt = $nowUtc
if ($versionConfig.PSObject.Properties["updatedAt"]) {
    $versionConfig.updatedAt = $nowUtc
} else {
    $versionConfig | Add-Member -MemberType NoteProperty -Name "updatedAt" -Value $nowUtc
}

Write-Utf8NoBom -Path $VersionJsonPath -Content ($versionConfig | ConvertTo-Json -Depth 5)
Write-Host "Updated version.json to $($versionConfig.version) (code: $($versionConfig.versionCode), updated: $nowUtc)" -ForegroundColor Green

$ver = $versionConfig.version
$verCode = $versionConfig.versionCode
$fourPartVer = "$ver.0"
$todayFormatted = (Get-Date).ToString("dd MMMM yyyy", [System.Globalization.CultureInfo]::InvariantCulture)

Write-Host "=== Syncing KnowToMigrate Version: $ver (Code: $verCode, Quad: $fourPartVer, Date: $todayFormatted) ===" -ForegroundColor Cyan

# 1. Update apps/windows-wpf/KnowToMigrate.csproj
$wpfProj = Join-Path $RepoRoot "apps\windows-wpf\KnowToMigrate.csproj"
if (Test-Path $wpfProj) {
    $content = Get-Content $wpfProj -Raw
    $content = [regex]::Replace($content, '<AssemblyVersion>.*?</AssemblyVersion>', "<AssemblyVersion>$fourPartVer</AssemblyVersion>")
    $content = [regex]::Replace($content, '<FileVersion>.*?</FileVersion>', "<FileVersion>$fourPartVer</FileVersion>")
    $content = [regex]::Replace($content, '<InformationalVersion>.*?</InformationalVersion>', "<InformationalVersion>$ver</InformationalVersion>")
    Set-Content -Path $wpfProj -Value $content -NoNewline
    Write-Host "  Updated $wpfProj" -ForegroundColor Gray
}

# 2. Update Package.wxs
$wxsFile = Join-Path $RepoRoot "Package.wxs"
if (Test-Path $wxsFile) {
    $content = Get-Content $wxsFile -Raw
    $content = [regex]::Replace($content, 'Version="[\d\.]+"', "Version=""$fourPartVer""")
    Set-Content -Path $wxsFile -Value $content -NoNewline
    Write-Host "  Updated $wxsFile" -ForegroundColor Gray
}

# 3. Update apps/android/app/build.gradle.kts
$androidGradle = Join-Path $RepoRoot "apps\android\app\build.gradle.kts"
if (Test-Path $androidGradle) {
    $content = Get-Content $androidGradle -Raw
    $content = [regex]::Replace($content, 'versionCode\s*=\s*\d+', "versionCode = $verCode")
    $content = [regex]::Replace($content, 'versionName\s*=\s*"[^"]+"', "versionName = ""$ver""")
    Set-Content -Path $androidGradle -Value $content -NoNewline
    Write-Host "  Updated $androidGradle" -ForegroundColor Gray
}

# 4. Update DEVELOPMENT_STATUS.md
$devStatusPath = Join-Path $RepoRoot "DEVELOPMENT_STATUS.md"
if (Test-Path $devStatusPath) {
    $content = Get-Content $devStatusPath -Raw
    $content = [regex]::Replace($content, '\*\*CURRENT VERSION:\*\*.*', "**CURRENT VERSION:** $ver  ")
    $content = [regex]::Replace($content, '\*\*LAST UPDATED:\*\*.*', "**LAST UPDATED:** $todayFormatted  ")
    $content = [regex]::Replace($content, '\|\s*\*\*CURRENT VERSION\*\*\s*\|\s*.*?\|', "| **CURRENT VERSION** | $ver |")
    $content = [regex]::Replace($content, '\|\s*\*\*LAST UPDATED\*\*\s*\|\s*.*?\|', "| **LAST UPDATED** | $todayFormatted |")
    Set-Content -Path $devStatusPath -Value $content -NoNewline
    Write-Host "  Updated $devStatusPath" -ForegroundColor Gray
}

# 5. Update apps/website/src/pages/DownloadPage.tsx
$downloadPagePath = Join-Path $RepoRoot "apps\website\src\pages\DownloadPage.tsx"
if (Test-Path $downloadPagePath) {
    $content = Get-Content $downloadPagePath -Raw
    $content = [regex]::Replace($content, "export const RELEASE_DATE = '[^']*'", "export const RELEASE_DATE = '$todayFormatted'")
    $content = [regex]::Replace($content, "const RELEASE_DATE = '[^']*'", "const RELEASE_DATE = '$todayFormatted'")
    Set-Content -Path $downloadPagePath -Value $content -NoNewline
    Write-Host "  Updated $downloadPagePath" -ForegroundColor Gray
}

# 6. Update apps/website/public/updates/android/stable.json
$androidStablePath = Join-Path $RepoRoot "apps\website\public\updates\android\stable.json"
if (Test-Path $androidStablePath) {
    $asConfig = Get-Content $androidStablePath | ConvertFrom-Json
    $asConfig.versionCode = $verCode
    $asConfig.versionName = $ver
    $asConfig.releaseDate = (Get-Date).ToString("yyyy-MM-dd")
    $asConfig.title = "KnowToMigrate $ver"
    $asConfig.apkUrl = "https://knowtomigrate.web.app/download/KnowToMigrate-$ver.apk.bin"
    Write-Utf8NoBom -Path $androidStablePath -Content ($asConfig | ConvertTo-Json -Depth 5)
    Write-Host "  Updated $androidStablePath" -ForegroundColor Gray
}

# 7. Update apps/website/public/update-manifest.json android block
$updateManifestPath = Join-Path $RepoRoot "apps\website\public\update-manifest.json"
if (Test-Path $updateManifestPath) {
    $umConfig = Get-Content $updateManifestPath | ConvertFrom-Json
    if ($umConfig.android) {
        $umConfig.android.versionCode = $verCode
        $umConfig.android.versionName = $ver
        $umConfig.android.filename = "KnowToMigrate-$ver.apk"
        $umConfig.android.url = "https://knowtomigrate.web.app/download/KnowToMigrate-$ver.apk.bin"
        Write-Utf8NoBom -Path $updateManifestPath -Content ($umConfig | ConvertTo-Json -Depth 6)
        Write-Host "  Updated $updateManifestPath (android block)" -ForegroundColor Gray
    }
}

Write-Host "Version and date sync completed successfully." -ForegroundColor Green
