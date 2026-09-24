Add-Type -AssemblyName System.Drawing

$masterJpg = "c:\KnowToMigrate\design\master_logo.jpg"
$masterPngTrans = "c:\KnowToMigrate\design\master_logo_transparent.png"

if (-not (Test-Path $masterJpg)) {
    throw "Master logo file not found at $masterJpg"
}
if (-not (Test-Path $masterPngTrans)) {
    throw "Transparent master logo not found at $masterPngTrans"
}

# Helper to create rounded rectangle path
function Get-RoundedRectanglePath([System.Drawing.RectangleF]$rect, [float]$radius) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $diameter = $radius * 2.0
    $arc = New-Object System.Drawing.RectangleF($rect.X, $rect.Y, $diameter, $diameter)
    
    # Top-Left
    $path.AddArc($arc, 180, 90)
    
    # Top-Right
    $arc.X = $rect.Right - $diameter
    $path.AddArc($arc, 270, 90)
    
    # Bottom-Right
    $arc.Y = $rect.Bottom - $diameter
    $path.AddArc($arc, 0, 90)
    
    # Bottom-Left
    $arc.X = $rect.Left
    $path.AddArc($arc, 90, 90)
    
    $path.CloseFigure()
    return $path
}

# Helper to build valid multi-resolution ICO file
function Build-IcoFile([string[]]$pngPaths, [string]$outputPath) {
    $ms = New-Object System.IO.MemoryStream
    $bw = New-Object System.IO.BinaryWriter($ms)
    
    $bw.Write([uint16]0) # Reserved
    $bw.Write([uint16]1) # Type: 1 = ICO
    $bw.Write([uint16]$pngPaths.Count) # Image count
    
    $pngBytesList = @()
    $offset = 6 + ($pngPaths.Count * 16)
    
    foreach ($path in $pngPaths) {
        $bytes = [System.IO.File]::ReadAllBytes($path)
        $img = [System.Drawing.Image]::FromFile($path)
        $w = if ($img.Width -ge 256) { [byte]0 } else { [byte]$img.Width }
        $h = if ($img.Height -ge 256) { [byte]0 } else { [byte]$img.Height }
        $img.Dispose()
        
        $bw.Write($w) # Width
        $bw.Write($h) # Height
        $bw.Write([byte]0) # ColorCount
        $bw.Write([byte]0) # Reserved
        $bw.Write([uint16]1) # Planes
        $bw.Write([uint16]32) # BitCount
        $bw.Write([uint32]$bytes.Length) # BytesInRes
        $bw.Write([uint32]$offset) # ImageOffset
        
        $offset += $bytes.Length
        $pngBytesList += ,$bytes
    }
    
    foreach ($bytes in $pngBytesList) {
        $bw.Write($bytes)
    }
    
    [System.IO.File]::WriteAllBytes($outputPath, $ms.ToArray())
    $bw.Dispose()
    $ms.Dispose()
}

Write-Host "=== 1. GENERATING ANDROID ASSETS ===" -ForegroundColor Cyan

$masterImg = [System.Drawing.Image]::FromFile($masterJpg)
$transImg = [System.Drawing.Image]::FromFile($masterPngTrans)

# 1.1 In-app drawables
# High-res master logo on black (512x512)
$bmp512 = New-Object System.Drawing.Bitmap(512, 512, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp512)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.Clear([System.Drawing.Color]::Black)
$g.DrawImage($masterImg, 0, 0, 512, 512)
$g.Dispose()
$bmp512.Save("c:\KnowToMigrate\apps\android\app\src\main\res\drawable\logo.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmp512.Dispose()

# Transparent splash logo (512x512)
$splash512 = New-Object System.Drawing.Bitmap(512, 512, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($splash512)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.Clear([System.Drawing.Color]::Transparent)
$g.DrawImage($transImg, 0, 0, 512, 512)
$g.Dispose()
$splash512.Save("c:\KnowToMigrate\apps\android\app\src\main\res\drawable\splash_logo.png", [System.Drawing.Imaging.ImageFormat]::Png)
$splash512.Dispose()

# 1.2 Android Adaptive Icons (Foreground 108dp canvas, safe zone 60% = 0.60 * canvas)
$adaptiveSizes = @{
    "mipmap-mdpi" = 108
    "mipmap-hdpi" = 162
    "mipmap-xhdpi" = 216
    "mipmap-xxhdpi" = 324
    "mipmap-xxxhdpi" = 432
}

foreach ($entry in $adaptiveSizes.GetEnumerator()) {
    $dir = "c:\KnowToMigrate\apps\android\app\src\main\res\" + $entry.Key
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    
    $canvasSize = $entry.Value
    $bmp = New-Object System.Drawing.Bitmap($canvasSize, $canvasSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    
    # 60% safe scale ensures double arrows and text are NEVER clipped by squircle / round masks
    $scale = 0.60
    $drawW = [int]($canvasSize * $scale)
    $drawH = [int]($canvasSize * $scale)
    $drawX = [int](($canvasSize - $drawW) / 2)
    $drawY = [int](($canvasSize - $drawH) / 2)
    
    $g.DrawImage($transImg, $drawX, $drawY, $drawW, $drawH)
    $g.Dispose()
    
    $dest = Join-Path $dir "ic_launcher_foreground.png"
    $bmp.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# 1.3 Android Legacy Square & Round Icons
$legacySizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($entry in $legacySizes.GetEnumerator()) {
    $dir = "c:\KnowToMigrate\apps\android\app\src\main\res\" + $entry.Key
    $size = $entry.Value
    
    # Legacy Square Icon
    $bmpSquare = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmpSquare)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Black)
    $g.DrawImage($masterImg, 0, 0, $size, $size)
    $g.Dispose()
    $bmpSquare.Save((Join-Path $dir "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmpSquare.Dispose()
    
    # Legacy Round Icon
    $bmpRound = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmpRound)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    
    $circlePath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $circlePath.AddEllipse(0, 0, $size, $size)
    $g.SetClip($circlePath)
    $g.Clear([System.Drawing.Color]::Black)
    
    # 80% scale for round icon so circular mask never clips arrows or text
    $scale = 0.80
    $drawW = [int]($size * $scale)
    $drawH = [int]($size * $scale)
    $drawX = [int](($size - $drawW) / 2)
    $drawY = [int](($size - $drawH) / 2)
    $g.DrawImage($masterImg, $drawX, $drawY, $drawW, $drawH)
    $g.ResetClip()
    $circlePath.Dispose()
    $g.Dispose()
    $bmpRound.Save((Join-Path $dir "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmpRound.Dispose()
}

Write-Host "Android icons generated successfully." -ForegroundColor Green

Write-Host "=== 2. GENERATING WINDOWS ASSETS WITH EXACT 15% ROUNDED CORNER ===" -ForegroundColor Cyan

# Windows requires EXACT 15% rounded corner radius on the container
# For size S: radius = S * 0.15
$winSizes = @(16, 24, 32, 48, 64, 128, 256, 512)
$tempPngs = @()

foreach ($s in $winSizes) {
    $radius = [float]($s * 0.15)
    $bmp = New-Object System.Drawing.Bitmap($s, $s, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    
    $rect = New-Object System.Drawing.RectangleF(0, 0, $s, $s)
    $path = Get-RoundedRectanglePath $rect $radius
    
    # Fill rounded container with pure AMOLED black
    $blackBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::Black)
    $g.FillPath($blackBrush, $path)
    $blackBrush.Dispose()
    
    # Clip drawing inside the 15% rounded rectangle
    $g.SetClip($path)
    
    # Draw master logo preserving exact aspect ratio
    # 90% content size provides a clean safe margin inside the rounded boundary
    $scale = 0.90
    $drawW = [float]($s * $scale)
    $drawH = [float]($s * $scale)
    $drawX = [float](($s - $drawW) / 2.0)
    $drawY = [float](($s - $drawH) / 2.0)
    
    $g.DrawImage($masterImg, $drawX, $drawY, $drawW, $drawH)
    $g.ResetClip()
    $path.Dispose()
    $g.Dispose()
    
    $pngPath = "c:\KnowToMigrate\tools\win_icon_${s}.png"
    $bmp.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    
    if ($s -le 256) {
        $tempPngs += $pngPath
    }
}

# Generate multi-resolution Windows ICO (16, 24, 32, 48, 64, 128, 256)
$icoWpf = "c:\KnowToMigrate\apps\windows-wpf\Assets\KnowToMigrate.ico"
$icoWin = "c:\KnowToMigrate\apps\windows\Assets\KnowToMigrate.ico"
$icoWeb = "c:\KnowToMigrate\apps\website\public\favicon.ico"

Build-IcoFile $tempPngs $icoWpf
Copy-Item -Force $icoWpf $icoWin
Copy-Item -Force $icoWpf $icoWeb

# Copy 512px rounded PNG as application logo
Copy-Item -Force "c:\KnowToMigrate\tools\win_icon_512.png" "c:\KnowToMigrate\apps\windows-wpf\Assets\logo.png"
Copy-Item -Force "c:\KnowToMigrate\tools\win_icon_512.png" "c:\KnowToMigrate\apps\windows\Assets\AppIcon.png"
Copy-Item -Force "c:\KnowToMigrate\tools\win_icon_512.png" "c:\KnowToMigrate\apps\website\public\logo.png"
Copy-Item -Force "c:\KnowToMigrate\tools\win_icon_32.png" "c:\KnowToMigrate\apps\website\public\favicon.png"

# Copy master JPG for WPF image sources
Copy-Item -Force $masterJpg "c:\KnowToMigrate\apps\windows-wpf\Assets\logo.jpg"
Copy-Item -Force $masterJpg "c:\KnowToMigrate\apps\windows\Assets\logo.jpg"
Copy-Item -Force $masterJpg "c:\KnowToMigrate\apps\website\public\logo.jpg"

# Cleanup temp files
foreach ($p in $tempPngs) {
    Remove-Item -Force $p -ErrorAction SilentlyContinue
}
Remove-Item -Force "c:\KnowToMigrate\tools\win_icon_512.png" -ErrorAction SilentlyContinue

$masterImg.Dispose()
$transImg.Dispose()

Write-Host "Windows icons generated successfully with 15% corner radius." -ForegroundColor Green
