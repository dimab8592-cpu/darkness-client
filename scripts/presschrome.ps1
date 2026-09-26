# Press a key in the Chrome window. Usage: presschrome.ps1 <pid> <VK hex> [count]
param([int]$ProcId, [string]$vk = '23', [int]$count = 1)
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class P2 {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern void keybd_event(byte bVk, byte bScan, uint f, UIntPtr e);
}
"@
$p = Get-Process -Id $ProcId -ErrorAction Stop
[P2]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 600
$b = [Convert]::ToByte($vk, 16)
for ($i = 0; $i -lt $count; $i++) {
    [P2]::keybd_event($b, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 60
    [P2]::keybd_event($b, 0, 2, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 150
}
Write-Host "PRESSED_$($count)x"
