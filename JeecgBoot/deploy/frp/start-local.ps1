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

# 只认业务隧道：按命令行里的 frpc.toml 区分，不能只看进程名 frpc。
# 运维隧道 frpc-admin.toml 同样是 frpc.exe，开机时可能先起，
# 若只看进程名会把业务隧道误判为“已在运行”而跳过，导致公网 502（App 加载失败）。
$bizFrpc = Get-CimInstance Win32_Process -Filter "Name = 'frpc.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine.Contains($frpcToml) }
if (-not $bizFrpc) {
    Write-Host '[frpc] start business tunnel'
    Unblock-File -LiteralPath $frpcExe -ErrorAction SilentlyContinue
    $p = Start-Process -FilePath $frpcExe -ArgumentList @('-c', $frpcToml) -WorkingDirectory $frpHome -WindowStyle Hidden -PassThru
    if (-not $p) { throw "failed to start frpc (Access Denied? allow C:\homeai\frp\frpc.exe in Windows Security)" }
    Start-Sleep -Milliseconds 800
} else {
    Write-Host '[frpc] business tunnel already running'
}

Start-Sleep -Seconds 1
if (Test-TcpPortOpen -Port $nginxPort) {
    Write-Host ("[OK] http://127.0.0.1:" + $nginxPort + "/")
} else {
    Write-Host '[FAIL] nginx not listening, see C:\homeai\nginx\logs\error.log'
}

Write-Host ("public: " + $cfg['PUBLIC_BASE'] + "/")
Write-Host ("frpc log: " + (Join-Path $frpHome 'frpc.log'))
