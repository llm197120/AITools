# -*- coding: utf-8 -*-
# 按目标发布：后端 / 管理端前端 / APP / 文档预览，可单独或组合。
# 不传目标 = 全部（含文档预览容器）。
# 用法：
#   .\publish-all.ps1
#   .\publish-all.ps1 -Backend
#   .\publish-all.ps1 -Backend -SkipDocsPreview
#   .\publish-all.ps1 -Frontend
#   .\publish-all.ps1 -App
#   .\publish-all.ps1 -DocsPreview
#   .\publish-all.ps1 -Backend -Frontend
#   .\publish-all.ps1 -Target backend,app
#   .\publish-all.ps1 -App -UploadApk
#   .\publish-all.ps1 -App -RegisterVersion [-UpdateMode apk]
param(
    [string[]]$Target = @(),
    [switch]$Frontend,
    [switch]$Backend,
    [switch]$App,
    [switch]$DocsPreview,
    [switch]$SkipDocsPreview,
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
        @{ Id = '1'; Text = '全部（后端 + 前端 + APP，上传 APK 到下载页并登记版本）'; Backend = $true; Frontend = $true; App = $true; UploadApk = $true; RegisterVersion = $true; DocsPreview = $true }
        @{ Id = '2'; Text = '仅后端（含文档预览）'; Backend = $true; Frontend = $false; App = $false; DocsPreview = $true }
        @{ Id = '3'; Text = '仅前端（管理端）'; Backend = $false; Frontend = $true; App = $false; DocsPreview = $false }
        @{ Id = '4'; Text = '仅 APP（上传 APK 到下载页并登记版本）'; Backend = $false; Frontend = $false; App = $true; UploadApk = $true; RegisterVersion = $true; DocsPreview = $false }
        @{ Id = '5'; Text = '后端 + 前端（不出 APP，含文档预览）'; Backend = $true; Frontend = $true; App = $false; DocsPreview = $true }
    )
    if ($null -eq $picked) {
        Write-Host '已取消。'
        exit 0
    }
    $Backend = [bool]$picked.Backend
    $Frontend = [bool]$picked.Frontend
    $App = [bool]$picked.App
    $UploadApk = [bool]$picked.UploadApk
    $RegisterVersion = [bool]$picked.RegisterVersion
    $DocsPreview = [bool]$picked.DocsPreview
}

$want = Resolve-HomeaiTargets -Target $Target -Frontend:$Frontend -Backend:$Backend -App:$App -DocsPreview:$DocsPreview
if ($want.Backend -and -not $SkipDocsPreview) { $want.DocsPreview = $true }
if ($SkipDocsPreview) { $want.DocsPreview = $false }

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
    if ($AppVersion) { $packArgs += @('-Version', $AppVersion) }
    & $pack @packArgs
    $apk = Join-Path $script:UniDir 'dist\apk\homeai-release.apk'
    if (Test-Path -LiteralPath $apk) {
        Write-Host ("[APP] 产物 {0}" -f $apk)
    }
}

function Invoke-HomeaiDownloadPageUpload {
    $meta = Get-HomeaiLastAppVersionMeta
    if (-not $meta) {
        throw "未找到产物元数据 $($script:UniDir)\dist\apk\last-version.json —— 无法上传下载页"
    }
    $apk = [string]$meta.apk
    if (-not $apk -or -not (Test-Path -LiteralPath $apk)) {
        throw "last-version.json 中 APK 路径无效：$apk"
    }
    $ver = [string]$meta.versionName
    $uploadScript = Join-Path $script:FrpDir 'upload-apk.ps1'
    Write-Host ("[下载页] 上传 {0}  ({1})" -f $ver, $apk)
    & $uploadScript -ApkPath $apk -Version $ver
    if ($LASTEXITCODE -ne 0) { throw "下载页上传失败，exit=$LASTEXITCODE" }
}

function Get-HomeaiLastAppVersionMeta {
    $metaPath = Join-Path $script:UniDir 'dist\apk\last-version.json'
    if (-not (Test-Path -LiteralPath $metaPath)) {
        return $null
    }
    try {
        $meta = Get-Content -LiteralPath $metaPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $code = $meta.versionCode
        if ($null -eq $code -or [string]$code -eq '') {
            $manifestTs = Join-Path $script:UniDir 'manifest.config.ts'
            if (Test-Path -LiteralPath $manifestTs) {
                $ts = Get-Content -LiteralPath $manifestTs -Raw -Encoding UTF8
                if ($ts -match "versionCode:\s*'([^']+)'") {
                    $parsed = 0
                    if ([int]::TryParse($Matches[1], [ref]$parsed)) {
                        $meta | Add-Member -NotePropertyName versionCode -NotePropertyValue $parsed -Force
                    }
                }
            }
        }
        return $meta
    } catch {
        return $null
    }
}

function Write-HomeaiAppVersionLine {
    param(
        [string]$Prefix = '[APP]',
        [object]$Meta
    )
    if (-not $Meta -or -not $Meta.versionName) {
        return
    }
    Write-Host ("{0} 版本号 {1}  (versionCode={2})" -f $Prefix, $Meta.versionName, $Meta.versionCode)
}

# APP 版本自动登记：读打包产物元数据 last-version.json，登录管理端上传 APK + H5 zip 并更新后台版本号
function Invoke-HomeaiAppVersionRegister {
    param([string]$Mode)
    $meta = Get-HomeaiLastAppVersionMeta
    if (-not $meta) {
        throw "未找到产物元数据 $($script:UniDir)\dist\apk\last-version.json —— 请先执行 APP 打包（pack-apk-local.ps1）"
    }
    $apk = [string]$meta.apk
    $zip = [string]$meta.zip
    if (-not $apk -or -not (Test-Path -LiteralPath $apk)) {
        throw "last-version.json 中 APK 路径无效：$apk"
    }
    if (-not $zip) {
        $guessZip = Join-Path $script:UniDir ("dist\apk\homeai-h5-" + [string]$meta.versionName + ".zip")
        if (Test-Path -LiteralPath $guessZip) { $zip = $guessZip }
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
    Write-HomeaiAppVersionLine -Prefix '[APP版本] 登记' -Meta $meta
    Write-Host ("[APP版本] 更新模式 {0}（enabled=1，即刻对 APP 生效）" -f $mode)
    & node $mjs @mjsArgs
    if ($LASTEXITCODE -ne 0) { throw "版本登记失败，exit=$LASTEXITCODE" }
    Write-Host '[APP版本] 登记完成'
    Write-HomeaiAppVersionLine -Prefix '[APP版本] 已发布' -Meta $meta
}

Write-Host '========== HomeAI 发布 =========='
Write-Host ("仓库: {0}" -f $script:RepoRoot)
Write-HomeaiTargetBanner -Want $want -Action '发布'
Write-Host ''

$failed = @()

if ($want.Backend -and (Test-HomeaiDockerDatastoresEnabled)) {
    try {
        Start-HomeaiRuntimeDatastores
    } catch {
        Write-Host ("[数据] 失败：{0}" -f $_.Exception.Message)
        $failed += 'MySQL/Redis'
    }
}

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

if ($want.DocsPreview) {
    try {
        Start-HomeaiDocsPreview
    } catch {
        Write-Host ("[文档预览] 失败：{0}" -f $_.Exception.Message)
        $failed += '文档预览'
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
        Write-HomeaiAppVersionLine -Prefix '[APP] 已打包' -Meta (Get-HomeaiLastAppVersionMeta)
    } catch {
        $appOk = $false
        Write-Host ("[APP] 失败：{0}" -f $_.Exception.Message)
        $failed += 'APP'
    }
    if ($appOk -and $UploadApk) {
        try {
            Invoke-HomeaiDownloadPageUpload
        } catch {
            Write-Host ("[下载页] 失败：{0}" -f $_.Exception.Message)
            $failed += '下载页上传'
        }
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
    Write-Host ("公网入口:   {0}/" -f $cfg['PUBLIC_BASE'])
}
if ($want.Backend) {
    Write-Host ("本地 API:   http://127.0.0.1:{0}/jeecg-boot/" -f $cfg['BACKEND_PORT'])
}
if ($want.DocsPreview) {
    Write-HomeaiDocsPreviewHint
}
if ($want.App) {
    Write-Host ("下载页:     {0}" -f $cfg['DOWNLOAD_URL'])
    $endMeta = Get-HomeaiLastAppVersionMeta
    if ($appOk -and $endMeta) {
        Write-Host ("APP 版本号: {0}  (versionCode={1})" -f $endMeta.versionName, $endMeta.versionCode)
    }
    if ($UploadApk -and $appOk -and ($failed -notcontains '下载页上传')) {
        Write-Host '下载页：已覆盖 homeai-latest.apk 与 version.txt'
    } elseif ($UploadApk -and -not $appOk) {
        Write-Host '下载页：未上传（APP 打包失败）'
    }
    if ($RegisterVersion -and $appOk) {
        Write-Host 'APP版本：已自动登记（APK + H5 zip 已上传，enabled=1）'
    } elseif ($RegisterVersion -and -not $appOk) {
        Write-Host 'APP版本：未登记（APP 打包失败）'
    } else {
        Write-Host 'APP 管理端登记：家庭AI小工具 → APP版本（见 docs/guide/app-release.md）；或下次发布加 -RegisterVersion 自动登记'
    }
}
if ($failed.Count -gt 0) {
    Write-Host ("失败项：{0}" -f ($failed -join '、'))
    exit 1
}
Write-Host '完成。'
