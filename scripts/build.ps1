# Build Darkness Client from source using the bundled Java 21 runtime.
# Output: build\libs\darkness-client.jar
param(
    [string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = 'Stop'

# Locate a Java 21 JDK: prefer the Legacy Launcher runtime, then the .minecraft one.
$javaCandidates = @(
    "$env:USERPROFILE\AppData\Roaming\.tlauncher\legacy\Minecraft\jre\java-runtime-delta\windows-x64\java-runtime-delta",
    "$env:APPDATA\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta"
)
$javaHome = $null
foreach ($c in $javaCandidates) {
    if (Test-Path (Join-Path $c 'bin\javac.exe')) { $javaHome = $c; break }
}
if (-not $javaHome) { throw 'Java 21 (javac) not found in known launcher runtimes.' }

Write-Host "[build] JAVA_HOME = $javaHome"
Write-Host "[build] project   = $ProjectRoot"

$env:JAVA_HOME = $javaHome
$env:PATH      = "$javaHome\bin;$env:PATH"

Push-Location $ProjectRoot
try {
    # Gradle stdout is piped to the host so that it never enters the script's
    # return value - callers capture only the final jar path.
    & .\gradlew.bat build --no-daemon -x test -x check 2>&1 |
        ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) { throw "gradle build failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

$jar = Get-ChildItem (Join-Path $ProjectRoot 'build\libs\*.jar') |
    Where-Object { $_.Name -notmatch '-sources' } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw 'Build finished but no jar found in build\libs.' }
Write-Host "[build] OK -> $($jar.FullName)"
return $jar.FullName
