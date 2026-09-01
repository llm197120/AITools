# -*- coding: utf-8 -*-
# 一次性：服务器 nginx conf.d/homeai.conf 的 /app location 加 no-store 缓存头并 reload（幂等）
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
$cfg = Get-HomeaiFrpConfig
$sec = Initialize-HomeaiFrpSecrets
$sshUser = if ($sec['SSH_USER']) { $sec['SSH_USER'] } else { 'root' }
$target = "${sshUser}@$($cfg['SERVER_IP'])"
$sshArgs = @('-T', '-p', '22', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'NumberOfPasswordPrompts=1', '-o', 'RequestTTY=no')
$remoteCmd = 'grep -q no-store /etc/nginx/conf.d/homeai.conf || sed -i ''/autoindex off;/a\        add_header Cache-Control "no-store, must-revalidate";'' /etc/nginx/conf.d/homeai.conf; nginx -t && systemctl reload nginx && echo NGINX_UPDATED'
try {
    Initialize-HomeaiSshAuth -Secrets $sec -Target $target
    Invoke-HomeaiSsh -SshArgs $sshArgs -Target $target -RemoteCommand $remoteCmd
} finally {
    Clear-HomeaiSshAuth
}