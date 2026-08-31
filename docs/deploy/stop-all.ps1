# -*- coding: utf-8 -*-
# 按目标停止：后端 Java / 管理端 Nginx+frpc。APP 是安装包，本机没有常驻进程。
# 不传目标 = 停止后端 + 前端。
# 用法：
#   .\stop-all.ps1
#   .\stop-all.ps1 -Backend
#   .\stop-all.ps1 -Frontend
#   .\stop-all.ps1 -Backend -Frontend
#   .\stop-all.ps1 -Target backend
param(
    [string[]]$Target = @(),
    [switch]$Frontend,
    [switch]$Backend,
    [switch]$App,
    [switch]$KeepTunnel,
    [switch]$Interactive
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

if ($Interactive) {
    $picked = Show-HomeaiConsoleMenu -Title 'HomeAI 停止' -Items @(
        @{ Id = '1'; Text = '全部（后端 + 前端）'; Backend = $true; Frontend = $true; App = $false }
        @{ Id = '2'; Text = '仅后端'; Backend = $true; Frontend = $false; App = $false }
        @{ Id = '3'; Text = '仅前端（Nginx / frpc）'; Backend = $false; Frontend = $true; App = $false }
    )
    if ($null -eq $picked) {
        Write-Host '已取消。'
        exit 0
    }
    $Backend = [bool]$picked.Backend
    $Frontend = [bool]$picked.Frontend
    $App = [bool]$picked.App
}

if ($KeepTunnel -and -not $Frontend -and -not $Backend -and -not $App -and $Target.Count -eq 0) {
    $Backend = $true
}

$want = Resolve-HomeaiTargets -Target $Target -Frontend:$Frontend -Backend:$Backend -App:$App
if ($KeepTunnel) { $want.Frontend = $false }
if ($Target.Count -eq 0 -and -not $Frontend -and -not $Backend -and -not $App -and -not $KeepTunnel) {
    $want.App = $false
}

Write-Host '========== HomeAI 停止 =========='
Write-HomeaiTargetBanner -Want $want -Action '停止'

if ($want.Backend) {
    Stop-HomeaiBackend
}
if ($want.Frontend) {
    Stop-HomeaiFrontend
}
if ($want.App) {
    Write-Host '[APP] 安装包没有本机常驻进程，无需停止。侧载后的手机 App 请在手机上划掉/卸载。'
}

Write-Host '完成。MySQL / Redis 未改动。'
