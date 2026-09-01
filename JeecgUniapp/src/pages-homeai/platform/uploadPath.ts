/**
 * Capacitor 原生环境上传路径适配。
 *
 * APP 是 H5 构建 + Capacitor 壳，uni.uploadFile 走 uni-h5 的 Web 实现（XHR + FormData）。
 * 它只能读取 blob:/data:/http(s): 等 Web 可读地址；而原生文件选择器（HomeaiUpdate.pickFile）
 * 返回的是文件系统绝对路径（/data/user/0/com.homeai.app/cache/homeai-pick/...），直接传入会：
 *   1. 被 uni-h5 getRealPath 拼成当前 origin 下的 URL（https://localhost/data/...）→ 404/空 → 上传失败；
 *   2. 即使路径可读，multipart 文件名也会退化成 file-<时间戳>，丢失扩展名导致后端按扩展名校验失败。
 * 这里统一做两件事：绝对路径转 Capacitor 本地代理 URL（convertFileSrc），并取回带原始文件名的 File。
 */
import { isCapacitorNative } from './runtime'

const WEB_READABLE_RE = /^(blob:|data:|https?:|capacitor:)/i

function convertFileSrc(path: string): string | undefined {
  try {
    const cap = (typeof window !== 'undefined' && (window as any).Capacitor) || undefined
    return typeof cap?.convertFileSrc === 'function' ? cap.convertFileSrc(path) : undefined
  } catch {
    return undefined
  }
}

/** 把本机绝对路径转成 WebView 可读取的 URL；非原生或已是 Web 可读地址则原样返回 */
export function toUploadablePath(path: string): string {
  if (!path || !isCapacitorNative()) return path
  if (WEB_READABLE_RE.test(path)) return path
  if (!/^\/(?!\/)/.test(path)) return path
  return convertFileSrc(path) || path
}

export interface HomeaiUploadInput {
  /** 传给 uni.uploadFile 的 filePath（Web 可读地址） */
  filePath?: string
  /** 带原始文件名的 File（Capacitor 原生 + 本地绝对路径时使用） */
  file?: File
}

/**
 * 构造 uni.uploadFile 的上传参数。
 * Capacitor 原生 + 本地绝对路径：转代理 URL 并取回带原始文件名的 File；
 * 其余场景（blob:/data:/http 或浏览器/小程序）原样返回 filePath。
 */
export async function toUploadParams(path: string, fileName?: string): Promise<HomeaiUploadInput> {
  if (!path || !isCapacitorNative() || WEB_READABLE_RE.test(path)) {
    return { filePath: path }
  }
  if (!/^\/(?!\/)/.test(path)) {
    return { filePath: path }
  }
  const url = convertFileSrc(path)
  if (!url || url === path) {
    return { filePath: path }
  }
  try {
    const blob = await fetch(url).then((res) => {
      if (!res.ok) throw new Error('HTTP ' + res.status)
      return res.blob()
    })
    const leaf =
      String(fileName || path)
        .split(/[/\\]/)
        .pop() || 'file'
    return { file: new File([blob], leaf, { type: blob.type || 'application/octet-stream' }) }
  } catch {
    // 读不到内容时降级：路径至少已转成 WebView 可访问地址
    return { filePath: url }
  }
}
