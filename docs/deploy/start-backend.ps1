# -*- coding: utf-8 -*-
# 在当前窗口启动 JeecgBoot 后端（阻塞）。日常请用 start-all.ps1 -Backend。
# 必须在 jeecg-system-start 目录执行，不要在父 POM 上 spring-boot:run。
$ErrorActionPreference = 'Stop'
try { $Host.UI.RawUI.WindowTitle = 'HomeAI Backend' } catch { }
. "$PSScriptRoot\common.ps1"
Ensure-HomeaiJavaHome
$mvn = Get-HomeaiMavenCmd
if (-not (Test-Path -LiteralPath (Join-Path $script:StartDir 'pom.xml'))) {
    throw "找不到启动模块：$($script:StartDir)"
}
Set-Location $script:StartDir
Write-Host ("JAVA_HOME = {0}" -f $env:JAVA_HOME)
Write-Host ("工作目录  = {0}" -f $script:StartDir)
& $mvn @(
    'spring-boot:run',
    '-DskipTests',
    '-Dspring-boot.run.profiles=dev'
)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
