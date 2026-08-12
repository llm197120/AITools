/**
 * 资料存储统一上传（图片/视频/文档，含白名单校验）
 * 选文件统一走 useHomeaiFilePick
 */
import { getServerBaseUrl } from '../api/request'
import { getToken } from './auth'
import { validateUploadFile } from './fileWhitelist'
import { resolveOriginalFileName } from './resolveOriginalFileName'
import { useHomeaiFilePick } from './useHomeaiFilePick'

export async function uploadStorageFile(
  filePath: string,
  options: {
    folderId?: string
    visibility?: string
    fileName?: string
    familyIds?: string[]
  } = {},
): Promise<any> {
  const originalName =
    options.fileName?.trim() ||
    (await resolveOriginalFileName(filePath, { providedName: options.fileName }))

  if (!(await validateUploadFile(filePath, originalName))) {
    throw new Error('文件格式不允许')
  }

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: getServerBaseUrl() + '/homeai/storage/files/upload',
      filePath,
      name: 'file',
      formData: {
        folderId: options.folderId || '',
        visibility: options.visibility || 'private',
        fileName: originalName,
        familyIds: options.familyIds?.length ? options.familyIds.join(',') : '',
      },
      header: { 'X-Access-Token': getToken() || '' },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.success) resolve(data.result)
          else reject(new Error(data.message || '上传失败'))
        } catch (e) {
          reject(e)
        }
      },
      fail: reject,
    })
  })
}

async function pickResolved(
  onPick: (filePath: string, fileName?: string) => void | Promise<void>,
  filePath: string,
  opts: { providedName?: string; prefix?: string; defaultExt?: string },
) {
  const fileName = await resolveOriginalFileName(filePath, opts)
  await onPick(filePath, fileName)
}

/** 弹出上传菜单：拍照 / 相册 / 视频 / 文件（R27：统一 useHomeaiFilePick） */
export function showStorageUploadMenu(onPick: (filePath: string, fileName?: string) => void | Promise<void>) {
  const { showStoragePickMenu } = useHomeaiFilePick()
  showStoragePickMenu(async (files, source) => {
    for (const f of files) {
      if (source === 'camera' || source === 'album') {
        await pickResolved(onPick, f.path, { providedName: f.name, prefix: 'IMG' })
      } else if (source === 'video') {
        await pickResolved(onPick, f.path, { providedName: f.name, prefix: 'VIDEO', defaultExt: 'mp4' })
      } else {
        await pickResolved(onPick, f.path, { providedName: f.name, prefix: 'FILE' })
      }
    }
  })
}
