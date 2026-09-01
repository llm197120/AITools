/**
 * 离线数据访问层：页面统一通过 readList/readDetail/mutate 读写，不直接感知在线/离线。
 *
 * - 读：在线时后台拉新并做版本比对（无变化返回缓存避免抖动）；离线时回退本地缓存；
 * - 写：在线直发（网络失败自动入队兜底）；离线直接入队（乐观），恢复后由同步引擎缓慢补传。
 */
import { getConnState } from './conn'
import { readCache, writeCache, maxUpdatedAt } from './cache'
import { enqueue } from './syncQueue'

export interface OfflineReadResult<T> {
  data: T
  /** true = 数据来自本地缓存（离线或服务端无变化） */
  fromCache: boolean
  offline: boolean
}

/** 列表读：version 取 max(updateTime)，无变化则沿用缓存 */
export async function readList<T = any>(
  module: string,
  scope: string,
  fetcher: () => Promise<T>,
  recordsOf?: (data: any) => any[],
): Promise<OfflineReadResult<T>> {
  const cached = readCache<T>(module, scope)
  const offline = getConnState() === 'offline'
  if (offline) {
    if (cached) return { data: cached.data, fromCache: true, offline: true }
    return { data: [] as unknown as T, fromCache: true, offline: true }
  }
  try {
    const fresh = await fetcher()
    const version = maxUpdatedAt(recordsOf ? recordsOf(fresh) : (fresh as any)?.records || [])
    if (cached && version && version === cached.version) {
      return { data: cached.data, fromCache: true, offline: false }
    }
    writeCache(module, scope, fresh, version || String(Date.now()))
    return { data: fresh, fromCache: false, offline: false }
  } catch {
    if (cached) return { data: cached.data, fromCache: true, offline: false }
    throw new Error('网络异常，且无本地缓存')
  }
}

/** 详情读：version 取记录 updateTime */
export async function readDetail<T = any>(
  module: string,
  id: string,
  fetcher: () => Promise<T>,
  versionOf?: (data: any) => string,
): Promise<OfflineReadResult<T>> {
  const scope = 'detail:' + id
  const cached = readCache<T>(module, scope)
  if (getConnState() === 'offline') {
    if (cached) return { data: cached.data, fromCache: true, offline: true }
    throw new Error('离线且无本地缓存')
  }
  try {
    const fresh = await fetcher()
    const version = versionOf
      ? versionOf(fresh)
      : String((fresh as any)?.updateTime || (fresh as any)?.createTime || '')
    if (cached && version && version === cached.version) {
      return { data: cached.data, fromCache: true, offline: false }
    }
    writeCache(module, scope, fresh, version || String(Date.now()))
    return { data: fresh, fromCache: false, offline: false }
  } catch {
    if (cached) return { data: cached.data, fromCache: true, offline: false }
    throw new Error('网络异常，且无本地缓存')
  }
}

/** 乐观写：入队后同步更新本地缓存（页面立即生效） */
export function updateLocalCache(module: string, scope: string, data: any, version?: string): void {
  writeCache(module, scope, data, version || String(Date.now()))
}
export interface MutateResult {
  queued: boolean
  clientId: string
  /** 在线成功时返回服务端结果（如新建记录的 id） */
  result?: unknown
}

/**
 * 写操作统一入口：
 * - 在线：直发请求；网络类失败（无状态/5xx/408/429）自动入队兜底；
 * - 离线：直接入队（乐观）。
 * 4xx 业务失败直接抛出，由页面提示。
 */
export async function mutate(
  module: string,
  action: string,
  payload: any,
  send: (payload: any, clientId: string) => Promise<unknown>,
  id?: string,
): Promise<MutateResult> {
  const clientId = id || `${module}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  if (getConnState() === 'offline') {
    enqueue(module, action, payload, clientId)
    return { queued: true, clientId }
  }
  try {
    const result = await send(payload, clientId)
    return { queued: false, clientId, result }
  } catch (e: any) {
    const status = Number(e?.status || e?.statusCode || 0)
    const networkish = !status || status >= 500 || status === 408 || status === 429
    if (networkish) {
      enqueue(module, action, payload, clientId)
      return { queued: true, clientId }
    }
    throw e
  }
}
