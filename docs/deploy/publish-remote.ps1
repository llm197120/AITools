# -*- coding: utf-8 -*-
# 从开发机发布到服务主机：git push → 远程 pull → publish-all（默认后端+管理端，不出 APK）
#   .\publish-remote.ps1
#   .\publish-remote.ps1 -Backend
#   .\publish-remote.ps1 -Frontend
#   .\publish-remote.ps1 -SkipPush
#   .\publish-remote.ps1 -NoCheck
param(
    [switch]$Backend,
    [switch]$Frontend,
    [switch]$SkipPush,
    [switch]$SkipPull,
    [switch]$NoCheck
)
$ErrorActionPreference = 'Stop'
$argsList = @('-Action', 'Publish')
if ($Backend) { $argsList += '-Backend' }
if ($Frontend) { $argsList += '-Frontend' }
if ($SkipPush) { $argsList += '-SkipPush' }
if ($SkipPull) { $argsList += '-SkipPull' }
if ($NoCheck) { $argsList += '-NoCheck' }
& "$PSScriptRoot\remote-host.ps1" @argsList
