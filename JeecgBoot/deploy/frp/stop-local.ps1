# -*- coding: utf-8 -*-
# 停止本机 Nginx + frpc。未运行时直接跳过，不报错。
. "$PSScriptRoot\common.ps1"
$ErrorActionPreference = 'SilentlyContinue'

$cfg = Get-HomeaiFrpConfig
$homeRoot = $cfg['HOME_ROOT']
$nginxHome = Join-Path $homeRoot 'nginx'
$nginxExe = Join-Path $nginxHome 'nginx.exe'
$pidFile = Join-Path $nginxHome 'logs\nginx.pid'

if (Get-Process -Name 'nginx' -ErrorAction SilentlyContinue) {
    Write-Host '[nginx] stop'
    if ((Test-Path -LiteralPath $nginxExe) -and (Test-Path -LiteralPath $pidFile)) {
        Push-Location $nginxHome
        try {
            & $nginxExe -s stop | Out-Null
        } finally {
            Pop-Location
        }
        Start-Sleep -Milliseconds 300
    }
    Get-Process -Name 'nginx' -ErrorAction SilentlyContinue | Stop-Process -Force
} else {
    Write-Host '[nginx] not running'
}

$frpc = Get-Process -Name 'frpc' -ErrorAction SilentlyContinue
if ($frpc) {
    Write-Host '[frpc] stop'
    $frpc | Stop-Process -Force
} else {
    Write-Host '[frpc] not running'
}

Write-Host 'local nginx / frpc stopped (Java backend unchanged)'
