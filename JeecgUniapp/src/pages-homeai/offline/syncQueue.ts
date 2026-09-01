/**
 * 离线写队列与缓慢同步引擎。
 *
 * 规则（默认值，阶段四由后端 /homeai/config/sync 可配置）：
 * - 每批 1 条、批间隔 5 秒（防恢复后高频打服务端）；
 * - 单条 24 小时滑动窗口内最多尝试 20 次，超限当天挂起、次日重置；
 * - 4xx（业务校验失败）不再重试并移入失败区；5xx/网络波动计入尝试次数。
 */
import { getConnState, onConnChange } from './conn'

export interface SyncItem {
  /** clientId：幂等键 */
  id: string
  module: string
  action: string
  payload: any
  createdAt: number
  /** 尝试时间戳（滑动窗口计数用） */
  attempts: number[]
  lastError?: string
}

export type SyncResult = 'ok' | 'retry' | 'fail'

export interface SyncConfig {
  batchSize: number
  intervalMs: number
  maxRetriesPerItemPerDay: number
  imageCacheLimitMB: number
}

const DEFAULT_CONFIG: SyncConfig = {
  batchSize: 1,
  intervalMs: 5000,
  maxRetriesPerItemPerDay: 20,
  imageCacheLimitMB: 4096,
}

const Q_KEY = 'homeai_sync_queue_v1'
const FAILED_KEY = 'homeai_sync_failed_v1'
const MAX_QUEUE = 200

let config: SyncConfig = { ...DEFAULT_CONFIG }

export function setSyncConfig(cfg: Partial<SyncConfig>): void {
  config = { ...config, ...cfg }
}

export function getSyncConfig(): SyncConfig {
  return { ...config }
}

function load(): SyncItem[] {
  try {
    const raw = uni.getStorageSync(Q_KEY)
    return Array.isArray(raw) ? (raw as SyncItem[]) : []
  } catch {
    return []
  }
}

function save(items: SyncItem[]): void {
  try {
    uni.setStorageSync(Q_KEY, items.slice(0, MAX_QUEUE))
  } catch {
    /* 容量满时静默 */
  }
}

/** 入队（同 module+id 去重覆盖）。返回 clientId */
export function enqueue(module: string, action: string, payload: any, id?: string): string {
  const clientId = id || `${module}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  const items = load().filter((i) => !(i.module === module && i.id === clientId))
  items.push({ id: clientId, module, action, payload, createdAt: Date.now(), attempts: [] })
  save(items)
  notifyProgress()
  return clientId
}

export function removeQueued(module: string, id: string): void {
  save(load().filter((i) => !(i.module === module && i.id === id)))
  notifyProgress()
}

export function getPendingCount(): number {
  return load().length
}

export function getPendingItems(): SyncItem[] {
  return load()
}

/** 记录业务失败（4xx），移出队列避免死循环 */
export function markFailed(item: SyncItem, error: string): void {
  item.lastError = error
  try {
    const failed = uni.getStorageSync(FAILED_KEY)
    const list: SyncItem[] = Array.isArray(failed) ? failed : []
    list.push(item)
    uni.setStorageSync(FAILED_KEY, list.slice(-50))
  } catch {
    /* ignore */
  }
  removeQueued(item.module, item.id)
}

function attemptsIn24h(item: SyncItem): number {
  const cutoff = Date.now() - 24 * 3600 * 1000
  return item.attempts.filter((t) => t > cutoff).length
}

/**
 * 模块 × action 的发送器注册表。
 * 阶段三各模块注册：plan/create、plan/update、bill/create、recipe/save、storage/upload 等。
 * 发送器返回成功即视为 ok；抛错时按 HTTP 状态区分 retry/fail。
 */
export type SyncSender = (payload: any, clientId: string) => Promise<unknown>

const senders = new Map<string, Map<string, SyncSender>>()

export function registerSender(module: string, action: string, sender: SyncSender): void {
  if (!senders.has(module)) senders.set(module, new Map())
  senders.get(module)!.set(action, sender)
}

async function dispatch(item: SyncItem): Promise<SyncResult> {
  const sender = senders.get(item.module)?.get(item.action)
  if (!sender) {
    return 'fail'
  }
  try {
    await sender(item.payload, item.id)
    return 'ok'
  } catch (e: any) {
    const status = Number(e?.status || e?.statusCode || 0)
    // 4xx（除 408/429）视为业务失败，不再重试
    if (status >= 400 && status < 500 && status !== 408 && status !== 429) {
      return 'fail'
    }
    return 'retry'
  }
}

// ---------- 同步引擎 ----------

let loopTimer: ReturnType<typeof setInterval> | null = null
let running = false
const progressListeners = new Set<(pending: number, syncing: boolean) => void>()

function notifyProgress() {
  const pending = getPendingCount()
  progressListeners.forEach((cb) => {
    try {
      cb(pending, running)
    } catch {
      /* ignore */
    }
  })
}

export function onSyncProgress(cb: (pending: number, syncing: boolean) => void): () => void {
  progressListeners.add(cb)
  notifyProgress()
  return () => {
    progressListeners.delete(cb)
  }
}

async function pumpOnce(): Promise<void> {
  if (running) return
  if (getConnState() !== 'online') return
  const items = load()
  if (!items.length) return
  running = true
  notifyProgress()
  try {
    const item = items.find((i) => attemptsIn24h(i) < config.maxRetriesPerItemPerDay)
    if (!item) return // 全部当日超限，等待窗口滚动
    item.attempts.push(Date.now())
    const result = await dispatch(item)
    if (result === 'ok') {
      removeQueued(item.module, item.id)
    } else if (result === 'fail') {
      markFailed(item, item.lastError || '业务失败')
    } else {
      item.lastError = '网络波动，等待重试'
      save(load())
    }
  } finally {
    running = false
    notifyProgress()
  }
}

/** App 启动初始化：订阅在线事件 + 定时泵。 */
export function initSyncLoop(): void {
  if (loopTimer) return
  onConnChange(() => {
    if (getConnState() === 'online') pumpOnce()
  })
  loopTimer = setInterval(() => {
    if (getConnState() === 'online') pumpOnce()
  }, config.intervalMs)
  pumpOnce()
}
