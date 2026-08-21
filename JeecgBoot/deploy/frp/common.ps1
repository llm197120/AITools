# -*- coding: utf-8 -*-
# HomeAI FRP 本机脚本公共函数。由同目录其它 .ps1 dot-source。
$ErrorActionPreference = 'Stop'

$script:FrpDir = $PSScriptRoot
$script:ConfigPath = Join-Path $script:FrpDir 'config.env'
$script:SecretsPath = Join-Path $script:FrpDir 'secrets.env'

function Read-DotEnv {
    param([Parameter(Mandatory = $true)][string]$Path)
    $map = @{}
    if (-not (Test-Path -LiteralPath $Path)) { return $map }
    Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $eq = $line.IndexOf('=')
        if ($eq -lt 1) { return }
        $key = $line.Substring(0, $eq).Trim()
        $val = $line.Substring($eq + 1).Trim().Trim("'").Trim('"')
        $map[$key] = $val
    }
    return $map
}

function Get-HomeaiFrpConfig {
    $cfg = Read-DotEnv -Path $script:ConfigPath
    if (-not $cfg.ContainsKey('SERVER_IP')) { $cfg['SERVER_IP'] = '116.62.115.226' }
    if (-not $cfg.ContainsKey('FRP_VERSION')) { $cfg['FRP_VERSION'] = '0.71.0' }
    if (-not $cfg.ContainsKey('FRP_BIND_PORT')) { $cfg['FRP_BIND_PORT'] = '7000' }
    if (-not $cfg.ContainsKey('FRP_REMOTE_PORT')) { $cfg['FRP_REMOTE_PORT'] = '18080' }
    if (-not $cfg.ContainsKey('HOME_NGINX_PORT')) { $cfg['HOME_NGINX_PORT'] = '8088' }
    if (-not $cfg.ContainsKey('BACKEND_PORT')) { $cfg['BACKEND_PORT'] = '8080' }
    if (-not $cfg.ContainsKey('HOME_ROOT')) { $cfg['HOME_ROOT'] = 'C:\homeai' }
    if (-not $cfg.ContainsKey('NGINX_WINDOWS_VERSION')) { $cfg['NGINX_WINDOWS_VERSION'] = '1.26.3' }
    return $cfg
}

function New-RandomHex {
    param([int]$Bytes = 24)
    $buf = New-Object byte[] $Bytes
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($buf)
    $rng.Dispose()
    return ([BitConverter]::ToString($buf) -replace '-', '').ToLowerInvariant()
}

function Initialize-HomeaiFrpSecrets {
    if (Test-Path -LiteralPath $script:SecretsPath) {
        $existing = Read-DotEnv -Path $script:SecretsPath
        if ($existing['FRP_TOKEN']) { return $existing }
    }
    $token = New-RandomHex -Bytes 24
    $dash = New-RandomHex -Bytes 12
    $nl = [Environment]::NewLine
    $text = "FRP_TOKEN=$token$nl" + "FRP_DASHBOARD_USER=admin$nl" + "FRP_DASHBOARD_PASSWORD=$dash$nl"
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($script:SecretsPath, $text, $utf8)
    Write-Host "[ok] secrets.env created (gitignore, do not commit)"
    return Read-DotEnv -Path $script:SecretsPath
}

function Get-HomeaiRepoRoot {
    return (Resolve-Path (Join-Path $script:FrpDir '..\..\..')).Path
}

function Test-TcpPortOpen {
    param([string]$TargetHost = '127.0.0.1', [int]$Port)
    try {
        $c = New-Object System.Net.Sockets.TcpClient
        $iar = $c.BeginConnect($TargetHost, $Port, $null, $null)
        $ok = $iar.AsyncWaitHandle.WaitOne(800)
        if (-not $ok) { $c.Close(); return $false }
        $c.EndConnect($iar)
        $c.Close()
        return $true
    } catch {
        return $false
    }
}

function Get-DownloadFile {
    param(
        [Parameter(Mandatory = $true)][string]$Destination,
        [Parameter(Mandatory = $true)][string[]]$Urls
    )
    $dir = Split-Path -Parent $Destination
    if ($dir -and -not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir | Out-Null
    }
    foreach ($url in $Urls) {
        Write-Host "  下载 $url"
        try {
            Invoke-WebRequest -Uri $url -OutFile $Destination -UseBasicParsing -TimeoutSec 120
            if ((Test-Path -LiteralPath $Destination) -and ((Get-Item -LiteralPath $Destination).Length -gt 10000)) {
                return
            }
        } catch {
            Write-Host "  失败: $($_.Exception.Message)"
        }
    }
    throw "下载失败: $($Urls[0])"
}

function Get-HomeaiTarExe {
    $sys = Join-Path $env:SystemRoot 'System32\tar.exe'
    if (Test-Path -LiteralPath $sys) { return $sys }
    $cmd = Get-Command tar -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw "tar.exe not found. Use Windows 10+ tar."
}

function ConvertTo-UnixText {
    param([Parameter(Mandatory = $true)][string]$Text)
    # Must use string,string Replace. Mixing string+char makes PS pick char,char and fail on CRLF (2 chars).
    $lf = [string]([char]10)
    $cr = [string]([char]13)
    return $Text.Replace(($cr + $lf), $lf).Replace($cr, $lf)
}

function ConvertTo-UnixFile {
    param([Parameter(Mandatory = $true)][string]$Path)
    $raw = ConvertTo-UnixText -Text ([System.IO.File]::ReadAllText($Path))
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($Path, $raw, $utf8)
}

function Ensure-HomeaiSshClient {
    if ((Get-Command ssh -ErrorAction SilentlyContinue) -and (Get-Command scp -ErrorAction SilentlyContinue)) {
        return
    }
    $candidates = @(
        'C:\Windows\System32\OpenSSH',
        (Join-Path $env:USERPROFILE 'scoop\apps\git\current\usr\bin'),
        'C:\Program Files\Git\usr\bin',
        'C:\Program Files (x86)\Git\usr\bin',
        (Join-Path $env:LOCALAPPDATA 'Programs\Git\usr\bin')
    )
    if (Get-Command git -ErrorAction SilentlyContinue) {
        $gitCmd = (Get-Command git).Source
        $gitRoot = Split-Path (Split-Path $gitCmd)
        $candidates += @(Join-Path $gitRoot 'usr\bin')
    }
    foreach ($dir in $candidates) {
        if (-not $dir) { continue }
        $sshExe = Join-Path $dir 'ssh.exe'
        $scpExe = Join-Path $dir 'scp.exe'
        if ((Test-Path -LiteralPath $sshExe) -and (Test-Path -LiteralPath $scpExe)) {
            $env:Path = $dir + ';' + $env:Path
            Write-Host ("Using SSH from: " + $dir)
            return
        }
    }
    throw "ssh/scp not found. Install OpenSSH Client (Windows Settings -> Optional Features), or add Git usr\bin to PATH."
}

function Resolve-HomeaiSshIdentity {
    param([string]$Path)
    if (-not $Path) { return $null }
    $names = @($Path)
    if (-not [System.IO.Path]::HasExtension($Path)) {
        $names += @("$Path.pem", "$Path.key")
    }
    $dirs = @(
        (Get-Location).Path,
        $script:FrpDir,
        (Join-Path $env:USERPROFILE '.ssh')
    )
    $tried = New-Object System.Collections.Generic.List[string]
    foreach ($n in $names) {
        if ([System.IO.Path]::IsPathRooted($n) -and (Test-Path -LiteralPath $n)) {
            return (Resolve-Path -LiteralPath $n).Path
        }
        foreach ($d in $dirs) {
            $c = Join-Path $d (Split-Path -Leaf $n)
            $tried.Add($c) | Out-Null
            if (Test-Path -LiteralPath $c) {
                return (Resolve-Path -LiteralPath $c).Path
            }
        }
        if (Test-Path -LiteralPath $n) {
            return (Resolve-Path -LiteralPath $n).Path
        }
    }
    throw ("SSH identity file not found: {0}. Looked in .ssh / current dir / deploy/frp. Example: -SshIdentityFile `$env:USERPROFILE\.ssh\AITools0820" -f $Path)
}

function ConvertFrom-HomeaiSecureString {
    param([Parameter(Mandatory = $true)][System.Security.SecureString]$Secure)
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($Secure)
    try {
        return [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
    } finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Initialize-HomeaiSshAuth {
    param(
        [hashtable]$Secrets,
        [string]$Target,
        [string]$IdentityFile = ''
    )
    $script:HomeaiSshUseAskpass = $false
    if ($IdentityFile) { return }
    $plain = $Secrets['SSH_PASSWORD']
    if ($plain) {
        Write-Host "Using SSH_PASSWORD from secrets.env (one SSH session, no prompt)"
    } else {
        $typed = Read-Host -Prompt ("SSH password for " + $Target + " (once)") -AsSecureString
        $plain = ConvertFrom-HomeaiSecureString -Secure $typed
    }
    if (-not $plain) { throw "SSH password is empty" }
    $script:HomeaiAskpassCmd = Join-Path $env:TEMP ("homeai-askpass-" + [guid]::NewGuid().ToString('n') + ".cmd")
    $utf8 = New-Object System.Text.UTF8Encoding $false
    $cmd = "@echo off`r`npowershell.exe -NoProfile -Command `"[Console]::Out.Write(`$env:HOMEAI_SSH_PASSWORD)`"`r`n"
    [System.IO.File]::WriteAllText($script:HomeaiAskpassCmd, $cmd, $utf8)
    $env:HOMEAI_SSH_PASSWORD = $plain
    $env:SSH_ASKPASS = $script:HomeaiAskpassCmd
    $env:SSH_ASKPASS_REQUIRE = 'force'
    if (-not $env:DISPLAY) { $env:DISPLAY = 'localhost:0' }
    $script:HomeaiSshUseAskpass = $true
}

function Clear-HomeaiSshAuth {
    if (Test-Path Env:HOMEAI_SSH_PASSWORD) { Remove-Item Env:HOMEAI_SSH_PASSWORD }
    if (Test-Path Env:SSH_ASKPASS) { Remove-Item Env:SSH_ASKPASS }
    if (Test-Path Env:SSH_ASKPASS_REQUIRE) { Remove-Item Env:SSH_ASKPASS_REQUIRE }
    if ($script:HomeaiAskpassCmd -and (Test-Path -LiteralPath $script:HomeaiAskpassCmd)) {
        Remove-Item -LiteralPath $script:HomeaiAskpassCmd -Force -ErrorAction SilentlyContinue
    }
    $script:HomeaiAskpassCmd = $null
    $script:HomeaiSshUseAskpass = $false
}

function Invoke-HomeaiSsh {
    param(
        [string[]]$SshArgs,
        [Parameter(Mandatory = $true)][string]$Target,
        [Parameter(Mandatory = $true)][string]$RemoteCommand,
        [string]$StdinFile = ''
    )
    $sshExe = (Get-Command ssh).Source
    $parts = New-Object System.Collections.Generic.List[string]
    $parts.Add(('"{0}"' -f $sshExe))
    foreach ($a in $SshArgs) {
        if ($null -eq $a -or $a -eq '') { continue }
        $s = [string]$a
        if ($s -match '[\s&<>^|()"]') { $parts.Add(('"{0}"' -f ($s -replace '"', '\"'))) }
        else { $parts.Add($s) }
    }
    $parts.Add($Target)
    $parts.Add(('"{0}"' -f ($RemoteCommand -replace '"', '\"')))
    $line = $parts -join ' '
    if ($StdinFile) {
        if (-not (Test-Path -LiteralPath $StdinFile)) { throw ("stdin file missing: " + $StdinFile) }
        $line = $line + ' < "' + $StdinFile + '"'
    }
    cmd.exe /c $line
    if ($LASTEXITCODE -ne 0) {
        throw ("ssh failed, exit=" + $LASTEXITCODE + " target=" + $Target)
    }
}
