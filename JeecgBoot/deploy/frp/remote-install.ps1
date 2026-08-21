# -*- coding: utf-8 -*-
# 从本机 SSH 一键部署服务器（frps + Nginx + 下载页）
# 密码：写入 secrets.env 的 SSH_PASSWORD= 则不再询问；否则只问一次。
# 用法：
#   .\remote-install.ps1
#   .\remote-install.ps1 -SshUser ubuntu
#   .\remote-install.ps1 -SshIdentityFile AITools0820
param(
    [string]$SshHost = '',
    [string]$SshUser = 'root',
    [int]$SshPort = 22,
    [string]$SshIdentityFile = ''
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

Ensure-HomeaiSshClient

$cfg = Get-HomeaiFrpConfig
$sec = Initialize-HomeaiFrpSecrets
if (-not $SshHost) { $SshHost = $cfg['SERVER_IP'] }
if (-not $PSBoundParameters.ContainsKey('SshUser') -and $sec['SSH_USER']) {
    $SshUser = $sec['SSH_USER']
}

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
    Write-Host ("Using SSH key: " + $keyPath)
    $sshArgs += @('-i', $keyPath)
}
$target = "${SshUser}@${SshHost}"

$localSh = Join-Path $PSScriptRoot 'install-server.sh'
if (-not (Test-Path -LiteralPath $localSh)) { throw ("missing " + $localSh) }

$bundle = Join-Path $env:TEMP ("homeai-frp-bundle-" + [guid]::NewGuid().ToString('n'))
$tarFile = Join-Path $env:TEMP ("homeai-frp-bundle-" + [guid]::NewGuid().ToString('n') + ".tar")
New-Item -ItemType Directory -Path (Join-Path $bundle 'download') -Force | Out-Null
Copy-Item -LiteralPath $localSh -Destination (Join-Path $bundle 'install-server.sh') -Force
ConvertTo-UnixFile -Path (Join-Path $bundle 'install-server.sh')
$html = Join-Path $PSScriptRoot 'download\index.html'
if (Test-Path -LiteralPath $html) {
    Copy-Item -LiteralPath $html -Destination (Join-Path $bundle 'download\index.html') -Force
}
$nl = [string]([char]10)
$bundleSecrets = @(
    "FRP_TOKEN=$($sec['FRP_TOKEN'])",
    "FRP_DASHBOARD_USER=$($sec['FRP_DASHBOARD_USER'])",
    "FRP_DASHBOARD_PASSWORD=$($sec['FRP_DASHBOARD_PASSWORD'])",
    "FRP_VERSION=$($cfg['FRP_VERSION'])",
    "SERVER_IP=$($cfg['SERVER_IP'])",
    "FRP_BIND_PORT=$($cfg['FRP_BIND_PORT'])",
    "FRP_REMOTE_PORT=$($cfg['FRP_REMOTE_PORT'])"
) -join $nl
$utf8 = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText((Join-Path $bundle 'secrets.env'), $bundleSecrets + $nl, $utf8)

$tarExe = Get-HomeaiTarExe
Push-Location $bundle
try {
    & $tarExe -cf $tarFile install-server.sh secrets.env download
    if ($LASTEXITCODE -ne 0) { throw "failed to create upload tar" }
} finally {
    Pop-Location
}

$remoteCmd = 'mkdir -p /tmp/frp-script-dir && tar -xf - -C /tmp/frp-script-dir && cd /tmp/frp-script-dir && bash ./install-server.sh'

try {
    Initialize-HomeaiSshAuth -Secrets $sec -Target $target -IdentityFile $keyPath
    Write-Host ("Upload + install in one SSH to " + $target)
    Invoke-HomeaiSsh -SshArgs $sshArgs -Target $target -RemoteCommand $remoteCmd -StdinFile $tarFile
} finally {
    Clear-HomeaiSshAuth
    Remove-Item -LiteralPath $tarFile -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $bundle -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "Server install done. Next on this PC:"
Write-Host "  .\setup-local.ps1"
Write-Host ("Download page: http://" + $cfg['SERVER_IP'] + "/app/")
Write-Host "Open Aliyun security group TCP 22 / 80 / 443 / 7000"
