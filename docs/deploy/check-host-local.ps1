# -*- coding: utf-8 -*-
# 在服务主机本机检查进程/端口（也被 check-remote.ps1 经 SSH 调用）。
$ErrorActionPreference = 'Continue'
. "$PSScriptRoot\common.ps1"

$script:failed = @()
$script:warn = @()

function Write-HomeaiCheckLine {
    param([bool]$Ok, [string]$Label, [switch]$Optional)
    $mark = if ($Ok) { 'OK' } else { if ($Optional) { 'WARN' } else { 'FAIL' } }
    Write-Host ("[{0}] {1}" -f $mark, $Label)
    if ($Ok) { return }
    if ($Optional) { $script:warn += $Label } else { $script:failed += $Label }
}

Write-Host '========== 服务主机本机检查 =========='
Write-Host ("时间 {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))

Write-HomeaiCheckLine -Ok (Test-HomeaiTcpPort -Port 3306) -Label 'MySQL :3306'
Write-HomeaiCheckLine -Ok (Test-HomeaiTcpPort -Port 6379) -Label 'Redis :6379'
Write-HomeaiCheckLine -Ok (Test-HomeaiTcpPort -Port 8080) -Label 'JeecgBoot :8080'
$cfg = Get-HomeaiFrpConfigSafe
$nginxPort = 8088
try { $nginxPort = [int]$cfg['HOME_NGINX_PORT'] } catch { }
Write-HomeaiCheckLine -Ok (Test-HomeaiTcpPort -Port $nginxPort) -Label ("Nginx :{0}" -f $nginxPort)
Write-HomeaiCheckLine -Ok (Test-HomeaiTcpPort -Port 3000) -Label 'Gotenberg :3000' -Optional
Write-HomeaiCheckLine -Ok (Test-HomeaiTcpPort -Port 8012) -Label 'kkFileView :8012' -Optional

$nginx = Get-Process -Name nginx -ErrorAction SilentlyContinue
Write-HomeaiCheckLine -Ok ([bool]$nginx) -Label '进程 nginx'

# frpc 有业务 / 运维两条隧道，进程名都是 frpc.exe，必须按命令行区分：
# 业务隧道 frpc.toml 缺失会直接导致公网 502、App 加载失败（关键项，记 FAIL）；
# 运维隧道 frpc-admin.toml 为备用通道，缺失记 WARN 即可。
$frpHome = Join-Path (Get-HomeaiHomeRoot) 'frp'
$frpcProcs = Get-CimInstance Win32_Process -Filter "Name = 'frpc.exe'" -ErrorAction SilentlyContinue
$bizToml = Join-Path $frpHome 'frpc.toml'
$adminToml = Join-Path $frpHome 'frpc-admin.toml'
$bizRunning = @($frpcProcs | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($bizToml) }).Count -gt 0
$adminRunning = @($frpcProcs | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($adminToml) }).Count -gt 0
Write-HomeaiCheckLine -Ok $bizRunning -Label 'frpc 业务隧道 (frpc.toml -> 18080)'
Write-HomeaiCheckLine -Ok $adminRunning -Label 'frpc 运维隧道 (frpc-admin.toml)' -Optional

$probe = "http://127.0.0.1:${nginxPort}/jeecg-boot/sys/randomImage/homeai-probe"
try {
    $resp = Invoke-WebRequest -Uri $probe -UseBasicParsing -TimeoutSec 8
    Write-HomeaiCheckLine -Ok ($resp.StatusCode -eq 200) -Label ("本机探测 {0} -> {1}" -f $probe, $resp.StatusCode)
} catch {
    Write-HomeaiCheckLine -Ok $false -Label ("本机探测 {0} ({1})" -f $probe, $_.Exception.Message)
}

# 公网探测（App 实际走的路）：连不上只记 WARN，避免外网波动误判本机故障
$public = [string]$cfg['PUBLIC_BASE']
if ($public) {
    $pubProbe = ($public.TrimEnd('/')) + '/jeecg-boot/sys/randomImage/homeai-probe'
    try {
        $pubResp = Invoke-WebRequest -Uri $pubProbe -UseBasicParsing -TimeoutSec 10
        Write-HomeaiCheckLine -Ok ($pubResp.StatusCode -eq 200) -Label ("公网探测 {0} -> {1}" -f $pubProbe, $pubResp.StatusCode) -Optional
    } catch {
        Write-HomeaiCheckLine -Ok $false -Label ("公网探测 {0} ({1})" -f $pubProbe, $_.Exception.Message) -Optional
    }
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host '----- docker ps -----'
    docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' 2>$null
} else {
    Write-Host '[WARN] 无 docker 命令'
}

$log = Get-HomeaiBackendLogPath
Write-Host ("----- {0} 末尾 -----" -f $log)
if (Test-Path -LiteralPath $log) {
    Get-Content -LiteralPath $log -Tail 20 -ErrorAction SilentlyContinue
} else {
    Write-Host '尚无 backend.log'
}

$drive = Get-PSDrive -Name C -ErrorAction SilentlyContinue
if ($drive) {
    $freeGb = [math]::Round($drive.Free / 1GB, 1)
    Write-Host ("C: 剩余 {0} GB" -f $freeGb)
    if ($freeGb -lt 10) { $script:warn += '磁盘剩余不足 10GB' }
}

Write-Host ''
if ($script:failed.Count -gt 0) {
    Write-Host ("失败：{0}" -f ($script:failed -join '、'))
    if ($script:warn.Count -gt 0) { Write-Host ("警告：{0}" -f ($script:warn -join '、')) }
    exit 1
}
if ($script:warn.Count -gt 0) {
    Write-Host ("警告：{0}" -f ($script:warn -join '、'))
}
Write-Host '本机检查通过。'
exit 0
