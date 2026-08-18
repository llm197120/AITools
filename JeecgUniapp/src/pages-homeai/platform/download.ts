/**
 * HomeAI 平台下载/保存适配层：小程序走 uni API，Android APP 走 plus 原生能力
 *
 * - 保存图片/视频到相册：APP 端 plus.gallery.save + Android 运行时权限申请
 *   （Android 13+ 申请 READ_MEDIA_IMAGES/READ_MEDIA_VIDEO，更早版本申请 WRITE_EXTERNAL_STORAGE）
 * - 下载临时文件 / 打开本地文档：APP 端 plus.downloader / plus.runtime.openFile，小程序走 uni API
 */

/** Android 13+（API 33）媒体权限模型：以 READ_MEDIA_* 替代写外部存储 */
const ANDROID_13_PERMISSIONS = [
  'android.permission.READ_MEDIA_IMAGES',
  'android.permission.READ_MEDIA_VIDEO',
]
/** Android 13 以下：保存到相册需要写外部存储权限 */
const ANDROID_LEGACY_PERMISSIONS = ['android.permission.WRITE_EXTERNAL_STORAGE']

/**
 * plus.android.runtimePermissions 未收录于 @dcloudio/types 3.4.x 类型定义（运行时真实存在），
 * 此处补充声明以便直接检查权限状态，避免 as any
 */
interface AndroidApiWithRuntimePermissions {
  runtimePermissions: Record<string, string>
}

/** 判断当前是否 Android 13+（按系统版本号主版本判断，仅 APP 端调用） */
function isAndroid13Plus(): boolean {
  // #ifdef APP-PLUS
  const version = String(plus.os.version || '')
  const major = parseInt(version.split('.')[0], 10)
  return !Number.isNaN(major) && major >= 13
  // #endif
  // #ifndef APP-PLUS
  return false
  // #endif
}

/** 权限被拒引导：说明用途并跳转系统设置页 */
function showPermissionGuide() {
  uni.showModal({
    title: '需要相册权限',
    content: '保存图片/视频到本地需要相册访问权限，请在系统设置中开启后重试',
    confirmText: '去设置',
    cancelText: '取消',
    success: (res) => {
      if (!res.confirm) return
      // #ifdef APP-PLUS
      plus.runtime.openURL('app-settings:')
      // #endif
    },
  })
}

/**
 * 申请 Android 相册保存权限（APP 端）
 * - Android 13+：READ_MEDIA_IMAGES + READ_MEDIA_VIDEO
 * - Android 13 以下：WRITE_EXTERNAL_STORAGE
 * 已授权返回 true；被拒绝/永久拒绝会弹窗引导去系统设置，返回 false
 */
export function requestAlbumPermission(): Promise<boolean> {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    const permissions = isAndroid13Plus() ? ANDROID_13_PERMISSIONS : ANDROID_LEGACY_PERMISSIONS

    // 先检查运行时权限状态，已全部授权则直接放行
    const api = plus.android as PlusAndroid & AndroidApiWithRuntimePermissions
    const allGranted = permissions.every((p) => api.runtimePermissions[p] === 'granted')
    if (allGranted) {
      resolve(true)
      return
    }

    plus.android.requestPermissions(
      permissions,
      (result) => {
        const r = result as { granted?: string[]; deniedPresent?: string[]; deniedAlways?: string[] }
        // 永久拒绝：系统不再弹窗，必须引导用户去设置手动开启
        if (r.deniedAlways && r.deniedAlways.length > 0) {
          showPermissionGuide()
          resolve(false)
          return
        }
        resolve(permissions.every((p) => (r.granted || []).includes(p)))
      },
      () => {
        // 申请失败（如系统拦截），引导用户去设置手动开启
        showPermissionGuide()
        resolve(false)
      },
    )
    // #endif

    // #ifndef APP-PLUS
    // 非 APP 端（小程序/H5）：无 Android 权限模型，直接放行
    resolve(true)
    // #endif
  })
}

/**
 * 保存图片到相册
 * - APP：plus.gallery.save（需先申请相册权限）
 * - 小程序：uni.saveImageToPhotosAlbum
 */
export function saveImageToAlbum(filePath: string): Promise<void> {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    plus.gallery.save(
      filePath,
      () => resolve(),
      (e) => reject(new Error((e && e.message) || '保存失败')),
    )
    // #endif

    // #ifdef MP-WEIXIN
    uni.saveImageToPhotosAlbum({
      filePath,
      success: () => resolve(),
      fail: (err) => reject(err),
    })
    // #endif
  })
}

/**
 * 保存视频到相册
 * - APP：plus.gallery.save（需先申请相册权限）
 * - 小程序：uni.saveVideoToPhotosAlbum
 */
export function saveVideoToAlbum(filePath: string): Promise<void> {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    plus.gallery.save(
      filePath,
      () => resolve(),
      (e) => reject(new Error((e && e.message) || '保存失败')),
    )
    // #endif

    // #ifdef MP-WEIXIN
    uni.saveVideoToPhotosAlbum({
      filePath,
      success: () => resolve(),
      fail: (err) => reject(err),
    })
    // #endif
  })
}

/**
 * 下载文件到本地临时目录
 * - APP：plus.downloader 原生下载（保存到 _downloads/ 目录）
 * - 小程序：uni.downloadFile
 */
export function downloadToTemp(url: string): Promise<string> {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    const task = plus.downloader.createDownload(
      url,
      { filename: '_downloads/' },
      (download, status) => {
        if (status === 200 && download.filename) {
          resolve(download.filename)
        } else {
          reject(new Error('下载失败'))
        }
      },
    )
    task.start()
    // #endif

    // #ifdef MP-WEIXIN
    uni.downloadFile({
      url,
      success: (res) => {
        if (res.statusCode === 200 && res.tempFilePath) {
          resolve(res.tempFilePath)
        } else {
          reject(new Error('下载失败'))
        }
      },
      fail: (err) => reject(err),
    })
    // #endif
  })
}

/**
 * 打开本地文档（可转发/另存）
 * - APP：plus.runtime.openFile 原生打开
 * - 小程序：uni.openDocument
 */
export function openLocalDocument(filePath: string) {
  // #ifdef APP-PLUS
  plus.runtime.openFile(
    filePath,
    {},
    () => uni.showToast({ title: '无法打开该文件', icon: 'none' }),
  )
  // #endif

  // #ifdef MP-WEIXIN
  uni.openDocument({
    filePath,
    showMenu: true,
    fail: () => uni.showToast({ title: '无法打开该文件', icon: 'none' }),
  })
  // #endif
}