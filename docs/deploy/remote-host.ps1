# -*- coding: utf-8 -*-
# 从开发机对服务主机：发布 / 检查 / 启停 / 拉代码。
# 用法：
#   复制 host.env.example 为 host.env 并填写 HOST_IP
#   .\check-remote.ps1
#   .\publish-remote.ps1
#   .\remote-host.ps1 -Action Start
#   .\remote-host.ps1 -Action Stop
#   .\remote-host.ps1 -Action Pull
param(
    [ValidateSet('Check', 'Publish', 'Start', 'Stop', 'Pull')]
    [string]$Action = 'Check',
    [switch]$Backend,
    [switch]$Frontend,
    [switch]$SkipPush,
    [switch]$SkipPull,
    [switch]$NoCheck
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\remote-common.ps1"

$cfg = Get-HomeaiHostConfig
Assert-HomeaiHostConfig -Cfg $cfg
$repoLit = ConvertTo-HomeaiPwshLiteral -Text $cfg['REMOTE_REPO']
$branch = [string]$cfg['GIT_BRANCH']
$gitRemote = [string]$cfg['GIT_REMOTE']
$deployRel = 'docs\deploy'

function Invoke-HomeaiRemoteGitPull {
    $bLit = ConvertTo-HomeaiPwshLiteral -Text $branch
    $rLit = ConvertTo-HomeaiPwshLiteral -Text $gitRemote
    $script = @"
`$ErrorActionPreference = 'Stop'
Set-Location $repoLit
if (-not (Test-Path -LiteralPath '.git')) { throw '远程目录不是 git 仓库。请先在主机 git clone 到 REMOTE_REPO。' }
git fetch $rLit
git checkout $bLit
git pull --ff-only $rLit $bLit
if (`$LASTEXITCODE -ne 0) { throw "git pull 失败，exit=`$LASTEXITCODE" }
Write-Host ('远程 HEAD ' + (git rev-parse --short HEAD))
"@
    Write-Host ("[远程] git pull {0} {1}" -f $gitRemote, $branch)
    Invoke-HomeaiHostPwsh -Cfg $cfg -ScriptText $script
}

function Invoke-HomeaiRemotePublish {
    $wantBackend = $Backend
    $wantFrontend = $Frontend
    if (-not $wantBackend -and -not $wantFrontend) {
        $wantBackend = $true
        $wantFrontend = $true
    }
    $flags = @()
    if ($wantBackend) { $flags += '-Backend' }
    if ($wantFrontend) { $flags += '-Frontend' }
    $flagText = $flags -join ' '
    $script = @"
`$ErrorActionPreference = 'Stop'
`$pub = Join-Path $repoLit 'docs\deploy\publish-all.ps1'
if (-not (Test-Path -LiteralPath `$pub)) { throw "找不到 `$pub" }
Set-Location (Split-Path `$pub)
& `$pub $flagText
if (`$LASTEXITCODE -ne 0) { throw "远程发布失败，exit=`$LASTEXITCODE" }
"@
    Write-Host ("[远程] publish-all.ps1 {0}" -f $flagText)
    Invoke-HomeaiHostPwsh -Cfg $cfg -ScriptText $script
}

function Invoke-HomeaiRemoteStartStop {
    param([string]$Kind)
    $name = if ($Kind -eq 'Start') { 'start-all.ps1' } else { 'stop-all.ps1' }
    $script = @"
`$ErrorActionPreference = 'Stop'
`$f = Join-Path $repoLit 'docs\deploy\$name'
if (-not (Test-Path -LiteralPath `$f)) { throw "找不到 `$f" }
Set-Location (Split-Path `$f)
& `$f
if (`$LASTEXITCODE -ne 0) { throw "$name 失败，exit=`$LASTEXITCODE" }
"@
    Write-Host ("[远程] {0}" -f $name)
    Invoke-HomeaiHostPwsh -Cfg $cfg -ScriptText $script
}

function Invoke-HomeaiRemoteCheck {
    $nginxPort = '8088'
    try { $nginxPort = [string](Get-HomeaiFrpConfigSafe)['HOME_NGINX_PORT'] } catch { }
    $hostIp = [string]$cfg['HOST_IP']
    $publicBase = [string]$cfg['PUBLIC_BASE'].TrimEnd('/')

    Write-Host '========== 远程检查（开发机侧） =========='
    Write-Host ("目标 {0}" -f $hostIp)
    $ports = @(22, 3306, 6379, 8080, [int]$nginxPort, 3000, 8012)
    $optional = @{ 3000 = $true; 8012 = $true }
    $lanFail = @()
    foreach ($p in $ports) {
        $ok = Test-HomeaiTcpPort -TargetHost $hostIp -Port $p
        $opt = [bool]$optional[$p]
        $mark = if ($ok) { 'OK' } elseif ($opt) { 'WARN' } else { 'FAIL' }
        Write-Host ("[{0}] {1}:{2}" -f $mark, $hostIp, $p)
        if (-not $ok -and -not $opt) { $lanFail += "$p" }
    }

    $lanProbe = "http://${hostIp}:${nginxPort}/jeecg-boot/sys/randomImage/homeai-probe"
    $lanAdmin = "http://${hostIp}:${nginxPort}/"
    $lanProbeOk = Test-HomeaiHttpOk -Url $lanProbe
    $lanAdminOk = Test-HomeaiHttpOk -Url $lanAdmin
    Write-Host ("[{0}] {1}" -f $(if ($lanProbeOk) { 'OK' } else { 'FAIL' }), $lanProbe)
    Write-Host ("[{0}] {1}" -f $(if ($lanAdminOk) { 'OK' } else { 'FAIL' }), $lanAdmin)

    $pubFail = $false
    if ($publicBase) {
        $pubProbe = "$publicBase/jeecg-boot/sys/randomImage/homeai-probe"
        $pubHome = "$publicBase/"
        $pubOk = Test-HomeaiHttpOk -Url $pubProbe -TimeoutSec 12
        $pubHomeOk = Test-HomeaiHttpOk -Url $pubHome -TimeoutSec 12
        Write-Host ("[{0}] {1}" -f $(if ($pubOk) { 'OK' } else { 'FAIL' }), $pubProbe)
        Write-Host ("[{0}] {1}" -f $(if ($pubHomeOk) { 'OK' } else { 'FAIL' }), $pubHome)
        if (-not $pubOk) { $pubFail = $true }
    }

    Write-Host ''
    Write-Host '========== 远程检查（SSH 主机侧） =========='
    $localCheck = @"
`$ErrorActionPreference = 'Continue'
`$f = Join-Path $repoLit 'docs\deploy\check-host-local.ps1'
if (-not (Test-Path -LiteralPath `$f)) { throw "找不到 `$f ，请先把仓库放到 REMOTE_REPO" }
& `$f
exit `$LASTEXITCODE
"@
    try {
        Invoke-HomeaiHostPwsh -Cfg $cfg -ScriptText $localCheck
        $sshCode = 0
    } catch {
        $sshCode = 1
        Write-Host ("[FAIL] SSH 本机检查：{0}" -f $_.Exception.Message)
    }

    Write-Host ''
    if ($lanFail.Count -gt 0) {
        Write-Host ("开发机连主机失败端口：{0}（确认同一局域网、主机防火墙专用网络放行）" -f ($lanFail -join ','))
    }
    if ($pubFail) {
        Write-Host '公网探测失败：主机 frpc 是否在跑、云服务器 frps/nginx 是否正常。局域网通而公网不通时先看主机 frpc。'
    }
    if ($lanFail.Count -gt 0 -or $sshCode -ne 0) {
        exit 1
    }
    Write-Host '远程检查完成。'
}

Write-Host ("========== 服务主机 {0} ==========" -f $Action)
Write-Host ("SSH {0}  仓库 {1}" -f (Get-HomeaiHostSshTarget -Cfg $cfg), $cfg['REMOTE_REPO'])

switch ($Action) {
    'Pull' { Invoke-HomeaiRemoteGitPull }
    'Check' { Invoke-HomeaiRemoteCheck }
    'Start' {
        Invoke-HomeaiRemoteStartStop -Kind Start
        if (-not $NoCheck) { Invoke-HomeaiRemoteCheck }
    }
    'Stop' { Invoke-HomeaiRemoteStartStop -Kind Stop }
    'Publish' {
        if (-not $SkipPush) {
            Push-Location $script:RepoRoot
            try {
                $st = git status --porcelain
                if ($st) {
                    Write-Host '[注意] 开发机工作区有未提交改动，不会同步到主机。请先 commit + push。'
                }
                Write-Host ("[本地] git push {0} {1}" -f $gitRemote, $branch)
                git push $gitRemote $branch
                if ($LASTEXITCODE -ne 0) { throw "本地 git push 失败，exit=$LASTEXITCODE" }
            } finally {
                Pop-Location
            }
        }
        if (-not $SkipPull) {
            Invoke-HomeaiRemoteGitPull
        }
        Invoke-HomeaiRemotePublish
        if (-not $NoCheck) {
            Invoke-HomeaiRemoteCheck
        }
    }
}
