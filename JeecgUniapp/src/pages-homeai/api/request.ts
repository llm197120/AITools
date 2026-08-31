/**
 * HomeAI 小程序 API 请求封装
 */
import { getToken } from '../utils/auth'
import { consumeHomeaiUnauthorized } from '../utils/homeaiAuth'
import { getServerBaseUrl as getPlatformServerBaseUrl } from '../platform/env'

const BASE_URL = '/homeai'

/** 获取 API 基础路径 */
export function getBaseUrl(): string {
  return BASE_URL
}

/** 获取服务器完整地址（读取 VITE_SERVER_BASEURL，微信小程序按环境版本区分，见 platform/env.ts） */
export function getServerBaseUrl(): string {
  return getPlatformServerBaseUrl()
}

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  data?: any
  params?: Record<string, string>
  header?: Record<string, string>
}

/**
 * 解构调用方传入的 { data, params } 包装对象，兼容两种传参风格
 * - postApi('/xxx', { data: {...} })  -> 作为 JSON body
 * - postApi('/xxx', { params: {...} }) -> 拼接为 URL 查询参数
 * - postApi('/xxx', {...})            -> 直接作为 JSON body
 */
function unwrapOptions(options: RequestOptions): RequestOptions {
  const data = options.data
  let params = options.params
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    if ('data' in data || 'params' in data) {
      params = data.params !== undefined ? data.params : params
      return {
        ...options,
        data: data.data !== undefined ? data.data : undefined,
        params,
      }
    }
  }
  // get 等调用可能把 { params: {...} } 直接作为 params 传入
  if (params && typeof params === 'object' && 'params' in params) {
    params = (params as any).params
    return { ...options, params }
  }
  return options
}

/**
 * 基础请求封装
 */
async function request<T = any>(options: RequestOptions): Promise<T> {
  const unwrapped = unwrapOptions(options)
  const token = getToken()
  const header: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.header,
  }
  if (token) {
    header['X-Access-Token'] = token
  }

  // 将 params 拼接为 URL 查询参数
  let url = BASE_URL + unwrapped.url
  const params = unwrapped.params
  if (params && typeof params === 'object') {
    const query = Object.keys(params)
      .filter((key) => params[key] !== undefined && params[key] !== null)
      .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(String(params[key]))}`)
      .join('&')
    if (query) {
      url += (url.includes('?') ? '&' : '?') + query
    }
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: getServerBaseUrl() + url,
      method: options.method || 'GET',
      data: unwrapped.data,
      header,
      timeout: 30000,
      success: (res) => {
        const data = res.data as any
        if (consumeHomeaiUnauthorized(res.statusCode, data)) {
          reject(new Error(data?.message || '登录已过期'))
          return
        }
        if (data.success) {
          resolve(data.result)
        } else {
          uni.showToast({ title: data.message || '请求失败', icon: 'none' })
          reject(new Error(data.message || '请求失败'))
        }
      },
      fail: (err) => {
        const msg = String((err as any)?.errMsg || '')
        const title = /127\.0\.0\.1|localhost/.test(msg)
          ? '连不上本机 8080，真机请用电脑局域网 IP'
          : '网络异常，请稍后重试'
        uni.showToast({ title, icon: 'none', duration: 3000 })
        reject(err)
      },
    })
  })
}

/** GET 请求 */
export function get<T = any>(url: string, params?: Record<string, string>) {
  return request<T>({ url, method: 'GET', params })
}

/** POST 请求 */
export function post<T = any>(url: string, data?: any) {
  return request<T>({ url, method: 'POST', data })
}

/** PUT 请求 */
export function put<T = any>(url: string, data?: any) {
  return request<T>({ url, method: 'PUT', data })
}

/** PATCH 请求 */
export function patch<T = any>(url: string, params?: Record<string, string>) {
  return request<T>({ url, method: 'PATCH', params })
}

/** DELETE 请求 */
export function del<T = any>(url: string, data?: any) {
  return request<T>({ url, method: 'DELETE', data })
}
