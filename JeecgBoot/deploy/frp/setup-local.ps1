# -*- coding: utf-8 -*-
# 本机一键准备：frpc + Nginx(:8088) + 配置 + 可选开机启动
# 用法：
#   cd JeecgBoot\deploy\frp
#   .\setup-local.ps1
#   .\setup-local.ps1 -BuildAdmin          # 同时构建管理端（较慢）
#   .\setup-local.ps1 -PatchAppEnv         # 把 UniApp 生产 API 改成公网地址
#   .\setup-local.ps1 -RegisterStartup     # 登录时自动拉起 nginx/frpc
#   .\setup-local.ps1 -SkipStart           # 只装不启动
param(
    [switch]$BuildAdmin,
    [switch]$PatchAppEnv,
    [switch]$RegisterStartup,
    [switch]$SkipStart
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

$cfg = Get-HomeaiFrpConfig
$sec = Initialize-HomeaiFrpSecrets
$homeRoot = $cfg['HOME_ROOT']
$nginxPort = [int]$cfg['HOME_NGINX_PORT']
$backendPort = [int]$cfg['BACKEND_PORT']
$frpVer = $cfg['FRP_VERSION']
$nginxVer = $cfg['NGINX_WINDOWS_VERSION']
$serverIp = $cfg['SERVER_IP']

$frpHome = Join-Path $homeRoot 'frp'
$nginxHome = Join-Path $homeRoot 'nginx'
$adminHome = Join-Path $homeRoot 'admin'
$tmp = Join-Path $homeRoot 'tmp'
New-Item -ItemType Directory -Force -Path $frpHome, $nginxHome, $adminHome, $tmp | Out-Null

function Install-Frpc {
    $exe = Join-Path $frpHome 'frpc.exe'
    $mark = Join-Path $frpHome "version-$frpVer"
    if ((Test-Path -LiteralPath $exe) -and (Test-Path -LiteralPath $mark)) {
        Write-Host "[frpc] 已存在 $frpVer"
        return
    }
    Write-Host "[frpc] 下载 $frpVer ..."
    $zipName = "frp_${frpVer}_windows_amd64.zip"
    $gh = "https://github.com/fatedier/frp/releases/download/v$frpVer/$zipName"
    $zip = Join-Path $tmp $zipName
    Get-DownloadFile -Destination $zip -Urls @(
        $gh,
        "https://ghfast.top/$gh",
        "https://ghproxy.net/$gh"
    )
    $extract = Join-Path $tmp "frp-$frpVer"
    if (Test-Path -LiteralPath $extract) { Remove-Item -Recurse -Force $extract }
    Expand-Archive -LiteralPath $zip -DestinationPath $extract -Force
    $inner = Get-ChildItem $extract -Directory | Select-Object -First 1
    Copy-Item (Join-Path $inner.FullName 'frpc.exe') $exe -Force
    Unblock-File -LiteralPath $exe -ErrorAction SilentlyContinue
    New-Item -ItemType File -Force -Path $mark | Out-Null
}

function Install-NginxWin {
    $exe = Join-Path $nginxHome 'nginx.exe'
    if (Test-Path -LiteralPath $exe) {
        Write-Host "[nginx] 已存在 $nginxHome"
        return
    }
    Write-Host "[nginx] 下载 $nginxVer ..."
    $zipName = "nginx-$nginxVer.zip"
    $zip = Join-Path $tmp $zipName
    Get-DownloadFile -Destination $zip -Urls @(
        "https://nginx.org/download/$zipName"
    )
    $extract = Join-Path $tmp "nginx-$nginxVer"
    if (Test-Path -LiteralPath $extract) { Remove-Item -Recurse -Force $extract }
    Expand-Archive -LiteralPath $zip -DestinationPath $extract -Force
    $inner = Join-Path $extract "nginx-$nginxVer"
    if (-not (Test-Path -LiteralPath $inner)) {
        $inner = (Get-ChildItem $extract -Directory | Select-Object -First 1).FullName
    }
    Copy-Item -Path (Join-Path $inner '*') -Destination $nginxHome -Recurse -Force
    New-Item -ItemType Directory -Force -Path (Join-Path $nginxHome 'logs'), (Join-Path $nginxHome 'temp') | Out-Null
}

function Write-FrpcToml {
    $toml = Join-Path $frpHome 'frpc.toml'
    $text = @"
serverAddr = "$serverIp"
serverPort = $($cfg['FRP_BIND_PORT'])

auth.method = "token"
auth.token = "$($sec['FRP_TOKEN'])"

log.to = "./frpc.log"
log.level = "info"
log.maxDays = 7

transport.heartbeatInterval = 30
transport.heartbeatTimeout = 90

[[proxies]]
name = "homeai-web"
type = "tcp"
localIP = "127.0.0.1"
localPort = $nginxPort
remotePort = $($cfg['FRP_REMOTE_PORT'])
"@
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($toml, $text, $utf8)
    Write-Host "[frpc] 已写 $toml"
}

function Write-NginxConf {
    $conf = Join-Path $nginxHome 'conf\nginx.conf'
    $adminRoot = ($adminHome -replace '\\', '/')
    $text = @"
worker_processes  1;
error_log  logs/error.log;
pid        logs/nginx.pid;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;
    client_max_body_size 50m;

    server {
        listen 127.0.0.1:$nginxPort;
        server_name _;
        root $adminRoot;
        index index.html;

        location /jeecg-boot/ {
            proxy_pass http://127.0.0.1:$backendPort/jeecg-boot/;
            proxy_http_version 1.1;
            proxy_set_header Host `$host;
            proxy_set_header X-Real-IP `$remote_addr;
            proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto `$scheme;
            proxy_set_header Upgrade `$http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_connect_timeout 10s;
            proxy_send_timeout 300s;
            proxy_read_timeout 300s;
            proxy_buffering off;
        }

        location / {
            try_files `$uri `$uri/ /index.html;
        }
    }
}
"@
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($conf, $text, $utf8)
    Write-Host "[nginx] 已写 $conf"
}

function Write-AppConfigJsFallback {
    param([string]$DistDir)
    # 构建后 postBuild 才会生成此文件；缺了管理端会一直停在 loading。
    $name = '__PRODUCTION__JEECGBOOT_PRO__CONF__'
    $obj = '{"VITE_GLOB_APP_TITLE":"JeecgBoot 企业级低代码平台","VITE_GLOB_APP_SHORT_NAME":"JeecgBoot_Pro","VITE_GLOB_APP_CAS_BASE_URL":"http://cas.test.com:8443/cas","VITE_GLOB_APP_OPEN_SSO":"false","VITE_GLOB_APP_OPEN_QIANKUN":"false","VITE_GLOB_ONLINE_VIEW_URL":"http://fileview.jeecg.com/onlinePreview","VITE_GLOB_API_URL":"/jeecg-boot","VITE_GLOB_DOMAIN_URL":"/jeecg-boot","VITE_GLOB_API_URL_PREFIX":"","VITE_GLOB_ONLINE_DOCUMENT_VERSION":"wps","VITE_GLOB_ICONIFY_USE_TYPE":"local"}'
    $raw = "window.$name=$obj;"
    $raw += "Object.freeze(window.$name);Object.defineProperty(window,`"$name`",{configurable:false,writable:false,});"
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText((Join-Path $DistDir '_app.config.js'), $raw, $utf8)
}

function Patch-AppConfigJs {
    param([string]$DistDir)
    $cfgJs = Join-Path $DistDir '_app.config.js'
    if (-not (Test-Path -LiteralPath $cfgJs)) {
        Write-Host "[管理端] 缺少 _app.config.js，已按同域 /jeecg-boot 生成"
        Write-AppConfigJsFallback -DistDir $DistDir
        return
    }
    $raw = [System.IO.File]::ReadAllText($cfgJs)
    $raw = $raw -replace '"VITE_GLOB_API_URL":"[^"]+"', '"VITE_GLOB_API_URL":"/jeecg-boot"'
    $raw = $raw -replace '"VITE_GLOB_DOMAIN_URL":"[^"]+"', '"VITE_GLOB_DOMAIN_URL":"/jeecg-boot"'
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($cfgJs, $raw, $utf8)
    Write-Host '[管理端] 已将 _app.config.js 接口改为同域 /jeecg-boot'
}

function Publish-Admin {
    $repo = Get-HomeaiRepoRoot
    $vue = Join-Path $repo 'JeecgBoot\jeecgboot-vue3'
    $dist = Join-Path $vue 'dist'
    if ($BuildAdmin) {
        if (-not (Get-Command pnpm -ErrorAction SilentlyContinue)) {
            throw '未找到 pnpm，无法构建管理端。可先不带 -BuildAdmin，App 仍可用。'
        }
        Write-Host '[管理端] pnpm run build:docker:prod （可能需要几分钟）...'
        Push-Location $vue
        try {
            if (-not (Test-Path 'node_modules')) {
                pnpm install --frozen-lockfile
            }
            pnpm run build:docker:prod
            if ($LASTEXITCODE -ne 0) { throw "管理端构建失败，exit=$LASTEXITCODE" }
        } finally {
            Pop-Location
        }
    }
    if (-not (Test-Path -LiteralPath $dist)) {
        Write-Host '[管理端] 尚无 dist。App 走 /jeecg-boot 仍可用；浏览器管理端需稍后加 -BuildAdmin。'
        $placeholder = Join-Path $adminHome 'index.html'
        if (-not (Test-Path -LiteralPath $placeholder)) {
            $html = "<!DOCTYPE html><meta charset='UTF-8'><title>HomeAI</title><p>管理端尚未构建。API 已反代。本机构建：setup-local.ps1 -BuildAdmin</p>"
            $utf8 = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($placeholder, $html, $utf8)
        }
        return
    }
    Write-Host "[管理端] 复制 dist → $adminHome"
    & robocopy $dist $adminHome /MIR /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "复制管理端失败，robocopy exit=$LASTEXITCODE" }
    Patch-AppConfigJs -DistDir $adminHome
}

function Patch-UniAppEnv {
    if (-not $PatchAppEnv) { return }
    $repo = Get-HomeaiRepoRoot
    $envFile = Join-Path $repo 'JeecgUniapp\env\.env.production'
    if (-not (Test-Path -LiteralPath $envFile)) { throw "找不到 $envFile" }
    $api = "http://$serverIp/jeecg-boot"
    $raw = [System.IO.File]::ReadAllText($envFile)
    $raw = $raw -replace "VITE_SERVER_BASEURL\s*=\s*'[^']*'", "VITE_SERVER_BASEURL = '$api'"
    $raw = $raw -replace "VITE_UPLOAD_BASEURL\s*=\s*'[^']*'", "VITE_UPLOAD_BASEURL = '$api'"
    if ($raw -notmatch 'VITE_SERVER_BASEURL_APP') {
        $raw += "`r`nVITE_SERVER_BASEURL_APP = '$api'`r`nVITE_UPLOAD_BASEURL_APP = '$api'`r`n"
    } else {
        $raw = $raw -replace "VITE_SERVER_BASEURL_APP\s*=\s*'[^']*'", "VITE_SERVER_BASEURL_APP = '$api'"
        $raw = $raw -replace "VITE_UPLOAD_BASEURL_APP\s*=\s*'[^']*'", "VITE_UPLOAD_BASEURL_APP = '$api'"
    }
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($envFile, $raw, $utf8)
    Write-Host "[App] 已改 $envFile → $api （需重新打 APK 才生效）"
}

function Register-StartupTasks {
    if (-not $RegisterStartup) { return }
    $startPs1 = Join-Path $PSScriptRoot 'start-local.ps1'
    $tr = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$startPs1`""
    schtasks /Create /TN 'HomeAI-FRP' /SC ONLOGON /RL LIMITED /TR $tr /F | Out-Null
    Write-Host '[开机] 已注册计划任务 HomeAI-FRP（用户登录时启动）'
}

Write-Host "=== HomeAI 本机 FRP 部署 ==="
Write-Host "服务器: $serverIp"
Install-Frpc
Install-NginxWin
Write-FrpcToml
Write-NginxConf
Publish-Admin
Patch-UniAppEnv
Register-StartupTasks

if (-not $SkipStart) {
    & (Join-Path $PSScriptRoot 'stop-local.ps1')
    & (Join-Path $PSScriptRoot 'start-local.ps1')
}

Write-Host ''
Write-Host '========== 本机已就绪 =========='
Write-Host "本地入口:  http://127.0.0.1:$nginxPort/"
Write-Host "本地 API:  http://127.0.0.1:$nginxPort/jeecg-boot/"
Write-Host "公网入口:  http://$serverIp/   （需 frpc 已连上且后端 :$backendPort 在跑）"
Write-Host "下载页:    http://$serverIp/app/"
if (-not (Test-TcpPortOpen -Port $backendPort)) {
    Write-Host "[注意] 127.0.0.1:$backendPort 未监听。请先启动 JeecgBoot 后端，否则 App/管理端会失败。"
} else {
    Write-Host "[后端] 127.0.0.1:$backendPort 已在监听。"
}
