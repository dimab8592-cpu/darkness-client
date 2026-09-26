# Select-all + copy page text from Chrome, return it. Usage: grabpage.ps1 <pid>
param([int]$ProcId)
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class G2 {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern void keybd_event(byte bVk, byte bScan, uint f, UIntPtr e);
}
"@
$p = Get-Process -Id $ProcId -ErrorAction Stop
[G2]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 700
# Ctrl+A
[G2]::keybd_event(0xA2, 0, 0, [UIntPtr]::Zero); Start-Sleep -Milliseconds 60
[G2]::keybd_event(0x41, 0, 0, [UIntPtr]::Zero); Start-Sleep -Milliseconds 60
[G2]::keybd_event(0x41, 0, 2, [UIntPtr]::Zero); Start-Sleep -Milliseconds 60
[G2]::keybd_event(0xA2, 0, 2, [UIntPtr]::Zero); Start-Sleep -Milliseconds 250
# Ctrl+C
[G2]::keybd_event(0xA2, 0, 0, [UIntPtr]::Zero); Start-Sleep -Milliseconds 60
[G2]::keybd_event(0x43, 0, 0, [UIntPtr]::Zero); Start-Sleep -Milliseconds 60
[G2]::keybd_event(0x43, 0, 2, [UIntPtr]::Zero); Start-Sleep -Milliseconds 60
[G2]::keybd_event(0xA2, 0, 2, [UIntPtr]::Zero); Start-Sleep -Milliseconds 500
$m = Get-Clipboard -Raw -ErrorAction SilentlyContinue
if ($m) { $m.Substring(0, [Math]::Min(3000, $m.Length)) } else { Write-Host 'CLIPBOARD_EMPTY' }
