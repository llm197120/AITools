/**
 * 服务器连接状态管理：ping 探测 + 指数退避 + 事件广播。
 * 探测地址复用 env.ts 的 pingBase（/sys/randomImage/homeai-probe）。
 */
import { getServerBaseUrl } from '../api/request'

export type ConnState = 'online' | 'offline' | 'unknown'

const RETRY_DELAYS = [5000, 15000, 60000, 300000, 600000]

let state: ConnState = 'unknown'
let lastCheckAt = 0
let timer: ReturnType<typeof setTimeout> | null = null
let inFlight = false
let failCount = 0
const listeners = new Set<(s: ConnState) => void>()

export function getConnState(): ConnState {
  return state
}

export function getLastCheckAt(): number {
  return lastCheckAt
}

export function onConnChange(cb: (s: ConnState) => void): () => void {
  listeners.add(cb)
  return () => {
    listeners.delete(cb)
  }
}

function setState(next: ConnState) {
  if (next === state) return
  state = next
  lastCheckAt = Date.now()
  listeners.forEach((cb) => {
    try {
      cb(next)
    } catch {
      /* ignore */
    }
  })
}

function ping(): Promise<boolean> {
  return new Promise((resolve) => {
    uni.request({
      url: `${getServerBaseUrl()}/sys/randomImage/homeai-probe`,
      method: 'GET',
      timeout: 5000,
      success: () => resolve(true),
      fail: () => resolve(false),
    })
  })
}

/** 立即探测一次并更新状态 */
export async function checkNow(): Promise<ConnState> {
  if (inFlight) return state
  inFlight = true
  try {
    const ok = await ping()
    setState(ok ? 'online' : 'offline')
    return state
  } finally {
    inFlight = false
  }
}

function scheduleNext() {
  if (timer) clearTimeout(timer)
  const delay = RETRY_DELAYS[Math.min(failCount, RETRY_DELAYS.length - 1)]
  timer = setTimeout(() => {
    checkNow().then((s) => {
      failCount = s === 'online' ? 0 : failCount + 1
      scheduleNext()
    })
  }, delay)
}

/** App 启动时初始化：立即探测 + 启动退避重试循环 */
export function initConnectionMonitor() {
  if (timer) return
  checkNow()
  scheduleNext()
}

/** 页面回到前台 / 手动重探 */
export function pokeConnection() {
  checkNow()
}
