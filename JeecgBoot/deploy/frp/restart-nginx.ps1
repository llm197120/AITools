# 恢复远程 nginx 并放宽 /var/www 权限（下载页 403 的原因）
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
Ensure-HomeaiSshClient
$cfg = Get-HomeaiFrpConfig
$sec = Initialize-HomeaiFrpSecrets
$user = if ($sec['SSH_USER']) { $sec['SSH_USER'] } else { 'root' }
$target = $user + '@' + $cfg['SERVER_IP']
$sshArgs = @('-T', '-p', '22', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'NumberOfPasswordPrompts=1', '-o', 'RequestTTY=no')
$remote = @'
set -e
chmod 755 /var /var/www /var/www/homeai-apk 2>/dev/null || true
chmod 644 /var/www/homeai-apk/index.html 2>/dev/null || true
systemctl start nginx
systemctl is-active nginx frps
ss -lntp | grep -E ':80|:7000' || true
curl -sI --max-time 5 http://127.0.0.1/app/ | head -n 6
'@
$tmp = Join-Path $env:TEMP 'homeai-restart-nginx.sh'
$utf8 = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($tmp, (ConvertTo-UnixText -Text $remote), $utf8)
try {
    Initialize-HomeaiSshAuth -Secrets $sec -Target $target -IdentityFile ''
    Invoke-HomeaiSsh -SshArgs $sshArgs -Target $target -RemoteCommand 'bash -s' -StdinFile $tmp
} finally {
    Clear-HomeaiSshAuth
    Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
}
