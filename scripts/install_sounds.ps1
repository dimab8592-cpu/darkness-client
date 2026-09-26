# Install the user's MP3 as totem/crit custom sounds. ASCII only.
$src = 'D:\Downloads\19645481618174 (online-audio-converter.com).mp3'
if (-not (Test-Path $src)) { throw "MP3 not found: $src" }
$game = Join-Path $env:USERPROFILE 'AppData\Roaming\.tlauncher\legacy\Minecraft\game\config\darkness\sounds'
$dot  = Join-Path $env:USERPROFILE 'AppData\Roaming\.minecraft\config\darkness\sounds'
foreach ($d in @($game, $dot)) {
    New-Item -ItemType Directory -Force -Path $d | Out-Null
    Copy-Item $src (Join-Path $d 'totem.mp3') -Force
    Copy-Item $src (Join-Path $d 'crit.mp3') -Force
    Get-ChildItem $d | ForEach-Object { Write-Host "$d\$($_.Name) $($_.Length)" }
}
Write-Host 'SOUNDS_INSTALLED'
