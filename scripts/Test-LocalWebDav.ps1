[CmdletBinding()]
param([string]$ServiceRoot = "$PSScriptRoot\..\..\WebDav")
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path "$PSScriptRoot\..").Path
$service = (Resolve-Path $ServiceRoot).Path
$qaRoot = Join-Path ([IO.Path]::GetTempPath()) ('collecter-webdav-qa-' + [guid]::NewGuid())
$oldConfig=$env:WEBDAV_CONFIG; $oldRuntime=$env:WEBDAV_RUNTIME; $oldLive=$env:COLLECTOR_WEBDAV_TEST_CONFIG
$oldAndroid=$env:COLLECTOR_WEBDAV_ANDROID_TEST_CONFIG
$child=$null
New-Item -ItemType Directory -Path $qaRoot | Out-Null
try {
    @{server=@{host='127.0.0.1';port=0;data_dir=(Join-Path $qaRoot 'data')};auth=@{enabled=$true;users=@(@{username='qa';password='isolated-test-only';role='admin'})};tunnel=@{enabled=$false}} |
        ConvertTo-Json -Depth 8 | Set-Content (Join-Path $qaRoot 'server.json')
    $env:WEBDAV_CONFIG=Join-Path $qaRoot 'server.json'; $env:WEBDAV_RUNTIME=$qaRoot
    $child=Start-Process -FilePath (Get-Command node.exe).Source -ArgumentList ('"'+(Join-Path $service 'server.js')+'"') -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $qaRoot 'server.log') -RedirectStandardError (Join-Path $qaRoot 'error.log')
    for ($attempt=0; $attempt -lt 100 -and -not (Test-Path (Join-Path $qaRoot 'status.json')); $attempt++) { Start-Sleep -Milliseconds 100 }
    $state=Get-Content (Join-Path $qaRoot 'status.json') -Raw | ConvertFrom-Json
    @{url="http://127.0.0.1:$($state.port)/";username='qa';password='isolated-test-only'} | ConvertTo-Json | Set-Content (Join-Path $qaRoot 'client.json')
    $env:COLLECTOR_WEBDAV_TEST_CONFIG=Join-Path $qaRoot 'client.json'
    $env:COLLECTOR_WEBDAV_ANDROID_TEST_CONFIG=$null
    Push-Location $repo
    try {
        & .\gradlew.bat :desktop:test --tests '*WebDavLiveTest.publicHttpsBackupRoundTripWithRealStore' --rerun-tasks
        if ($LASTEXITCODE -ne 0) { throw '真实桌面 Helper 本机往返验收失败' }
        Write-Output 'PASS：隔离本机 HTTP 往返、预览取消、恢复；并非公网 HTTPS 或物理 Android 验收。'
    } finally { Pop-Location }
} finally {
    $env:WEBDAV_CONFIG=$oldConfig; $env:WEBDAV_RUNTIME=$oldRuntime; $env:COLLECTOR_WEBDAV_TEST_CONFIG=$oldLive
    $env:COLLECTOR_WEBDAV_ANDROID_TEST_CONFIG=$oldAndroid
    if ($child -and -not $child.HasExited) { $child.Kill(); $child.WaitForExit() }
    $resolved=[IO.Path]::GetFullPath($qaRoot)
    $tempBase=[IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\')+'\'
    if (-not $resolved.StartsWith($tempBase,[StringComparison]::OrdinalIgnoreCase) -or -not ([IO.Path]::GetFileName($resolved)).StartsWith('collecter-webdav-qa-')) { throw '拒绝清理非隔离目录' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
