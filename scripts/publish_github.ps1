# Publish Darkness Client sources to GitHub via REST API (no git needed).
# Usage: publish_github.ps1 -Token <github_pat> [-RepoName darkness-client] [-Private]
param(
    [Parameter(Mandatory = $true)][string]$Token,
    [string]$RepoName = 'darkness-client',
    [switch]$Private
)
$ErrorActionPreference = 'Stop'
# кириллица в путях ломается при чтении ps1 как ANSI — берём путь от скрипта
$project = Split-Path -Parent $PSScriptRoot
$headers = @{
    'Authorization' = "Bearer $Token"
    'Accept'        = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
}
$api = 'https://api.github.com'

# 1) who am I
$me = Invoke-RestMethod -Uri "$api/user" -Headers $headers
$owner = $me.login
Write-Host "[publish] account: $owner"

# 2) create repo (ignore 422 = already exists)
$body = @{
    name        = $RepoName
    description = 'Darkness Client - PvP client mod for Minecraft 1.21.11 (Fabric)'
    private     = [bool]$Private
    has_issues  = $true
} | ConvertTo-Json
try {
    $repo = Invoke-RestMethod -Method Post -Uri "$api/user/repos" -Headers $headers -Body $body -ContentType 'application/json'
    Write-Host "[publish] repo created: $($repo.html_url)"
} catch {
    $repo = Invoke-RestMethod -Uri "$api/repos/$owner/$RepoName" -Headers $headers
    Write-Host "[publish] repo exists: $($repo.html_url)"
}

# 3) init-коммит через Contents API (Git Data API не работает в пустом репо)
$readmeBody = @{
    message = 'init'
    content = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('# Darkness Client'))
} | ConvertTo-Json
try {
    Invoke-RestMethod -Method Put -Uri "$api/repos/$owner/$RepoName/contents/README.md" -Headers $headers -Body $readmeBody -ContentType 'application/json' | Out-Null
    Write-Host '[publish] init commit done'
} catch {
    Write-Host '[publish] init skipped (already has files)'
}

# 4) collect files (source only)
$files = Get-ChildItem $project -Recurse -File | Where-Object {
    $_.FullName -notmatch '\\build\\|\\.gradle\\|\\.git\\|\\run\\|\\logs\\' -and
    $_.Name -notin @('.gitignore.disabled')
}
Write-Host "[publish] files: $($files.Count)"

# 4) blobs
$tree = @()
$i = 0
foreach ($f in $files) {
    $rel = $f.FullName.Substring($project.Length + 1).Replace('\', '/')
    $bytes = [IO.File]::ReadAllBytes($f.FullName)
    $b64 = [Convert]::ToBase64String($bytes)
    $blob = Invoke-RestMethod -Method Post -Uri "$api/repos/$owner/$RepoName/git/blobs" -Headers $headers `
        -Body (@{ content = $b64; encoding = 'base64' } | ConvertTo-Json) -ContentType 'application/json'
    $tree += @{ path = $rel; mode = '100644'; type = 'blob'; sha = $blob.sha }
    $i++
    if ($i % 25 -eq 0) { Write-Host "[publish] blobs $i/$($files.Count)" }
}

# 5) tree + commit + branch
$newTree = Invoke-RestMethod -Method Post -Uri "$api/repos/$owner/$RepoName/git/trees" -Headers $headers `
    -Body (@{ tree = $tree } | ConvertTo-Json -Depth 5) -ContentType 'application/json'
$commitBody = @{
    message = 'Darkness Client 1.8.6 - full source (Pulse-style UI, account switcher, sounds, FPS boost)'
    tree    = $newTree.sha
} | ConvertTo-Json
$commit = Invoke-RestMethod -Method Post -Uri "$api/repos/$owner/$RepoName/git/commits" -Headers $headers `
    -Body $commitBody -ContentType 'application/json'
$refBody = @{ sha = $commit.sha; force = $true } | ConvertTo-Json
try {
    Invoke-RestMethod -Method Patch -Uri "$api/repos/$owner/$RepoName/git/refs/heads/main" -Headers $headers -Body $refBody -ContentType 'application/json' | Out-Null
} catch {
    Invoke-RestMethod -Method Post -Uri "$api/repos/$owner/$RepoName/git/refs" -Headers $headers `
        -Body (@{ ref = 'refs/heads/main'; sha = $commit.sha } | ConvertTo-Json) -ContentType 'application/json' | Out-Null
}
Write-Host "[publish] DONE: https://github.com/$owner/$RepoName"
