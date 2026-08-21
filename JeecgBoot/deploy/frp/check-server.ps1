# 检查本机隧道 + 远程 frps/nginx（不打印密码）
$ErrorActionPreference = 'Continue'
. "$PSScriptRoot\common.ps1"

Write-Host "=== local ports ==="
foreach ($p in 8080, 8088) {
    $ok = Test-TcpPortOpen -Port $p
    Write-Host ("127.0.0.1:{0} {1}" -f $p, $(if ($ok) { 'open' } else { 'closed' }))
}
Write-Host "=== local processes ==="
Get-Process nginx, frpc -ErrorAction SilentlyContinue | Format-Table Name, Id -AutoSize
$log = 'C:\homeai\frp\frpc.log'
if (Test-Path -LiteralPath $log) {
    Write-Host "=== frpc.log tail ==="
    Get-Content -LiteralPath $log -Tail 25
} else {
    Write-Host "no C:\homeai\frp\frpc.log"
}

Ensure-HomeaiSshClient
$cfg = Get-HomeaiFrpConfig
$sec = Initialize-HomeaiFrpSecrets
$user = if ($sec['SSH_USER']) { $sec['SSH_USER'] } else { 'root' }
$target = $user + '@' + $cfg['SERVER_IP']
$sshArgs = @('-T', '-p', '22', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'NumberOfPasswordPrompts=1', '-o', 'RequestTTY=no')
$remote = @'
set +e
echo "=== services ==="
systemctl is-active frps nginx
echo "=== listen ==="
ss -lntp | grep -E ':80|:7000|:18080' || true
echo "=== frps recent ==="
journalctl -u frps -n 25 --no-pager
echo "=== nginx recent ==="
journalctl -u nginx -n 15 --no-pager
echo "=== apk dir ==="
ls -la /var/www/homeai-apk 2>/dev/null || echo "apk dir missing"
echo "=== local curl ==="
curl -sI --max-time 5 http://127.0.0.1/app/ | head -n 8
curl -sI --max-time 5 http://127.0.0.1/ | head -n 8
curl -sI --max-time 5 http://127.0.0.1:18080/ | head -n 8
'@
$tmp = Join-Path $env:TEMP 'homeai-check-server.sh'
$utf8 = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($tmp, (ConvertTo-UnixText -Text $remote), $utf8)
try {
    Initialize-HomeaiSshAuth -Secrets $sec -Target $target -IdentityFile ''
    Write-Host ("=== ssh " + $target + " ===")
    Invoke-HomeaiSsh -SshArgs $sshArgs -Target $target -RemoteCommand 'bash -s' -StdinFile $tmp
} finally {
    Clear-HomeaiSshAuth
    Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
}
