# Screenshot the Minecraft window to %TEMP%\dc_shot.png and print its rect.
# Usage: shot.ps1  (no args). ASCII only.
Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class W {
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out R r);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [StructLayout(LayoutKind.Sequential)] public struct R { public int L, T, Rt, B; }
}
"@
$p = Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -match 'Minecraft' } |
    Select-Object -First 1
if (-not $p) { Write-Host 'NO_WINDOW'; exit 1 }
$r = New-Object 'W+R'
[W]::GetWindowRect($p.MainWindowHandle, [ref]$r) | Out-Null
if ($r.L -le -30000) { Write-Host 'WINDOW_MINIMIZED'; exit 2 }
[W]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 600
$w = $r.Rt - $r.L; $h = $r.B - $r.T
$bmp = New-Object System.Drawing.Bitmap($w, $h)
$gfx = [System.Drawing.Graphics]::FromImage($bmp)
$gfx.CopyFromScreen($r.L, $r.T, 0, 0, $bmp.Size)
$gfx.Dispose()
$out = Join-Path $env:TEMP 'dc_shot.png'
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host ("RECT={0},{1},{2},{3} SAVED={4}" -f $r.L, $r.T, $w, $h, $out)
