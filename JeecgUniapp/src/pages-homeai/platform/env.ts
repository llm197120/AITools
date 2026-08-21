/**
 * HomeAI 平台环境地址适配层
 *
 * - 微信小程序：按小程序环境版本（develop/trial/release）切换 VITE_SERVER_BASEURL__WEIXIN_*
 * - Android APP：优先编译进包的 VITE_SERVER_BASEURL_APP；本机/局域网旧缓存不会盖过公网地址
 */
import { getEnvBaseUrl, getEnvBaseUploadUrl } from '@/utils/index'
import { isStandaloneApp } from './runtime'

const API_BASE_KEY = 'homeai_api_base'

function normalizeBase(url: string): string {
  return String(url || '').replace(/\/$/, '')
}

function isLoopbackOrPrivate(url: string): boolean {
  try {
    const host = new URL(url).hostname
    if (host === 'localhost' || host === '127.0.0.1' || host === '10.0.2.2') return true
    if (/^192\.168\./.test(host) || /^10\./.test(host)) return true
    if (/^172\.(1[6-9]|2\d|3[0-1])\./.test(host)) return true
    return false
  } catch {
    return true
  }
}

function pingBase(base: string): Promise<boolean> {
  return new Promise((resolve) => {
    uni.request({
      url: `${normalizeBase(base)}/sys/randomImage/homeai-probe`,
      method: 'GET',
      timeout: 2000,
      success: () => resolve(true),
      fail: () => resolve(false),
    })
  })
}

/**
 * APP 启动时探测可达的后端地址，写入本地覆盖，避免模拟器/真机各打不通一套 IP。
 */
export async function probeAndCacheAppBaseUrl(): Promise<string> {
  // #ifdef MP-WEIXIN
  return getEnvBaseUrl()
  // #endif
  // 独立 App（Capacitor H5；旧云打包壳为 APP-PLUS）探测公网/编译地址
  if (!isStandaloneApp()) return getEnvBaseUrl()
  const candidates: string[] = []
  const push = (u?: string) => {
    const n = u ? normalizeBase(u) : ''
    if (n && n.startsWith('http') && !candidates.includes(n)) candidates.push(n)
  }
  const appEnv = normalizeBase(import.meta.env.VITE_SERVER_BASEURL_APP || '')
  // 编译地址优先，避免上次缓存的局域网 IP 挡住公网入口
  push(appEnv)
  try {
    push(uni.getStorageSync(API_BASE_KEY))
  } catch {
    // ignore
  }
  push(import.meta.env.VITE_SERVER_BASEURL)
  if (!appEnv || isLoopbackOrPrivate(appEnv)) {
    push('http://10.0.2.2:8080/jeecg-boot')
    push('http://127.0.0.1:8080/jeecg-boot')
  }

  for (const base of candidates) {
    if (await pingBase(base)) {
      uni.setStorageSync(API_BASE_KEY, base)
      return base
    }
  }
  return getEnvBaseUrl()
}

/** 内测手动覆盖后端地址（写入后立即对后续请求生效） */
export function setAppBaseUrl(url: string) {
  uni.setStorageSync(API_BASE_KEY, normalizeBase(url))
}

export function pingAppBaseUrl(url: string): Promise<boolean> {
  return pingBase(url)
}

/** 获取服务器 API 基础地址（转发 utils/index.ts 的 getEnvBaseUrl） */
export function getServerBaseUrl(): string {
  return getEnvBaseUrl()
}

/** 获取上传基础地址（转发 utils/index.ts 的 getEnvBaseUploadUrl） */
export function getUploadBaseUrl(): string {
  return getEnvBaseUploadUrl()
}