# -*- coding: utf-8 -*-
# 把签名 APK 上传到服务器下载页（一次 SSH，密码只问一次或读 secrets.env）
param(
    [Parameter(Mandatory = $true)][string]$ApkPath,
    [string]$SshHost = '',
    [string]$SshUser = 'root',
    [int]$SshPort = 22,
    [string]$SshIdentityFile = '',
    [string]$Version = ''
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
Ensure-HomeaiSshClient

if (-not (Test-Path -LiteralPath $ApkPath)) { throw ("APK not found: " + $ApkPath) }
$cfg = Get-HomeaiFrpConfig
$sec = Initialize-HomeaiFrpSecrets
if (-not $SshHost) { $SshHost = $cfg['SERVER_IP'] }
if (-not $PSBoundParameters.ContainsKey('SshUser') -and $sec['SSH_USER']) {
    $SshUser = $sec['SSH_USER']
}
$target = "${SshUser}@${SshHost}"

$sshArgs = @(
    '-T',
    '-p', "$SshPort",
    '-o', 'StrictHostKeyChecking=accept-new',
    '-o', 'NumberOfPasswordPrompts=1',
    '-o', 'RequestTTY=no'
)
$keyPath = $null
if ($SshIdentityFile) {
    $keyPath = Resolve-HomeaiSshIdentity -Path $SshIdentityFile
    $sshArgs += @('-i', $keyPath)
}

$verEsc = ($Version -replace "'", '').Trim()
$verLine = if ($verEsc) { "echo '$verEsc' > /var/www/homeai-apk/version.txt;" } else { '' }
$remoteCmd = "mkdir -p /var/www/homeai-apk && cat > /tmp/homeai.apk && mv /tmp/homeai.apk /var/www/homeai-apk/homeai-latest.apk && chmod 644 /var/www/homeai-apk/homeai-latest.apk && $verLine echo uploaded"
$indexHtml = Join-Path $PSScriptRoot 'download\index.html'

try {
    Initialize-HomeaiSshAuth -Secrets $sec -Target $target -IdentityFile $keyPath
    Write-Host ("Upload APK in one SSH to " + $target)
    if ($verEsc) { Write-Host ("Download page version.txt -> " + $verEsc) }
    Invoke-HomeaiSsh -SshArgs $sshArgs -Target $target -RemoteCommand $remoteCmd -StdinFile $ApkPath
    if (Test-Path -LiteralPath $indexHtml) {
        Write-Host 'Upload download/index.html'
        Invoke-HomeaiSsh -SshArgs $sshArgs -Target $target -RemoteCommand 'cat > /var/www/homeai-apk/index.html && chmod 644 /var/www/homeai-apk/index.html && echo html-ok' -StdinFile $indexHtml
    }
} finally {
    Clear-HomeaiSshAuth
}

Write-Host ("Done: http://" + $cfg['SERVER_IP'] + "/app/")
