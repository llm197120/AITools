/**
 * HomeAI 统一文件选择：白名单校验 + chooseMessageFile / 图片 / 视频
 */
import { AUDIO_EXTS, extensionFromMime } from '../platform/fileAccept'
import { pickDocument } from '../platform/filePicker'
import { getExtension, preloadWhitelist, validateUploadFile } from './fileWhitelist'

export interface HomeaiPickedFile {
  path: string
  name: string
  ext: string
  size?: number
}

export interface PickFilesOptions {
  count?: number
  /** 传给 chooseMessageFile */
  type?: 'all' | 'file' | 'video' | 'image'
  extension?: string[]
  /** 额外业务限制（在白名单基础上再收窄） */
  allowedExt?: string[]
}

function toPicked(path: string, name?: string, size?: number, mimeType?: string, fallbackExt?: string): HomeaiPickedFile {
  const n = name || path.split(/[/\\]/).pop() || path
  let ext = getExtension(n) || getExtension(path) || extensionFromMime(mimeType) || (fallbackExt || '')
  if (ext === 'jpeg') ext = 'jpg'
  let displayName = n
  if (ext && !getExtension(displayName)) {
    displayName = `${displayName}.${ext}`
  }
  return { path, name: displayName, ext, size }
}

function extAllowed(ext: string, allowedExt: string[]): boolean {
  const set = new Set(allowedExt.map((e) => e.toLowerCase()))
  if (set.has(ext)) return true
  if (ext === 'jpg' && set.has('jpeg')) return true
  if (ext === 'jpeg' && set.has('jpg')) return true
  return false
}

async function filterAllowed(files: HomeaiPickedFile[], allowedExt?: string[]): Promise<HomeaiPickedFile[]> {
  const out: HomeaiPickedFile[] = []
  for (const f of files) {
    if (!(await validateUploadFile(f.path, f.name))) continue
    if (allowedExt?.length && !extAllowed(f.ext, allowedExt)) {
      uni.showToast({ title: `仅支持 ${allowedExt.join('/')}`, icon: 'none' })
      continue
    }
    out.push(f)
  }
  return out
}

export function useHomeaiFilePick() {
  function preload() {
    preloadWhitelist()
  }

  /** 文档/通用文件：平台适配器（小程序 chooseMessageFile / APP chooseFile）+ 白名单 */
  async function pickFiles(opts?: PickFilesOptions): Promise<HomeaiPickedFile[]> {
    const count = opts?.count ?? 1
    const files = await pickDocument({ count, type: opts?.type, extension: opts?.extension })
    const raw = files.map((f) => toPicked(f.path, f.name, f.size, f.mimeType))
    return filterAllowed(raw, opts?.allowedExt)
  }

  /** 图片：优先 chooseImage，失败可走 messageFile */
  async function pickImages(opts?: {
    count?: number
    sourceType?: Array<'album' | 'camera'>
  }): Promise<HomeaiPickedFile[]> {
    const count = opts?.count ?? 1
    return new Promise((resolve) => {
      uni.chooseImage({
        count,
        sourceType: opts?.sourceType || ['album', 'camera'],
        success: async (r) => {
          const paths = r.tempFilePaths || []
          const tempFiles = (r.tempFiles || []) as any[]
          const raw = paths.map((p, i) => {
            const tf = tempFiles[i]
            const name = tf?.name || (typeof tf?.path === 'string' ? String(tf.path).split(/[/\\]/).pop() : undefined)
            return toPicked(p, name, tf?.size, tf?.type, 'jpg')
          })
          resolve(await filterAllowed(raw))
        },
        fail: async () => {
          resolve(
            await pickFiles({
              count,
              type: 'image',
              extension: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'],
            }),
          )
        },
      })
    })
  }

  /** 资料场景：chooseMedia 保留原图质量 */
  async function pickMediaImages(opts?: {
    count?: number
    sourceType?: Array<'album' | 'camera'>
  }): Promise<HomeaiPickedFile[]> {
    const count = opts?.count ?? 1
    return new Promise((resolve) => {
      uni.chooseMedia({
        count,
        mediaType: ['image'],
        sourceType: opts?.sourceType || ['album', 'camera'],
        sizeType: ['original'],
        success: async (r) => {
          const raw = (r.tempFiles || [])
            .filter((f: any) => f?.tempFilePath)
            .map((f: any) => toPicked(f.tempFilePath, f.name, f.size, f.fileType || f.type, 'jpg'))
          resolve(await filterAllowed(raw))
        },
        fail: async () => {
          resolve(await pickImages(opts))
        },
      })
    })
  }

  /** 选择视频 + 白名单 */
  async function pickVideo(opts?: {
    sourceType?: Array<'album' | 'camera'>
    maxDuration?: number
  }): Promise<HomeaiPickedFile[]> {
    return new Promise((resolve) => {
      uni.chooseVideo({
        sourceType: opts?.sourceType || ['album', 'camera'],
        maxDuration: opts?.maxDuration ?? 300,
        success: async (r) => {
          if (!r.tempFilePath) {
            resolve([])
            return
          }
          const name = r.tempFilePath.split(/[/\\]/).pop() || `VIDEO_${Date.now()}.mp4`
          resolve(await filterAllowed([toPicked(r.tempFilePath, name, r.size, undefined, 'mp4')]))
        },
        fail: () => resolve([]),
      })
    })
  }

  /** ActionSheet：拍照 / 相册 / 文件 */
  function showPickMenu(
    onPick: (files: HomeaiPickedFile[], source: 'camera' | 'album' | 'file') => void | Promise<void>,
    opts?: { allowFile?: boolean; imageCount?: number; fileCount?: number },
  ) {
    const allowFile = opts?.allowFile !== false
    const itemList = allowFile ? ['拍照', '从相册选择', '选择文件'] : ['拍照', '从相册选择']
    uni.showActionSheet({
      itemList,
      success: async (res) => {
        let files: HomeaiPickedFile[] = []
        let source: 'camera' | 'album' | 'file' = 'album'
        if (res.tapIndex === 0) {
          source = 'camera'
          files = await pickImages({ count: opts?.imageCount ?? 9, sourceType: ['camera'] })
        } else if (res.tapIndex === 1) {
          source = 'album'
          files = await pickImages({ count: opts?.imageCount ?? 9, sourceType: ['album'] })
        } else if (allowFile && res.tapIndex === 2) {
          source = 'file'
          files = await pickFiles({ count: opts?.fileCount ?? 5, type: 'all' })
        }
        if (files.length) await onPick(files, source)
      },
    })
  }

  /** 资料上传菜单：拍照 / 相册 / 视频 / 文件 */
  function showStoragePickMenu(
    onPick: (
      files: HomeaiPickedFile[],
      source: 'camera' | 'album' | 'video' | 'file',
    ) => void | Promise<void>,
  ) {
    uni.showActionSheet({
        itemList: ['拍照', '从相册选择', '选择视频', '选择音频', '选择文件(PDF/文档等)'],
        success: async (res) => {
        let files: HomeaiPickedFile[] = []
        let source: 'camera' | 'album' | 'video' | 'file' = 'file'
        if (res.tapIndex === 0) {
          source = 'camera'
          files = await pickMediaImages({ count: 1, sourceType: ['camera'] })
        } else if (res.tapIndex === 1) {
          source = 'album'
          files = await pickMediaImages({ count: 9, sourceType: ['album'] })
        } else if (res.tapIndex === 2) {
          source = 'video'
          files = await pickVideo()
        } else if (res.tapIndex === 3) {
          source = 'file'
          files = await pickFiles({
            count: 1,
            type: 'all',
            extension: [...AUDIO_EXTS],
            allowedExt: [...AUDIO_EXTS],
          })
        } else if (res.tapIndex === 4) {
          source = 'file'
          files = await pickFiles({ count: 9, type: 'all' })
        }
        if (files.length) await onPick(files, source)
      },
    })
  }

  return {
    preload,
    pickFiles,
    pickImages,
    pickMediaImages,
    pickVideo,
    showPickMenu,
    showStoragePickMenu,
  }
}
