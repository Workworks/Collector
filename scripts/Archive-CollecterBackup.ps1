[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Source,
    [Parameter(Mandatory)][string]$ArchiveDirectory,
    [long]$MinimumFreeBytes = 1073741824
)
$ErrorActionPreference='Stop'
# Explicit, one-shot offline archive. Never reads WebDAV credentials or prunes old archives.
$sourcePath=(Resolve-Path -LiteralPath $Source).Path
$sourceItem=Get-Item -LiteralPath $sourcePath
if($sourceItem.PSIsContainer -or ($sourceItem.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw '源必须是普通备份文件' }
if($sourceItem.Length -gt 67108992) { throw '源文件超过备份大小上限' }
$destination=[IO.Path]::GetFullPath($ArchiveDirectory)
if(Test-Path -LiteralPath $destination) {
    if((Get-Item -LiteralPath $destination).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw '归档目录不能是链接' }
}
New-Item -ItemType Directory -Force -Path $destination | Out-Null
$drive=[IO.DriveInfo]::new([IO.Path]::GetPathRoot($destination))
if($drive.AvailableFreeSpace -lt ($MinimumFreeBytes+$sourceItem.Length)) { throw '可用磁盘空间低于归档安全阈值' }
$extension=[IO.Path]::GetExtension($sourcePath)
if($extension -notin @('.json','.collecter')) { throw '仅接受 .json 或 .collecter 备份' }
$name='Collecter-'+(Get-Date -Format yyyyMMddHHmmssfff)+'-'+[guid]::NewGuid().ToString('N')+$extension
$output=Join-Path $destination $name
$partial=$output+'.partial'
try {
    $before=(Get-FileHash -LiteralPath $sourcePath -Algorithm SHA256).Hash
    Copy-Item -LiteralPath $sourcePath -Destination $partial
    $copied=(Get-FileHash -LiteralPath $partial -Algorithm SHA256).Hash
    $after=(Get-FileHash -LiteralPath $sourcePath -Algorithm SHA256).Hash
    if($before -ne $copied -or $before -ne $after) { throw '复制期间源发生变化或副本校验不一致，请重新导出后归档' }
    Move-Item -LiteralPath $partial -Destination $output
    $manifest=[ordered]@{file=$name;sha256=$copied;bytes=(Get-Item -LiteralPath $output).Length;archivedAt=(Get-Date).ToString('o');freeBytes=$drive.AvailableFreeSpace;scope='byte-copy-verified; perform application restore separately'}
    $manifest | ConvertTo-Json | Set-Content -LiteralPath ($output+'.manifest.json') -Encoding utf8
    $manifest | ConvertTo-Json
} finally {
    # Only this invocation's uniquely named staging file, no directory deletion.
    if(Test-Path -LiteralPath $partial) { Remove-Item -LiteralPath $partial }
}
