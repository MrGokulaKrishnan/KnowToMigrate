# Serves a test update manifest locally for testing the Windows update flow in Debug builds.
param(
    [string]$TestVersion = "9.9.9",
    [int]$Port = 8089,
    [switch]$LaunchApp,
    [switch]$UseFile
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Get-Item $PSScriptRoot).Parent.FullName

$testManifest = [ordered]@{
    product = "KnowToMigrate"
    channel = "dev-test"
    version = $TestVersion
    minimumSupportedVersion = "1.0.0"
    publishedAt = [DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ")
    windows = [ordered]@{
        architecture = "x64"
        installerType = "exe"
        version = $TestVersion
        filename = "KnowToMigrate-Setup.exe"
        url = "http://127.0.0.1:$Port/download/KnowToMigrate-Setup.exe"
        size = 67577856
        sha256 = "62561cae27289a1f3ea03b67fc18cd21b9dd785128a6c3c64efbb250de2ffbfe"
        signature = ""
        standalone = [ordered]@{
            filename = "KnowToMigrate-$TestVersion-x64.exe"
            url = "http://127.0.0.1:$Port/download/KnowToMigrate-$TestVersion-x64.exe"
            size = 72864768
            sha256 = "388f62c0e588c19694d4bea9a8cfc95596699aba30fc2827d67b82ead4d10216"
        }
    }
    releaseNotes = @(
        "Test Release v$TestVersion for update flow validation",
        "Automated local simulated update payload"
    )
    versionCode = 99999
}

$jsonText = $testManifest | ConvertTo-Json -Depth 6

if ($UseFile) {
    $tempFile = Join-Path $RepoRoot "artifacts\test-manifest.json"
    $null = New-Item -ItemType Directory -Force -Path (Split-Path $tempFile)
    [System.IO.File]::WriteAllText($tempFile, $jsonText, [System.Text.Encoding]::UTF8)
    Write-Host "Created test manifest file: $tempFile" -ForegroundColor Green
    Write-Host "To test update flow in Debug build, set:" -ForegroundColor Cyan
    Write-Host "  `$env:KTM_UPDATE_MANIFEST_FILE = '$tempFile'" -ForegroundColor Yellow

    if ($LaunchApp) {
        $env:KTM_UPDATE_MANIFEST_FILE = $tempFile
        Write-Host "Launching WPF app in Debug mode..." -ForegroundColor Cyan
        & "C:\Users\gokul\.dotnet\dotnet.exe" run --project (Join-Path $RepoRoot "apps\windows-wpf\KnowToMigrate.csproj") -c Debug
    }
    return
}

# HTTP Server mode
$url = "http://127.0.0.1:$Port/"
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add($url)
$listener.Start()

Write-Host "=== Test Update Server Running ===" -ForegroundColor Green
Write-Host "Listening at: $url" -ForegroundColor Cyan
Write-Host "Serving update-manifest.json for version $TestVersion" -ForegroundColor Gray
Write-Host ""
Write-Host "Set environment variable in your test shell:" -ForegroundColor Yellow
Write-Host "  `$env:KTM_UPDATE_MANIFEST_URL = 'http://127.0.0.1:$Port/update-manifest.json'" -ForegroundColor White
Write-Host "Press Ctrl+C to stop server." -ForegroundColor Gray
Write-Host ""

if ($LaunchApp) {
    $env:KTM_UPDATE_MANIFEST_URL = "http://127.0.0.1:$Port/update-manifest.json"
    Write-Host "Launching WPF app in Debug mode in background..." -ForegroundColor Cyan
    Start-Process -FilePath "C:\Users\gokul\.dotnet\dotnet.exe" -ArgumentList "run --project `"$RepoRoot\apps\windows-wpf\KnowToMigrate.csproj`" -c Debug" -Environment @{
        KTM_UPDATE_MANIFEST_URL = "http://127.0.0.1:$Port/update-manifest.json"
    }
}

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response
        
        $path = $request.Url.AbsolutePath
        Write-Host "[$([DateTime]::Now.ToString('HH:mm:ss'))] $($request.HttpMethod) $path" -ForegroundColor DarkGray
        
        if ($path -eq "/update-manifest.json") {
            $buffer = [System.Text.Encoding]::UTF8.GetBytes($jsonText)
            $response.ContentType = "application/json"
            $response.ContentLength64 = $buffer.Length
            $response.OutputStream.Write($buffer, 0, $buffer.Length)
        } else {
            $response.StatusCode = 404
            $errBytes = [System.Text.Encoding]::UTF8.GetBytes("Not Found")
            $response.OutputStream.Write($errBytes, 0, $errBytes.Length)
        }
        $response.Close()
    }
}
finally {
    $listener.Stop()
    $listener.Close()
    Write-Host "Server stopped." -ForegroundColor Yellow
}
