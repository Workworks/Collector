param([string]$OutputPath = "")

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $repoRoot "app\src\main\java"
$themeFiles = @(
    (Join-Path $repoRoot "app\src\main\res\values\themes.xml"),
    (Join-Path $repoRoot "app\src\main\res\values-night\themes.xml")
)

$dialogFiles = Get-ChildItem -LiteralPath $sourceRoot -Filter *.kt -Recurse | Where-Object {
    Select-String -LiteralPath $_.FullName -Pattern 'MaterialAlertDialogBuilder|\bDialog\(|DatePickerDialog|ProgressDialog' -Quiet
}
$allKotlin = Get-ChildItem -LiteralPath $sourceRoot -Filter *.kt -Recurse
$materialCalls = ($allKotlin | Select-String -Pattern 'MaterialAlertDialogBuilder' | Measure-Object).Count
$legacy = Get-ChildItem -LiteralPath $sourceRoot -Filter *.kt -Recurse | Select-String -Pattern '^import android\.app\.(ProgressDialog|DatePickerDialog|AlertDialog)$|\bProgressDialog\(|\bDatePickerDialog\('
$themeMissing = $themeFiles | Where-Object { -not (Select-String -LiteralPath $_ -Pattern 'materialAlertDialogTheme.*ThemeOverlay\.Collecter\.AlertDialog' -Quiet) }
$enter = Get-Content -LiteralPath (Join-Path $repoRoot "app\src\main\res\anim\anim_dialog_enter.xml") -Raw
$exit = Get-Content -LiteralPath (Join-Path $repoRoot "app\src\main\res\anim\anim_dialog_exit.xml") -Raw
$motionOk = $enter -match 'fromXScale="0\.965"' -and $enter -match '<translate' -and $exit -match 'toXScale="0\.985"'
$themeSource = Get-Content -LiteralPath $themeFiles[0] -Raw
$styleCompatible = $themeSource -match 'TextAppearance\.Collecter\.Dialog\.Title" parent="MaterialAlertDialog\.Material3\.Title\.Text"' -and
    $themeSource -match 'TextAppearance\.Collecter\.Dialog\.Body" parent="MaterialAlertDialog\.Material3\.Body\.Text"'

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("# Collecter 弹框风格静态审计")
$lines.Add("")
$lines.Add("- 弹框相关 Kotlin 文件：$($dialogFiles.Count)")
$lines.Add("- MaterialAlertDialogBuilder 调用：$materialCalls")
$lines.Add("- 深浅主题统一入口：$(if($themeMissing.Count -eq 0){'PASS'}else{'FAIL'})")
$lines.Add("- 统一淡入缩放动画：$(if($motionOk){'PASS'}else{'FAIL'})")
$lines.Add("- 标题/正文样式保留 Material 布局属性：$(if($styleCompatible){'PASS'}else{'FAIL'})")
$lines.Add("- 原生旧式 Alert/DatePicker/ProgressDialog：$(if($legacy.Count -eq 0){'0，PASS'}else{"$($legacy.Count)，FAIL"})")
$lines.Add("")
$lines.Add("## 已检查文件")
$lines.Add("")
foreach ($file in $dialogFiles) { $lines.Add("- ``$($file.Name)``") }
if ($legacy.Count -gt 0) {
    $lines.Add("")
    $lines.Add("## 未通过项")
    $lines.Add("")
    foreach ($hit in $legacy) { $lines.Add("- $($hit.Path):$($hit.LineNumber) $($hit.Line.Trim())") }
}

$report = $lines -join [Environment]::NewLine
if ($OutputPath) {
    $resolvedOutput = if ([IO.Path]::IsPathRooted($OutputPath)) { $OutputPath } else { Join-Path $repoRoot $OutputPath }
    [IO.File]::WriteAllText($resolvedOutput, $report, [Text.UTF8Encoding]::new($false))
}
$report
if ($themeMissing.Count -gt 0 -or -not $motionOk -or -not $styleCompatible -or $legacy.Count -gt 0) { exit 1 }
