# -*- coding: utf-8 -*-
# 开发机连接服务主机的配置与 SSH。由 check-remote / publish-remote / remote-host 引用。
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"
. (Join-Path $script:RepoRoot 'JeecgBoot\deploy\frp\common.ps1')

$script:HostEnvPath = Join-Path $PSScriptRoot 'host.env'

function Get-HomeaiHostConfig {
    $map = @{}
    if (Test-Path -LiteralPath $script:HostEnvPath) {
        Get-Content -LiteralPath $script:HostEnvPath -Encoding UTF8 | ForEach-Object {
            $line = $_.Trim()
            if ($line -eq '' -or $line.StartsWith('#')) { return }
            $eq = $line.IndexOf('=')
            if ($eq -lt 1) { return }
            $key = $line.Substring(0, $eq).Trim()
            $val = $line.Substring($eq + 1).Trim().Trim("'").Trim('"')
            $map[$key] = $val
        }
    }
    if (-not $map['SSH_PORT']) { $map['SSH_PORT'] = '22' }
    if (-not $map['REMOTE_REPO']) { $map['REMOTE_REPO'] = 'C:\homeai\src\AITools' }
    if (-not $map['GIT_REMOTE']) { $map['GIT_REMOTE'] = 'origin' }
    if (-not $map['GIT_BRANCH']) { $map['GIT_BRANCH'] = 'dev' }
    if (-not $map['SSH_USER']) { $map['SSH_USER'] = $env:USERNAME }
    if (-not $map['PUBLIC_BASE']) {
        $frp = Get-HomeaiFrpConfigSafe
        $map['PUBLIC_BASE'] = ('http://{0}' -f $frp['SERVER_IP'])
    }
    if (-not $map['SSH_IDENTITY']) {
        $guess = Join-Path $env:USERPROFILE '.ssh\id_ed25519'
        if (Test-Path -LiteralPath $guess) { $map['SSH_IDENTITY'] = $guess }
    }
    return $map
}

function Assert-HomeaiHostConfig {
    param([hashtable]$Cfg)
    if ([string]::IsNullOrWhiteSpace($Cfg['HOST_IP'])) {
        throw "请复制 docs/deploy/host.env.example 为 host.env 并填写 HOST_IP（服务主机局域网地址）。"
    }
}

function Get-HomeaiHostSshTarget {
    param([hashtable]$Cfg)
    return ('{0}@{1}' -f $Cfg['SSH_USER'], $Cfg['HOST_IP'])
}

function Get-HomeaiHostSshArgs {
    param([hashtable]$Cfg)
    $args = @(
        '-T',
        '-p', [string]$Cfg['SSH_PORT'],
        '-o', 'StrictHostKeyChecking=accept-new',
        '-o', 'ConnectTimeout=12',
        '-o', 'ServerAliveInterval=30',
        '-o', 'ServerAliveCountMax=12',
        '-o', 'RequestTTY=no'
    )
    $id = [string]$Cfg['SSH_IDENTITY']
    if ($id) {
        if (-not (Test-Path -LiteralPath $id)) {
            throw "找不到 SSH 私钥：$id"
        }
        $args += @('-i', $id, '-o', 'BatchMode=yes', '-o', 'IdentitiesOnly=yes')
    } else {
        $args += @('-o', 'NumberOfPasswordPrompts=1')
    }
    return $args
}

function Invoke-HomeaiHostSsh {
    param(
        [hashtable]$Cfg,
        [Parameter(Mandatory = $true)][string]$RemoteCommand,
        [string]$StdinFile = ''
    )
    Ensure-HomeaiSshClient
    $target = Get-HomeaiHostSshTarget -Cfg $Cfg
    $sshArgs = Get-HomeaiHostSshArgs -Cfg $Cfg
    $id = [string]$Cfg['SSH_IDENTITY']
    try {
        if (-not $id) {
            $sec = @{}
            if ($Cfg['SSH_PASSWORD']) { $sec['SSH_PASSWORD'] = $Cfg['SSH_PASSWORD'] }
            Initialize-HomeaiSshAuth -Secrets $sec -Target $target -IdentityFile ''
        }
        Invoke-HomeaiSsh -SshArgs $sshArgs -Target $target -RemoteCommand $RemoteCommand -StdinFile $StdinFile
    } finally {
        Clear-HomeaiSshAuth
    }
}

function Invoke-HomeaiHostPwsh {
    param(
        [hashtable]$Cfg,
        [Parameter(Mandatory = $true)][string]$ScriptText
    )
    $b64 = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($ScriptText))
    Invoke-HomeaiHostSsh -Cfg $Cfg -RemoteCommand ("powershell.exe -NoProfile -ExecutionPolicy Bypass -EncodedCommand " + $b64)
}

function ConvertTo-HomeaiPwshLiteral {
    param([string]$Text)
    if ($null -eq $Text) { return "''" }
    return ("'" + ($Text -replace "'", "''") + "'")
}

function Test-HomeaiHttpOk {
    param([string]$Url, [int]$TimeoutSec = 8)
    try {
        $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec $TimeoutSec -MaximumRedirection 2
        return ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 400)
    } catch {
        return $false
    }
}
