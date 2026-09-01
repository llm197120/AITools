/**
 * 离线文件暂存与恢复上传：
 * 离线新增的图片/文件先存 IndexedDB（pending），恢复在线后自动上传并清除。
 * 与 syncQueue 分离：这是「文件字节」级暂存，syncQueue 存的是「结构化写操作」。
 */
import { getToken } from '../utils/auth'
import { getConnState, onConnChange } from './conn'

const DB_NAME = 'homeai_pending_upload'
const DB_VERSION = 1
const STORE = 'files'

export interface PendingUpload {
  key: string
  module: string
  /** 上传接口完整地址（含 base） */
  url: string
  /** 表单字段名（默认 file） */
  name: string
  /** 文件名（保留扩展名，供后端校验） */
  fileName: string
  formData: Record<string, string>
  blob: Blob
  createdAt: number
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
          db.createObjectStore(STORE, { keyPath: 'key' })
        }
      }
      req.onsuccess = () => resolve(req.result)
      req.onerror = () => reject(req.error || new Error('open failed'))
    } catch (e) {
      reject(e)
    }
  })
  return dbPromise
}

export async function savePendingUpload(
  rec: Omit<PendingUpload, 'key' | 'createdAt'>,
): Promise<string> {
  const key = `${rec.module}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  try {
    const db = await openDb()
    await new Promise<void>((resolve) => {
      const tx = db.transaction(STORE, 'readwrite')
      tx.objectStore(STORE).put({ ...rec, key, createdAt: Date.now() } as PendingUpload)
      tx.oncomplete = () => resolve()
      tx.onerror = () => resolve()
    })
  } catch {
    /* ignore */
  }
  return key
}

export async function listPendingUploads(): Promise<PendingUpload[]> {
  try {
    const db = await openDb()
    return await new Promise((resolve) => {
      const tx = db.transaction(STORE, 'readonly')
      const req = tx.objectStore(STORE).getAll()
      req.onsuccess = () => resolve((req.result || []) as PendingUpload[])
      req.onerror = () => resolve([])
    })
  } catch {
    return []
  }
}

export async function deletePendingUpload(key: string): Promise<void> {
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

export async function getPendingUploadCount(): Promise<number> {
  return (await listPendingUploads()).length
}

function uploadOne(it: PendingUpload): Promise<unknown> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: it.url,
      file: it.blob,
      name: it.name || 'file',
      fileName: it.fileName,
      formData: it.formData || {},
      header: { 'X-Access-Token': getToken() || '' },
      timeout: 120000,
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.success) resolve(data.result)
          else reject(new Error(data.message || '上传失败'))
        } catch (e) {
          reject(e)
        }
      },
      fail: reject,
    })
  })
}

/** 恢复上传：逐个上传（失败跳过不阻塞），全部完成返回失败数 */
export async function flushPendingUploads(
  onOne?: (ok: boolean, err?: string) => void,
): Promise<number> {
  const items = await listPendingUploads()
  let failed = 0
  for (const it of items) {
    try {
      await uploadOne(it)
      await deletePendingUpload(it.key)
      onOne?.(true)
    } catch (e: any) {
      failed++
      onOne?.(false, String(e?.message || e))
    }
  }
  return failed
}

let inited = false

/** App 启动初始化：订阅连接恢复事件，在线时自动补传 pending 文件 */
export function initPendingUploadFlush(): void {
  if (inited) return
  inited = true
  onConnChange(() => {
    if (getConnState() === 'online') flushPendingUploads()
  })
}
