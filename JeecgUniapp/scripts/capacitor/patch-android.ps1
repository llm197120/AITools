# -*- coding: utf-8 -*-
# Patch Capacitor android/ after `cap add` / before assembleRelease
param(
    [Parameter(Mandatory = $true)][string]$AndroidDir,
    [string]$VersionName = '1.0.0',
    [string]$VersionCode = '100'
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\..\android-offline\common.ps1"

$resXml = Join-Path $AndroidDir 'app\src\main\res\xml'
New-Item -ItemType Directory -Force -Path $resXml | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot '..\android-offline\overlays\network_security_config.xml') `
    -Destination (Join-Path $resXml 'network_security_config.xml') -Force

$manifestPath = Join-Path $AndroidDir 'app\src\main\AndroidManifest.xml'
$manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8

function Add-UsesPermission([string]$Xml, [string]$Name) {
    if ($Xml -match [regex]::Escape("android:name=`"$Name`"")) { return $Xml }
    $line = "    <uses-permission android:name=`"$Name`" />`r`n"
    if ($Xml -match '<application') {
        return $Xml -replace '<application', ($line + '    <application')
    }
    return $Xml
}

$manifest = Add-UsesPermission $manifest 'android.permission.INTERNET'
$manifest = Add-UsesPermission $manifest 'android.permission.POST_NOTIFICATIONS'
$manifest = Add-UsesPermission $manifest 'android.permission.VIBRATE'
$manifest = Add-UsesPermission $manifest 'android.permission.READ_EXTERNAL_STORAGE'
$manifest = Add-UsesPermission $manifest 'android.permission.READ_MEDIA_IMAGES'
$manifest = Add-UsesPermission $manifest 'android.permission.READ_MEDIA_VIDEO'
$manifest = Add-UsesPermission $manifest 'android.permission.READ_MEDIA_VISUAL_USER_SELECTED'
$manifest = Add-UsesPermission $manifest 'android.permission.WRITE_EXTERNAL_STORAGE'
$manifest = Add-UsesPermission $manifest 'android.permission.CAMERA'
$manifest = Add-UsesPermission $manifest 'android.permission.READ_MEDIA_AUDIO'
$manifest = Add-UsesPermission $manifest 'android.permission.REQUEST_INSTALL_PACKAGES'
$manifest = Add-UsesPermission $manifest 'android.permission.ACCESS_WIFI_STATE'
$manifest = Add-UsesPermission $manifest 'android.permission.ACCESS_NETWORK_STATE'
$manifest = Add-UsesPermission $manifest 'android.permission.WAKE_LOCK'

function Add-UsesFeature([string]$Xml, [string]$Name) {
    if ($Xml -match [regex]::Escape("android:name=`"$Name`"")) { return $Xml }
    $line = "    <uses-feature android:name=`"$Name`" android:required=`"false`" />`r`n"
    if ($Xml -match '<application') {
        return $Xml -replace '<application', ($line + '    <application')
    }
    return $Xml
}
$manifest = Add-UsesFeature $manifest 'android.hardware.camera'
$manifest = Add-UsesFeature $manifest 'android.hardware.camera.autofocus'

if ($manifest -notmatch 'networkSecurityConfig') {
    $manifest = $manifest -replace '<application([^>]*)>', '<application$1 android:networkSecurityConfig="@xml/network_security_config" android:usesCleartextTraffic="true">'
} elseif ($manifest -notmatch 'usesCleartextTraffic') {
    $manifest = $manifest -replace '<application([^>]*)>', '<application$1 android:usesCleartextTraffic="true">'
}

Write-Utf8NoBom -Path $manifestPath -Content $manifest

& (Join-Path $PSScriptRoot 'sync-android-branding.ps1') -AndroidDir $AndroidDir -SkipFavicon

# 国内镜像：避免 Java 走 services.gradle.org / google maven 时 PKIX 失败
$wrapper = Join-Path $AndroidDir 'gradle\wrapper\gradle-wrapper.properties'
if (Test-Path -LiteralPath $wrapper) {
    $w = Get-Content -LiteralPath $wrapper -Raw -Encoding UTF8
    $w = $w -replace 'https\\://services\.gradle\.org/distributions/gradle-8\.2\.1-all\.zip', 'https\://mirrors.cloud.tencent.com/gradle/gradle-8.2.1-all.zip'
    Write-Utf8NoBom -Path $wrapper -Content $w
}

function Add-AliyunMavenRepos([string]$GradlePath) {
    if (-not (Test-Path -LiteralPath $GradlePath)) { return }
    $g = Get-Content -LiteralPath $GradlePath -Raw -Encoding UTF8
    if ($g -match 'maven.aliyun.com') { return }
    $aliyun = @"
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/central' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        maven { url 'https://maven.aliyun.com/repository/public' }

"@
    $g = $g -replace '(repositories\s*\{\s*)', ('$1' + $aliyun)
    Write-Utf8NoBom -Path $GradlePath -Content $g
}

$rootGradle = Join-Path $AndroidDir 'build.gradle'
Add-AliyunMavenRepos $rootGradle

$settingsGradle = Join-Path $AndroidDir 'settings.gradle'
if ((Test-Path -LiteralPath $settingsGradle) -and ((Get-Content -LiteralPath $settingsGradle -Raw -Encoding UTF8) -notmatch 'pluginManagement')) {
    $pluginMgmt = @"
pluginManagement {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        maven { url 'https://maven.aliyun.com/repository/central' }
        maven { url 'https://maven.aliyun.com/repository/public' }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

"@
    $sg = Get-Content -LiteralPath $settingsGradle -Raw -Encoding UTF8
    Write-Utf8NoBom -Path $settingsGradle -Content ($pluginMgmt + $sg)
}

# :capacitor-android 等插件工程自带 buildscript { google() }，根 allprojects 管不到
$capSettings = Join-Path $AndroidDir 'capacitor.settings.gradle'
if (Test-Path -LiteralPath $capSettings) {
    $cs = Get-Content -LiteralPath $capSettings -Raw -Encoding UTF8
    [regex]::Matches($cs, "projectDir = new File\('([^']+)'\)") | ForEach-Object {
        $rel = $_.Groups[1].Value -replace '/', [IO.Path]::DirectorySeparatorChar
        Add-AliyunMavenRepos (Join-Path (Join-Path $AndroidDir $rel) 'build.gradle')
    }
}
Add-AliyunMavenRepos (Join-Path $AndroidDir 'capacitor-cordova-android-plugins\build.gradle')

$varsGradle = Join-Path $AndroidDir 'variables.gradle'
if (Test-Path -LiteralPath $varsGradle) {
    $vg = Get-Content -LiteralPath $varsGradle -Raw -Encoding UTF8
    $vg = [regex]::Replace($vg, 'minSdkVersion\s*=\s*\d+', 'minSdkVersion = 26')
    Write-Utf8NoBom -Path $varsGradle -Content $vg
}

$appGradle = Join-Path $AndroidDir 'app\build.gradle'
if (Test-Path -LiteralPath $appGradle) {
    $g = Get-Content -LiteralPath $appGradle -Raw -Encoding UTF8
    $g = [regex]::Replace($g, 'versionCode\s+\d+', "versionCode $VersionCode")
    $g = [regex]::Replace($g, 'versionName\s+"[^"]+"', "versionName `"$VersionName`"")
    $signing = Join-Path $AndroidDir 'app\homeai-signing.gradle'
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot '..\android-offline\overlays\homeai-signing.gradle') -Destination $signing -Force
    if ($g -notmatch 'homeai-signing\.gradle') {
        $g = $g.TrimEnd() + "`r`napply from: 'homeai-signing.gradle'`r`n"
    }
    Write-Utf8NoBom -Path $appGradle -Content $g
}

# 出包时抑制 Gradle 已知噪音（flatDir / SDK XML 版本等）
$gradleProps = Join-Path $AndroidDir 'gradle.properties'
$props = if (Test-Path -LiteralPath $gradleProps) {
    Get-Content -LiteralPath $gradleProps -Raw -Encoding UTF8
} else {
    ''
}
if ($props -notmatch 'org\.gradle\.warning\.mode') {
    $props = $props.TrimEnd() + "`r`norg.gradle.warning.mode=none`r`n"
    Write-Utf8NoBom -Path $gradleProps -Content $props
}

Write-Host "patched Capacitor Android ($VersionName / $VersionCode)"
