# -*- coding: utf-8 -*-
# 从 src/static/app/icons/1024x1024.png 生成 Capacitor Android 品牌资源 + favicon
param(
    [string]$AndroidDir = '',
    [switch]$SkipFavicon
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\..\android-offline\common.ps1"

$uniRoot = Get-UniappRoot
$srcPng = Join-Path $uniRoot 'src\static\app\icons\1024x1024.png'
if (-not (Test-Path -LiteralPath $srcPng)) {
    throw "未找到 HA 源图标：$srcPng"
}

if (-not ('HomeaiBrandAssets' -as [type])) {
    $cs = Join-Path $PSScriptRoot 'HomeaiBrandAssets.cs'
    Add-Type -Path $cs -ReferencedAssemblies @('System.Drawing')
}

$resDir = ''
if ($AndroidDir) {
    $resDir = Join-Path $AndroidDir 'app\src\main\res'
} else {
    $guess = Join-Path $uniRoot 'android\app\src\main\res'
    if (Test-Path -LiteralPath $guess) { $resDir = $guess }
}

$favicon = if ($SkipFavicon) { '' } else { Join-Path $uniRoot 'favicon.ico' }
$summary = [HomeaiBrandAssets]::Generate($srcPng, $resDir, $favicon)
if ($resDir) { Write-Host "HA 品牌资源 -> $resDir ($summary)" }
if ($favicon) { Write-Host "favicon -> $favicon" }
