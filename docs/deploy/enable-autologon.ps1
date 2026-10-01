# -*- coding: utf-8 -*-
# 开启 Windows 自动登录：服务主机重启后无需人工登录，即可触发 HomeAI 计划任务
# （HomeAI-Backend / HomeAI-FRP / HomeAI-FRP-Admin 均为「登录时触发」）。
#
# 必须以【管理员】运行。脚本交互读取密码，只写入本机注册表
#   HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon
# DefaultPassword 为明文存储（这是 Windows 自动登录的机制），仅建议家用服务主机使用。
#
# 用法：
#   .\enable-autologon.ps1                 # 默认账户 = 当前用户
#   .\enable-autologon.ps1 -UserName 57089
#   .\enable-autologon.ps1 -Disable        # 关闭自动登录并清除已存密码
param(
    [string]$UserName = $env:USERNAME,
    [string]$DomainName = '.',
    [switch]$Disable
)

$ErrorActionPreference = 'Stop'

$id = [Security.Principal.WindowsIdentity]::GetCurrent()
$isAdmin = (New-Object Security.Principal.WindowsPrincipal $id).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) { throw '请以【管理员】身份运行（右键 PowerShell → 以管理员身份运行）。' }

$wl = 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon'

if ($Disable) {
    Set-ItemProperty -Path $wl -Name 'AutoAdminLogon' -Value '0'
    Remove-ItemProperty -Path $wl -Name 'DefaultPassword' -ErrorAction SilentlyContinue
    Write-Host '[自动登录] 已关闭，并清除已存密码。'
    return
}

Write-Host ("将开启自动登录：账户 {0}\{1}" -f $DomainName, $UserName)
Write-Host '注意：密码会以明文写入本机注册表 HKLM\...\Winlogon\DefaultPassword。'
$secure = Read-Host -AsSecureString -Prompt '请输入该账户的 Windows 登录密码'
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}
if ([string]::IsNullOrEmpty($plain)) { throw '密码为空，已取消。' }

Set-ItemProperty -Path $wl -Name 'AutoAdminLogon' -Value '1'
Set-ItemProperty -Path $wl -Name 'DefaultUserName' -Value $UserName
Set-ItemProperty -Path $wl -Name 'DefaultDomainName' -Value $DomainName
Set-ItemProperty -Path $wl -Name 'DefaultPassword' -Value $plain
# 清除可能限制自动登录次数的旧值，避免只自动登录一次
Remove-ItemProperty -Path $wl -Name 'AutoLogonCount' -ErrorAction SilentlyContinue

Write-Host '[自动登录] 已开启。下次重启将自动登录上述账户，HomeAI 计划任务随之触发。'
Write-Host '如需关闭：.\enable-autologon.ps1 -Disable'
