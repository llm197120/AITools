/**
 * 文件预览工具（小程序）
 */
export function getFileExt(nameOrUrl: string): string {
  const name = nameOrUrl.split('?')[0]
  const idx = name.lastIndexOf('.')
  return idx >= 0 ? name.substring(idx + 1).toLowerCase() : ''
}

export function isImageExt(ext: string): boolean {
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)
}

export function isVideoExt(ext: string): boolean {
  return ['mp4', 'mov', 'avi', 'mkv', 'webm', 'm4v'].includes(ext)
}

export function isPdfExt(ext: string): boolean {
  return ext === 'pdf'
}

export function isTextExt(ext: string): boolean {
  return ['txt', 'md', 'log', 'json', 'csv'].includes(ext)
}

export type PreviewFileInput = {
  id?: string
  fileUrl?: string
  url?: string
  originalName?: string
  title?: string
  extension?: string
}

/** 跳转资料预览页（支持图片/视频/PDF/TXT） */
export function previewFile(file: PreviewFileInput) {
  if (file.id) {
    uni.navigateTo({
      url: `/pages-homeai-more/storage/preview?fileId=${encodeURIComponent(file.id)}`,
    })
    return
  }
  const url = file.fileUrl || file.url
  if (!url) {
    uni.showToast({ title: '无可预览内容', icon: 'none' })
    return
  }
  const ext = file.extension || getFileExt(file.originalName || file.title || url)
  uni.navigateTo({
    url: `/pages-homeai-more/storage/preview?url=${encodeURIComponent(url)}&name=${encodeURIComponent(file.originalName || file.title || '文件')}&ext=${encodeURIComponent(ext)}`,
  })
}

/** 直接预览图片（列表缩略图点击） */
export function previewImageUrl(url: string) {
  uni.previewImage({ urls: [url], current: url })
}
