/**
 * 图片 blob 缓存（IndexedDB，磁盘持久化存储，非内存）。
 *
 * - 上限默认 4GB（阶段四由后端 /homeai/config/sync 的 imageCacheLimitMB 可配置）；
 * - 启动时用 navigator.storage.estimate() 探测实际配额，取 min(配置, 可用配额)；
 * - 超限按最近访问时间 LRU 清理（pending 待上传图片不清理，见 putImageBlob 的 pending 标记）；
 * - 跨域：OSS 预签名 URL 未配置 CORS，浏览器 fetch 会被拦，统一走后端同域代理
 *   /homeai/storage/image-proxy?url=... 获取 blob（后端拉取无 CORS 限制）。
 */
import { getServerBaseUrl } from '../api/request'
import { getToken } from '../utils/auth'

const DB_NAME = 'homeai_image_cache'
const DB_VERSION = 1
const STORE = 'images'
const DB_READY_KEY = 'homeai_imgdb_ready'

export interface ImageCacheRecord {
  key: string
  blob: Blob
  contentType?: string
  size: number
  fetchedAt: number
  lastAccess: number
  /** true = 待上传（离线新增的本地图片），不参与 LRU 清理 */
  pending: boolean
}

let dbPromise: Promise<IDBDatabase> | null = null

function openDb(): Promise<IDBDatabase> {
  if (dbPromise) return dbPromise
  dbPromise = new Promise((resolve, reject) => {
    try {
      if (typeof indexedDB === 'undefined') {
        reject(new Error('indexedDB unsupported'))
        return
      }
      const req = indexedDB.open(DB_NAME, DB_VERSION)
      req.onupgradeneeded = () => {
        const db = req.result
        if (!db.objectStoreNames.contains(STORE)) {
          const store = db.createObjectStore(STORE, { keyPath: 'key' })
          store.createIndex('lastAccess', 'lastAccess')
          store.createIndex('pending', 'pending')
        }
      }
      req.onsuccess = () => resolve(req.result)
      req.onerror = () => reject(req.error || new Error('indexedDB open failed'))
    } catch (e) {
      reject(e)
    }
  })
  return dbPromise
}

/** URL 规范化：去掉签名参数（?Expires=...&Signature=...），保留路径；blob/data 原样 */
export function normalizeImageKey(url: string): string {
  if (!url) return url
  if (/^(blob:|data:|capacitor:)/i.test(url)) return url
  const idx = url.indexOf('?')
  return idx >= 0 ? url.substring(0, idx) : url
}

/** 经后端代理获取图片 blob（同域，绕开 OSS CORS；失败返回 null） */
export async function fetchImageBlobViaProxy(
  url: string,
): Promise<{ blob: Blob; contentType?: string } | null> {
  try {
    const token = getToken() || ''
    const res = await fetch(
      `${getServerBaseUrl()}/homeai/storage/image-proxy?url=${encodeURIComponent(url)}`,
      { headers: token ? { 'X-Access-Token': token } : {}, timeout: 30000 } as RequestInit & {
        timeout?: number
      },
    )
    if (!res.ok) return null
    const blob = await res.blob()
    if (!blob || blob.size === 0) return null
    return { blob, contentType: res.headers.get('content-type') || undefined }
  } catch {
    return null
  }
}

export async function getImageBlob(
  url: string,
): Promise<{ blob: Blob; contentType?: string } | null> {
  const key = normalizeImageKey(url)
  if (!key || /^(blob:|data:)/i.test(key)) return null
  try {
    const db = await openDb()
    return await new Promise((resolve) => {
      const tx = db.transaction(STORE, 'readwrite')
      const req = tx.objectStore(STORE).get(key)
      req.onsuccess = () => {
        const rec = req.result as ImageCacheRecord | undefined
        if (rec && rec.blob) {
          rec.lastAccess = Date.now()
          tx.objectStore(STORE).put(rec)
          resolve({ blob: rec.blob, contentType: rec.contentType })
        } else {
          resolve(null)
        }
      }
      req.onerror = () => resolve(null)
    })
  } catch {
    return null
  }
}

export async function putImageBlob(
  url: string,
  blob: Blob,
  opts: { contentType?: string; pending?: boolean } = {},
): Promise<void> {
  const key = normalizeImageKey(url)
  if (!key) return
  try {
    const db = await openDb()
    await new Promise<void>((resolve) => {
      const tx = db.transaction(STORE, 'readwrite')
      const store = tx.objectStore(STORE)
      store.put({
        key,
        blob,
        contentType: opts.contentType,
        size: blob.size,
        fetchedAt: Date.now(),
        lastAccess: Date.now(),
        pending: opts.pending === true,
      } as ImageCacheRecord)
      tx.oncomplete = () => resolve()
      tx.onerror = () => resolve()
    })
    // 写后尝试按配额清理（pending 不清理）
    evictIfOverLimit()
  } catch {
    /* ignore */
  }
}

/** 读取待上传（pending）记录：离线新增图片同步前遍历用 */
export async function listPendingImages(): Promise<Array<{ key: string; blob: Blob }>> {
  try {
    const db = await openDb()
    return await new Promise((resolve) => {
      const tx = db.transaction(STORE, 'readonly')
      const req = tx.objectStore(STORE).index('pending').getAll()
      req.onsuccess = () => {
        const recs = (req.result || []) as ImageCacheRecord[]
        resolve(recs.filter((r) => r.pending && r.blob).map((r) => ({ key: r.key, blob: r.blob })))
      }
      req.onerror = () => resolve([])
    })
  } catch {
    return []
  }
}

/** 清除 pending 标记（同步成功后转为普通缓存） */
export async function clearPendingFlag(url: string): Promise<void> {
  const key = normalizeImageKey(url)
  if (!key) return
  try {
    const db = await openDb()
    await new Promise<void>((resolve) => {
      const tx = db.transaction(STORE, 'readwrite')
      const req = tx.objectStore(STORE).get(key)
      req.onsuccess = () => {
        const rec = req.result as ImageCacheRecord | undefined
        if (rec) {
          rec.pending = false
          tx.objectStore(STORE).put(rec)
        }
        resolve()
      }
      req.onerror = () => resolve()
    })
  } catch {
    /* ignore */
  }
}

export async function deleteImage(url: string): Promise<void> {
  const key = normalizeImageKey(url)
  if (!key) return
  try {
    const db = await openDb()
    await new Promise<void>((resolve) => {
      const tx = db.transaction(STORE, 'readwrite')
      tx.objectStore(STORE).delete(key)
      tx.oncomplete = () => resolve()
      tx.onerror = () => resolve()
    })
  } catch {
    /* ignore */
  }
}

/** 当前缓存总字节数 */
export async function getCacheUsage(): Promise<number> {
  try {
    const db = await openDb()
    return await new Promise((resolve) => {
      const tx = db.transaction(STORE, 'readonly')
      const req = tx.objectStore(STORE).getAll()
      req.onsuccess = () => {
        const recs = (req.result || []) as ImageCacheRecord[]
        resolve(recs.reduce((sum, r) => sum + (Number(r.size) || 0), 0))
      }
      req.onerror = () => resolve(0)
    })
  } catch {
    return 0
  }
}

let cachedLimitMB = 0

/** 缓存上限（MB）：min(配置 4096, 浏览器可用配额) */
export async function getCacheLimitMB(): Promise<number> {
  if (cachedLimitMB) return cachedLimitMB
  let configured = 4096
  try {
    const cfg = uni.getStorageSync('homeai_sync_config') as Record<string, any> | undefined
    if (cfg && Number(cfg.imageCacheLimitMB) > 0) configured = Number(cfg.imageCacheLimitMB)
  } catch {
    /* ignore */
  }
  try {
    if (navigator.storage && typeof navigator.storage.estimate === 'function') {
      const est = await navigator.storage.estimate()
      const quotaMB = Number(est.quota || 0) / 1024 / 1024
      if (quotaMB > 0) {
        cachedLimitMB = Math.min(configured, Math.floor(quotaMB * 0.9))
        return cachedLimitMB
      }
    }
  } catch {
    /* ignore */
  }
  cachedLimitMB = configured
  return cachedLimitMB
}

/** LRU 清理：总量超上限时按最近访问升序删除（pending 不删） */
export async function evictIfOverLimit(): Promise<void> {
  const limitBytes = (await getCacheLimitMB()) * 1024 * 1024
  const usage = await getCacheUsage()
  if (usage <= limitBytes) return
  try {
    const db = await openDb()
    await new Promise<void>((resolve) => {
      const tx = db.transaction(STORE, 'readwrite')
      const store = tx.objectStore(STORE)
      const req = store.index('lastAccess').openCursor(null, 'next')
      let freed = 0
      const target = usage - limitBytes
      req.onsuccess = () => {
        const cursor = req.result
        if (!cursor || freed >= target) {
          resolve()
          return
        }
        const rec = cursor.value as ImageCacheRecord
        if (!rec.pending) {
          freed += Number(rec.size) || 0
          cursor.delete()
        }
        cursor.continue()
      }
      req.onerror = () => resolve()
    })
  } catch {
    /* ignore */
  }
}

/** 检测 IndexedDB 可用性（兼容性兜底） */
export function isIndexedDbSupported(): boolean {
  try {
    return typeof indexedDB !== 'undefined'
  } catch {
    return false
  }
}

export function markDbReady(): void {
  try {
    uni.setStorageSync(DB_READY_KEY, true)
  } catch {
    /* ignore */
  }
}
