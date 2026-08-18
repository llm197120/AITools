/**
 * 资料存储：下载 / 保存到本地（小程序 + Android APP）
 * 平台差异（相册权限、保存/下载/打开）统一走 platform/download.ts 适配层
 */
import { storageApi } from '../api/storage'
import { getFileExt, isImageExt, isVideoExt } from './filePreview'
import {
  downloadToTemp,
  openLocalDocument,
  requestAlbumPermission,
  saveImageToAlbum,
  saveVideoToAlbum,
} from '../platform/download'

export type DownloadFileInput = {
  id?: string
  fileUrl?: string
  originalName?: string
  extension?: string
}

/** 获取最新可下载 URL（私有 OSS 预签名可能过期） */
async function resolveDownloadUrl(input: DownloadFileInput): Promise<string> {
  if (input.id) {
    try {
      const url = await storageApi.fileAccessUrl(input.id)
      if (url) return url
    } catch {
      // 回退 fileUrl
    }
  }
  return input.fileUrl || ''
}

/**
 * 请求相册写入权限（保存图片/视频到本地前调用）
 * - 微信小程序：scope.writePhotosAlbum 授权
 * - Android APP：运行时权限申请（13+ 媒体权限 / 更早 WRITE_EXTERNAL_STORAGE），拒绝时引导去设置
 */
function requestAlbumAuth(): Promise<boolean> {
  // #ifdef APP-PLUS
  // Android：走 platform/download.ts 的运行时权限申请
  return requestAlbumPermission()
  // #endif

  // #ifdef MP-WEIXIN
  return new Promise((resolve) => {
    uni.getSetting({
      success: (setting) => {
        if (setting.authSetting['scope.writePhotosAlbum']) {
          resolve(true)
          return
        }
        uni.authorize({
          scope: 'scope.writePhotosAlbum',
          success: () => resolve(true),
          fail: () => {
            uni.showModal({
              title: '需要相册权限',
              content: '保存图片/视频到本地需要您授权访问相册',
              confirmText: '去设置',
              success: (res) => {
                if (res.confirm) {
                  uni.openSetting({
                    success: (s) => resolve(!!s.authSetting['scope.writePhotosAlbum']),
                    fail: () => resolve(false),
                  })
                } else {
                  resolve(false)
                }
              },
            })
          },
        })
      },
      fail: () => resolve(false),
    })
  })
  // #endif
}

/**
 * 下载资料文件
 * - 图片/视频：保存到系统相册
 * - 其他：下载后用系统文档菜单打开（可转发/另存）
 */
export async function downloadStorageFile(input: DownloadFileInput) {
  const ext = (input.extension || getFileExt(input.originalName || input.fileUrl || '')).toLowerCase()
  const url = await resolveDownloadUrl(input)
  if (!url) {
    uni.showToast({ title: '无法获取下载地址', icon: 'none' })
    return
  }

  uni.showLoading({ title: '下载中...', mask: true })
  try {
    const tempPath = await downloadToTemp(url)
    uni.hideLoading()

    if (isImageExt(ext)) {
      const ok = await requestAlbumAuth()
      if (!ok) return
      uni.showLoading({ title: '保存中...', mask: true })
      await saveImageToAlbum(tempPath)
      uni.hideLoading()
      uni.showToast({ title: '已保存到相册', icon: 'success' })
      return
    }

    if (isVideoExt(ext)) {
      const ok = await requestAlbumAuth()
      if (!ok) return
      uni.showLoading({ title: '保存中...', mask: true })
      await saveVideoToAlbum(tempPath)
      uni.hideLoading()
      uni.showToast({ title: '已保存到相册', icon: 'success' })
      return
    }

    openLocalDocument(tempPath)
  } catch (e) {
    uni.hideLoading()
    console.error('资料下载失败', e)
    uni.showToast({ title: '下载失败，请稍后重试', icon: 'none' })
  }
}

/** 图片专用：保存到相册 */
export async function saveStorageImage(input: DownloadFileInput) {
  await downloadStorageFile(input)
}
