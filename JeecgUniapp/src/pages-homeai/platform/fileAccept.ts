/**
 * 上传扩展名 ↔ MIME，供 Android/H5 `<input accept>` 与 APP chooseFile 过滤。
 * 仅扩展名时部分国产 WebView 会当成图片选择器；需同时给 MIME。
 */
export const AUDIO_EXTS = ['mp3', 'wav', 'm4a', 'aac'] as const
export const IMAGE_EXTS = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'] as const
export const VIDEO_EXTS = ['mp4', 'avi', 'mov', 'mkv', 'webm'] as const

/** 与后端 homeai_file_whitelist 默认项对齐（含第 63 轮 webp/音频） */
export const DEFAULT_UPLOAD_EXTS = [
  ...IMAGE_EXTS,
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'ppt',
  'pptx',
  ...VIDEO_EXTS,
  ...AUDIO_EXTS,
  'zip',
  'rar',
  '7z',
  'txt',
  'csv',
  'md',
]

const EXT_MIME: Record<string, string> = {
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  gif: 'image/gif',
  bmp: 'image/bmp',
  webp: 'image/webp',
  mp4: 'video/mp4',
  mov: 'video/quicktime',
  avi: 'video/x-msvideo',
  mkv: 'video/x-matroska',
  webm: 'video/webm',
  mp3: 'audio/mpeg',
  wav: 'audio/wav',
  m4a: 'audio/mp4',
  aac: 'audio/aac',
  pdf: 'application/pdf',
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  xls: 'application/vnd.ms-excel',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ppt: 'application/vnd.ms-powerpoint',
  pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  zip: 'application/zip',
  rar: 'application/vnd.rar',
  '7z': 'application/x-7z-compressed',
  txt: 'text/plain',
  csv: 'text/csv',
  md: 'text/markdown',
}

const MIME_EXT: Record<string, string> = {
  'image/jpeg': 'jpg',
  'image/jpg': 'jpg',
  'image/png': 'png',
  'image/gif': 'gif',
  'image/bmp': 'bmp',
  'image/webp': 'webp',
  'video/mp4': 'mp4',
  'video/quicktime': 'mov',
  'video/x-msvideo': 'avi',
  'video/x-matroska': 'mkv',
  'video/webm': 'webm',
  'audio/mpeg': 'mp3',
  'audio/mp3': 'mp3',
  'audio/wav': 'wav',
  'audio/x-wav': 'wav',
  'audio/wave': 'wav',
  'audio/mp4': 'm4a',
  'audio/x-m4a': 'm4a',
  'audio/aac': 'aac',
  'application/pdf': 'pdf',
  'application/msword': 'doc',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'docx',
  'application/vnd.ms-excel': 'xls',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'xlsx',
  'application/vnd.ms-powerpoint': 'ppt',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'pptx',
  'application/zip': 'zip',
  'application/x-zip-compressed': 'zip',
  'application/vnd.rar': 'rar',
  'application/x-7z-compressed': '7z',
  'text/plain': 'txt',
  'text/csv': 'csv',
  'text/markdown': 'md',
}

export function normalizeExt(ext?: string): string {
  if (!ext) return ''
  return ext.trim().toLowerCase().replace(/^\./, '')
}

export function isAudioExts(exts?: string[]): boolean {
  if (!exts?.length) return false
  const audio = new Set<string>(AUDIO_EXTS)
  return exts.every((e) => audio.has(normalizeExt(e)))
}

export function extensionFromMime(mime?: string): string {
  if (!mime) return ''
  const m = mime.split(';')[0].trim().toLowerCase()
  return MIME_EXT[m] || ''
}

/**
 * H5/Capacitor `<input accept>`：
 * - 指定扩展名时同时输出 MIME + `.ext`（Android 认 MIME）
 * - type=all 且无扩展名时显式全部类型（避免空 accept 被 WebView 当成只选图片）
 */
export function buildAccept(opts?: {
  extension?: string[]
  type?: 'all' | 'file' | 'video' | 'image'
}): string {
  const exts = (opts?.extension || []).map(normalizeExt).filter(Boolean)
  if (exts.length) {
    const mimes = [...new Set(exts.map((e) => EXT_MIME[e]).filter(Boolean))]
    const dots = exts.map((e) => `.${e}`)
    if (isAudioExts(exts) && !mimes.some((m) => m.startsWith('audio/'))) {
      mimes.unshift('audio/*')
    }
    return [...mimes, ...dots].join(',')
  }
  if (opts?.type === 'image') return 'image/*,.jpg,.jpeg,.png,.gif,.bmp,.webp'
  if (opts?.type === 'video') return 'video/*,.mp4,.mov,.mkv,.webm,.avi'
  if (opts?.type === 'file') {
    return [
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/vnd.ms-excel',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-powerpoint',
      'application/vnd.openxmlformats-officedocument.presentationml.presentation',
      'application/zip',
      'text/plain',
      'text/csv',
      'audio/*',
      '.pdf',
      '.doc',
      '.docx',
      '.xls',
      '.xlsx',
      '.ppt',
      '.pptx',
      '.zip',
      '.rar',
      '.7z',
      '.txt',
      '.csv',
      '.md',
      '.mp3',
      '.wav',
      '.m4a',
      '.aac',
    ].join(',')
  }
  return '*/*'
}

/** APP-PLUS chooseFile：Android 上 type=file 不展示音频，音频白名单改走 all */
export function resolveAppChooseType(
  type: 'all' | 'file' | 'video' | 'image' | undefined,
  extension?: string[],
): 'all' | 'file' | 'video' | 'image' {
  if (isAudioExts(extension)) return 'all'
  return type || 'all'
}
