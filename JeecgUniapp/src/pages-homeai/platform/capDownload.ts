/**
 * Capacitor 下载 / 相册 / 打开文档（现行发版 H5 壳）。
 * 走已注册的 HomeaiUpdate 原生插件：系统 HTTP 下载 + FileProvider ACTION_VIEW，
 * 避开 WebView CORS，也不把「打开文档」误做成分享面板。
 */
import { accessTokenHeaders } from './accessToken'
import { isCapacitorNative } from './runtime'

type HomeaiNativePlugin = {
  download: (o: {
    url: string
    fileName?: string
    headers?: Record<string, string>
  }) => Promise<{ path: string }>
  openFile: (o: { path: string; mime?: string }) => Promise<void>
  readBase64: (o: { path: string }) => Promise<{ data: string }>
}

function nativePlugin() {
  return import('@capacitor/core').then(({ registerPlugin }) =>
    registerPlugin<HomeaiNativePlugin>('HomeaiUpdate'),
  )
}

function guessName(path: string, fallback: string): string {
  try {
    const u = path.split('?')[0]
    const leaf = u.split(/[/\\]/).pop() || ''
    if (leaf && leaf.includes('.')) return leaf.replace(/[^\w.\-()+]+/g, '_')
  } catch {
    // ignore
  }
  return fallback
}

function safeFileName(name?: string, fallback = `file-${Date.now()}`): string {
  const raw = (name || fallback).split(/[/\\]/).pop() || fallback
  const cleaned = raw.replace(/[^\w.\-()+]+/g, '_').slice(0, 80)
  return cleaned.includes('.') ? cleaned : `${cleaned}.bin`
}

function isRemoteUrl(path: string): boolean {
  return /^https?:\/\//i.test(path)
}

export function guessMime(name?: string): string {
  const ext = (name || '').split('?')[0].split('.').pop()?.toLowerCase() || ''
  const map: Record<string, string> = {
    pdf: 'application/pdf',
    doc: 'application/msword',
    docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    xls: 'application/vnd.ms-excel',
    xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    ppt: 'application/vnd.ms-powerpoint',
    pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    txt: 'text/plain',
    md: 'text/markdown',
    csv: 'text/csv',
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    png: 'image/png',
    gif: 'image/gif',
    webp: 'image/webp',
    mp4: 'video/mp4',
    mp3: 'audio/mpeg',
    zip: 'application/zip',
  }
  return map[ext] || 'application/octet-stream'
}

export function base64ToArrayBuffer(b64: string): ArrayBuffer {
  const bin = atob(b64)
  const bytes = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
  return bytes.buffer
}

export function base64ToUtf8(b64: string): string {
  return new TextDecoder('utf-8').decode(new Uint8Array(base64ToArrayBuffer(b64)))
}

export async function capacitorDownloadToTemp(url: string, fileName?: string): Promise<string> {
  if (!url) throw new Error('下载地址为空')
  const name = safeFileName(fileName, guessName(url, `file-${Date.now()}`))
  if (!isCapacitorNative()) {
    return url
  }
  const plugin = await nativePlugin()
  const { path } = await plugin.download({
    url,
    fileName: name,
    headers: accessTokenHeaders(url),
  })
  if (!path) throw new Error('下载失败')
  return path
}

export async function capacitorReadBase64(path: string): Promise<string> {
  const plugin = await nativePlugin()
  const { data } = await plugin.readBase64({ path })
  if (!data) throw new Error('读取失败')
  return data
}

export async function capacitorSaveImage(filePath: string, fileName?: string): Promise<void> {
  if (!isCapacitorNative()) {
    const a = document.createElement('a')
    a.href = filePath
    a.download = guessName(filePath, fileName || 'image')
    a.click()
    return
  }
  const { Media } = await import('@capacitor-community/media')
  const local = isRemoteUrl(filePath)
    ? await capacitorDownloadToTemp(filePath, fileName || guessName(filePath, 'image.jpg'))
    : filePath
  try {
    await Media.savePhoto({ path: local })
  } catch (e: any) {
    throw new Error(e?.message || '保存到相册失败')
  }
}

export async function capacitorSaveVideo(filePath: string, fileName?: string): Promise<void> {
  if (!isCapacitorNative()) {
    const a = document.createElement('a')
    a.href = filePath
    a.download = guessName(filePath, fileName || 'video')
    a.click()
    return
  }
  const { Media } = await import('@capacitor-community/media')
  const local = isRemoteUrl(filePath)
    ? await capacitorDownloadToTemp(filePath, fileName || guessName(filePath, 'video.mp4'))
    : filePath
  try {
    await Media.saveVideo({ path: local })
  } catch (e: any) {
    throw new Error(e?.message || '保存到相册失败')
  }
}

export async function capacitorOpenDocument(filePath: string, fileName?: string): Promise<void> {
  if (!isCapacitorNative()) {
    if (isRemoteUrl(filePath)) {
      const a = document.createElement('a')
      a.href = filePath
      a.download = safeFileName(fileName, guessName(filePath, 'file'))
      a.target = '_blank'
      a.rel = 'noopener noreferrer'
      a.click()
      return
    }
    window.open(filePath, '_blank', 'noopener,noreferrer')
    return
  }
  const local = isRemoteUrl(filePath)
    ? await capacitorDownloadToTemp(filePath, fileName)
    : filePath
  const plugin = await nativePlugin()
  await plugin.openFile({ path: local, mime: guessMime(fileName || local) })
}
