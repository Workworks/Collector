[CmdletBinding()]
param(
    [string]$OutputDirectory,
    [string]$WebView2Version = "1.0.4129.50"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path "$PSScriptRoot\..").Path
$targetRoot = [System.IO.Path]::GetFullPath((Join-Path $repoRoot "desktop\build"))
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $targetRoot "native-window"
}
$outputRoot = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force $outputRoot | Out-Null

$expected = @{
    "1.0.4129.50" = "D3934F482D484B89FB4825DF720C710664E1143A1E90F7B3A60794EF33F473D2"
}
if (-not $expected.ContainsKey($WebView2Version)) {
    throw "WebView2 SDK version is not pinned with a reviewed SHA-256: $WebView2Version"
}

$dependency = Join-Path $targetRoot "webview2-sdk"
$package = Join-Path $dependency "microsoft.web.webview2.$WebView2Version.nupkg"
$expanded = Join-Path $dependency "package-$WebView2Version"
New-Item -ItemType Directory -Force $dependency,$outputRoot | Out-Null
if (-not (Test-Path -LiteralPath $package)) {
    Write-Host "Downloading WebView2 SDK $WebView2Version ..."
    $uri = "https://api.nuget.org/v3-flatcontainer/microsoft.web.webview2/$WebView2Version/microsoft.web.webview2.$WebView2Version.nupkg"
    Invoke-WebRequest -UseBasicParsing -Uri $uri -OutFile $package
}
$actualHash = (Get-FileHash -LiteralPath $package -Algorithm SHA256).Hash
if ($actualHash -ne $expected[$WebView2Version]) {
    throw "WebView2 SDK SHA-256 mismatch. Expected $($expected[$WebView2Version]), got $actualHash"
}
if (-not (Test-Path -LiteralPath (Join-Path $expanded "build\native\include\WebView2.h"))) {
    $zip = Join-Path $dependency "package-$WebView2Version.zip"
    Copy-Item -LiteralPath $package -Destination $zip -Force
    Expand-Archive -LiteralPath $zip -DestinationPath $expanded -Force
}

$vswhere = Join-Path ${env:ProgramFiles(x86)} "Microsoft Visual Studio\Installer\vswhere.exe"
if (-not (Test-Path -LiteralPath $vswhere)) { 
    Write-Warning "Visual Studio Build Tools not found via vswhere.exe. Native compilation skipped."
    return
}
$visualStudio = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if ([string]::IsNullOrWhiteSpace($visualStudio)) { 
    Write-Warning "MSVC x64 build tools are not installed. Native compilation skipped."
    return
}
$vcvars = Join-Path $visualStudio "VC\Auxiliary\Build\vcvarsall.bat"

$source = Join-Path $repoRoot "desktop\src\main\native\webview2\CollecterWindow.cpp"
$include = Join-Path $expanded "build\native\include"
$loader = Join-Path $expanded "build\native\x64\WebView2LoaderStatic.lib"
$executable = Join-Path $outputRoot "CollecterWindow.exe"

Write-Host "Compiling Native WebView2 Host CollecterWindow.exe ..."
$cmd = "`"$vcvars`" x64 && cl.exe /nologo /std:c++17 /O2 /W4 /EHsc /utf-8 /I`"$include`" `"$source`" `"$loader`" user32.lib gdi32.lib shell32.lib ole32.lib oleaut32.lib advapi32.lib /Fe`"$executable`" /link /SUBSYSTEM:WINDOWS"
cmd.exe /c $cmd

if (Test-Path -LiteralPath $executable) {
    Write-Host "Native WebView2 Host build SUCCESS: $executable" -ForegroundColor Green
} else {
    Write-Warning "Compilation completed without producing $executable"
}
