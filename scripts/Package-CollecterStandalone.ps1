[CmdletBinding()]
param([string]$Version, [switch]$SkipNativeBuild)
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path "$PSScriptRoot\..").Path
$gradleText = Get-Content -LiteralPath (Join-Path $repoRoot 'desktop\build.gradle.kts') -Raw
$declaredVersion = [regex]::Match($gradleText, '(?m)^version\s*=\s*"([^"]+)"').Groups[1].Value
if (-not $declaredVersion) { throw 'No desktop version declared' }
if ($Version -and $Version -ne $declaredVersion) { throw "Requested version $Version differs from $declaredVersion" }
$Version = $declaredVersion
$kind = if ($SkipNativeBuild) { 'jvm-development' } else { 'windows-native' }
$distDir = Join-Path $repoRoot ("dist\desktop\$Version-$kind-" + (Get-Date -Format 'yyyyMMddHHmmss'))
if (Test-Path -LiteralPath $distDir) { throw 'Output directory already exists' }
Push-Location $repoRoot
$previousQaDirectory = $env:COLLECTER_DATA_DIR
try {
    $env:COLLECTER_DATA_DIR = Join-Path $repoRoot 'desktop\build\packaging-test-data'
    & "$repoRoot\gradlew.bat" :shared:test :desktop:test :app:testReleaseUnitTest :desktop:jar
    if ($LASTEXITCODE -ne 0) { throw 'Tests/JAR build failed' }
    $jarPath = Join-Path $repoRoot "desktop\build\libs\Collecter-Desktop-$Version.jar"
    if (-not (Test-Path -LiteralPath $jarPath)) { throw "Exact version artifact missing: $jarPath" }
    if (-not $SkipNativeBuild) {
        if (-not $IsWindows) { throw 'Native Windows packaging requires Windows' }
        & "$PSScriptRoot\Build-DesktopWebViewHost.ps1"
        $native = Join-Path $repoRoot 'desktop\build\native-window\CollecterWindow.exe'
        if (-not (Test-Path -LiteralPath $native)) { throw 'Native host missing' }
    }
    New-Item -ItemType Directory -Path $distDir | Out-Null
    $outputJar = Join-Path $distDir 'Collecter-Desktop.jar'
    Copy-Item -LiteralPath $jarPath -Destination $outputJar
    if (-not $SkipNativeBuild) { Copy-Item -LiteralPath $native -Destination (Join-Path $distDir 'CollecterWindow.exe') }
    $launcherArgs = if ($SkipNativeBuild) { '' } else { '--native' }
    $launch = "@echo off`r`ncd /d `"%~dp0`"`r`njava -jar `"%~dp0Collecter-Desktop.jar`" $launcherArgs %*`r`n"
    [IO.File]::WriteAllText((Join-Path $distDir 'Start-Collecter.cmd'), $launch, [Text.Encoding]::ASCII)
    $manifest = [ordered]@{
        app = 'Collecter'; version = $Version; artifactType = $kind
        buildTime = (Get-Date).ToString('o')
        sha256 = (Get-FileHash -LiteralPath $outputJar -Algorithm SHA256).Hash
        entry = 'Start-Collecter.cmd'; requires = @('Java 17')
        nativeHostIncluded = (-not $SkipNativeBuild); signed = $false
        dataDirWindows = '%LOCALAPPDATA%\CollecterStandalone\data'; networkDefault = 'loopback-only'
    }
    if (-not $SkipNativeBuild) {
        $manifest['nativeSha256'] = (Get-FileHash -LiteralPath (Join-Path $distDir 'CollecterWindow.exe') -Algorithm SHA256).Hash
        $manifest['requires'] += 'Microsoft Edge WebView2 Runtime'
    }
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $distDir 'release_manifest.json') -Encoding utf8
    Write-Host "Verified $kind artifact: $distDir"
    if ($SkipNativeBuild) { Write-Warning 'Development JVM bundle only; not a Native installer.' }
    else { Write-Warning 'Unsigned portable bundle; clean-machine install/upgrade acceptance still required.' }
} finally {
    $env:COLLECTER_DATA_DIR = $previousQaDirectory
    Pop-Location
}
