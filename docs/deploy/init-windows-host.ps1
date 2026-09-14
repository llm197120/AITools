# -*- coding: utf-8 -*-
# 空系统 Windows 主机初始化（管理员）。装工具 +（可选）Docker 数据面。
# 前提：已把本仓库拷到新电脑，或本机已能访问该目录。
#
# 用法（管理员 PowerShell）：
#   cd docs\deploy
#   .\init-windows-host.ps1
#   .\init-windows-host.ps1 -Phase Apps
#   .\init-windows-host.ps1 -Phase Datastores     # 重启并打开 Docker Desktop 之后
#   .\init-windows-host.ps1 -SkipLibreOffice
#
# 装完后：setup-local.ps1 → scripts\init-db.bat → publish-all / start-all
param(
    [ValidateSet('All', 'Apps', 'Datastores')]
    [string]$Phase = 'All',
    [switch]$SkipLibreOffice,
    [switch]$SkipDocker,
    [switch]$HardenFirewall,
    [switch]$SkipSsh
)

$ErrorActionPreference = 'Stop'
$PSDefaultParameterValues['*:Encoding'] = 'utf8'

function Test-HomeaiAdmin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $p = New-Object Security.Principal.WindowsPrincipal($id)
    return $p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-HomeaiAdmin)) {
    throw '请右键「以管理员身份运行」。需要管理员才能装软件、开 WSL、写 C:\homeai。'
}

. "$PSScriptRoot\common.ps1"

$homeRoot = Get-HomeaiHomeRoot
$needReboot = $false

function Write-HomeaiStep {
    param([string]$Message)
    Write-Host ''
    Write-Host "===== $Message ====="
}

function Test-HomeaiWinget {
    $cmd = Get-Command winget.exe -ErrorAction SilentlyContinue
    return [bool]$cmd
}

function Test-HomeaiWingetId {
    param([string]$Id)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $out = & winget list -e --id $Id --accept-source-agreements 2>$null | Out-String
        return ($LASTEXITCODE -eq 0 -and $out -match [regex]::Escape($Id))
    } catch {
        return $false
    } finally {
        $ErrorActionPreference = $prev
    }
}

function Install-HomeaiWingetId {
    param([string]$Id, [string]$Label)
    if (Test-HomeaiWingetId -Id $Id) {
        Write-Host "[跳过] $Label 已安装 ($Id)"
        return
    }
    Write-Host "[安装] $Label ($Id) ..."
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & winget install -e --id $Id --source winget --accept-package-agreements --accept-source-agreements --disable-interactivity
    $code = $LASTEXITCODE
    $ErrorActionPreference = $prev
    # 0 成功；-1978335189 已安装
    if ($code -ne 0 -and $code -ne -1978335189) {
        throw "$Label 安装失败，winget exit=$code"
    }
}

function Update-HomeaiProcessPath {
    $machine = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $user = [Environment]::GetEnvironmentVariable('Path', 'User')
    $env:Path = "$machine;$user"
}

function Set-HomeaiJavaHomeIfMissing {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        Write-Host ("JAVA_HOME = {0}" -f $env:JAVA_HOME)
        return
    }
    $candidates = @()
    $candidates += Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue
    $candidates += Get-ChildItem 'C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue
    $candidates += Get-ChildItem 'C:\Program Files\Microsoft' -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'jdk' }
    $jdk = $candidates | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
    if (-not $jdk) {
        Write-Host '[注意] 未自动找到 JDK 17。装完后若 java 不在 PATH，请注销再登录。'
        return
    }
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk.FullName, 'Machine')
    $env:JAVA_HOME = $jdk.FullName
    Write-Host ("已设置 JAVA_HOME = {0}" -f $jdk.FullName)
}

function Enable-HomeaiLongPaths {
    New-ItemProperty -Path 'HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem' -Name 'LongPathsEnabled' -Value 1 -PropertyType DWord -Force | Out-Null
    Write-Host '[系统] 已启用 NTFS 长路径'
}

function Set-HomeaiPowerNoSleep {
    Write-Host '[电源] 接通电源时禁止睡眠/休眠（显示器 30 分钟关闭）'
    powercfg -change -standby-timeout-ac 0 | Out-Null
    powercfg -change -hibernate-timeout-ac 0 | Out-Null
    powercfg -change -monitor-timeout-ac 30 | Out-Null
    powercfg -h off | Out-Null
}

function Set-HomeaiChinaTime {
    try {
        tzutil /s 'China Standard Time' | Out-Null
        Write-Host '[系统] 时区 China Standard Time'
    } catch {
        Write-Host '[系统] 未改时区（可稍后手动设为中国）'
    }
}

function Enable-HomeaiWsl {
    Write-Host '[WSL] 启用适用于 Linux 的 Windows 子系统 / 虚拟机平台（Docker 需要）'
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux -All -NoRestart | Out-Null
    Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All -NoRestart | Out-Null
    $ErrorActionPreference = $prev
    $script:needReboot = $true
}

function Install-HomeaiPnpm {
    Update-HomeaiProcessPath
    $node = Get-Command node.exe -ErrorAction SilentlyContinue
    if (-not $node) {
        Write-Host '[pnpm] 未找到 node，跳过 corepack。注销后重跑 -Phase Apps 或手动：corepack enable'
        return
    }
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & corepack enable
    & corepack prepare pnpm@9 --activate
    $ErrorActionPreference = $prev
    Write-Host '[pnpm] 已通过 corepack 启用 pnpm@9'
}

function New-HomeaiLayout {
    $dirs = @(
        $homeRoot,
        (Join-Path $homeRoot 'logs'),
        (Join-Path $homeRoot 'tmp'),
        (Join-Path $homeRoot 'secrets')
    )
    foreach ($d in $dirs) {
        New-Item -ItemType Directory -Force -Path $d | Out-Null
    }
    Write-Host ("[目录] {0}" -f $homeRoot)
}

function Set-HomeaiRuntimeEnvFile {
    $example = Join-Path $script:RuntimeDir '.env.example'
    $envFile = Join-Path $script:RuntimeDir '.env'
    if ((Test-Path -LiteralPath $example) -and -not (Test-Path -LiteralPath $envFile)) {
        Copy-Item -LiteralPath $example -Destination $envFile
        Write-Host "[数据] 已写入 $envFile （默认 root/123456，与 application-dev.yml 一致）"
    }
}

function Enable-HomeaiFirewallHarden {
    Write-Host '[防火墙] 公用网络配置文件拦截 MySQL/Redis/后端/文档预览入站'
    $rules = @(
        @{ Name = 'HomeAI-Block-MySQL-Public'; Port = 3306 },
        @{ Name = 'HomeAI-Block-Redis-Public'; Port = 6379 },
        @{ Name = 'HomeAI-Block-Jeecg-Public'; Port = 8080 },
        @{ Name = 'HomeAI-Block-Gotenberg-Public'; Port = 3000 },
        @{ Name = 'HomeAI-Block-KkFileView-Public'; Port = 8012 }
    )
    foreach ($r in $rules) {
        if (Get-NetFirewallRule -DisplayName $r.Name -ErrorAction SilentlyContinue) { continue }
        New-NetFirewallRule -DisplayName $r.Name -Direction Inbound -Protocol TCP -LocalPort $r.Port -Action Block -Profile Public | Out-Null
    }
}

function Enable-HomeaiOpenSshServer {
    Write-Host '[SSH] 安装并启动 OpenSSH Server（仅专用/专用网络防火墙放行 22）'
    $cap = Get-WindowsCapability -Online | Where-Object { $_.Name -like 'OpenSSH.Server*' } | Select-Object -First 1
    if ($cap -and $cap.State -ne 'Installed') {
        Add-WindowsCapability -Online -Name $cap.Name | Out-Null
    }
    $svc = Get-Service -Name sshd -ErrorAction SilentlyContinue
    if (-not $svc) {
        Write-Host '[SSH] 未找到 sshd 服务，请重启后再跑一次初始化，或在「可选功能」里安装 OpenSSH 服务器'
        return
    }
    Set-Service -Name sshd -StartupType Automatic
    if ($svc.Status -ne 'Running') { Start-Service sshd }
    $ruleName = 'HomeAI-OpenSSH-Private'
    if (-not (Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue)) {
        New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Protocol TCP -LocalPort 22 -Action Allow -Profile Private | Out-Null
    }
    Write-Host '[SSH] sshd 已启动。请把开发机公钥写入本机用户 .ssh\authorized_keys'
}

function Invoke-HomeaiAppsPhase {
    Write-HomeaiStep '检查 winget'
    if (-not (Test-HomeaiWinget)) {
        throw "未找到 winget。请先从 Microsoft Store 安装「应用安装程序」，或改用 Win11。"
    }
    Write-Host '[winget] 更新源...'
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & winget source update --disable-interactivity 2>$null | Out-Null
    $ErrorActionPreference = $prev

    Write-HomeaiStep '系统选项'
    Set-HomeaiChinaTime
    Enable-HomeaiLongPaths
    Set-HomeaiPowerNoSleep
    New-HomeaiLayout

    Write-HomeaiStep '安装软件'
    Install-HomeaiWingetId -Id 'Git.Git' -Label 'Git'
    Install-HomeaiWingetId -Id 'EclipseAdoptium.Temurin.17.JDK' -Label 'JDK 17'
    Install-HomeaiWingetId -Id 'Apache.Maven' -Label 'Maven'
    Install-HomeaiWingetId -Id 'OpenJS.NodeJS.LTS' -Label 'Node.js LTS'
    if (-not $SkipLibreOffice) {
        Install-HomeaiWingetId -Id 'TheDocumentFoundation.LibreOffice' -Label 'LibreOffice'
    }
    if (-not $SkipDocker) {
        Enable-HomeaiWsl
        Install-HomeaiWingetId -Id 'Docker.DockerDesktop' -Label 'Docker Desktop'
        $script:needReboot = $true
    }

    Update-HomeaiProcessPath
    Set-HomeaiJavaHomeIfMissing
    Install-HomeaiPnpm

    if ($HardenFirewall) {
        Enable-HomeaiFirewallHarden
    }
    if (-not $SkipSsh) {
        Enable-HomeaiOpenSshServer
    }

    Enable-HomeaiDockerDatastoresFlag
    Set-HomeaiRuntimeEnvFile
}

function Invoke-HomeaiDatastoresPhase {
    Write-HomeaiStep 'Docker 数据面 + 文档预览'
    Enable-HomeaiDockerDatastoresFlag
    Set-HomeaiRuntimeEnvFile
    if (-not (Test-HomeaiDockerReady)) {
        throw 'Docker 引擎未就绪。请先登录 Windows、打开 Docker Desktop 等到左下角变绿，再执行：.\init-windows-host.ps1 -Phase Datastores'
    }
    Start-HomeaiRuntimeDatastores -Required
    Start-HomeaiDocsPreview
    Write-Host ''
    Write-Host '数据面已启动：MySQL 127.0.0.1:3306（库 jeecg，root / 见 runtime/.env），Redis 127.0.0.1:6379'
    Write-HomeaiDocsPreviewHint
}

Write-Host '========== HomeAI Windows 主机初始化 =========='
Write-Host ("仓库: {0}" -f $script:RepoRoot)
Write-Host ("阶段: {0}" -f $Phase)

if ($Phase -in @('All', 'Apps')) {
    Invoke-HomeaiAppsPhase
}
if ($Phase -in @('All', 'Datastores')) {
    if ($SkipDocker) {
        Write-Host '[数据] 已指定 -SkipDocker，跳过容器。请自行安装 MySQL 8 / Redis 并建库 jeecg。'
    } elseif ($Phase -eq 'Datastores' -or (Test-HomeaiDockerReady)) {
        Invoke-HomeaiDatastoresPhase
    } else {
        Write-Host ''
        Write-Host '[下一步] 刚装的 Docker / WSL 需要重启一次。'
        Write-Host '重启并打开 Docker Desktop 就绪后，再执行：'
        Write-Host '  cd docs\deploy'
        Write-Host '  .\init-windows-host.ps1 -Phase Datastores'
        $script:needReboot = $true
    }
}

Write-Host ''
Write-Host '========== 初始化结束 =========='
Write-Host '本机仍不装：Android SDK（出 APK 回开发机）、ComfyUI、Microsoft Office（有则更好）。'
Write-Host '路由器不要映射 3306/6379/8080/8088/3000/8012。'
Write-Host ''
Write-Host '接着做：'
Write-Host '  1. 复制 JeecgBoot/.../application-dev-local.yml.example 为 application-dev-local.yml，填 OSS'
Write-Host '  2. 若改了 MySQL 密码，同步改 application-dev.yml 或 local 覆盖'
Write-Host '  3. 导入库：仓库 scripts\init-db.bat（密码默认 123456，库名 jeecg）'
Write-Host '  4. 本机 Nginx/frpc：JeecgBoot\deploy\frp\setup-local.ps1 -BuildAdmin -RegisterStartup'
Write-Host '  5. 发布/启动：docs\deploy\publish-all.ps1 或 start-all.ps1'
Write-Host '  6. 管理端「系统配置 → Office」填 http://127.0.0.1:3000 与 http://127.0.0.1:8012'
Write-Host '  7. 把旧电脑的 MySQL 备份导入本机（若有数据）'
if ($needReboot) {
    Write-Host ''
    Write-Host '需要重启电脑后再跑 -Phase Datastores。'
}
Write-Host '完成。'
