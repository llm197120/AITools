/**
 * 尽量还原相册/拍照文件的原始或接近原始的展示名（微信 chooseMedia 不返回系统文件名）
 */

function getExtension(pathOrName: string): string {
  const idx = pathOrName.lastIndexOf('.')
  return idx >= 0 ? pathOrName.substring(idx + 1).toLowerCase() : ''
}

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

function formatTimestampName(prefix: string, ext: string, date = new Date()): string {
  const stamp = `${date.getFullYear()}${pad2(date.getMonth() + 1)}${pad2(date.getDate())}_${pad2(date.getHours())}${pad2(date.getMinutes())}${pad2(date.getSeconds())}`
  const suffix = ext ? `.${ext.replace(/^\./, '')}` : ''
  return `${prefix}_${stamp}${suffix}`
}

/** 临时路径片段是否为无意义哈希/tmp 名 */
function isTempPathSegment(segment: string): boolean {
  const s = decodeURIComponent(segment).trim()
  if (!s) return true
  if (/^(tmp_|temp_|wxfile)/i.test(s)) return true
  if (/^[a-f0-9]{20,}(\.[a-z0-9]+)?$/i.test(s)) return true
  if (/^VoSkJ4Md/i.test(s)) return true
  return false
}

/** PC 微信等场景：临时路径末段可能含真实文件名 */
export function extractNameFromTempPath(filePath: string): string | undefined {
  if (!filePath) return undefined
  const normalized = filePath.replace(/\\/g, '/')
  const segment = normalized.split('/').pop() || ''
  if (isTempPathSegment(segment)) return undefined
  if (/[/\\:*?"<>|]/.test(segment)) return undefined
  if (!/\.[a-z0-9]{1,10}$/i.test(segment)) return undefined
  return decodeURIComponent(segment).trim()
}

function readFileArrayBuffer(filePath: string): Promise<ArrayBuffer | null> {
  return new Promise((resolve) => {
    try {
      uni.getFileSystemManager().readFile({
        filePath,
        success: (res) => resolve(res.data as ArrayBuffer),
        fail: () => resolve(null),
      })
    } catch {
      resolve(null)
    }
  })
}

/** 从 JPEG EXIF 中查找拍摄时间，格式化为 yyyyMMdd_HHmmss */
async function readImageExifStamp(filePath: string): Promise<string | undefined> {
  const buffer = await readFileArrayBuffer(filePath)
  if (!buffer || buffer.byteLength < 64) return undefined
  const slice = new Uint8Array(buffer.slice(0, Math.min(buffer.byteLength, 256 * 1024)))
  let text = ''
  for (let i = 0; i < slice.length; i++) {
    const c = slice[i]
    if (c >= 32 && c <= 126) text += String.fromCharCode(c)
  }
  const match = text.match(/(\d{4}):(\d{2}):(\d{2}) (\d{2}):(\d{2}):(\d{2})/)
  if (!match) return undefined
  return `${match[1]}${match[2]}${match[3]}_${match[4]}${match[5]}${match[6]}`
}

async function resolveImageExtension(filePath: string, fallback = 'jpg'): Promise<string> {
  const fromPath = getExtension(filePath)
  if (fromPath) return fromPath
  try {
    const info = await new Promise<UniApp.GetImageInfoSuccess>((resolve, reject) => {
      uni.getImageInfo({ src: filePath, success: resolve, fail: reject })
    })
    const t = (info.type || '').toLowerCase()
    if (t === 'jpeg') return 'jpg'
    if (t) return t
  } catch {
    /* ignore */
  }
  return fallback
}

export type ResolveOriginalNameOptions = {
  /** 客户端已提供的原名（如 chooseMessageFile 的 name） */
  providedName?: string
  /** 无原名时的前缀，如 IMG / VIDEO */
  prefix?: string
  /** 默认扩展名 */
  defaultExt?: string
}

/**
 * 解析上传展示名：优先 providedName / 路径末段；相册图再尝试 EXIF 拍摄时间；最后才用时间戳
 */
export async function resolveOriginalFileName(
  filePath: string,
  options: ResolveOriginalNameOptions = {},
): Promise<string> {
  const prefix = options.prefix || 'FILE'
  const provided = options.providedName?.trim()
  if (provided) return provided

  const fromPath = extractNameFromTempPath(filePath)
  if (fromPath) return fromPath

  const ext = options.defaultExt || getExtension(filePath) || (prefix === 'IMG' ? await resolveImageExtension(filePath) : 'dat')

  if (prefix === 'IMG' || ext.match(/^(jpe?g|png|webp|gif|bmp|heic)$/i)) {
    const exifStamp = await readImageExifStamp(filePath)
    if (exifStamp) return `IMG_${exifStamp}.${ext.replace(/^\./, '')}`
  }

  return formatTimestampName(prefix, ext)
}
