/**
 * 统一文件上传 composable（白名单校验 + 进度）
 */
import { getServerBaseUrl } from '../api/request'
import { getToken } from './auth'
import { validateUploadFile } from './fileWhitelist'
import { useHomeaiFilePick } from './useHomeaiFilePick'

export interface HomeUploadOptions {
  url: string
  filePath: string
  name?: string
  formData?: Record<string, string>
}

export async function homeUpload(options: HomeUploadOptions): Promise<any> {
  const ok = await validateUploadFile(options.filePath)
  if (!ok) throw new Error('文件格式不允许')
  uni.showLoading({ title: '上传中...' })
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: getServerBaseUrl() + '/homeai' + options.url,
      filePath: options.filePath,
      name: options.name || 'file',
      formData: options.formData,
      header: { 'X-Access-Token': getToken() || '' },
      success: (res) => {
        uni.hideLoading()
        try {
          const data = JSON.parse(res.data)
          if (data.success) resolve(data.result)
          else reject(new Error(data.message || '上传失败'))
        } catch (e) {
          reject(e)
        }
      },
      fail: (err) => {
        uni.hideLoading()
        reject(err)
      },
    })
  })
}

export function chooseAndUpload(options: {
  url: string
  formData?: Record<string, string>
  sourceType?: ('album' | 'camera')[]
}) {
  const { pickImages } = useHomeaiFilePick()
  pickImages({ count: 1, sourceType: options.sourceType || ['album', 'camera'] }).then(async (files) => {
    if (!files[0]) return
    try {
      await homeUpload({ url: options.url, filePath: files[0].path, formData: options.formData })
      uni.showToast({ title: '上传成功', icon: 'success' })
    } catch (e: any) {
      uni.showToast({ title: e.message || '上传失败', icon: 'none' })
    }
  })
}
