[CmdletBinding()]
param(
    [string]$Version = "4.3.6",
    [switch]$SkipNativeBuild
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path "$PSScriptRoot\..").Path
$distDir = Join-Path $repoRoot "dist\desktop"
New-Item -ItemType Directory -Force $distDir | Out-Null

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Collecter Standalone Desktop Packaging Pipeline" -ForegroundColor Cyan
Write-Host "Version: $Version" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. 运行桌面端与核心单元测试
Write-Host "[1/4] Running unit tests ..."
& "$repoRoot\gradlew.bat" :desktop:test testReleaseUnitTest
if ($LASTEXITCODE -ne 0) { throw "Unit tests failed!" }

# 2. 构建桌面端 Jar 包
Write-Host "[2/4] Assembling Collecter Desktop Jar ..."
& "$repoRoot\gradlew.bat" :desktop:jar
if ($LASTEXITCODE -ne 0) { throw "Jar assembly failed!" }

$jarPath = Join-Path $repoRoot "desktop\build\libs\Collecter-Desktop-$Version.jar"
if (-not (Test-Path -LiteralPath $jarPath)) {
    # 尝试寻找生成的 jar
    $foundJar = Get-ChildItem -Path (Join-Path $repoRoot "desktop\build\libs") -Filter "*.jar" | Select-Object -First 1
    if ($foundJar) {
        $jarPath = $foundJar.FullName
    }
}

# 3. 编译 Native WebView2 宿主（如果非 SkipNativeBuild 并且在 Windows 上）
if (-not $SkipNativeBuild -and $IsWindows) {
    Write-Host "[3/4] Building Native WebView2 Shell ..."
    try {
        & "$PSScriptRoot\Build-DesktopWebViewHost.ps1"
    } catch {
        Write-Warning "Native WebView2 compilation failed or skipped: $($_.Exception.Message)"
    }
} else {
    Write-Host "[3/4] Native build skipped by argument."
}

# 4. 输出分发构件
Write-Host "[4/4] Generating Release Manifest & Checksums ..."
if (Test-Path -LiteralPath $jarPath) {
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $distDir "Collecter-Desktop-Universal.jar") -Force
    $hash = (Get-FileHash -LiteralPath (Join-Path $distDir "Collecter-Desktop-Universal.jar") -Algorithm SHA256).Hash
    
    $manifest = @"
{
  "app": "Collecter Standalone Desktop",
  "version": "$Version",
  "buildTime": "$((Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))",
  "sha256": "$hash",
  "entry": "com.kfaino.collector.desktop.MainKt",
  "dataDirWindows": "%LOCALAPPDATA%\\CollecterStandalone\\data",
  "port": 8848
}
"@
    Set-Content -Path (Join-Path $distDir "release_manifest.json") -Value $manifest -Encoding UTF8
    Write-Host "Collecter Desktop Universal Artifact: $distDir\Collecter-Desktop-Universal.jar" -ForegroundColor Green
    Write-Host "SHA-256: $hash" -ForegroundColor Green
}

Write-Host "Desktop Standalone Packaging Complete!" -ForegroundColor Green
