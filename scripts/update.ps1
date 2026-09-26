# Full pipeline: fetch latest Darkness Client source from GitHub,
# optionally stamp a version, build, install into Legacy Launcher mods, launch.
#
#   powershell -File scripts\update.ps1                       # keep upstream version
#   powershell -File scripts\update.ps1 -Version 1.6.0        # stamp our version
#   powershell -File scripts\update.ps1 -Version 1.6.0 -SkipLaunch
#   powershell -File scripts\update.ps1 -LocalOnly -Version 1.6.0   # no GitHub fetch
param(
    [string]$Version = '',
    [switch]$SkipLaunch,
    [switch]$LocalOnly,
    [string]$Repo = 'BelyakDima/dc',
    [string]$Branch = 'main'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

# ---------- 1) Fetch the latest source from GitHub ----------
if (-not $LocalOnly) {
    $zip = Join-Path $env:TEMP 'dc-update.zip'
    $dst = Join-Path $env:TEMP 'dc-update-extract'
    Write-Host "[update] downloading https://github.com/$Repo ($Branch)..."
    & curl.exe -sSL -o $zip "https://codeload.github.com/$Repo/zip/refs/heads/$Branch"
    if ($LASTEXITCODE -ne 0) { throw 'curl download failed' }
    if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
    Expand-Archive -Path $zip -DestinationPath $dst -Force
    $src = Get-ChildItem $dst -Directory | Select-Object -First 1
    if (-not $src) { throw 'Extracted archive is empty.' }

    # Sync upstream into the project, keeping local-only dirs (scripts, build, .gradle).
    Write-Host "[update] syncing $($src.FullName) -> $root"
    robocopy $src.FullName $root /E /NFL /NDL /NJH /NP /R:2 /W:1 | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "robocopy failed (code $LASTEXITCODE)" }
    Remove-Item $zip -Force -ErrorAction SilentlyContinue
    Remove-Item $dst -Recurse -Force -ErrorAction SilentlyContinue
}

# ---------- 2) Stamp the version (both source and gradle.properties) ----------
if ($Version -ne '') {
    $dcFile = Join-Path $root 'src\main\java\dev\darkness\client\DarknessClient.java'
    $gpFile = Join-Path $root 'gradle.properties'
    foreach ($f in @($dcFile, $gpFile)) {
        if (-not (Test-Path $f)) { throw "File not found: $f" }
        $text = [IO.File]::ReadAllText($f)
        if ($f -eq $dcFile) {
            $text = $text -replace 'VERSION\s*=\s*"[^"]*"', ('VERSION = "' + $Version + '"')
        } else {
            $text = $text -replace '(?m)^mod_version\s*=.*$', ('mod_version=' + $Version)
        }
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [IO.File]::WriteAllText($f, $text, $utf8NoBom)
    }
    Write-Host "[update] version stamped: $Version"
}

# ---------- 3) Build ----------
$jarOut = & (Join-Path $PSScriptRoot 'build.ps1') -ProjectRoot $root
$jar = @($jarOut) | Where-Object { "$_" -match '\.jar$' } | Select-Object -Last 1
if (-not $jar) { throw 'Build produced no jar path.' }
Write-Host "[update] built jar: $jar"

# ---------- 4) Install into the launcher mods folders ----------
& (Join-Path $PSScriptRoot 'install.ps1') -Jar $jar -Version $Version

# ---------- 5) Launch through Legacy Launcher ----------
if (-not $SkipLaunch) {
    & (Join-Path $PSScriptRoot 'launch.ps1')
}
Write-Host '[update] ALL DONE'
