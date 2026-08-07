/**
 * 文件白名单同步与校验（本地缓存 5 分钟）
 */
import { get as getApi } from '../api/request'

const CACHE_KEY = 'homeai_file_whitelist'
const CACHE_TIME_KEY = 'homeai_file_whitelist_time'
const TTL = 5 * 60 * 1000

const DEFAULT_EXTENSIONS = [
  'jpg', 'jpeg', 'png', 'gif', 'bmp',
  'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
  'mp4', 'avi', 'mov', 'mkv',
  'zip', 'rar', '7z',
  'txt', 'csv', 'md',
]

export function getExtension(filePathOrName: string): string {
  if (!filePathOrName) return ''
  const name = filePathOrName.split(/[/\\]/).pop() || filePathOrName
  const idx = name.lastIndexOf('.')
  return idx >= 0 ? name.substring(idx + 1).toLowerCase() : ''
}

export async function getWhitelistExtensions(force = false): Promise<string[]> {
  if (!force) {
    try {
      const cached = uni.getStorageSync(CACHE_KEY)
      const cacheTime = Number(uni.getStorageSync(CACHE_TIME_KEY) || 0)
      if (cached && Date.now() - cacheTime < TTL) {
        return JSON.parse(cached)
      }
    } catch {
      /* ignore */
    }
  }
  try {
    const res: any = await getApi('/config/file-whitelist')
    const extensions: string[] = res?.extensions || DEFAULT_EXTENSIONS
    uni.setStorageSync(CACHE_KEY, JSON.stringify(extensions))
    uni.setStorageSync(CACHE_TIME_KEY, String(Date.now()))
    return extensions
  } catch {
    return DEFAULT_EXTENSIONS
  }
}

export function isAllowedExtension(ext: string, whitelist: string[]): boolean {
  if (!ext) return false
  return whitelist.includes(ext.toLowerCase())
}

/** 上传前校验，不通过时 toast 并返回 false */
export async function validateUploadFile(filePath: string, fileName?: string): Promise<boolean> {
  const ext = getExtension(fileName || filePath)
  const whitelist = await getWhitelistExtensions()
  if (!isAllowedExtension(ext, whitelist)) {
    uni.showToast({ title: '不支持该文件格式', icon: 'none' })
    return false
  }
  return true
}

/** 页面 onShow 时预加载白名单 */
export function preloadWhitelist() {
  getWhitelistExtensions().catch(() => {})
}
