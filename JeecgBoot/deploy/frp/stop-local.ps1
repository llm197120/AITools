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

# 只停业务隧道（frpc.toml）。运维隧道 frpc-admin.toml 独立于「前端」生命周期，
# 停止 Nginx/前端时不应连带关闭远程运维通道；不能按进程名 frpc 一刀切。
$frpHome = Join-Path $homeRoot 'frp'
$frpcToml = Join-Path $frpHome 'frpc.toml'
$bizFrpc = Get-CimInstance Win32_Process -Filter "Name = 'frpc.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine.Contains($frpcToml) }
if ($bizFrpc) {
    Write-Host '[frpc] stop business tunnel'
    $bizFrpc | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
} else {
    Write-Host '[frpc] business tunnel not running'
}

Write-Host 'local nginx / frpc stopped (Java backend unchanged)'
