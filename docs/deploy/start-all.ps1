# -*- coding: utf-8 -*-
# 按目标启动（不编译、不出包）：后端 Java / 管理端 Nginx+frpc / 文档预览 Docker。
# 不传目标 = 启动后端 + 前端 + 文档预览（Gotenberg / kkFileView）。
# 用法：
#   .\start-all.ps1
#   .\start-all.ps1 -Backend
#   .\start-all.ps1 -Frontend
#   .\start-all.ps1 -DocsPreview
#   .\start-all.ps1 -Backend -SkipDocsPreview
param(
    [string[]]$Target = @(),
    [switch]$Frontend,
    [switch]$Backend,
    [switch]$App,
    [switch]$DocsPreview,
    [switch]$SkipDocsPreview,
    [switch]$Interactive
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

if ($Interactive) {
    $picked = Show-HomeaiConsoleMenu -Title 'HomeAI 启动（不编译、不出包）' -Items @(
        @{ Id = '1'; Text = '全部（后端 + 前端 + 文档预览）'; Backend = $true; Frontend = $true; App = $false; DocsPreview = $true }
        @{ Id = '2'; Text = '仅后端（含文档预览）'; Backend = $true; Frontend = $false; App = $false; DocsPreview = $true }
        @{ Id = '3'; Text = '仅前端（Nginx / frpc）'; Backend = $false; Frontend = $true; App = $false; DocsPreview = $false }
        @{ Id = '4'; Text = '仅文档预览（Gotenberg / kkFileView）'; Backend = $false; Frontend = $false; App = $false; DocsPreview = $true }
    )
    if ($null -eq $picked) {
        Write-Host '已取消。'
        exit 0
    }
    $Backend = [bool]$picked.Backend
    $Frontend = [bool]$picked.Frontend
    $App = [bool]$picked.App
    $DocsPreview = [bool]$picked.DocsPreview
}

$want = Resolve-HomeaiTargets -Target $Target -Frontend:$Frontend -Backend:$Backend -App:$App -DocsPreview:$DocsPreview
if ($Target.Count -eq 0 -and -not $Frontend -and -not $Backend -and -not $App -and -not $DocsPreview) {
    $want.App = $false
}
if ($want.Backend -and -not $SkipDocsPreview) { $want.DocsPreview = $true }
if ($SkipDocsPreview) { $want.DocsPreview = $false }

Write-Host '========== HomeAI 启动 =========='
Write-HomeaiTargetBanner -Want $want -Action '启动'

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
        Start-HomeaiBackendAndWait
    } catch {
        Write-Host ("[后端] 失败：{0}" -f $_.Exception.Message)
        $failed += '后端'
    }
}
if ($want.Frontend) {
    try {
        Start-HomeaiFrontendTunnel
    } catch {
        Write-Host ("[前端] 失败：{0}" -f $_.Exception.Message)
        $failed += '前端'
    }
}
if ($want.DocsPreview) {
    try {
        Start-HomeaiDocsPreview -Required:(-not $want.Backend)
    } catch {
        Write-Host ("[文档预览] 失败：{0}" -f $_.Exception.Message)
        $failed += '文档预览'
    }
}
if ($want.App) {
    Write-Host '[APP] 没有本机常驻进程。要出包装请运行 publish-all.ps1 -App'
}

$cfg = Get-HomeaiFrpConfigSafe
Write-Host ''
if ($want.Frontend) {
    Write-Host ("本地管理端: http://127.0.0.1:{0}/" -f $cfg['HOME_NGINX_PORT'])
    Write-Host ("公网入口:   http://{0}/" -f $cfg['SERVER_IP'])
}
if ($want.Backend) {
    Write-Host ("本地 API:   http://127.0.0.1:{0}/jeecg-boot/" -f $cfg['BACKEND_PORT'])
}
if ($want.DocsPreview) {
    Write-HomeaiDocsPreviewHint
}
if ($failed.Count -gt 0) {
    Write-Host ("失败项：{0}" -f ($failed -join '、'))
    exit 1
}
Write-Host '完成。'
