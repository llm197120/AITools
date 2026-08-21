# -*- coding: utf-8 -*-
# Install Android command-line SDK (no Android Studio required)
param(
    [string]$SdkRoot = (Join-Path $env:LOCALAPPDATA 'Android\Sdk'),
    [switch]$SkipDownload
)

$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
}
$env:SKIP_JDK_VERSION_CHECK = '1'
$zipName = 'commandlinetools-win-15859902_latest.zip'

New-Item -ItemType Directory -Force -Path $SdkRoot | Out-Null
$sdkmanager = Join-Path $SdkRoot 'cmdline-tools\latest\bin\sdkmanager.bat'

if ($SkipDownload -or (Test-Path -LiteralPath $sdkmanager)) {
    Write-Host 'cmdline-tools already present, skip zip download'
} else {
    $tmp = Join-Path $env:TEMP $zipName
    $ok = $false
    foreach ($url in @(
        "https://dl.google.com/android/repository/$zipName",
        "https://dl.google.com.cn/android/repository/$zipName"
    )) {
        Write-Host "Download $url"
        & curl.exe -L --fail --retry 3 --connect-timeout 20 -o $tmp $url
        if ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $tmp) -and ((Get-Item -LiteralPath $tmp).Length -gt 1MB)) {
            $ok = $true
            break
        }
    }
    if (-not $ok) { throw 'Failed to download commandlinetools zip' }

    $extract = Join-Path $env:TEMP 'android-cmdline-tools-extract'
    if (Test-Path -LiteralPath $extract) { Remove-Item -LiteralPath $extract -Recurse -Force }
    Expand-Archive -LiteralPath $tmp -DestinationPath $extract -Force
    $latest = Join-Path $SdkRoot 'cmdline-tools\latest'
    New-Item -ItemType Directory -Force -Path $latest | Out-Null
    $inner = Join-Path $extract 'cmdline-tools'
    if (-not (Test-Path -LiteralPath $inner)) {
        $inner = (Get-ChildItem $extract -Directory | Select-Object -First 1).FullName
    }
    Copy-Item -Path (Join-Path $inner '*') -Destination $latest -Recurse -Force
}

if (-not (Test-Path -LiteralPath $sdkmanager)) { throw "sdkmanager not found: $sdkmanager" }

Write-Host 'Write SDK license files'
$licDir = Join-Path $SdkRoot 'licenses'
New-Item -ItemType Directory -Force -Path $licDir | Out-Null
# hashes used by Android SDK Manager (CI-style non-interactive accept)
@{
    'android-sdk-license'         = "24333f8a63b6825ea9c5514f83c2829b004d1fee"
    'android-sdk-preview-license' = "84831b9409646167bbf89b684dfb2f96111be2c9"
    'google-gdk-license'          = "33b6a2b64607f11b759f320ef9cffcbed6d6b10"
} | ForEach-Object { $_.GetEnumerator() } | ForEach-Object {
    Set-Content -LiteralPath (Join-Path $licDir $_.Key) -Value $_.Value -Encoding ASCII
}

Write-Host 'Install platform-tools / android-34 / build-tools 34.0.0'
& $sdkmanager "--sdk_root=$SdkRoot" 'platform-tools' 'platforms;android-34' 'build-tools;34.0.0'
if ($LASTEXITCODE -ne 0) { throw "sdkmanager install failed, exit=$LASTEXITCODE" }

[Environment]::SetEnvironmentVariable('ANDROID_HOME', $SdkRoot, 'User')
[Environment]::SetEnvironmentVariable('ANDROID_SDK_ROOT', $SdkRoot, 'User')
$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SDK_ROOT = $SdkRoot
Write-Host "ANDROID_HOME=$SdkRoot"
Write-Host 'Done. Next: download DCloud offline SDK, see docs/guide/android-local-apk.md'
