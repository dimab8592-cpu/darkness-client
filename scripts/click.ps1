# Click at absolute screen coordinates. Usage: click.ps1 <x> <y>
param([int]$X, [int]$Y)
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class C2 {
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int X, int Y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, uint dw, UIntPtr e);
}
"@
[C2]::SetCursorPos($X, $Y) | Out-Null
Start-Sleep -Milliseconds 400
[C2]::mouse_event(2, [uint32]$X, [uint32]$Y, 0, [UIntPtr]::Zero)
Start-Sleep -Milliseconds 80
[C2]::mouse_event(4, [uint32]$X, [uint32]$Y, 0, [UIntPtr]::Zero)
Start-Sleep -Milliseconds 200
Write-Host "CLICKED $X,$Y"
