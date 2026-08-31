<#
.SYNOPSIS
    GEMINI.md §4 Definition of Done 一键自检。

.DESCRIPTION
    把 §4 的六项检查压成一条命令，并直接输出可粘贴进汇报的 Markdown 表格。
    存在的意义：让「贴出真实自检输出」的成本降到接近 0，从而让跳过自检变成一件
    需要刻意为之、且一眼可见的事 —— 而不是一件忙起来就自然发生的事。

.PARAMETER SkipBuild
    跳过 assembleRelease 与单元测试（仅跑静态检查）。
    ⚠️ 仅用于改动过程中的快速自查；**交付汇报必须跑完整版**，否则请在汇报里写明"未验证编译与测试"。

.EXAMPLE
    .\tools\selfcheck.ps1
    .\tools\selfcheck.ps1 -SkipBuild
#>
param([switch]$SkipBuild)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root

$rows = @()
$fail = 0

function Add-Row($no, $name, $result, $ok, $note = '') {
    $script:rows += [PSCustomObject]@{
        No = $no; Name = $name; Result = $result
        Verdict = $(if ($ok) { '✅ 通过' } else { '❌ 未通过' })
        Note = $note
    }
    if (-not $ok) { $script:fail++ }
}

Write-Host "`n=== GEMINI.md §4 交付前自检 ===`n" -ForegroundColor Cyan

# ---- 1. 编译 ----
if ($SkipBuild) {
    Add-Row 1 'assembleRelease 编译' '未运行 (-SkipBuild)' $false '汇报中必须写明「未验证编译」'
} else {
    Write-Host '[1/6] 编译 assembleRelease ...' -ForegroundColor Gray
    $out = & .\gradlew.bat assembleRelease 2>&1
    $ok = ($LASTEXITCODE -eq 0)
    $err = @($out | Select-String -Pattern '^e: ').Count
    Add-Row 1 'assembleRelease 编译' $(if ($ok) { 'BUILD SUCCESSFUL' } else { "BUILD FAILED ($err 个编译错误)" }) $ok
}

# ---- 2. 单元测试 ----
if ($SkipBuild) {
    Add-Row 2 '单元测试 testReleaseUnitTest' '未运行 (-SkipBuild)' $false '汇报中必须写明「未验证测试」'
} else {
    Write-Host '[2/6] 单元测试 testReleaseUnitTest ...' -ForegroundColor Gray
    & .\gradlew.bat :app:testReleaseUnitTest 2>&1 | Out-Null
    $ok = ($LASTEXITCODE -eq 0)
    $xml = Get-ChildItem 'app\build\test-results\testReleaseUnitTest\*.xml' -ErrorAction SilentlyContinue
    $total = 0; $failed = 0
    foreach ($f in $xml) {
        $x = [xml](Get-Content $f.FullName)
        $total += [int]$x.testsuite.tests
        $failed += [int]$x.testsuite.failures + [int]$x.testsuite.errors
    }
    Add-Row 2 '单元测试 testReleaseUnitTest' "$total 个用例，$failed 个失败" ($ok -and $failed -eq 0) `
        '安全不变量回归护栏，禁止 -x test 跳过，禁止改断言让它变绿'
}

# ---- 3. 硬编码颜色 ----
Write-Host '[3/6] 检查布局硬编码颜色 ...' -ForegroundColor Gray
$colors = @(Select-String -Path 'app\src\main\res\layout\*.xml' -Pattern '#[0-9A-Fa-f]{6}' -ErrorAction SilentlyContinue)
$scanner = @($colors | Where-Object { $_.Path -match 'activity_scanner\.xml' }).Count
$other = $colors.Count - $scanner
Add-Row 3 '布局硬编码颜色' "共 $($colors.Count) 处（scanner 相机遮罩 $scanner 处属合理例外，其余 $other 处）" ($other -eq 0) `
    '见 TECH_DEBT_AUDIT P2-1；此项当前为已知欠账，未通过属预期'

# ---- 4. 空 catch ----
Write-Host '[4/6] 检查空 catch ...' -ForegroundColor Gray
$empties = @(
    Get-ChildItem -Recurse -Include *.kt -Path 'app\src\main', 'desktop\src\main' -ErrorAction SilentlyContinue |
    Select-String -Pattern 'catch \([^)]*\)\s*\{\s*(//[^\r\n]*)?\s*\}' |
    Where-Object { $_.Line -notmatch 'socket|close\(\)' }
)
Add-Row 4 '空 catch（含仅注释）' "$($empties.Count) 处" ($empties.Count -eq 0) `
    '已知良性例外：ViewExt 震动失败、BluetoothPrinterHelper 的 socket close'

# ---- 5. 版本号三处一致 ----
Write-Host '[5/6] 检查版本号一致性 ...' -ForegroundColor Gray
function Get-First($path, $pattern) {
    $m = Select-String -Path $path -Pattern $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($m -and $m.Matches[0].Groups.Count -gt 1) { $m.Matches[0].Groups[1].Value } else { '(未找到)' }
}
$vApp = Get-First 'app\build.gradle.kts' 'versionName\s*=\s*"([^"]+)"'
$vReadme = Get-First 'README.md' 'Version-v([0-9.]+)'
$vDesk = Get-First 'desktop\build.gradle.kts' '^version\s*=\s*"([^"]+)"'
$same = ($vApp -eq $vReadme -and $vApp -eq $vDesk)
Add-Row 5 '版本号三处一致' "app=$vApp / README=$vReadme / desktop=$vDesk" $same `
    'desktop 若已与 Android 解耦（见 P0-1 方案 A），此项改为「不适用」并在汇报中说明'

# ---- 6. 模块文档覆盖 ----
Write-Host '[6/6] 检查架构文档覆盖率 ...' -ForegroundColor Gray
$kt = @(Get-ChildItem 'app\src\main\java\com\kfaino\diapertracker\*.kt' | ForEach-Object { $_.Name })
$doc = @(Select-String -Path 'docs\ARCHITECTURE.md' -Pattern '`(\w+\.kt)`' -AllMatches |
    ForEach-Object { $_.Matches } | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique)
$missing = @($kt | Where-Object { $doc -notcontains $_ })
Add-Row 6 '模块文档覆盖' "$($kt.Count) 个模块，$($missing.Count) 个未记录" ($missing.Count -eq 0) `
    '见 TECH_DEBT_AUDIT P1-2；分批补录中，本次新增模块必须已补录'

# ---- 输出可粘贴的 Markdown ----
Write-Host "`n--- 以下内容整段复制进汇报，不要改写、不要只贴通过项 ---`n" -ForegroundColor Yellow

$md = @()
$md += '#### §4 交付前自检结果'
$md += ''
$md += '| # | 检查项 | 实测结果 | 判定 |'
$md += '| :-: | :--- | :--- | :--- |'
foreach ($r in $rows) {
    $md += "| $($r.No) | $($r.Name) | $($r.Result) | $($r.Verdict) |"
}
$md += ''
if ($fail -gt 0) {
    $md += "> ⚠️ 有 **$fail** 项未通过。按铁律 5，必须在汇报里逐项说明：是本次改动引入的，还是既有欠账（附 TECH_DEBT_AUDIT 编号）。"
} else {
    $md += '> ✅ 六项全部通过。'
}
$md += ''
$md += '#### 桌面端状态'
$md += ''
$md += '- [ ] 已跟进本次改动　/　- [x] 未跟进，原因：纯移动端架构文档补录，不涉及桌面端代码'
$md += ''
$md += '> 禁止以修改版本号冒充全平台交付（铁律 2）。'

$md -join "`n" | Write-Host

Pop-Location
if ($fail -gt 0) { exit 1 } else { exit 0 }
