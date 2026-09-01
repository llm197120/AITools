# -*- coding: utf-8 -*-
# Long-term local APK: uni-app H5 + Capacitor (no DCloud native SDK)
# Usage (JeecgUniapp):
#   pnpm pack:apk:local
# First time: pnpm pack:apk:local -- -InitAndroid
param(
    [switch]$SkipBuild,
    [switch]$InitAndroid,
    [switch]$Upload,
    [string]$Version = ''
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\..\android-offline\common.ps1"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
. (Join-Path $repoRoot 'docs\deploy\common.ps1')
Set-HomeaiBuildQuietEnv

$uniRoot = Get-UniappRoot
$cfgPath = Join-Path $uniRoot 'android-pack.local.json'
if (-not (Test-Path -LiteralPath $cfgPath)) {
    throw "Missing $cfgPath (copy android-pack.local.json.example and fill keystorePath / passwords)"
}
$cfg = Get-Content -LiteralPath $cfgPath -Raw -Encoding UTF8 | ConvertFrom-Json
$androidDir = Join-Path $uniRoot 'android'
$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Java\jdk-17' }
if (-not $env:ANDROID_HOME) {
    $defaultSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path -LiteralPath $defaultSdk) {
        $env:ANDROID_HOME = $defaultSdk
        $env:ANDROID_SDK_ROOT = $defaultSdk
    }
}
if (-not $env:ANDROID_HOME) {
    throw 'ANDROID_HOME is empty. Run pnpm pack:apk:sdk or install Android cmdline tools.'
}

$ver = Get-ManifestVersion
if (-not $Version) { $Version = $ver.Name }
$stamp = Get-PackTimestamp
$androidVersionName = "$($ver.Name)+$stamp"

# H5 构建会打进 favicon.ico，须在 build:h5 之前生成
& (Join-Path $PSScriptRoot 'sync-android-branding.ps1')

Push-Location $uniRoot
try {
    # webDir must exist before `cap add`
    if (-not $SkipBuild) {
        Write-Host 'pnpm build:h5'
        pnpm build:h5
        if ($LASTEXITCODE -ne 0) { throw "build:h5 failed, exit=$LASTEXITCODE" }
    } elseif (-not (Test-Path -LiteralPath (Join-Path $uniRoot 'dist\build\h5\index.html'))) {
        throw 'dist/build/h5 is missing; run without -SkipBuild first'
    }

    if ($InitAndroid -or -not (Test-Path -LiteralPath (Join-Path $androidDir 'gradlew.bat'))) {
        if (-not (Test-Path -LiteralPath (Join-Path $uniRoot 'node_modules\@capacitor\android'))) {
            throw 'Missing @capacitor/android. In JeecgUniapp run: pnpm add @capacitor/core@6.2.1 @capacitor/cli@6.2.1 @capacitor/android@6.2.1 @capacitor/app@6.0.2'
        }
        Write-Host 'npx cap add android (first time)'
        npx --yes cap add android
        if ($LASTEXITCODE -ne 0 -and -not (Test-Path -LiteralPath $androidDir)) {
            throw "cap add android failed, exit=$LASTEXITCODE"
        }
    }

    Write-Host 'npx cap sync android'
    npx --yes cap sync android
    if ($LASTEXITCODE -ne 0) { throw "cap sync failed, exit=$LASTEXITCODE" }
} finally {
    Pop-Location
}

& (Join-Path $PSScriptRoot 'patch-android.ps1') -AndroidDir $androidDir -VersionName $androidVersionName -VersionCode $ver.Code
Write-KeystoreProperties -ModuleDir (Join-Path $androidDir 'app') -Config $cfg | Out-Null

$localProps = Join-Path $androidDir 'local.properties'
$sdkHome = ($env:ANDROID_HOME -replace '\\', '/')
Write-Utf8NoBom -Path $localProps -Content ("sdk.dir=$sdkHome`n")

# Java 走 HTTPS 常 PKIX；用 Windows 证书下载发行包后改 file://
$gradleZipDir = Join-Path $env:USERPROFILE '.gradle\wrapper\manual'
$gradleZip = Join-Path $gradleZipDir 'gradle-8.2.1-all.zip'
if (-not (Test-Path -LiteralPath $gradleZip)) {
    New-Item -ItemType Directory -Force -Path $gradleZipDir | Out-Null
    Write-Host 'Downloading gradle-8.2.1-all.zip (Tencent mirror)'
    Invoke-WebRequest -Uri 'https://mirrors.cloud.tencent.com/gradle/gradle-8.2.1-all.zip' -OutFile $gradleZip -UseBasicParsing
}
$wrapper = Join-Path $androidDir 'gradle\wrapper\gradle-wrapper.properties'
$unixZip = ($gradleZip -replace '\\', '/')
$w = Get-Content -LiteralPath $wrapper -Raw -Encoding UTF8
$w = $w -replace '(?m)^distributionUrl=.*', ("distributionUrl=file\:///" + $unixZip)
$w = $w -replace '(?m)^validateDistributionUrl=.*', 'validateDistributionUrl=false'
Write-Utf8NoBom -Path $wrapper -Content $w

$gradlew = Join-Path $androidDir 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradlew)) { throw "Missing $gradlew — run with -InitAndroid" }

$initAliyun = Join-Path $PSScriptRoot 'init-aliyun.gradle'
Write-Host 'Gradle assembleRelease'
Push-Location $androidDir
try {
    & $gradlew ':app:assembleRelease' --no-daemon --warning-mode none --init-script $initAliyun
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed, exit=$LASTEXITCODE" }
} finally {
    Pop-Location
}

$apkDir = Join-Path $androidDir 'app\build\outputs\apk\release'
$apk = Get-ChildItem -LiteralPath $apkDir -Filter '*.apk' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch 'unsigned' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $apk) { throw "APK not found in $apkDir" }

$outDir = Join-Path $uniRoot 'dist\apk'
$outApk = Copy-StampedApk -SourceApk $apk.FullName -OutDir $outDir -VersionName $ver.Name -Stamp $stamp

$h5Dir = Join-Path $uniRoot 'dist\build\h5'
if (Test-Path -LiteralPath (Join-Path $h5Dir 'index.html')) {
    $zipPath = Join-Path $outDir ("homeai-h5-" + $ver.Name + ".zip")
    if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::CreateFromDirectory($h5Dir, $zipPath)
    Write-Host "H5 zip: $zipPath"
}

# 产物元数据：发布脚本据此自动登记版本（publish-all.ps1 -RegisterVersion）
$zipForMeta = if (Test-Path -LiteralPath (Join-Path $h5Dir 'index.html')) { $zipPath } else { '' }
$meta = @{
    apk         = $outApk
    zip         = $zipForMeta
    versionName = $ver.Name
    versionCode = [int]$ver.Code
} | ConvertTo-Json
$lastVersion = Join-Path $outDir 'last-version.json'
Write-Utf8NoBom -Path $lastVersion -Content $meta
Write-Host "last-version: $lastVersion"

if ($Upload) {
    $uploadScript = (Resolve-Path (Join-Path $uniRoot '..\JeecgBoot\deploy\frp\upload-apk.ps1')).Path
    & $uploadScript -ApkPath $outApk -Version $Version
}
