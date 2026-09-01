[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Source,
    [Parameter(Mandatory)][string]$ArchiveDirectory,
    [int]$RetentionDays = 30,
    [int]$MinimumCopies = 7,
    [long]$MinimumFreeBytes = 1073741824
)
$ErrorActionPreference = 'Stop'
$archiveScript = Join-Path $PSScriptRoot 'Archive-CollecterBackup.ps1'
$archiveRoot = [IO.Path]::GetFullPath($ArchiveDirectory)
New-Item -ItemType Directory -Force -Path $archiveRoot | Out-Null
$statusPath = Join-Path $archiveRoot 'maintenance-status.json'
$logPath = Join-Path $archiveRoot 'maintenance.log'

try {
    $result = & $archiveScript -Source $Source -ArchiveDirectory $archiveRoot -MinimumFreeBytes $MinimumFreeBytes | ConvertFrom-Json
    $verified = @(Get-ChildItem -LiteralPath $archiveRoot -File | Where-Object {
        $_.Name -match '^Collecter-\d{17}-[0-9a-f]{32}\.(json|collecter)$' -and
        (Test-Path -LiteralPath ($_.FullName + '.manifest.json'))
    } | Sort-Object LastWriteTime -Descending)
    $cutoff = (Get-Date).AddDays(-[Math]::Max(1, $RetentionDays))
    $prunable = @($verified | Select-Object -Skip ([Math]::Max(1, $MinimumCopies)) | Where-Object LastWriteTime -lt $cutoff)
    foreach ($file in $prunable) {
        $manifestPath = $file.FullName + '.manifest.json'
        $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
        if ($manifest.file -ne $file.Name -or $manifest.sha256 -ne (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash) {
            throw "归档清理前校验失败：$($file.Name)"
        }
        Remove-Item -LiteralPath $file.FullName
        Remove-Item -LiteralPath $manifestPath
    }
    $status = [ordered]@{ok=$true;at=(Get-Date).ToString('o');latest=$result.file;sha256=$result.sha256;verifiedCopies=$verified.Count;pruned=$prunable.Count}
    $status | ConvertTo-Json | Set-Content -LiteralPath $statusPath -Encoding utf8
    "$(Get-Date -Format o) OK latest=$($result.file) verified=$($verified.Count) pruned=$($prunable.Count)" | Add-Content -LiteralPath $logPath -Encoding utf8
} catch {
    $status = [ordered]@{ok=$false;at=(Get-Date).ToString('o');error=$_.Exception.Message}
    $status | ConvertTo-Json | Set-Content -LiteralPath $statusPath -Encoding utf8
    "$(Get-Date -Format o) FAILED $($_.Exception.Message)" | Add-Content -LiteralPath $logPath -Encoding utf8
    throw
}
