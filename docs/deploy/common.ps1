# -*- coding: utf-8 -*-
# HomeAI 一键发布/启停共用函数。由同目录其它 .ps1 dot-source。
$ErrorActionPreference = 'Stop'

$script:DeployDir = $PSScriptRoot
$script:RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$script:FrpDir = Join-Path $script:RepoRoot 'JeecgBoot\deploy\frp'
$script:BootDir = Join-Path $script:RepoRoot 'JeecgBoot\jeecg-boot'
$script:VueDir = Join-Path $script:RepoRoot 'JeecgBoot\jeecgboot-vue3'
$script:UniDir = Join-Path $script:RepoRoot 'JeecgUniapp'
$script:StartModule = 'jeecg-module-system/jeecg-system-start'
$script:StartDir = Join-Path $script:BootDir ($script:StartModule -replace '/', '\')
$script:DefaultJavaHome = 'C:\Program Files\Java\jdk-17'

function Get-HomeaiDeployRepoRoot { return $script:RepoRoot }

function Get-HomeaiFrpConfigSafe {
    if ($script:CachedFrpConfig) { return $script:CachedFrpConfig }
    $frpCommon = Join-Path $script:FrpDir 'common.ps1'
    if (Test-Path -LiteralPath $frpCommon) {
        . $frpCommon
        $script:CachedFrpConfig = Get-HomeaiFrpConfig
        return $script:CachedFrpConfig
    }
    $script:CachedFrpConfig = @{
        SERVER_IP       = '116.62.115.226'
        HOME_NGINX_PORT = '8088'
        BACKEND_PORT    = '8080'
        HOME_ROOT       = 'C:\homeai'
    }
    return $script:CachedFrpConfig
}

function Get-HomeaiBackendPort {
    $cfg = Get-HomeaiFrpConfigSafe
    return [int]$cfg['BACKEND_PORT']
}

function Get-HomeaiHomeRoot {
    $cfg = Get-HomeaiFrpConfigSafe
    return [string]$cfg['HOME_ROOT']
}

function Test-HomeaiTcpPort {
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

function Get-HomeaiMavenCmd {
    $mvn = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $mvn) { $mvn = Get-Command mvn -ErrorAction SilentlyContinue }
    if (-not $mvn) { throw '未找到 Maven（mvn）。请先安装并加入 PATH。' }
    return $mvn.Source
}

function Ensure-HomeaiJavaHome {
    if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath $env:JAVA_HOME)) {
        if (Test-Path -LiteralPath $script:DefaultJavaHome) {
            $env:JAVA_HOME = $script:DefaultJavaHome
        }
    }
    if (-not $env:JAVA_HOME) {
        throw '未设置 JAVA_HOME。请安装 JDK 17，或设置 JAVA_HOME。'
    }
    $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $javaExe)) {
        throw "JAVA_HOME 无效：$env:JAVA_HOME"
    }
}

function Get-HomeaiListenPids {
    param([int]$Port)
    $pids = @()
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        if ($c.OwningProcess -and $c.OwningProcess -gt 0) {
            $pids += [int]$c.OwningProcess
        }
    }
    return ($pids | Select-Object -Unique)
}

function Test-HomeaiBackendCmd {
    param([string]$CommandLine)
    if ([string]::IsNullOrWhiteSpace($CommandLine)) { return $false }
    if ($CommandLine -match 'JeecgSystemApplication') { return $true }
    if ($CommandLine -match 'org\.jeecg\.JeecgSystemApplication') { return $true }
    if ($CommandLine -match 'jeecg-system-start' -and $CommandLine -match 'spring-boot:run') { return $true }
    if ($CommandLine -match 'docs\\deploy\\start-backend\.ps1') { return $true }
    if ($CommandLine -match 'docs/deploy/start-backend.ps1') { return $true }
    if ($CommandLine -match 'run-backend\.cmd') { return $true }
    return $false
}

function Stop-HomeaiBackend {
    param([int]$Port = 0)
    if ($Port -le 0) { $Port = Get-HomeaiBackendPort }
    $killed = @{}

    $procs = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue
    foreach ($p in $procs) {
        if (-not (Test-HomeaiBackendCmd -CommandLine $p.CommandLine)) { continue }
        Write-Host ("[后端] 停止 PID {0} ({1})" -f $p.ProcessId, $p.Name)
        & taskkill.exe /PID $p.ProcessId /T /F 2>$null | Out-Null
        $killed[[int]$p.ProcessId] = $true
    }

    foreach ($listenPid in (Get-HomeaiListenPids -Port $Port)) {
        if ($killed.ContainsKey($listenPid)) { continue }
        $proc = Get-Process -Id $listenPid -ErrorAction SilentlyContinue
        $name = if ($proc) { $proc.ProcessName } else { '?' }
        if ($name -match '^(java|javaw|mvn)$') {
            Write-Host ("[后端] 停止端口 {0} 监听进程 PID {1} ({2})" -f $Port, $listenPid, $name)
            & taskkill.exe /PID $listenPid /T /F 2>$null | Out-Null
            $killed[$listenPid] = $true
        } else {
            Write-Host ("[后端] 端口 {0} 被 {1} PID {2} 占用，未强制结束。请确认是否为 JeecgBoot。" -f $Port, $name, $listenPid)
        }
    }

    Start-Sleep -Seconds 1
    if ($killed.Count -eq 0) {
        Write-Host '[后端] 未在运行'
    } else {
        Write-Host '[后端] 已停止'
    }
}

function Resolve-HomeaiTargets {
    param(
        [string[]]$Target = @(),
        [switch]$Frontend,
        [switch]$Backend,
        [switch]$App
    )
    $want = @{ Frontend = $false; Backend = $false; App = $false }
    $named = $false
    foreach ($t in @($Target)) {
        if ([string]::IsNullOrWhiteSpace($t)) { continue }
        $k = $t.Trim().ToLowerInvariant()
        if ($k -in @('all', '*')) {
            return @{ Frontend = $true; Backend = $true; App = $true }
        }
        if ($k -in @('frontend', 'front', 'admin', 'vue')) { $want.Frontend = $true; $named = $true }
        elseif ($k -in @('backend', 'back', 'java', 'api')) { $want.Backend = $true; $named = $true }
        elseif ($k -in @('app', 'apk', 'android')) { $want.App = $true; $named = $true }
        else { throw "未知目标 '$t'。可用：frontend / backend / app / all" }
    }
    if ($Frontend) { $want.Frontend = $true; $named = $true }
    if ($Backend) { $want.Backend = $true; $named = $true }
    if ($App) { $want.App = $true; $named = $true }
    if (-not $named) {
        $want.Frontend = $true
        $want.Backend = $true
        $want.App = $true
    }
    return $want
}

function Write-HomeaiTargetBanner {
    param([hashtable]$Want, [string]$Action)
    $parts = @()
    if ($Want.Backend) { $parts += '后端' }
    if ($Want.Frontend) { $parts += '前端' }
    if ($Want.App) { $parts += 'APP' }
    Write-Host ("目标: {0}（{1}）" -f ($parts -join ' + '), $Action)
}

function Get-HomeaiLogDir {
    $dir = Join-Path (Get-HomeaiHomeRoot) 'logs'
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    return $dir
}

function Get-HomeaiBackendLogPath {
    return (Join-Path (Get-HomeaiLogDir) 'backend.log')
}

function Write-HomeaiBackendLogTail {
    param([int]$Lines = 40)
    $log = Get-HomeaiBackendLogPath
    if (-not (Test-Path -LiteralPath $log)) {
        Write-Host '[后端] 尚无 backend.log'
        return
    }
    Write-Host ("----- {0} 末尾 -----" -f $log)
    Get-Content -LiteralPath $log -Tail $Lines -ErrorAction SilentlyContinue
}

function Wait-HomeaiPort {
    param(
        [int]$Port,
        [int]$TimeoutSec = 300,
        [string]$Label = '服务'
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-HomeaiTcpPort -Port $Port) {
            Write-Host ("[OK] {0} 已监听 127.0.0.1:{1}" -f $Label, $Port)
            return $true
        }
        Start-Sleep -Seconds 3
        Write-Host ("[等待] {0} 127.0.0.1:{1} ..." -f $Label, $Port)
    }
    Write-Host ("[FAIL] {0} 在 {1} 秒内未监听 127.0.0.1:{2}" -f $Label, $TimeoutSec, $Port)
    return $false
}

function Start-HomeaiBackendWindow {
    $port = Get-HomeaiBackendPort
    if (Test-HomeaiTcpPort -Port $port) {
        Write-Host ("[后端] 已在监听 127.0.0.1:{0}，跳过启动" -f $port)
        return $null
    }

    Ensure-HomeaiJavaHome
    $mvn = Get-HomeaiMavenCmd
    $logDir = Get-HomeaiLogDir
    $log = Get-HomeaiBackendLogPath
    $wrapper = Join-Path $logDir 'run-backend.cmd'
    $javaHome = $env:JAVA_HOME
    $startDir = $script:StartDir
    if (-not (Test-Path -LiteralPath (Join-Path $startDir 'pom.xml'))) {
        throw "找不到启动模块：$startDir"
    }

    # 必须在 jeecg-system-start 目录执行 spring-boot:run。
    # 在父工程加 -pl -am 会把 run 打到 jeecg-boot-parent 上，报找不到 main class，启动模块被 SKIPPED。
    $nl = "`r`n"
    $cmd = '@echo off' + $nl
    $cmd += "set `"JAVA_HOME=$javaHome`"" + $nl
    $cmd += 'set "PATH=%JAVA_HOME%\bin;%PATH%"' + $nl
    $cmd += "cd /d `"$startDir`"" + $nl
    $cmd += ">>`"$log`" echo JAVA_HOME=%JAVA_HOME%" + $nl
    $cmd += ">>`"$log`" echo WD=%CD%" + $nl
    $cmd += "call `"$mvn`" spring-boot:run -DskipTests -Dspring-boot.run.profiles=dev >> `"$log`" 2>&1" + $nl
    $cmd += "echo mvn_exit=%ERRORLEVEL% >> `"$log`"" + $nl
    $cmd += 'exit /b %ERRORLEVEL%' + $nl
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($wrapper, $cmd, $utf8)

    Write-Host ("[后端] 后台启动 JeecgBoot，日志 {0}" -f $log)
    $p = Start-Process -FilePath $wrapper -WorkingDirectory $startDir -WindowStyle Hidden -PassThru
    if (-not $p) { throw '无法启动后端进程（Start-Process 返回空）' }
    $pidFile = Join-Path $logDir 'backend.pid'
    [System.IO.File]::WriteAllText($pidFile, "$($p.Id)", $utf8)
    return $p
}

function Start-HomeaiBackendAndWait {
    param([int]$TimeoutSec = 300)
    $p = Start-HomeaiBackendWindow
    $port = Get-HomeaiBackendPort
    if (Test-HomeaiTcpPort -Port $port) { return }

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $tick = 0
    while ((Get-Date) -lt $deadline) {
        if (Test-HomeaiTcpPort -Port $port) {
            Write-Host ("[OK] 后端已监听 127.0.0.1:{0}" -f $port)
            return
        }
        if ($p -and $p.HasExited) {
            Start-Sleep -Milliseconds 400
            Write-HomeaiBackendLogTail
            throw ("后端进程已退出（PID {0}，exit={1}）。请查看 {2}" -f $p.Id, $p.ExitCode, (Get-HomeaiBackendLogPath))
        }
        Start-Sleep -Seconds 3
        $tick++
        if (($tick % 5) -eq 0) {
            Write-Host ("[等待] 后端 127.0.0.1:{0}（进程仍在启动，约 {1}s）" -f $port, ($tick * 3))
        }
    }
    Write-HomeaiBackendLogTail
    throw ("后端未在 {0} 秒内监听端口。请查看 {1}" -f $TimeoutSec, (Get-HomeaiBackendLogPath))
}

function Invoke-HomeaiFrpScript {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [object[]]$ArgumentList = @()
    )
    $path = Join-Path $script:FrpDir $Name
    if (-not (Test-Path -LiteralPath $path)) {
        throw "找不到 $path 。请先完成 FRP 本机部署（JeecgBoot/deploy/frp/setup-local.ps1）。"
    }
    & $path @ArgumentList
}

function Stop-HomeaiFrontend {
    $stopPs1 = Join-Path $script:FrpDir 'stop-local.ps1'
    if (Test-Path -LiteralPath $stopPs1) {
        Write-Host '[前端] 停止本机 Nginx / frpc'
        & $stopPs1
    } else {
        Write-Host '[前端] 未找到 stop-local.ps1，尝试按进程名停止 nginx / frpc'
        Get-Process -Name 'nginx', 'frpc' -ErrorAction SilentlyContinue | Stop-Process -Force
    }
}

function Start-HomeaiFrontendTunnel {
    $nginxExe = Join-Path (Get-HomeaiHomeRoot) 'nginx\nginx.exe'
    $startPs1 = Join-Path $script:FrpDir 'start-local.ps1'
    if (-not (Test-Path -LiteralPath $startPs1)) {
        Write-Host '[前端] 未找到 start-local.ps1，跳过 Nginx/frpc'
        return
    }
    if (-not (Test-Path -LiteralPath $nginxExe)) {
        Write-Host '[前端] 尚未安装本机 Nginx。请先运行 setup-local.ps1，或在发布时不要跳过管理端。'
        return
    }
    Write-Host '[前端] 启动本机 Nginx / frpc'
    & $startPs1
}
