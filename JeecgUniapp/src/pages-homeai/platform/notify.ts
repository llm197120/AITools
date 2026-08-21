/**
 * Capacitor 本地通知（现行发版 H5 壳）。已装的旧云打包壳仍走 plus.push。
 */
import { isCapacitorNative } from './runtime'

interface PlanRemindItem {
  planInstanceId: string
  title: string
  content: string
  startTime?: string
  remindMinutes?: number
  reminded?: number
  status?: string
}

function notifyId(key: string): number {
  let h = 0
  for (let i = 0; i < key.length; i++) h = (Math.imul(h, 31) + key.charCodeAt(i)) | 0
  const id = Math.abs(h) % 2147483646
  return id === 0 ? 1 : id
}

async function loadPlugin() {
  if (!isCapacitorNative()) return null
  try {
    const m = await import('@capacitor/local-notifications')
    return m.LocalNotifications
  } catch {
    console.warn('[push] Capacitor LocalNotifications 不可用')
    return null
  }
}

export async function initCapacitorLocalNotify(): Promise<void> {
  const LocalNotifications = await loadPlugin()
  if (!LocalNotifications) return
  await LocalNotifications.requestPermissions()
  await LocalNotifications.addListener('localNotificationActionPerformed', (event) => {
    const extra = event.notification.extra as { planInstanceId?: string } | undefined
    if (extra?.planInstanceId) {
      console.log('[push] 点击计划提醒通知，planInstanceId=', extra.planInstanceId)
    }
    uni.navigateTo({ url: '/pages-homeai-more/plan/index' })
  })
}

export async function scheduleCapacitorPlanNotifies(items: PlanRemindItem[]): Promise<void> {
  const LocalNotifications = await loadPlugin()
  if (!LocalNotifications) return
  const now = new Date()
  const notifications: Array<{
    id: number
    title: string
    body: string
    schedule: { at: Date; allowWhileIdle: boolean }
    extra: { planInstanceId: string }
  }> = []
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
    if (remindAt.getTime() <= now.getTime()) continue
    notifications.push({
      id: notifyId(item.planInstanceId),
      title: item.title,
      body: item.content,
      schedule: { at: remindAt, allowWhileIdle: true },
      extra: { planInstanceId: item.planInstanceId },
    })
  }
  if (!notifications.length) return
  await LocalNotifications.schedule({ notifications })
}

export async function scheduleCapacitorLearnGoal(delaySec: number, remain: number): Promise<void> {
  const LocalNotifications = await loadPlugin()
  if (!LocalNotifications || delaySec <= 0) return
  await LocalNotifications.schedule({
    notifications: [
      {
        id: 900001,
        title: '学习目标提醒',
        body: `今日学习还差 ${remain} 分钟`,
        schedule: { at: new Date(Date.now() + delaySec * 1000), allowWhileIdle: true },
      },
    ],
  })
}
