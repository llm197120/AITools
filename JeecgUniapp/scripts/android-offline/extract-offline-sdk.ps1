# -*- coding: utf-8 -*-
# 把 Downloads 里的 DCloud Android 离线 SDK zip 解压到 C:\Users\57089\homeai-android-offline
# 用法：先从 https://nativesupport.dcloud.net.cn/AppDocs/download/android.html 下 zip，再执行本脚本
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\android-offline\extract-offline-sdk.ps1
param(
    [string]$ZipPath = '',
    [string]$DestRoot = 'C:\Users\57089\homeai-android-offline'
)

$ErrorActionPreference = 'Stop'

function Find-SdkZip {
    param([string]$Hint)
    if ($Hint) {
        if (-not (Test-Path -LiteralPath $Hint)) { throw "找不到 $Hint" }
        return (Resolve-Path $Hint).Path
    }
    $dirs = @(
        (Join-Path $env:USERPROFILE 'Downloads'),
        (Join-Path $env:USERPROFILE 'Desktop')
    )
    $found = foreach ($d in $dirs) {
        if (Test-Path -LiteralPath $d) {
            Get-ChildItem -LiteralPath $d -File -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match '^Android-SDK@.*\.zip$' -or $_.Name -match 'HBuilder-Integrate' }
        }
    }
    $best = $found | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $best) {
        throw "Downloads/桌面没有 Android-SDK@*.zip。请打开 https://nativesupport.dcloud.net.cn/AppDocs/download/android.html 下载（优先历史版本里与 HBuilderX 4.03 对应的包；没有 4.03 则下最新正式版并在打包后真机确认是否白屏）。"
    }
    return $best.FullName
}

$zip = Find-SdkZip -Hint $ZipPath
Write-Host "使用 $zip"
New-Item -ItemType Directory -Force -Path $DestRoot | Out-Null
$extract = Join-Path $DestRoot '_extract'
if (Test-Path -LiteralPath $extract) { Remove-Item -LiteralPath $extract -Recurse -Force }
Expand-Archive -LiteralPath $zip -DestinationPath $extract -Force

$integrate = Get-ChildItem -LiteralPath $extract -Directory -Recurse -Filter 'HBuilder-Integrate-AS' |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'simpleDemo') } |
    Select-Object -First 1
if (-not $integrate) { throw "zip 内未找到 HBuilder-Integrate-AS/simpleDemo" }

$target = Join-Path $DestRoot 'HBuilder-Integrate-AS'
if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
Copy-Item -LiteralPath $integrate.FullName -Destination $target -Recurse
Write-Host "已解压到 $target"
Write-Host '接下来：pnpm pack:apk:init'
