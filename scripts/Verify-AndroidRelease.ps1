[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$PreviousApk,
    [Parameter(Mandatory)][string]$CandidateApk,
    [string]$SdkRoot = "$env:LOCALAPPDATA\Android\Sdk"
)
$ErrorActionPreference = 'Stop'
$signer = Get-ChildItem -LiteralPath (Join-Path $SdkRoot 'build-tools') -Filter apksigner.bat -Recurse |
    Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
if (-not $signer) { throw '缺少 apksigner，发布门禁拒绝继续' }
function Read-Certificate([string]$File) {
    $resolved = (Resolve-Path -LiteralPath $File).Path
    $output = & $signer verify --print-certs $resolved 2>&1
    if ($LASTEXITCODE -ne 0) { throw "APK 验签失败：$resolved" }
    $fingerprints = @($output | Select-String '^Signer #\d+ certificate SHA-256 digest: (.+)$' | ForEach-Object { $_.Matches[0].Groups[1].Value.Trim().ToLowerInvariant() })
    if ($fingerprints.Count -ne 1) { throw '目前门禁仅接受单一且可识别的签名；不得自动放行多签名或未知轮换' }
    return $fingerprints[0]
}
$old = Read-Certificate $PreviousApk
$new = Read-Certificate $CandidateApk
Write-Output "Previous SHA256 certificate: $old"
Write-Output "Candidate SHA256 certificate: $new"
if ($old -ne $new) { throw 'SIGNATURE-NOT-COMPATIBLE：禁止发布；必须恢复原签名，不得卸载旧版绕过' }
Write-Output '签名一致。仍需包名、版本号、真实覆盖升级和发布验收；此脚本不会发布。'
