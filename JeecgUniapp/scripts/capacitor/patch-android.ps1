# -*- coding: utf-8 -*-
# Patch Capacitor android/ after `cap add` / before assembleRelease
param(
    [Parameter(Mandatory = $true)][string]$AndroidDir,
    [string]$VersionName = '1.0.0',
    [string]$VersionCode = '100'
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\..\android-offline\common.ps1"
$HomeaiAndroidRoot = (Resolve-Path -LiteralPath $AndroidDir).Path

function Remove-HomeaiUnusedFlatDir {
    param([string]$GradlePath)
    if (-not (Test-Path -LiteralPath $GradlePath)) { return }
    $g = Get-Content -LiteralPath $GradlePath -Raw -Encoding UTF8
    if ($null -eq $g) { $g = '' }
    if ($g -notmatch 'flatDir') { return }
    $libDir = Join-Path (Split-Path -Parent $GradlePath) 'libs'
    $hits = @()
    if (Test-Path -LiteralPath $libDir) {
        $hits = @(Get-ChildItem -LiteralPath $libDir -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Extension -eq '.jar' -or $_.Extension -eq '.aar' })
    }
    if ($hits.Count -gt 0) { return }
    $ob = [string][char]123
    $cb = [string][char]125
    $flatPat = '(?s)\s*flatDir\s*' + [regex]::Escape($ob) + '.*?' + [regex]::Escape($cb)
    $emptyRepoPat = '(?s)\r?\n\s*repositories\s*' + [regex]::Escape($ob) + '\s*' + [regex]::Escape($cb)
    $g = [regex]::Replace($g, $flatPat, '')
    $g = [regex]::Replace($g, $emptyRepoPat, "`r`n")
    Write-Utf8NoBom -Path $GradlePath -Content $g
}

$resXml = Join-Path $HomeaiAndroidRoot 'app\src\main\res\xml'
New-Item -ItemType Directory -Force -Path $resXml | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot '..\android-offline\overlays\network_security_config.xml') `
    -Destination (Join-Path $resXml 'network_security_config.xml') -Force

$manifestPath = Join-Path $HomeaiAndroidRoot 'app\src\main\AndroidManifest.xml'
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

& (Join-Path $PSScriptRoot 'sync-android-branding.ps1') -AndroidDir $HomeaiAndroidRoot -SkipFavicon

# 国内镜像：避免 Java 走 services.gradle.org / google maven 时 PKIX 失败
$wrapper = Join-Path $HomeaiAndroidRoot 'gradle\wrapper\gradle-wrapper.properties'
if (Test-Path -LiteralPath $wrapper) {
    $w = Get-Content -LiteralPath $wrapper -Raw -Encoding UTF8
    if ($null -eq $w) { $w = '' }
    $w = $w -replace 'https\\://services\.gradle\.org/distributions/gradle-8\.2\.1-all\.zip', 'https\://mirrors.cloud.tencent.com/gradle/gradle-8.2.1-all.zip'
    Write-Utf8NoBom -Path $wrapper -Content $w
}

function Add-AliyunMavenRepos([string]$GradlePath) {
    if ([string]::IsNullOrWhiteSpace($GradlePath)) { return }
    if (Test-Path -LiteralPath $GradlePath) {
        $g = Get-Content -LiteralPath $GradlePath -Raw -Encoding UTF8
        if ($null -eq $g) { $g = '' }
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
}

$rootGradle = Join-Path $HomeaiAndroidRoot 'build.gradle'
Add-AliyunMavenRepos $rootGradle

$settingsGradle = Join-Path $HomeaiAndroidRoot 'settings.gradle'
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
$capGradleSettings = Join-Path $HomeaiAndroidRoot 'capacitor.settings.gradle'
if ($capGradleSettings -and (Test-Path -LiteralPath $capGradleSettings)) {
    $cs = Get-Content -LiteralPath $capGradleSettings -Raw -Encoding UTF8
    if ($null -eq $cs) { $cs = '' }
    [regex]::Matches($cs, "projectDir = new File\('([^']+)'\)") | ForEach-Object {
        $rel = $_.Groups[1].Value -replace '/', [IO.Path]::DirectorySeparatorChar
        if ([string]::IsNullOrWhiteSpace($rel)) { return }
        $pluginDir = [System.IO.Path]::GetFullPath((Join-Path $HomeaiAndroidRoot $rel))
        $pluginGradle = Join-Path $pluginDir 'build.gradle'
        if ($pluginGradle -and (Test-Path -LiteralPath $pluginGradle)) {
            Add-AliyunMavenRepos $pluginGradle
        }
    }
}
Add-AliyunMavenRepos (Join-Path $HomeaiAndroidRoot 'capacitor-cordova-android-plugins\build.gradle')

$varsGradle = Join-Path $HomeaiAndroidRoot 'variables.gradle'
if (Test-Path -LiteralPath $varsGradle) {
    $vg = Get-Content -LiteralPath $varsGradle -Raw -Encoding UTF8
    if ($null -eq $vg) { $vg = '' }
    $vg = [regex]::Replace($vg, 'minSdkVersion\s*=\s*\d+', 'minSdkVersion = 26')
    Write-Utf8NoBom -Path $varsGradle -Content $vg
}

$appGradle = Join-Path $HomeaiAndroidRoot 'app\build.gradle'
if (Test-Path -LiteralPath $appGradle) {
    $g = Get-Content -LiteralPath $appGradle -Raw -Encoding UTF8
    if ($null -eq $g) { $g = '' }
    $g = [regex]::Replace($g, 'versionCode\s+\d+', "versionCode $VersionCode")
    $g = [regex]::Replace($g, 'versionName\s+"[^"]+"', "versionName `"$VersionName`"")
    $signing = Join-Path $HomeaiAndroidRoot 'app\homeai-signing.gradle'
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot '..\android-offline\overlays\homeai-signing.gradle') -Destination $signing -Force
    if ($g -notmatch 'homeai-signing\.gradle') {
        $g = $g.TrimEnd() + "`r`napply from: 'homeai-signing.gradle'`r`n"
    }
    Write-Utf8NoBom -Path $appGradle -Content $g
}

Remove-HomeaiUnusedFlatDir (Join-Path $HomeaiAndroidRoot 'app\build.gradle')
Remove-HomeaiUnusedFlatDir (Join-Path $HomeaiAndroidRoot 'capacitor-cordova-android-plugins\build.gradle')

# 出包时抑制 Gradle 弃用提示（压不住 AGP 的 WARNING: 行）
$gradleProps = Join-Path $HomeaiAndroidRoot 'gradle.properties'
$props = if (Test-Path -LiteralPath $gradleProps) {
    Get-Content -LiteralPath $gradleProps -Raw -Encoding UTF8
} else {
    ''
}
if ($null -eq $props) { $props = '' }
if ($props -notmatch 'org\.gradle\.warning\.mode') {
    $props = $props.TrimEnd() + "`r`norg.gradle.warning.mode=none`r`n"
    Write-Utf8NoBom -Path $gradleProps -Content $props
}

Write-Host "patched Capacitor Android ($VersionName / $VersionCode)"
