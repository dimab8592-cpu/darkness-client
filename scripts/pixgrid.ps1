# Print pixel samples from a PNG (grid + variance) for objective verification.
# Usage: pixgrid.ps1 <png> [cols] [rows]
param([string]$png = "$(Join-Path $env:TEMP 'dc_shot.png')", [int]$cols = 8, [int]$rows = 5)
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap($png)
$rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$bytes = New-Object byte[] ($data.Stride * $data.Height)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data)
function Get-Px([int]$x, [int]$y) {
    $off = $y * $data.Stride + $x * 4
    return ($bytes[$off + 2], $bytes[$off + 1], $bytes[$off])  # R,G,B
}
$sum = 0.0; $sum2 = 0.0; $n = 0
for ($j = 0; $j -lt $rows; $j++) {
    $line = ''
    for ($i = 0; $i -lt $cols; $i++) {
        $x = [int](($i + 0.5) * $bmp.Width / $cols); $y = [int](($j + 0.5) * $bmp.Height / $rows)
        $px = Get-Px $x $y
        $lum = 0.299 * $px[0] + 0.587 * $px[1] + 0.114 * $px[2]
        $sum += $lum; $sum2 += $lum * $lum; $n++
        $line += ('({0,3},{1,3},{2,3})' -f $px[0], $px[1], $px[2])
    }
    Write-Host $line
}
$mean = $sum / $n
$var = $sum2 / $n - $mean * $mean
Write-Host ("MEAN_LUM={0:N1} STDDEV={1:N1} SIZE={2}x{3}" -f $mean, [Math]::Sqrt([Math]::Max(0, $var)), $bmp.Width, $bmp.Height)
$bmp.Dispose()
