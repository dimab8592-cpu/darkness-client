# Press a key in the Minecraft window. Usage: press.ps1 <VK hex> [holdMs]
# Example: press.ps1 A1   (A1 = Right Shift)
param([string]$vk = 'A1', [int]$holdMs = 120)
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class K {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);
}
"@
$p = Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -match 'Minecraft' } |
    Select-Object -First 1
if (-not $p) { Write-Host 'NO_WINDOW'; exit 1 }
[K]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 500
$b = [Convert]::ToByte($vk, 16)
[K]::keybd_event($b, 0, 0, [UIntPtr]::Zero)
Start-Sleep -Milliseconds $holdMs
[K]::keybd_event($b, 0, 2, [UIntPtr]::Zero)  # KEYEVENTF_KEYUP
Start-Sleep -Milliseconds 400
Write-Host "PRESSED_VK_$vk"
