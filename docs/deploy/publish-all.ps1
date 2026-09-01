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
#   .\publish-all.ps1 -App -RegisterVersion [-UpdateMode apk]
param(
    [string[]]$Target = @(),
    [switch]$Frontend,
    [switch]$Backend,
    [switch]$App,
    [switch]$UploadApk,
    # APP 打包成功后自动登记版本：上传 APK + H5 zip，更新后台版本号并 enabled=1
    [switch]$RegisterVersion,
    # 登记时的更新模式（默认 resource 热更新；改过壳/原生代码才用 apk）
    [string]$UpdateMode = '',
    [switch]$SkipAppBuild,
    [switch]$InitAndroid,
    [switch]$Offline,
    [string]$AppVersion = '',
    [switch]$Interactive
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

if ($Interactive) {
    $picked = Show-HomeaiConsoleMenu -Title 'HomeAI 发布' -Items @(
        @{ Id = '1'; Text = '全部（后端 + 前端 + APP，并上传 APK 到下载页）'; Backend = $true; Frontend = $true; App = $true; UploadApk = $true }
        @{ Id = '2'; Text = '仅后端'; Backend = $true; Frontend = $false; App = $false }
        @{ Id = '3'; Text = '仅前端（管理端）'; Backend = $false; Frontend = $true; App = $false }
        @{ Id = '4'; Text = '仅 APP（并上传 APK 到下载页）'; Backend = $false; Frontend = $false; App = $true; UploadApk = $true }
        @{ Id = '5'; Text = '后端 + 前端（不出 APP）'; Backend = $true; Frontend = $true; App = $false }
    )
    if ($null -eq $picked) {
        Write-Host '已取消。'
        exit 0
    }
    $Backend = [bool]$picked.Backend
    $Frontend = [bool]$picked.Frontend
    $App = [bool]$picked.App
    $UploadApk = [bool]$picked.UploadApk
}

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
        ) + (Get-HomeaiMavenQuietArgs)
        if ($Offline) { $mvnArgs = @('-o') + $mvnArgs }
        Set-HomeaiBuildQuietEnv
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

# APP 版本自动登记：读打包产物元数据 last-version.json，登录管理端上传 APK + H5 zip 并更新后台版本号
function Invoke-HomeaiAppVersionRegister {
    param([string]$Mode)
    $metaPath = Join-Path $script:UniDir 'dist\apk\last-version.json'
    if (-not (Test-Path -LiteralPath $metaPath)) {
        throw "未找到产物元数据 $metaPath —— 请先执行 APP 打包（pack-apk-local.ps1）"
    }
    $meta = Get-Content -LiteralPath $metaPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $apk = [string]$meta.apk
    $zip = [string]$meta.zip
    if (-not $apk -or -not (Test-Path -LiteralPath $apk)) {
        throw "last-version.json 中 APK 路径无效：$apk"
    }
    if ($zip -and -not (Test-Path -LiteralPath $zip)) {
        Write-Host "[APP版本] 提示：H5 zip 不存在（$zip），仅登记 APK"
        $zip = ''
    }
    $mode = if ($Mode) { $Mode } else { 'resource' }
    $mjs = Join-Path $PSScriptRoot 'register-app-version.mjs'
    $mjsArgs = @(
        '--apk', $apk,
        '--version', [string]$meta.versionName,
        '--code', [string]$meta.versionCode,
        '--mode', $mode
    )
    if ($zip) { $mjsArgs += @('--zip', $zip) }
    Write-Host ("[APP版本] 登记 versionName={0} versionCode={1} mode={2}（enabled=1，即刻对 APP 生效）" -f $meta.versionName, $meta.versionCode, $mode)
    & node $mjs @mjsArgs
    if ($LASTEXITCODE -ne 0) { throw "版本登记失败，exit=$LASTEXITCODE" }
    Write-Host '[APP版本] 登记完成'
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
    $appOk = $true
    try {
        Invoke-HomeaiAppPublish
    } catch {
        $appOk = $false
        Write-Host ("[APP] 失败：{0}" -f $_.Exception.Message)
        $failed += 'APP'
    }
    if ($appOk -and $RegisterVersion) {
        try {
            Invoke-HomeaiAppVersionRegister -Mode $UpdateMode
        } catch {
            Write-Host ("[APP版本] 登记失败：{0}" -f $_.Exception.Message)
            Write-Host '[APP版本] APK 已打包成功；可稍后手动执行 docs/deploy/register-app-version.mjs 补登记'
            $failed += 'APP版本登记'
        }
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
    if ($RegisterVersion) {
        Write-Host 'APP版本：已自动登记（APK + H5 zip 已上传，enabled=1）'
    } else {
        Write-Host 'APP 管理端登记：家庭AI小工具 → APP版本（见 docs/guide/app-release.md）；或下次发布加 -RegisterVersion 自动登记'
    }
}
if ($failed.Count -gt 0) {
    Write-Host ("失败项：{0}" -f ($failed -join '、'))
    exit 1
}
Write-Host '完成。'
