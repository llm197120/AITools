# -*- coding: utf-8 -*-
# 按目标发布：后端 / 管理端前端 / APP，可单独或组合。
# 不传目标 = 全部。
# 用法：
#   .\publish-all.ps1
#   .\publish-all.ps1 -Backend
#   .\publish-all.ps1 -Frontend
#   .\publish-all.ps1 -App
#   .\publish-all.ps1 -Backend -Frontend
#   .\publish-all.ps1 -Target backend,app
#   .\publish-all.ps1 -App -UploadApk
param(
    [string[]]$Target = @(),
    [switch]$Frontend,
    [switch]$Backend,
    [switch]$App,
    [switch]$UploadApk,
    [switch]$SkipAppBuild,
    [switch]$InitAndroid,
    [switch]$Offline,
    [string]$AppVersion = ''
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

$want = Resolve-HomeaiTargets -Target $Target -Frontend:$Frontend -Backend:$Backend -App:$App

function Invoke-HomeaiBackendCompile {
    Ensure-HomeaiJavaHome
    $mvn = Get-HomeaiMavenCmd
    Write-Host '[后端] 安装 jeecg-system-start（含依赖，写入本地仓库）...'
    Push-Location $script:BootDir
    try {
        $mvnArgs = @(
            '-f', 'pom.xml',
            '-pl', $script:StartModule,
            '-am',
            'install',
            '-DskipTests'
        )
        if ($Offline) { $mvnArgs = @('-o') + $mvnArgs }
        & $mvn @mvnArgs
        if ($LASTEXITCODE -ne 0) { throw "后端编译失败，exit=$LASTEXITCODE" }
    } finally {
        Pop-Location
    }
    Write-Host '[后端] 已写入本地仓库'
}

function Invoke-HomeaiFrontendPublish {
    $setup = Join-Path $script:FrpDir 'setup-local.ps1'
    if (-not (Test-Path -LiteralPath $setup)) {
        throw "找不到 $setup 。请先完成 FRP 本机部署。"
    }
    Write-Host '[前端] 构建管理端并发布到本机 Nginx（setup-local.ps1 -BuildAdmin）'
    & $setup -BuildAdmin
}

function Invoke-HomeaiAppPublish {
    $pack = Join-Path $script:UniDir 'scripts\capacitor\pack-apk-local.ps1'
    if (-not (Test-Path -LiteralPath $pack)) {
        throw "找不到 APP 出包脚本：$pack"
    }
    Write-Host '[APP] pnpm pack:apk:local（Capacitor 签名包）'
    $packArgs = @()
    if ($SkipAppBuild) { $packArgs += '-SkipBuild' }
    if ($InitAndroid) { $packArgs += '-InitAndroid' }
    if ($UploadApk) { $packArgs += '-Upload' }
    if ($AppVersion) { $packArgs += @('-Version', $AppVersion) }
    & $pack @packArgs
    $apk = Join-Path $script:UniDir 'dist\apk\homeai-release.apk'
    if (Test-Path -LiteralPath $apk) {
        Write-Host ("[APP] 产物 {0}" -f $apk)
    }
}

Write-Host '========== HomeAI 发布 =========='
Write-Host ("仓库: {0}" -f $script:RepoRoot)
Write-HomeaiTargetBanner -Want $want -Action '发布'
Write-Host ''

$failed = @()

if ($want.Backend) {
    try {
        Invoke-HomeaiBackendCompile
        Stop-HomeaiBackend
        Start-HomeaiBackendAndWait
    } catch {
        Write-Host ("[后端] 失败：{0}" -f $_.Exception.Message)
        $failed += '后端'
    }
}

if ($want.Frontend) {
    try {
        Invoke-HomeaiFrontendPublish
        if ($want.Backend -and -not (Test-HomeaiTcpPort -Port (Get-HomeaiBackendPort))) {
            Write-Host '[注意] 管理端已发布，但后端 :8080 未监听，页面会 502。请先发布/启动后端。'
        }
    } catch {
        Write-Host ("[前端] 失败：{0}" -f $_.Exception.Message)
        $failed += '前端'
    }
}

if ($want.App) {
    try {
        Invoke-HomeaiAppPublish
    } catch {
        Write-Host ("[APP] 失败：{0}" -f $_.Exception.Message)
        $failed += 'APP'
    }
}

$cfg = Get-HomeaiFrpConfigSafe
Write-Host ''
Write-Host '========== 发布结束 =========='
if ($want.Frontend) {
    Write-Host ("本地管理端: http://127.0.0.1:{0}/" -f $cfg['HOME_NGINX_PORT'])
    Write-Host ("公网入口:   http://{0}/" -f $cfg['SERVER_IP'])
}
if ($want.Backend) {
    Write-Host ("本地 API:   http://127.0.0.1:{0}/jeecg-boot/" -f $cfg['BACKEND_PORT'])
}
if ($want.App) {
    Write-Host ("下载页:     http://{0}/app/" -f $cfg['SERVER_IP'])
    Write-Host 'APP 管理端登记：家庭AI小工具 → APP版本（见 docs/guide/app-release.md）'
}
if ($failed.Count -gt 0) {
    Write-Host ("失败项：{0}" -f ($failed -join '、'))
    exit 1
}
Write-Host '完成。'
