/**
 * 资料存储统一上传（图片/视频/文档，含白名单校验）
 */
import { getServerBaseUrl } from '../api/request'
import { getToken } from './auth'
import { validateUploadFile } from './fileWhitelist'
import { resolveOriginalFileName } from './resolveOriginalFileName'

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

/** 弹出上传菜单：拍照 / 相册 / 视频 / 文件 */
export function showStorageUploadMenu(onPick: (filePath: string, fileName?: string) => void | Promise<void>) {
  uni.showActionSheet({
    itemList: ['拍照', '从相册选择', '选择视频', '选择文件(PDF/TXT等)'],
    success: (res) => {
      if (res.tapIndex === 0) {
        uni.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sourceType: ['camera'],
          sizeType: ['original'],
          success: (r) => {
            const f = r.tempFiles?.[0]
            if (f?.tempFilePath) {
              void pickResolved(onPick, f.tempFilePath, { prefix: 'IMG' })
            }
          },
        })
      } else if (res.tapIndex === 1) {
        uni.chooseMedia({
          count: 9,
          mediaType: ['image'],
          sourceType: ['album'],
          sizeType: ['original'],
          success: (r) => {
            r.tempFiles?.forEach((f) => {
              if (f.tempFilePath) {
                void pickResolved(onPick, f.tempFilePath, { prefix: 'IMG' })
              }
            })
          },
        })
      } else if (res.tapIndex === 2) {
        uni.chooseVideo({
          sourceType: ['album', 'camera'],
          maxDuration: 300,
          success: (r) => {
            if (r.tempFilePath) {
              void pickResolved(onPick, r.tempFilePath, { prefix: 'VIDEO', defaultExt: 'mp4' })
            }
          },
        })
      } else if (res.tapIndex === 3) {
        uni.chooseMessageFile({
          count: 9,
          type: 'all',
          success: (r) => {
            r.tempFiles?.forEach((f) => {
              void pickResolved(onPick, f.path, { providedName: f.name, prefix: 'FILE' })
            })
          },
        })
      }
    },
  })
}
