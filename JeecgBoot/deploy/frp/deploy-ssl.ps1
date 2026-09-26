# -*- coding: utf-8 -*-
# 在公网 ECS 上启用阿里云 Nginx 证书（系统 Nginx，不用宝塔）。
# 用法：.\deploy-ssl.ps1
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

Ensure-HomeaiSshClient
$cfg = Get-HomeaiFrpConfig
$sec = Initialize-HomeaiFrpSecrets
$user = if ($sec['SSH_USER']) { $sec['SSH_USER'] } else { 'root' }
$target = $user + '@' + $cfg['SERVER_IP']
$sshArgs = @('-T', '-p', '22', '-o', 'StrictHostKeyChecking=accept-new', '-o', 'NumberOfPasswordPrompts=1', '-o', 'RequestTTY=no')

$localSh = Join-Path $PSScriptRoot 'remote-deploy-ssl.sh'
if (-not (Test-Path -LiteralPath $localSh)) { throw "missing remote-deploy-ssl.sh" }

$tmpSh = Join-Path $env:TEMP 'homeai-deploy-ssl.sh'
Copy-Item -LiteralPath $localSh -Destination $tmpSh -Force
ConvertTo-UnixFile -Path $tmpSh

try {
    Initialize-HomeaiSshAuth -Secrets $sec -Target $target -IdentityFile ''
    Write-Host ("Deploy SSL via SSH " + $target)
    Invoke-HomeaiSsh -SshArgs $sshArgs -Target $target -RemoteCommand 'bash -s' -StdinFile $tmpSh
} finally {
    Clear-HomeaiSshAuth
    Remove-Item -LiteralPath $tmpSh -Force -ErrorAction SilentlyContinue
}
