# -*- coding: utf-8 -*-
# 一次性初始化：把 HomeAI overlay 写入本机 DCloud 离线工程
# 用法（在 JeecgUniapp 目录）：
#   pnpm pack:apk:init
# 或：
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\android-offline\init-offline-project.ps1
param(
    [string]$ConfigPath = ''
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

$uniRoot = Get-UniappRoot
$overlayDir = Join-Path $PSScriptRoot 'overlays'
$cfg = Get-PackConfig -ConfigPath $ConfigPath
$integrateAs = Resolve-IntegrateAsRoot -OfflineRoot ([string]$cfg.offlineRoot)
$moduleDir = Get-AppModuleDir -IntegrateAs $integrateAs -AppModule ([string]$cfg.appModule)
$srcMain = Join-Path $moduleDir 'src\main'
$ver = Get-ManifestVersion

Write-Host "离线工程：$integrateAs"
Write-Host "模块目录：$moduleDir"

# 若目标 keystore 不存在，尝试从 Temp 旧路径备份出来（清临时目录会丢证书）
$ksDest = [string]$cfg.keystorePath
$ksTemp = Join-Path $env:TEMP 'opencode\homeai-release.keystore'
if ($ksDest -and -not (Test-Path -LiteralPath $ksDest) -and (Test-Path -LiteralPath $ksTemp)) {
    $ksDir = Split-Path $ksDest -Parent
    if ($ksDir) { New-Item -ItemType Directory -Force -Path $ksDir | Out-Null }
    Copy-Item -LiteralPath $ksTemp -Destination $ksDest -Force
    Write-Host "已从 Temp 备份 keystore -> $ksDest （请再复制一份到密码管理器）"
}

# dcloud_control.xml
$dataDir = Join-Path $srcMain 'assets\data'
New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
$controlSrc = Join-Path $overlayDir 'dcloud_control.xml'
$controlText = (Get-Content -LiteralPath $controlSrc -Raw -Encoding UTF8) -replace 'appver="[^"]*"', ("appver=`"" + $ver.Name + "`"")
Write-Utf8NoBom -Path (Join-Path $dataDir 'dcloud_control.xml') -Content $controlText
Write-Host '已写入 assets/data/dcloud_control.xml'

# 明文 HTTP
$xmlDir = Join-Path $srcMain 'res\xml'
New-Item -ItemType Directory -Force -Path $xmlDir | Out-Null
Copy-Item -LiteralPath (Join-Path $overlayDir 'network_security_config.xml') -Destination (Join-Path $xmlDir 'network_security_config.xml') -Force
Write-Host '已写入 res/xml/network_security_config.xml'

# 签名 gradle
$signingSrc = Join-Path $overlayDir 'homeai-signing.gradle'
$signingDest = Join-Path $moduleDir 'homeai-signing.gradle'
Copy-Item -LiteralPath $signingSrc -Destination $signingDest -Force
$gradleFiles = @(
    (Join-Path $moduleDir 'build.gradle'),
    (Join-Path $moduleDir 'build.gradle.kts')
) | Where-Object { Test-Path -LiteralPath $_ }
if (-not $gradleFiles) { throw "未找到 $moduleDir\build.gradle" }
$gradlePath = $gradleFiles[0]
$gradleText = Get-Content -LiteralPath $gradlePath -Raw -Encoding UTF8
if ($gradleText -notmatch 'homeai-signing\.gradle') {
    if ($gradlePath -like '*.kts') {
        $gradleText = $gradleText.TrimEnd() + "`r`n`r`napply(from = `"homeai-signing.gradle`")`r`n"
    } else {
        $gradleText = $gradleText.TrimEnd() + "`r`n`r`napply from: 'homeai-signing.gradle'`r`n"
    }
    Write-Utf8NoBom -Path $gradlePath -Content $gradleText
    Write-Host "已在 $(Split-Path $gradlePath -Leaf) 追加 homeai-signing.gradle"
} else {
    Write-Host 'build.gradle 已引用 homeai-signing.gradle'
}

if ($gradleText -notmatch [regex]::Escape($script:HomeaiPackageName)) {
    Write-Warning "请把 applicationId 改成 $($script:HomeaiPackageName)（当前 build.gradle 未检测到该包名）"
}

# AndroidManifest：权限 / 明文 / AppKey
$manifestPath = Join-Path $srcMain 'AndroidManifest.xml'
if (-not (Test-Path -LiteralPath $manifestPath)) { throw "未找到 $manifestPath" }
$mf = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8

function Add-UsesPermission {
    param([string]$Xml, [string]$Permission)
    $needle = "android.permission.$Permission"
    if ($Xml -match [regex]::Escape($needle)) { return $Xml }
    $line = "    <uses-permission android:name=`"android.permission.$Permission`" />`r`n"
    return [regex]::Replace($Xml, '</manifest>', ($line + '</manifest>'), 1)
}

$mf = Add-UsesPermission $mf 'POST_NOTIFICATIONS'
$mf = Add-UsesPermission $mf 'INTERNET'
$mf = Add-UsesPermission $mf 'VIBRATE'
$mf = Add-UsesPermission $mf 'CAMERA'
$mf = Add-UsesPermission $mf 'WAKE_LOCK'
$mf = Add-UsesPermission $mf 'ACCESS_NETWORK_STATE'
$mf = Add-UsesPermission $mf 'ACCESS_WIFI_STATE'
$mf = Add-UsesPermission $mf 'WRITE_EXTERNAL_STORAGE'
$mf = Add-UsesPermission $mf 'READ_MEDIA_IMAGES'
$mf = Add-UsesPermission $mf 'READ_MEDIA_VIDEO'

function Add-ApplicationAttr {
    param([string]$Xml, [string]$Name, [string]$Value)
    if ($Xml -match ("android:" + $Name + "\s*=")) { return $Xml }
    return [regex]::Replace(
        $Xml,
        '<application\b([^>]*?)>',
        { param($m) "<application$($m.Groups[1].Value) android:$Name=`"$Value`">" },
        1
    )
}

$mf = Add-ApplicationAttr $mf 'usesCleartextTraffic' 'true'
$mf = Add-ApplicationAttr $mf 'networkSecurityConfig' '@xml/network_security_config'

$appKey = [string]$cfg.dcloudAppKey
if ([string]::IsNullOrWhiteSpace($appKey) -or $appKey -eq 'YOUR_DCLOUD_APPKEY') {
    $appKey = 'YOUR_DCLOUD_APPKEY'
    Write-Warning 'dcloudAppKey 尚未填写。到 https://dev.dcloud.net.cn 申请离线打包 Key 后写入 android-pack.local.json 再跑一次 init。'
}
if ($mf -match 'android:name="dcloud_appkey"') {
    $mf = [regex]::Replace($mf, '(android:name="dcloud_appkey"\s+android:value=")[^"]*"', ('${1}' + $appKey + '"'), 1)
    $mf = [regex]::Replace($mf, '(android:value=")[^"]*("\s+android:name="dcloud_appkey")', ('${1}' + $appKey + '${2}'), 1)
} else {
    $meta = "        <meta-data android:name=`"dcloud_appkey`" android:value=`"$appKey`" />`r`n"
    $mf = [regex]::Replace($mf, '<application\b([^>]*?)>', { param($m) $m.Value + "`r`n" + $meta }, 1)
}

Write-Utf8NoBom -Path $manifestPath -Content $mf
Write-Host '已更新 AndroidManifest.xml（权限 / 明文 HTTP / dcloud_appkey）'

# 启动图标（仓库若暂无图标则跳过）
$iconMap = @{
    '72x72.png'   = 'mipmap-hdpi'
    '96x96.png'   = 'mipmap-xhdpi'
    '144x144.png' = 'mipmap-xxhdpi'
    '192x192.png' = 'mipmap-xxxhdpi'
}
$iconRoot = Join-Path $uniRoot 'src\static\app\icons'
$copiedIcon = $false
foreach ($file in $iconMap.Keys) {
    $srcIcon = Join-Path $iconRoot $file
    if (-not (Test-Path -LiteralPath $srcIcon)) { continue }
    $dpiDir = Join-Path $srcMain ("res\" + $iconMap[$file])
    New-Item -ItemType Directory -Force -Path $dpiDir | Out-Null
    Copy-Item -LiteralPath $srcIcon -Destination (Join-Path $dpiDir 'icon.png') -Force
    Copy-Item -LiteralPath $srcIcon -Destination (Join-Path $dpiDir 'ic_launcher.png') -Force
    $copiedIcon = $true
}
if ($copiedIcon) { Write-Host '已拷贝启动图标到 res/mipmap-*' } else { Write-Warning "未找到 $iconRoot，离线工程将使用 SDK 自带图标" }

# 模块检查：本地通知需要 Push，不要配 UniPush
$props = Join-Path $dataDir 'dcloud_properties.xml'
if (Test-Path -LiteralPath $props) {
    $pt = Get-Content -LiteralPath $props -Raw -Encoding UTF8
    foreach ($feat in @('Camera', 'Gallery', 'Push')) {
        if ($pt -notmatch ("name=`"" + $feat + "`"")) {
            Write-Warning "dcloud_properties.xml 未声明 Feature $feat。请对照 SDK 包内 Feature-Android.xls 补上（Push 用 APSFeatureImpl，不要加 unipush 模块）。"
        }
    }
    if ($pt -match 'unipush') {
        Write-Warning '检测到 unipush。本项目只用 plus.push.createMessage 本地通知，请去掉个推/厂商通道相关 aar 与 unipush 模块。'
    }
} else {
    Write-Warning '未找到 dcloud_properties.xml（simpleDemo 应自带）。导入官方 Integrate-AS 工程后再跑 init。'
}

Write-Host ''
Write-Host '初始化完成。接下来请手工确认：'
Write-Host "  1. applicationId = $($script:HomeaiPackageName)"
Write-Host '  2. 离线 SDK 版本与 CLI（当前 4.03 / 3.0.0-4030620241128001）一致，或先 uvm 再换新 SDK'
Write-Host '  3. libs 只保留基础库 + Camera/Gallery + 本地 Push（aps-release），去掉地图/支付/分享/UniPush'
Write-Host '  4. 填好 android-pack.local.json 的密码与 dcloudAppKey'
Write-Host '  5. 用 Android Studio 打开 HBuilder-Integrate-AS，同步 Gradle 一次'
Write-Host '然后执行：pnpm pack:apk'
