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

export function isAudioExt(ext: string): boolean {
  return ['mp3', 'wav', 'm4a', 'aac'].includes(ext)
}

export function isPdfExt(ext: string): boolean {
  return ext === 'pdf'
}

export function isTextExt(ext: string): boolean {
  return ['txt', 'md', 'log', 'json', 'csv'].includes(ext)
}

export function isOfficeExt(ext: string): boolean {
  return ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext)
}

export type PreviewFileInput = {
  id?: string
  materialId?: string
  fileUrl?: string
  url?: string
  originalName?: string
  title?: string
  extension?: string
}

/** 跳转资料预览页（支持图片/视频/音频/PDF/Office/TXT） */
export function previewFile(file: PreviewFileInput) {
  const qs: string[] = []
  if (file.id) qs.push(`fileId=${encodeURIComponent(file.id)}`)
  if (file.materialId) qs.push(`materialId=${encodeURIComponent(file.materialId)}`)
  const url = file.fileUrl || file.url
  if (url) qs.push(`url=${encodeURIComponent(url)}`)
  const name = file.originalName || file.title || '文件'
  qs.push(`name=${encodeURIComponent(name)}`)
  const ext = file.extension || getFileExt(file.originalName || file.title || url || '')
  if (ext) qs.push(`ext=${encodeURIComponent(ext)}`)
  if (!file.id && !file.materialId && !url) {
    uni.showToast({ title: '无可预览内容', icon: 'none' })
    return
  }
  uni.navigateTo({
    url: `/pages-homeai-more/storage/preview?${qs.join('&')}`,
  })
}

/** 直接预览图片（列表缩略图点击） */
export function previewImageUrl(url: string) {
  uni.previewImage({ urls: [url], current: url })
}
