# -*- coding: utf-8 -*-
# 启动本机 Nginx + frpc（不启动 Java 后端）
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

$cfg = Get-HomeaiFrpConfig
$homeRoot = $cfg['HOME_ROOT']
$nginxHome = Join-Path $homeRoot 'nginx'
$frpHome = Join-Path $homeRoot 'frp'
$nginxPort = [int]$cfg['HOME_NGINX_PORT']

$nginxExe = Join-Path $nginxHome 'nginx.exe'
$frpcExe = Join-Path $frpHome 'frpc.exe'
$frpcToml = Join-Path $frpHome 'frpc.toml'
if (-not (Test-Path -LiteralPath $nginxExe)) { throw "nginx not installed. Run setup-local.ps1 first." }
if (-not (Test-Path -LiteralPath $frpcExe)) { throw "frpc not installed. Run setup-local.ps1 first." }

New-Item -ItemType Directory -Force -Path (Join-Path $nginxHome 'logs'), (Join-Path $nginxHome 'temp') | Out-Null

if (-not (Test-TcpPortOpen -Port $nginxPort)) {
    Write-Host ("[nginx] start 127.0.0.1:" + $nginxPort)
    Start-Process -FilePath $nginxExe -WorkingDirectory $nginxHome -WindowStyle Hidden
    Start-Sleep -Milliseconds 600
} else {
    Write-Host ("[nginx] already listening on " + $nginxPort)
}

if (-not (Get-Process -Name 'frpc' -ErrorAction SilentlyContinue)) {
    Write-Host '[frpc] start'
    Unblock-File -LiteralPath $frpcExe -ErrorAction SilentlyContinue
    $p = Start-Process -FilePath $frpcExe -ArgumentList @('-c', $frpcToml) -WorkingDirectory $frpHome -WindowStyle Hidden -PassThru
    if (-not $p) { throw "failed to start frpc (Access Denied? allow C:\homeai\frp\frpc.exe in Windows Security)" }
    Start-Sleep -Milliseconds 800
} else {
    Write-Host '[frpc] already running'
}

Start-Sleep -Seconds 1
if (Test-TcpPortOpen -Port $nginxPort) {
    Write-Host ("[OK] http://127.0.0.1:" + $nginxPort + "/")
} else {
    Write-Host '[FAIL] nginx not listening, see C:\homeai\nginx\logs\error.log'
}

Write-Host ("public: http://" + $cfg['SERVER_IP'] + "/")
Write-Host ("frpc log: " + (Join-Path $frpHome 'frpc.log'))
