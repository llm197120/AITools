# -*- coding: utf-8 -*-
# [已弃用] DCloud 离线 SDK 出包。发版请用：pnpm pack:apk:local
# 用法（在 JeecgUniapp 目录）：
#   pnpm pack:apk
#   pnpm pack:apk -- --SkipBuild
#   pnpm pack:apk -- --Upload -Version 1.0.0
param(
    [switch]$SkipBuild,
    [switch]$SkipGradle,
    [switch]$Upload,
    [string]$Version = '',
    [string]$ConfigPath = ''
)

$ErrorActionPreference = 'Stop'
throw '已弃用 DCloud 离线 SDK 出包。请改用 pnpm pack:apk:local（H5 + Capacitor）。'
. "$PSScriptRoot\common.ps1"

$uniRoot = Get-UniappRoot
$cfg = Get-PackConfig -ConfigPath $ConfigPath
$integrateAs = Resolve-IntegrateAsRoot -OfflineRoot ([string]$cfg.offlineRoot)
$moduleDir = Get-AppModuleDir -IntegrateAs $integrateAs -AppModule ([string]$cfg.appModule)
$appId = $script:HomeaiAppId
$wwwDir = Get-AppWwwDir -ModuleDir $moduleDir -AppId $appId
$ver = Get-ManifestVersion
if (-not $Version) { $Version = $ver.Name }
$stamp = Get-PackTimestamp

Write-Host "UniApp 根目录：$uniRoot"
Write-Host "离线工程：$integrateAs"
Write-Host "目标 www：$wwwDir"
if (-not $env:ANDROID_HOME) {
    $defaultSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path -LiteralPath $defaultSdk) {
        $env:ANDROID_HOME = $defaultSdk
        $env:ANDROID_SDK_ROOT = $defaultSdk
        Write-Host "ANDROID_HOME=$defaultSdk"
    }
}
if (-not $env:JAVA_HOME) {
    Write-Warning '未设置 JAVA_HOME。本机若已装 JDK 17，建议指向该目录后再打 Gradle 包。'
}

if (-not $SkipBuild) {
    Write-Host '编译 app-android 资源…'
    Push-Location $uniRoot
    try {
        pnpm build:app-android
        if ($LASTEXITCODE -ne 0) { throw "pnpm build:app-android 失败，exit=$LASTEXITCODE" }
    } finally {
        Pop-Location
    }
}

Copy-AppResources -WwwDir $wwwDir -AppId $appId

# 同步资源包版本到 dcloud_control.xml
$controlPath = Join-Path $moduleDir 'src\main\assets\data\dcloud_control.xml'
if (Test-Path -LiteralPath $controlPath) {
    $control = Get-Content -LiteralPath $controlPath -Raw -Encoding UTF8
    $control = $control -replace 'appid="[^"]*"', ("appid=`"$appId`"")
    $control = $control -replace 'appver="[^"]*"', ("appver=`"$Version`"")
    Write-Utf8NoBom -Path $controlPath -Content $control
} else {
    Write-Warning '未找到 dcloud_control.xml，请先运行 pnpm pack:apk:init'
}

Write-KeystoreProperties -ModuleDir $moduleDir -Config $cfg | Out-Null

if ($SkipGradle) {
    Write-Host '已跳过 Gradle。资源已就位，可在 Android Studio 里点 Generate Signed APK。'
    exit 0
}

$gradlew = Join-Path $integrateAs 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradlew)) {
    throw @"
未找到 $gradlew
请先用 Android Studio 打开 $integrateAs 并完成一次 Gradle Sync（会生成 wrapper）。
"@
}

$moduleName = [string]$cfg.appModule
Write-Host "Gradle :${moduleName}:assembleRelease …"
Push-Location $integrateAs
try {
    & $gradlew ":${moduleName}:assembleRelease" --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradle 打包失败，exit=$LASTEXITCODE" }
} finally {
    Pop-Location
}

$apkDir = Join-Path $moduleDir 'build\outputs\apk\release'
$apk = Get-ChildItem -LiteralPath $apkDir -Filter '*.apk' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch 'unsigned' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $apk) {
    $apk = Get-ChildItem -LiteralPath $apkDir -Filter '*.apk' -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}
if (-not $apk) { throw "未找到 APK：$apkDir" }

$outDir = Join-Path $uniRoot 'dist\apk'
$outApk = Copy-StampedApk -SourceApk $apk.FullName -OutDir $outDir -VersionName $ver.Name -Stamp $stamp
Write-Host ("来源 " + $apk.FullName)

if ($Upload) {
    $uploadScript = Join-Path $uniRoot '..\JeecgBoot\deploy\frp\upload-apk.ps1'
    $uploadScript = (Resolve-Path $uploadScript).Path
    Write-Host "上传 $outApk …"
    & $uploadScript -ApkPath $outApk -Version $Version
}

Write-Host '完成。侧载安装前请卸载旧包名冲突的包；包名必须是 com.homeai.app。'
