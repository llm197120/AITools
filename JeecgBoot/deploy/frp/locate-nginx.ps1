# -*- coding: utf-8 -*-
# 一次性：定位服务器 nginx 中 homeai-apk 配置所在文件
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
$cfg = Get-HomeaiFrpConfig
$sec = Initialize-HomeaiFrpSecrets
$sshUser = if ($sec['SSH_USER']) { $sec['SSH_USER'] } else { 'root' }
$target = "${sshUser}@$($cfg['SERVER_IP'])"
$sshArgs = @('-T', '-p', '22', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'NumberOfPasswordPrompts=1', '-o', 'RequestTTY=no')
$remoteCmd = "ls /etc/nginx/sites-available/ 2>/dev/null; ls /etc/nginx/conf.d/ 2>/dev/null; grep -rl homeai-apk /etc/nginx/ 2>/dev/null; nginx -T 2>/dev/null | grep -A6 'location /app'"
try {
    Initialize-HomeaiSshAuth -Secrets $sec -Target $target
    Invoke-HomeaiSsh -SshArgs $sshArgs -Target $target -RemoteCommand $remoteCmd
} finally {
    Clear-HomeaiSshAuth
}