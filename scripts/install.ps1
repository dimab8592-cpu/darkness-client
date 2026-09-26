# Install a freshly built Darkness Client jar into the Legacy Launcher mods folder
# (and the TLauncher .minecraft mods folder as a secondary location).
# Stops the running game first, removes old darkness jars, copies the new one.
param(
    [Parameter(Mandatory=$true)][string]$Jar,
    [string]$Version = ''
)

$ErrorActionPreference = 'Stop'

$legacyMods  = "$env:USERPROFILE\AppData\Roaming\.tlauncher\legacy\Minecraft\game\mods"
$vanillaMods = "$env:APPDATA\.minecraft\mods"

# 1) Stop the running Minecraft (only the game, not the launcher itself).
$game = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" |
    Where-Object { $_.CommandLine -match 'KnotClient' }
foreach ($p in $game) {
    Write-Host "[install] stopping game process PID $($p.ProcessId)"
    try { Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop } catch { }
}
if ($game) { Start-Sleep -Seconds 3 }

if (-not (Test-Path $Jar)) { throw "Jar not found: $Jar" }

# 2) Remove old darkness jars and copy the new one (name includes the version).
if ($Version -eq '') {
    if ($Jar -match 'darkness-client-([\d\.]+)\.jar$') { $Version = $Matches[1] } else { $Version = 'dev' }
}
$destName = "darkness-client-$Version.jar"

foreach ($mods in @($legacyMods, $vanillaMods)) {
    if (-not (Test-Path $mods)) { New-Item -ItemType Directory -Path $mods | Out-Null }
    Get-ChildItem $mods -Filter 'darkness*.jar' -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "[install] removing old $($_.Name) in $mods"
        Remove-Item $_.FullName -Force
    }
    Copy-Item $Jar (Join-Path $mods $destName) -Force
    Write-Host "[install] installed $destName -> $mods"
}
Write-Host "[install] DONE ($destName)"
