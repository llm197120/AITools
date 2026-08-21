# -*- coding: utf-8 -*-
# 本地 APK 打包共用函数（勿直接执行）

$script:AndroidOfflineDir = $PSScriptRoot
$script:HomeaiAppId = '__UNI__864D2D7'
$script:HomeaiPackageName = 'com.homeai.app'

function Write-Utf8NoBom {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)][string]$Content)
    $enc = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($Path, $Content, $enc)
}

function Get-UniappRoot {
    (Resolve-Path (Join-Path $script:AndroidOfflineDir '..\..')).Path
}

function Get-PackConfigPath {
    param([string]$Override = '')
    if ($Override) { return (Resolve-Path $Override).Path }
    Join-Path (Get-UniappRoot) 'android-pack.local.json'
}

function Get-PackConfig {
    param([string]$ConfigPath = '')
    $path = Get-PackConfigPath -Override $ConfigPath
    $example = Join-Path (Get-UniappRoot) 'android-pack.local.json.example'
    if (-not (Test-Path -LiteralPath $path)) {
        throw @"
未找到本地打包配置：$path
请复制示例并填入离线 SDK / keystore / AppKey：
  Copy-Item '$example' '$path'
"@
    }
    $cfg = Get-Content -LiteralPath $path -Raw -Encoding UTF8 | ConvertFrom-Json
    if (-not $cfg.offlineRoot) { throw 'android-pack.local.json 缺少 offlineRoot' }
    if (-not $cfg.appModule) { $cfg | Add-Member -NotePropertyName appModule -NotePropertyValue 'simpleDemo' -Force }
    if (-not $cfg.keystoreAlias) { $cfg | Add-Member -NotePropertyName keystoreAlias -NotePropertyValue 'homeai' -Force }
    return $cfg
}

function Resolve-IntegrateAsRoot {
    param([Parameter(Mandatory = $true)][string]$OfflineRoot)
    $root = $OfflineRoot.TrimEnd('\', '/')
    if (-not (Test-Path -LiteralPath $root)) {
        throw "离线工程目录不存在：$root"
    }
    $candidates = @(
        $root,
        (Join-Path $root 'HBuilder-Integrate-AS'),
        (Join-Path $root 'HBuilder-Integrate-AS\HBuilder-Integrate-AS')
    )
    foreach ($c in $candidates) {
        if (-not (Test-Path -LiteralPath $c)) { continue }
        if (Test-Path -LiteralPath (Join-Path $c 'simpleDemo')) { return (Resolve-Path $c).Path }
        if ((Split-Path $c -Leaf) -eq 'simpleDemo') { return (Resolve-Path (Split-Path $c -Parent)).Path }
    }
    throw @"
无法在 $root 下找到 HBuilder-Integrate-AS / simpleDemo。
offlineRoot 请指向解压后的 HBuilder-Integrate-AS 目录（其下应有 simpleDemo）。
"@
}

function Get-AppModuleDir {
    param(
        [Parameter(Mandatory = $true)][string]$IntegrateAs,
        [string]$AppModule = 'simpleDemo'
    )
    $dir = Join-Path $IntegrateAs $AppModule
    if (-not (Test-Path -LiteralPath $dir)) {
        throw "找不到模块目录：$dir"
    }
    return (Resolve-Path $dir).Path
}

function Get-AppWwwDir {
    param(
        [Parameter(Mandatory = $true)][string]$ModuleDir,
        [string]$AppId = $script:HomeaiAppId
    )
    Join-Path $ModuleDir "src\main\assets\apps\$AppId\www"
}

function Get-ManifestVersion {
    $manifest = Join-Path (Get-UniappRoot) 'src\manifest.json'
    if (-not (Test-Path -LiteralPath $manifest)) {
        return @{ Name = '1.0.0'; Code = '100' }
    }
    $json = Get-Content -LiteralPath $manifest -Raw -Encoding UTF8 | ConvertFrom-Json
    return @{
        Name = [string]$json.versionName
        Code = [string]$json.versionCode
    }
}

function Get-PackTimestamp {
    Get-Date -Format 'yyyyMMdd-HHmmss'
}

function Get-SafeVersionLabel {
    param([string]$VersionName)
    $s = [string]$VersionName
    if ([string]::IsNullOrWhiteSpace($s)) { $s = '0.0.0' }
    return ($s -replace '[\\/:*?"<>|\s]', '-')
}

# Copy to dist/apk/homeai-{version}-{yyyyMMdd-HHmmss}.apk, also write homeai-release.apk
function Copy-StampedApk {
    param(
        [Parameter(Mandatory = $true)][string]$SourceApk,
        [Parameter(Mandatory = $true)][string]$OutDir,
        [Parameter(Mandatory = $true)][string]$VersionName,
        [Parameter(Mandatory = $true)][string]$Stamp
    )
    $label = Get-SafeVersionLabel -VersionName $VersionName
    $name = "homeai-$label-$Stamp.apk"
    New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
    $outApk = Join-Path $OutDir $name
    Copy-Item -LiteralPath $SourceApk -Destination $outApk -Force
    $latest = Join-Path $OutDir 'homeai-release.apk'
    Copy-Item -LiteralPath $outApk -Destination $latest -Force
    Write-Host ("APK -> " + $outApk)
    Write-Host ("latest -> " + $latest)
    return $outApk
}

function Write-KeystoreProperties {
    param(
        [Parameter(Mandatory = $true)][string]$ModuleDir,
        [Parameter(Mandatory = $true)]$Config
    )
    if (-not $Config.keystorePath) { throw 'android-pack.local.json 缺少 keystorePath' }
    $ks = [string]$Config.keystorePath
    if (-not (Test-Path -LiteralPath $ks)) {
        throw "keystore 不存在：$ks`n请把证书挪到安全目录并写入 android-pack.local.json（不要放在 Temp）"
    }
    $storePass = [string]$Config.storePassword
    $keyPass = [string]$Config.keyPassword
    if ([string]::IsNullOrWhiteSpace($storePass) -or [string]::IsNullOrWhiteSpace($keyPass)) {
        throw '请在 android-pack.local.json 填写 storePassword / keyPassword（该文件已 gitignore）'
    }
    $alias = [string]$Config.keystoreAlias
    $storeFile = ($ks -replace '\\', '/')
    $text = @"
storeFile=$storeFile
storePassword=$storePass
keyAlias=$alias
keyPassword=$keyPass
"@
    $out = Join-Path $ModuleDir 'keystore.properties'
    Write-Utf8NoBom -Path $out -Content ($text.Trim() + "`n")
    return $out
}

function Copy-AppResources {
    param(
        [Parameter(Mandatory = $true)][string]$WwwDir,
        [string]$AppId = $script:HomeaiAppId
    )
    $src = Join-Path (Get-UniappRoot) 'dist\build\app'
    if (-not (Test-Path -LiteralPath (Join-Path $src 'manifest.json'))) {
        throw "未找到 CLI 资源包：$src\manifest.json`n请先执行 pnpm build:app-android"
    }
    $parent = Split-Path $WwwDir -Parent
    if (Test-Path -LiteralPath $parent) {
        Remove-Item -LiteralPath $parent -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $WwwDir | Out-Null
    Copy-Item -Path (Join-Path $src '*') -Destination $WwwDir -Recurse -Force
    $copiedId = ''
    $mf = Join-Path $WwwDir 'manifest.json'
    if (Test-Path -LiteralPath $mf) {
        $copiedId = ([string](Get-Content -LiteralPath $mf -Raw -Encoding UTF8 | ConvertFrom-Json).id)
        if (-not $copiedId) {
            $copiedId = ([string](Get-Content -LiteralPath $mf -Raw -Encoding UTF8 | ConvertFrom-Json).appid)
        }
    }
    if ($copiedId -and $copiedId -ne $AppId) {
        Write-Warning "资源包 appid=$copiedId，与离线工程目录 $AppId 不一致"
    }
    Write-Host ("已拷贝资源 -> " + $WwwDir)
}