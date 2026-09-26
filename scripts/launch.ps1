# Launch Minecraft through Legacy Launcher Stable (LL.exe) and press the
# "Zapustit" (Launch) button automatically, then wait for the game process.
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32 {
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int X, int Y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, uint dw, UIntPtr e);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
    public static void Click(int x, int y) {
        SetCursorPos(x, y);
        System.Threading.Thread.Sleep(200);
        mouse_event(0x02, (uint)x, (uint)y, 0, UIntPtr.Zero);
        System.Threading.Thread.Sleep(80);
        mouse_event(0x04, (uint)x, (uint)y, 0, UIntPtr.Zero);
    }
}
"@

$ErrorActionPreference = 'Stop'
$ll = "$env:USERPROFILE\AppData\Roaming\.tlauncher\legacy\Minecraft\LL.exe"
if (-not (Test-Path $ll)) { throw "Legacy Launcher not found: $ll" }

# 0) Ensure selected resource packs are listed in options.txt (game must be closed).
$optFile = "$env:USERPROFILE\AppData\Roaming\.tlauncher\legacy\Minecraft\game\options.txt"
foreach ($pack in @('file/Pufferfish Mace.zip')) {
    if (-not (Test-Path $optFile)) { break }
    $lines = Get-Content $optFile -Encoding UTF8
    $idx = -1
    for ($i = 0; $i -lt $lines.Count; $i++) { if ($lines[$i] -like 'resourcePacks:*') { $idx = $i; break } }
    if ($idx -ge 0 -and $lines[$idx].IndexOf($pack) -lt 0) {
        $suffix = ',"' + $pack + '"]'
        $lines[$idx] = $lines[$idx] -replace '\]$', $suffix
        Set-Content $optFile $lines -Encoding UTF8
        Write-Host "[launch] enabled resource pack: $pack"
    }
}

# 1) Start the launcher if it is not running yet (give a hidden/restoring window
#    up to 20 s to come back before spawning a new instance).
function Get-Launcher {
    Get-Process javaw -ErrorAction SilentlyContinue |
        Where-Object { $_.Path -match '\\legacy\\Minecraft\\jre\\' -and $_.MainWindowTitle }
}
$launcherProc = Get-Launcher
if (-not $launcherProc) {
    $restoreDeadline = (Get-Date).AddSeconds(20)
    while (-not $launcherProc -and (Get-Date) -lt $restoreDeadline) {
        Start-Sleep -Milliseconds 1000
        $launcherProc = Get-Launcher
    }
}
if (-not $launcherProc) {
    Write-Host "[launch] starting Legacy Launcher..."
    Start-Process $ll
    $deadline = (Get-Date).AddSeconds(90)
    while (-not $launcherProc -and (Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 1000
        $launcherProc = Get-Launcher
    }
}
if (-not $launcherProc) { throw 'Launcher window did not appear in 90 s.' }
Write-Host "[launch] launcher window: $($launcherProc.MainWindowTitle) (PID $($launcherProc.Id))"

# 2) If the game is already running, nothing to do.
$game = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" |
    Where-Object { $_.CommandLine -match 'KnotClient' }
if ($game) { Write-Host '[launch] game is already running.'; return }

# 3) Focus the launcher window and click the launch button, retrying a few times.
#    The process can expose several AWT windows (main frame + control panel);
#    we target the largest visible one and fall back to absolute screen
#    coordinates calibrated for the maximized 1600x900 window.
Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Collections.Generic;
public class WinEnum {
    public delegate bool EnumProc(IntPtr h, IntPtr l);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
    public static RECT Largest(int pid) {
        RECT best = new RECT(); long bestArea = 0;
        EnumWindows((h, l) => {
            uint wpid; GetWindowThreadProcessId(h, out wpid);
            if (wpid == (uint)pid && IsWindowVisible(h)) {
                RECT r; GetWindowRect(h, out r);
                long a = (long)(r.R - r.L) * (r.B - r.T);
                if (a > bestArea) { bestArea = a; best = r; }
            }
            return true;
        }, IntPtr.Zero);
        return best;
    }
}
"@
function Get-GameProc {
    Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" |
        Where-Object { $_.CommandLine -match 'KnotClient' }
}
for ($attempt = 1; $attempt -le 3; $attempt++) {
    $h = $launcherProc.MainWindowHandle
    [Win32]::SetForegroundWindow($h) | Out-Null
    Start-Sleep -Milliseconds 800
    $r = [WinEnum]::Largest($launcherProc.Id)
    $w = $r.R - $r.L; $ht = $r.B - $r.T
    if ($w -ge 1200) {
        $cx = [int]($r.L + $w * 0.306); $cy = [int]($r.T + $ht * 0.566)
    } else {
        $cx = 487; $cy = 487   # maximized 1600x900 calibration
    }
    Write-Host "[launch] attempt ${attempt}: window ${w}x${ht}, clicking ($cx,$cy)"
    [Win32]::Click($cx, $cy)

    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 5
        if (Get-GameProc) { break }
    }
    if (Get-GameProc) { break }
    Write-Host "[launch] game not started yet, retrying..."
}

# 4) Wait for the game process (KnotClient) to be fully up.
$game = Get-GameProc
if (-not $game) { throw 'Game process did not start after 3 click attempts.' }
Write-Host "[launch] game started (PID $($game.ProcessId))."
Start-Sleep -Seconds 25
$log = "$env:USERPROFILE\AppData\Roaming\.tlauncher\legacy\Minecraft\game\logs\latest.log"
if (Test-Path $log) {
    $hit = Select-String -Path $log -Pattern 'Darkness Client .+ loaded' |
        Select-Object -Last 1
    if ($hit) { Write-Host "[launch] log: $($hit.Line)" }
}
