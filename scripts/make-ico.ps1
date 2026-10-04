Add-Type -AssemblyName System.Drawing

$pngPath = "c:\KnowToMigrate\apps\windows-wpf\Assets\logo.png"
$icoPath = "c:\KnowToMigrate\apps\windows-wpf\Assets\KnowToMigrate.ico"

$src = [System.Drawing.Image]::FromFile($pngPath)

# Standard icon sizes
$sizes = @(16, 24, 32, 48, 64, 128, 256)
$bitmaps = @()

foreach ($sz in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($sz, $sz)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.DrawImage($src, 0, 0, $sz, $sz)
    $g.Dispose()
    $bitmaps += $bmp
}

# Write multi-resolution ICO file format
$stream = New-Object System.IO.MemoryStream
$writer = New-Object System.IO.BinaryWriter($stream)

# ICONDIR header
$writer.Write([uint16]0) # Reserved
$writer.Write([uint16]1) # Type (1 for ICO)
$writer.Write([uint16]$sizes.Length) # Image count

$pngDataList = @()
foreach ($bmp in $bitmaps) {
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngDataList += ,$ms.ToArray()
    $ms.Dispose()
}

$offset = 6 + (16 * $sizes.Length)
for ($i = 0; $i -lt $sizes.Length; $i++) {
    $sz = $sizes[$i]
    $w = if ($sz -ge 256) { [byte]0 } else { [byte]$sz }
    $h = if ($sz -ge 256) { [byte]0 } else { [byte]$sz }
    $data = $pngDataList[$i]

    # ICONDIRENTRY (16 bytes)
    $writer.Write([byte]$w)          # Width
    $writer.Write([byte]$h)          # Height
    $writer.Write([byte]0)           # Color count
    $writer.Write([byte]0)           # Reserved
    $writer.Write([uint16]1)         # Color planes
    $writer.Write([uint16]32)        # Bits per pixel
    $writer.Write([uint32]$data.Length) # Image size in bytes
    $writer.Write([uint32]$offset)   # Image offset in file
    $offset += $data.Length
}

foreach ($data in $pngDataList) {
    $writer.Write($data)
}

$writer.Flush()
[System.IO.File]::WriteAllBytes($icoPath, $stream.ToArray())
$writer.Dispose()
$stream.Dispose()
$src.Save("c:\KnowToMigrate\apps\windows-wpf\Assets\logo.jpg", [System.Drawing.Imaging.ImageFormat]::Jpeg)
$src.Dispose()
foreach ($bmp in $bitmaps) { $bmp.Dispose() }

Write-Host "Multi-resolution KnowToMigrate.ico successfully generated at $icoPath" -ForegroundColor Green
