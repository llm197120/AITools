# -*- coding: utf-8 -*-
# 打印 release 证书 SHA1/SHA256（密码从 android-pack.local.json 读取，不写进命令行）
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\android-offline\show-cert.ps1
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
$cfg = Get-PackConfig
if (-not (Test-Path -LiteralPath $cfg.keystorePath)) { throw "keystore 不存在：$($cfg.keystorePath)" }
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = 'keytool'
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.UseShellExecute = $false
$psi.Arguments = "-list -v -keystore `"$($cfg.keystorePath)`" -alias `"$($cfg.keystoreAlias)`""
$p = New-Object System.Diagnostics.Process
$p.StartInfo = $psi
[void]$p.Start()
$p.StandardInput.WriteLine([string]$cfg.storePassword)
$p.StandardInput.Close()
$out = $p.StandardOutput.ReadToEnd() + $p.StandardError.ReadToEnd()
$p.WaitForExit()
$out -split "`r?`n" | Where-Object { $_ -match 'Alias name|SHA-?1:|SHA-?256:|Valid from|Owner:' }
