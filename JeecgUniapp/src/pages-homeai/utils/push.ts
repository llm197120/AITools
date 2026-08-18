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
import { localDateStr } from './date'

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
    plus.push.createMessage(item.content, payload, {
      title: item.title,
      delay: delaySec,
    })
  }
  // #endif
  // #ifndef APP-PLUS
  // 小程序走微信订阅消息（requestSubscribeMessage），本地通知为空实现
  // #endif
}

/**
 * 初始化本地通知（APP 端）：注册通知点击监听，点击跳转计划页
 * - 小程序端为空实现
 */
export function initLocalNotify(): void {
  // #ifdef APP-PLUS
  plus.push.addEventListener('click', (result) => {
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
  // #ifndef APP-PLUS
  // 小程序走微信订阅消息，无需本地通知监听
  // #endif
}

/**
 * 拉取今日待提醒计划并调度本地通知（登录成功后调用）
 *
 * 调用建议位置：stores/user.ts 的 login()/setAuth() 登录成功写入 token 之后调用；
 * 或 App 启动且已登录时调用。
 *
 * ⚠️ 依赖后端接口：GET /homeai/plan/date/{date}（planApi.byDate）。
 * 当前该接口返回的计划实例缺少 startTime / remindMinutes 字段（二者在 PlanMaster 上，
 * fillMasterInfo 未填充到实例），导致本地通知暂时无法计算提醒时间而被跳过；
 * 需后端在 byDate 返回的实例上补充 startTime、remindMinutes 字段后本功能即生效。
 */
export async function scheduleTodayPlanReminds(): Promise<void> {
  // #ifdef APP-PLUS
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
  // #endif
  // #ifndef APP-PLUS
  // 小程序走微信订阅消息，无需本地通知调度
  // #endif
}
