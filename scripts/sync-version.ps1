# Synchronizes version numbers across all KnowToMigrate components from version.json
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
    $versionConfig | ConvertTo-Json -Depth 5 | Set-Content $VersionJsonPath -Encoding utf8
    Write-Host "Updated version.json to $($versionConfig.version) (code: $($versionConfig.versionCode))" -ForegroundColor Green
}

$ver = $versionConfig.version
$verCode = $versionConfig.versionCode
$fourPartVer = "$ver.0"

Write-Host "=== Syncing KnowToMigrate Version: $ver (Code: $verCode, Quad: $fourPartVer) ===" -ForegroundColor Cyan

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

Write-Host "Version sync completed successfully." -ForegroundColor Green
