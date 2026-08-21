/**
 * HomeAI 本地通知兜底方案（替代 EMAS 推送）
 *
 * 方案说明：
 * - App 存活/后台时，在「启动/登录后」从后端拉取今日待提醒计划，
 *   用 plus.push.createMessage 在提醒时间点弹本地通知。
 * - 小程序通道（requestSubscribeMessage 微信订阅消息）保持不变，本文件在小程序端为空实现。
 *
 * ⚠️ 方案局限：本地通知兜底——仅 App 进程存活/后台时生效，App 被杀后无法送达；
 * 后续接入厂商推送（如 UniPush/个推）可解决。
 */

import { planApi } from '../api/plan'
import { isStandaloneApp } from '../platform/runtime'
import { localDateStr } from './date'
// #ifdef H5
import {
  initCapacitorLocalNotify,
  scheduleCapacitorLearnGoal,
  scheduleCapacitorPlanNotifies,
} from '../platform/notify'
// #endif

/**
 * 5+ 运行时：未打入 Push 原生模块时，访问 plus.push 会直接抛「缺少push模块」，
 * typeof plus.push 同样会抛，必须 try-catch。
 */
function getPush(): PlusPush | null {
  // #ifdef APP-PLUS
  try {
    const push = plus.push
    if (!push || typeof push.createMessage !== 'function') {
      return null
    }
    return push
  } catch {
    console.warn('[push] 当前安装包未包含 Push 模块，本地通知已跳过')
    return null
  }
  // #endif
  return null
}

/** 待提醒计划项（用于创建本地通知） */
export interface PlanRemindItem {
  /** 计划实例 ID（写入通知 payload，点击通知可定位到具体计划） */
  planInstanceId: string
  /** 计划标题（通知标题） */
  title: string
  /** 提醒文案（通知内容） */
  content: string
  /** 计划开始时间 HH:mm（缺省时无法计算提醒时间，跳过） */
  startTime?: string
  /** 提前提醒分钟数（缺省时无法计算提醒时间，跳过） */
  remindMinutes?: number
  /** 是否已提醒（1=已提醒，跳过） */
  reminded?: number
  /** 实例状态（非 pending 跳过） */
  status?: string
}

/**
 * 为待提醒计划创建延迟本地通知（APP 端）
 * - 已提醒过（reminded=1）、非待完成（status != pending）、
 *   缺少 startTime/remindMinutes、提醒时间已过去 的项一律跳过
 * - delay 单位为秒：计划提醒时间 = 计划开始时间 - remindMinutes（分钟）
 * - 小程序端为空实现（走既有微信订阅消息通道）
 * @param items 今日待提醒计划列表
 */
export function schedulePlanLocalNotify(items: PlanRemindItem[]): void {
  // #ifdef APP-PLUS
  const push = getPush()
  if (!push) return
  const now = new Date()
  for (const item of items) {
    if (item.status && item.status !== 'pending') continue
    if (item.reminded === 1) continue
    if (!item.startTime || item.remindMinutes == null || item.remindMinutes < 0) continue
    const parts = item.startTime.split(':')
    const hour = Number(parts[0])
    const minute = Number(parts[1])
    if (Number.isNaN(hour) || Number.isNaN(minute)) continue
    const remindAt = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hour, minute)
    remindAt.setMinutes(remindAt.getMinutes() - item.remindMinutes)
    const delaySec = Math.round((remindAt.getTime() - now.getTime()) / 1000)
    if (delaySec <= 0) continue // 提醒时间已过去
    const payload = JSON.stringify({ planInstanceId: item.planInstanceId })
    push.createMessage(item.content, payload, {
      title: item.title,
      delay: delaySec,
    })
  }
  // #endif
  // #ifdef H5
  void scheduleCapacitorPlanNotifies(items)
  // #endif
}

/** Android 13+ 本地通知需运行时申请 POST_NOTIFICATIONS */
function requestPostNotificationsPermission(): void {
  try {
    const version = String(plus.os.version || '')
    const major = parseInt(version.split('.')[0], 10)
    if (Number.isNaN(major) || major < 13) return
    plus.android.requestPermissions(
      ['android.permission.POST_NOTIFICATIONS'],
      () => undefined,
      () => undefined,
    )
  } catch {
    // 旧 runtime 无 requestPermissions 时忽略
  }
}

/**
 * 初始化本地通知（APP 端）：注册通知点击监听，点击跳转计划页
 * - 小程序端为空实现
 */
export function initLocalNotify(): void {
  // #ifdef APP-PLUS
  const push = getPush()
  if (!push) return
  requestPostNotificationsPermission()
  push.addEventListener('click', (result) => {
    // 点击本地通知：跳转计划页（payload 携带 planInstanceId，可扩展跳转计划详情）
    const msg = result as unknown as PlusPushPushMessage
    const payload = msg?.payload
    if (typeof payload === 'string') {
      try {
        const data = JSON.parse(payload) as { planInstanceId?: string }
        if (data?.planInstanceId) {
          console.log('[push] 点击计划提醒通知，planInstanceId=', data.planInstanceId)
        }
      } catch {
        // payload 非 JSON，忽略
      }
    }
    uni.navigateTo({ url: '/pages-homeai-more/plan/index' })
  })
  // #endif
  // #ifdef H5
  void initCapacitorLocalNotify()
  // #endif
}

/**
 * 拉取今日待提醒计划并调度本地通知（登录成功后调用）
 *
 * 调用建议位置：stores/user.ts 的 login()/setAuth() 登录成功写入 token 之后调用；
 * 或 App 启动且已登录时调用。
 *
 * ⚠️ 依赖后端接口：GET /homeai/plan/date/{date}（planApi.byDate）。
 * 实例上的 startTime / remindMinutes 由 fillMasterInfo 从主计划填入。
 * APP 创建计划时须带开始时间，全天计划不调度本地提醒。
 */
export async function scheduleTodayPlanReminds(): Promise<void> {
  if (!isStandaloneApp()) return
  try {
    const instances = (await planApi.byDate(localDateStr())) || []
    const items: PlanRemindItem[] = instances.map((inst) => ({
      planInstanceId: inst.id,
      title: inst.title || '计划提醒',
      content: inst.title ? `计划「${inst.title}」即将开始` : '您有一条计划待完成',
      startTime: inst.startTime,
      remindMinutes: inst.remindMinutes,
      reminded: inst.reminded,
      status: inst.status,
    }))
    schedulePlanLocalNotify(items)
  } catch (e) {
    console.error('[push] 今日计划本地提醒调度失败', e)
  }
}

let learnGoalScheduledFor = ''

/**
 * 今晚 20:00 学习目标未达标时发本地通知（APP 端）。
 * 同一天只调度一次；已达标或已过 20:00 则跳过。
 */
export function scheduleLearnGoalRemind(goal?: {
  reached?: boolean
  goalMinutes?: number
  todayMinutes?: number
}): void {
  // #ifdef APP-PLUS
  const today = localDateStr()
  if (!goal || goal.reached) {
    learnGoalScheduledFor = today
    return
  }
  if (learnGoalScheduledFor === today) return
  const now = new Date()
  const at = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 20, 0, 0)
  const delaySec = Math.round((at.getTime() - now.getTime()) / 1000)
  if (delaySec <= 0) return
  const push = getPush()
  if (!push) {
    learnGoalScheduledFor = today
    return
  }
  const remain = Math.max(0, (goal.goalMinutes || 30) - (goal.todayMinutes || 0))
  push.createMessage(`今日学习还差 ${remain} 分钟`, 'learn-goal', {
    title: '学习目标提醒',
    delay: delaySec,
  })
  learnGoalScheduledFor = today
  // #endif
  // #ifdef H5
  const todayH5 = localDateStr()
  if (!goal || goal.reached) {
    learnGoalScheduledFor = todayH5
    return
  }
  if (learnGoalScheduledFor === todayH5) return
  const nowH5 = new Date()
  const atH5 = new Date(nowH5.getFullYear(), nowH5.getMonth(), nowH5.getDate(), 20, 0, 0)
  const delayH5 = Math.round((atH5.getTime() - nowH5.getTime()) / 1000)
  if (delayH5 <= 0) return
  const remainH5 = Math.max(0, (goal.goalMinutes || 30) - (goal.todayMinutes || 0))
  void scheduleCapacitorLearnGoal(delayH5, remainH5)
  learnGoalScheduledFor = todayH5
  // #endif
}
