/**
 * 资料存储统一上传（图片/视频/文档，含白名单校验）
 * 选文件统一走 useHomeaiFilePick
 */
import { getServerBaseUrl } from '../api/request'
import { getToken } from './auth'
import { consumeHomeaiUnauthorized } from './homeaiAuth'
import { validateUploadFile } from './fileWhitelist'
import { toUploadParams } from '../platform/uploadPath'
import { resolveOriginalFileName } from './resolveOriginalFileName'
import { useHomeaiFilePick } from './useHomeaiFilePick'
import { getConnState } from '../offline/conn'
import { savePendingUpload } from '../offline/pendingUpload'

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

  // 离线：文件字节暂存本地，恢复在线后由 pendingUpload 自动补传
  if (getConnState() === 'offline') {
    const input = await toUploadParams(filePath, originalName)
    if (!input.file) {
      throw new Error('离线无法读取该文件')
    }
    await savePendingUpload({
      module: 'storage',
      url: getServerBaseUrl() + '/homeai/storage/files/upload',
      name: 'file',
      fileName: originalName,
      formData: {
        folderId: options.folderId || '',
        visibility: options.visibility || 'private',
        fileName: originalName,
        familyIds: options.familyIds?.length ? options.familyIds.join(',') : '',
      },
      blob: input.file,
    })
    return { pending: true, fileName: originalName }
  }

  return new Promise((resolve, reject) => {
    toUploadParams(filePath, originalName)
      .then((upload) => {
        uni.uploadFile({
          url: getServerBaseUrl() + '/homeai/storage/files/upload',
          ...upload,
          name: 'file',
          formData: {
            folderId: options.folderId || '',
            visibility: options.visibility || 'private',
            fileName: originalName,
            familyIds: options.familyIds?.length ? options.familyIds.join(',') : '',
          },
          header: { 'X-Access-Token': getToken() || '' },
          timeout: 120000,
          success: (res) => {
            try {
              if (consumeHomeaiUnauthorized(res.statusCode, res.data)) {
                reject(new Error('登录已过期'))
                return
              }
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
      .catch(reject)
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
