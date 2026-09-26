# Focus a window, open a URL in the address bar (Ctrl+L), type it, press Enter.
# Usage: nav.ps1 <pid> <url>
param([int]$ProcId, [string]$Url)
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class N {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern void keybd_event(byte bVk, byte bScan, uint f, UIntPtr e);
    [DllImport("user32.dll")] public static extern uint SendInput(uint n, INPUT[] i, int s);
    [StructLayout(LayoutKind.Sequential)]
    public struct KEYBDINPUT { public ushort wVk; public ushort wScan; public uint dwFlags; public uint time; public IntPtr dwExtraInfo; }
    [StructLayout(LayoutKind.Explicit)]
    public struct INPUT { [FieldOffset(0)] public uint type; [FieldOffset(8)] public KEYBDINPUT ki; }
    public static void TypeUni(string s) {
        foreach (char c in s) {
            INPUT[] down = new INPUT[1]; INPUT[] up = new INPUT[1];
            down[0].type = 1; down[0].ki.wScan = c; down[0].ki.dwFlags = 0x0004;
            up[0].type = 1; up[0].ki.wScan = c; up[0].ki.dwFlags = 0x0004 | 0x0002;
            SendInput(1, down, Marshal.SizeOf(typeof(INPUT)));
            SendInput(1, up, Marshal.SizeOf(typeof(INPUT)));
            System.Threading.Thread.Sleep(12);
        }
    }
    public static void Chord(byte mod, byte key) {
        keybd_event(mod, 0, 0, UIntPtr.Zero);
        System.Threading.Thread.Sleep(80);
        keybd_event(key, 0, 0, UIntPtr.Zero);
        System.Threading.Thread.Sleep(60);
        keybd_event(key, 0, 2, UIntPtr.Zero);
        System.Threading.Thread.Sleep(40);
        keybd_event(mod, 0, 2, UIntPtr.Zero);
    }
    public static void Key(byte vk) {
        keybd_event(vk, 0, 0, UIntPtr.Zero);
        System.Threading.Thread.Sleep(50);
        keybd_event(vk, 0, 2, UIntPtr.Zero);
    }
}
"@
$p = Get-Process -Id $ProcId -ErrorAction Stop
if (-not $p.MainWindowHandle -or $p.MainWindowHandle -eq [IntPtr]::Zero) { throw 'NO_WINDOW' }
[N]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 800
[N]::Chord(0xA2, 0x4C)   # Ctrl+L
Start-Sleep -Milliseconds 300
[N]::TypeUni($Url)
Start-Sleep -Milliseconds 300
[N]::Key(0x0D)           # Enter
Start-Sleep -Milliseconds 400
Write-Host "NAVIGATED: $Url"
