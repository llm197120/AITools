/**
 * 对本机 API 的下载请求补 X-Access-Token（原生 HTTP / uni.downloadFile 共用）
 */
import { getToken } from '../utils/auth'
import { getServerBaseUrl } from '../api/request'

export function accessTokenHeaders(url: string): Record<string, string> | undefined {
  const token = getToken()
  if (!token || !url) return undefined
  const base = getServerBaseUrl().replace(/\/$/, '')
  if (base && url.startsWith(base)) {
    return { 'X-Access-Token': token }
  }
  return undefined
}
