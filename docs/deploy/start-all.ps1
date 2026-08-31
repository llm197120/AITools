# -*- coding: utf-8 -*-
# 按目标启动（不编译、不出包）：后端 Java / 管理端 Nginx+frpc。
# 不传目标 = 启动后端 + 前端。
# 用法：
#   .\start-all.ps1
#   .\start-all.ps1 -Backend
#   .\start-all.ps1 -Frontend
#   .\start-all.ps1 -Backend -Frontend
param(
    [string[]]$Target = @(),
    [switch]$Frontend,
    [switch]$Backend,
    [switch]$App,
    [switch]$Interactive
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

if ($Interactive) {
    $picked = Show-HomeaiConsoleMenu -Title 'HomeAI 启动（不编译、不出包）' -Items @(
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

$want = Resolve-HomeaiTargets -Target $Target -Frontend:$Frontend -Backend:$Backend -App:$App
if ($Target.Count -eq 0 -and -not $Frontend -and -not $Backend -and -not $App) {
    $want.App = $false
}

Write-Host '========== HomeAI 启动 =========='
Write-HomeaiTargetBanner -Want $want -Action '启动'

$failed = @()
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
if ($failed.Count -gt 0) {
    Write-Host ("失败项：{0}" -f ($failed -join '、'))
    exit 1
}
Write-Host '完成。'
