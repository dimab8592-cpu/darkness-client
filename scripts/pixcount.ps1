# Count pixels near given RGB in a PNG. Usage: pixcount.ps1 <png> <r> <g> <b> <tol>
param([string]$png = "$(Join-Path $env:TEMP 'dc_shot.png')", [int]$r, [int]$g, [int]$b, [int]$tol = 25)
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap($png)
$rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$bytes = New-Object byte[] ($data.Stride * $data.Height)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data)
$count = 0; $minX = 99999; $maxX = -1; $minY = 99999; $maxY = -1
for ($y = 0; $y -lt $bmp.Height; $y++) {
    $off = $y * $data.Stride
    for ($x = 0; $x -lt $bmp.Width; $x++) {
        $o = $off + $x * 4
        $pr = $bytes[$o + 2]; $pg = $bytes[$o + 1]; $pb = $bytes[$o]
        if ([Math]::Abs($pr - $r) -le $tol -and [Math]::Abs($pg - $g) -le $tol -and [Math]::Abs($pb - $b) -le $tol) {
            $count++
            if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
        }
    }
}
Write-Host ("COUNT={0} REGION={1},{2}..{3},{4}" -f $count, $minX, $minY, $maxX, $maxY)
$bmp.Dispose()
