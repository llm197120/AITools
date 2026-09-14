# -*- coding: utf-8 -*-
# 按目标停止：后端 Java / 管理端 Nginx+frpc / 文档预览 Docker。APP 是安装包，本机没有常驻进程。
# 不传目标 = 停止后端 + 前端 + 文档预览。
# 用法：
#   .\stop-all.ps1
#   .\stop-all.ps1 -Backend
#   .\stop-all.ps1 -Frontend
#   .\stop-all.ps1 -DocsPreview
#   .\stop-all.ps1 -Backend -KeepDocsPreview
#   .\stop-all.ps1 -Target backend
param(
    [string[]]$Target = @(),
    [switch]$Frontend,
    [switch]$Backend,
    [switch]$App,
    [switch]$DocsPreview,
    [switch]$KeepTunnel,
    [switch]$KeepDocsPreview,
    [switch]$Interactive
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

if ($Interactive) {
    $picked = Show-HomeaiConsoleMenu -Title 'HomeAI 停止' -Items @(
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

if ($KeepTunnel -and -not $Frontend -and -not $Backend -and -not $App -and -not $DocsPreview -and $Target.Count -eq 0) {
    $Backend = $true
}

$want = Resolve-HomeaiTargets -Target $Target -Frontend:$Frontend -Backend:$Backend -App:$App -DocsPreview:$DocsPreview
if ($KeepTunnel) { $want.Frontend = $false }
if ($Target.Count -eq 0 -and -not $Frontend -and -not $Backend -and -not $App -and -not $DocsPreview -and -not $KeepTunnel) {
    $want.App = $false
}
if ($want.Backend -and -not $KeepDocsPreview) { $want.DocsPreview = $true }
if ($KeepDocsPreview) { $want.DocsPreview = $false }

Write-Host '========== HomeAI 停止 =========='
Write-HomeaiTargetBanner -Want $want -Action '停止'

if ($want.Backend) {
    Stop-HomeaiBackend
}
if ($want.DocsPreview) {
    Stop-HomeaiDocsPreview
}
if ($want.Frontend) {
    Stop-HomeaiFrontend
}
if ($want.App) {
    Write-Host '[APP] 安装包没有本机常驻进程，无需停止。侧载后的手机 App 请在手机上划掉/卸载。'
}

Write-Host '完成。MySQL / Redis 未改动。文档预览容器用 compose stop，镜像仍保留。'
