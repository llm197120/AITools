# -*- coding: utf-8 -*-
# 在当前窗口启动 JeecgBoot 后端（阻塞）。日常请用 start-all.ps1 -Backend。
$ErrorActionPreference = 'Stop'
try { $Host.UI.RawUI.WindowTitle = 'HomeAI Backend' } catch { }
. "$PSScriptRoot\common.ps1"
Ensure-HomeaiJavaHome
$mvn = Get-HomeaiMavenCmd
Set-Location $script:BootDir
Write-Host ("JAVA_HOME = {0}" -f $env:JAVA_HOME)
Write-Host ("工作目录  = {0}" -f $script:BootDir)
& $mvn @(
    '-f', 'pom.xml',
    '-pl', $script:StartModule,
    '-am',
    'spring-boot:run',
    '-DskipTests',
    '-Dspring-boot.run.profiles=dev'
)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
