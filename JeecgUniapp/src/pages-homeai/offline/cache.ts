/**
 * 结构化数据读缓存（localStorage）。
 *
 * 策略：stale-while-revalidate。
 * - 在线：先返回缓存（秒开）→ 页面后台拉新 → 版本（maxUpdatedAt）无变化则保持缓存；
 * - 离线：直接返回缓存，页面展示"离线模式"提示。
 */
const PREFIX = 'homeai_cache_v1:'

export interface CacheEntry<T> {
  data: T
  fetchedAt: number
  /** 数据版本：列表取 max(updateTime/createTime)，详情取记录自身的 updateTime */
  version: string
}

function cacheKey(module: string, scope: string): string {
  return PREFIX + module + ':' + scope
}

export function readCache<T>(module: string, scope: string): CacheEntry<T> | null {
  try {
    const raw = uni.getStorageSync(cacheKey(module, scope))
    if (!raw) return null
    if (typeof raw === 'object' && raw.data !== undefined) {
      return raw as CacheEntry<T>
    }
    return JSON.parse(String(raw)) as CacheEntry<T>
  } catch {
    return null
  }
}

export function writeCache<T>(module: string, scope: string, data: T, version: string): void {
  const entry: CacheEntry<T> = { data, fetchedAt: Date.now(), version }
  try {
    uni.setStorageSync(cacheKey(module, scope), entry)
  } catch {
    // 容量超限时静默失败（localStorage 满）
  }
}

export function clearCache(module?: string, scope?: string): void {
  try {
    const all = uni.getStorageInfoSync().keys || []
    const prefix = module ? PREFIX + module + (scope ? ':' + scope : ':') : PREFIX
    for (const k of all) {
      if (k.startsWith(prefix)) uni.removeStorageSync(k)
    }
  } catch {
    /* ignore */
  }
}

/** 列表数据的最新版本：取所有记录 max(updateTime/updatedAt/createTime) */
export function maxUpdatedAt(records: any[] | null | undefined): string {
  let max = ''
  for (const r of records || []) {
    const t = String(r?.updateTime || r?.updatedAt || r?.createTime || '')
    if (t && t > max) max = t
  }
  return max
}
