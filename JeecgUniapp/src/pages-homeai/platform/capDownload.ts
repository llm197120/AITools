/**
 * Capacitor 下载/相册/分享（现行发版 H5 壳）。已装的旧云打包壳仍走 plus.*。
 */
import { isCapacitorNative } from './runtime'

async function toDataUrl(path: string): Promise<string> {
  if (!path) throw new Error('空路径')
  if (path.startsWith('data:')) return path
  if (/^https?:\/\//i.test(path)) return path
  const res = await fetch(path)
  if (!res.ok) throw new Error('读取文件失败')
  const blob = await res.blob()
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error || new Error('读取失败'))
    reader.readAsDataURL(blob)
  })
}

function guessName(path: string, fallback: string): string {
  try {
    const u = path.split('?')[0]
    const leaf = u.split(/[/\\]/).pop() || ''
    if (leaf && leaf.includes('.')) return leaf.replace(/[^\w.\-]+/g, '_')
  } catch {
    // ignore
  }
  return fallback
}

export async function capacitorSaveImage(filePath: string): Promise<void> {
  if (!isCapacitorNative()) {
    const a = document.createElement('a')
    a.href = filePath
    a.download = guessName(filePath, 'image')
    a.click()
    return
  }
  const { Media } = await import('@capacitor-community/media')
  await Media.savePhoto({ path: await toDataUrl(filePath) })
}

export async function capacitorSaveVideo(filePath: string): Promise<void> {
  if (!isCapacitorNative()) {
    const a = document.createElement('a')
    a.href = filePath
    a.download = guessName(filePath, 'video')
    a.click()
    return
  }
  const { Media } = await import('@capacitor-community/media')
  await Media.saveVideo({ path: await toDataUrl(filePath) })
}

export async function capacitorOpenDocument(filePath: string): Promise<void> {
  if (!isCapacitorNative()) {
    window.open(filePath, '_blank', 'noopener,noreferrer')
    return
  }
  const { Directory, Filesystem } = await import('@capacitor/filesystem')
  const { Share } = await import('@capacitor/share')
  const dataUrl = await toDataUrl(filePath)
  const comma = dataUrl.indexOf(',')
  const base64 = comma >= 0 ? dataUrl.slice(comma + 1) : dataUrl
  const name = guessName(filePath, `file-${Date.now()}`)
  await Filesystem.writeFile({
    path: name,
    data: base64,
    directory: Directory.Cache,
  })
  const { uri } = await Filesystem.getUri({ path: name, directory: Directory.Cache })
  await Share.share({ files: [uri], title: name })
}
