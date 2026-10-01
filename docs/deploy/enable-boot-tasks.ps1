# -*- coding: utf-8 -*-
# 把 HomeAI 三个计划任务从「登录时触发」改为「开机触发 + 不管用户是否登录(S4U)」，
# 实现无人登录也能自动拉起后端 / Nginx / FRP 隧道。无需输入密码。
#
# 与 enable-autologon.ps1 二选一：
#   - 本脚本：不保持登录、不存明文密码，靠 S4U 无人会话运行（推荐）。
#   - enable-autologon.ps1：开启自动登录，重启后照旧走「登录触发」。
#
# 【必须以管理员运行】。前置：本机 JAVA_HOME 与 Maven 已在【系统级】环境变量，
# 这样 S4U 会话里 start-backend.ps1 才能找到 java / mvn。
#
# 用法：
#   .\enable-boot-tasks.ps1                  # 改为开机触发（默认延迟 2 分钟等 MySQL/Redis 就绪）
#   .\enable-boot-tasks.ps1 -DelayMinutes 3
#   .\enable-boot-tasks.ps1 -Restore         # 还原为「登录时触发」
param(
    [int]$DelayMinutes = 2,
    [string]$UserId = "$env:USERDOMAIN\$env:USERNAME",
    [switch]$Restore
)

$ErrorActionPreference = 'Stop'

$id = [Security.Principal.WindowsIdentity]::GetCurrent()
$isAdmin = (New-Object Security.Principal.WindowsPrincipal $id).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) { throw '请以【管理员】身份运行（右键 PowerShell → 以管理员身份运行）。' }

$tasks = 'HomeAI-Backend', 'HomeAI-FRP', 'HomeAI-FRP-Admin'

if ($Restore) {
    foreach ($t in $tasks) {
        if (-not (Get-ScheduledTask -TaskName $t -ErrorAction SilentlyContinue)) { continue }
        $tr = New-ScheduledTaskTrigger -AtLogOn -User $UserId
        Set-ScheduledTask -TaskName $t -Trigger $tr | Out-Null
        Write-Host ("[计划任务] {0} -> 登录时触发" -f $t)
    }
    Write-Host '[计划任务] 已还原为「登录时触发」。'
    return
}

if ($DelayMinutes -lt 0) { $DelayMinutes = 0 }
$delay = 'PT{0}M' -f $DelayMinutes

Write-Host ("目标账户：{0}（S4U 无人会话）" -f $UserId)
foreach ($t in $tasks) {
    if (-not (Get-ScheduledTask -TaskName $t -ErrorAction SilentlyContinue)) {
        Write-Host ("[跳过] 未找到任务 {0}" -f $t)
        continue
    }
    $trigger = New-ScheduledTaskTrigger -AtStartup
    $trigger.Delay = $delay
    $principal = New-ScheduledTaskPrincipal -UserId $UserId -LogonType S4U -RunLevel Limited
    Set-ScheduledTask -TaskName $t -Trigger $trigger -Principal $principal | Out-Null
    Write-Host ("[计划任务] {0} -> 开机触发（延迟 {1} 分钟，S4U 无人会话）" -f $t, $DelayMinutes)
}

Write-Host ''
Write-Host '完成。重启后无需登录即会自动拉起服务。'
Write-Host '验证：重启后【不要登录】，等 3~5 分钟后运行 docs\deploy\check-host-local.ps1。'
Write-Host '还原：.\enable-boot-tasks.ps1 -Restore'
