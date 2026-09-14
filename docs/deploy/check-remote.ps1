# -*- coding: utf-8 -*-
# 从开发机检查服务主机（局域网端口 + 公网入口 + SSH 本机检查）
param(
    [string]$Action = 'Check'
)
$ErrorActionPreference = 'Stop'
& "$PSScriptRoot\remote-host.ps1" -Action Check
