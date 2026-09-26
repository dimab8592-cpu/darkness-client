# Screenshot the Chrome window (by pid) to %TEMP%\dc_chrome.png.
param([int]$ProcId)
Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class S2 {
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out R r);
    [StructLayout(LayoutKind.Sequential)] public struct R { public int L, T, Rt, B; }
}
"@
$p = Get-Process -Id $ProcId -ErrorAction Stop
$r = New-Object 'S2+R'
[S2]::GetWindowRect($p.MainWindowHandle, [ref]$r) | Out-Null
if ($r.L -le -30000) { Write-Host 'MINIMIZED'; exit 2 }
$w = $r.Rt - $r.L; $h = $r.B - $r.T
$bmp = New-Object System.Drawing.Bitmap($w, $h)
$gfx = [System.Drawing.Graphics]::FromImage($bmp)
$gfx.CopyFromScreen($r.L, $r.T, 0, 0, $bmp.Size)
$gfx.Dispose()
$out = Join-Path $env:TEMP 'dc_chrome.png'
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host ("RECT={0},{1},{2},{3} SAVED={4}" -f $r.L, $r.T, $w, $h, $out)
