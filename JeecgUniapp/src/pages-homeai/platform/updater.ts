/**
 * APP 启动更新：服务端指定 apk 覆盖安装，或 Capacitor 热更新 H5 zip。
 * 比较规则：仅当服务端 versionCode 大于本地 Web 版本才更新（避免回退死循环）。
 */
import { getServerBaseUrl } from '../api/request'
import { isCapacitorNative, isStandaloneApp } from './runtime'

export const WEB_VERSION_KEY = 'homeai_web_version'

export type UpdateAction = 'none' | 'apk' | 'resource'

export interface AppVersionInfo {
  versionName?: string
  versionCode?: number
  updateMode?: string
  forceUpdate?: boolean
  apkUrl?: string
  resourceUrl?: string
  apkSha256?: string
  resourceSha256?: string
  minShellCode?: number
  changelog?: string
  enabled?: boolean
}

export function resolveUpdateAction(opts: {
  enabled: boolean
  serverCode: number
  localWebCode: number
  shellCode: number
  updateMode?: string
  minShellCode: number
}): UpdateAction {
  if (!opts.enabled || opts.serverCode <= opts.localWebCode) return 'none'
  const mode = (opts.updateMode || 'apk').toLowerCase()
  if (mode === 'resource' && opts.shellCode >= opts.minShellCode) return 'resource'
  return 'apk'
}

async function fetchPublicVersion(): Promise<AppVersionInfo | null> {
  return new Promise((resolve) => {
    uni.request({
      url: `${getServerBaseUrl()}/homeai/app/version`,
      method: 'GET',
      timeout: 8000,
      success: (res) => {
        const data = res.data as any
        if (data?.success && data.result) {
          resolve(data.result as AppVersionInfo)
          return
        }
        resolve(null)
      },
      fail: () => resolve(null),
    })
  })
}

async function readShellCode(): Promise<number> {
  if (isCapacitorNative()) {
    try {
      const { App } = await import('@capacitor/app')
      const info = await App.getInfo()
      const n = Number(info.build)
      return Number.isFinite(n) ? n : 0
    } catch {
      return 0
    }
  }
  // #ifdef APP-PLUS
  try {
    const n = Number((plus as any).runtime.versionCode)
    return Number.isFinite(n) ? n : 0
  } catch {
    return 0
  }
  // #endif
  return 0
}

function readWebCode(shellCode: number): number {
  const stored = Number(uni.getStorageSync(WEB_VERSION_KEY))
  if (Number.isFinite(stored) && stored > 0) {
    return stored
  }
  return shellCode
}

function nativePlugin() {
  return import('@capacitor/core').then(({ registerPlugin }) =>
    registerPlugin<{
      download: (o: { url: string; fileName: string }) => Promise<{ path: string }>
      sha256: (o: { path: string }) => Promise<{ hash: string }>
      unzip: (o: { zipPath: string; destDirName: string }) => Promise<{ path: string }>
      installApk: (o: { path: string }) => Promise<void>
    }>('HomeaiUpdate'),
  )
}

async function verifySha(path: string, expected?: string) {
  if (!expected) return
  const plugin = await nativePlugin()
  const { hash } = await plugin.sha256({ path })
  if (hash.toLowerCase() !== expected.toLowerCase()) {
    throw new Error('安装包校验失败')
  }
}

async function capacitorInstallApk(url: string, sha256: string | undefined, onStatus: (t: string) => void) {
  onStatus('正在下载安装包…')
  const plugin = await nativePlugin()
  const { path } = await plugin.download({ url, fileName: `homeai-${Date.now()}.apk` })
  await verifySha(path, sha256)
  onStatus('正在打开安装…')
  try {
    await plugin.installApk({ path })
  } catch (e: any) {
    const msg = String(e?.message || e || '')
    if (msg.includes('NEED_PERMISSION')) {
      throw new Error('请允许本应用安装未知来源应用后重试')
    }
    throw e instanceof Error ? e : new Error(msg || '安装失败')
  }
}

async function capacitorHotUpdate(url: string, sha256: string | undefined, versionCode: number, onStatus: (t: string) => void) {
  onStatus('正在下载页面更新…')
  const plugin = await nativePlugin()
  const { path: zipPath } = await plugin.download({ url, fileName: `homeai-h5-${versionCode}.zip` })
  await verifySha(zipPath, sha256)
  onStatus('正在应用更新…')
  const { path } = await plugin.unzip({ zipPath, destDirName: String(versionCode) })
  const { WebView } = await import('@capacitor/core')
  await WebView.setServerBasePath({ path })
  await WebView.persistServerBasePath()
  uni.setStorageSync(WEB_VERSION_KEY, String(versionCode))
  onStatus('即将重启…')
  setTimeout(() => {
    if (typeof window !== 'undefined') window.location.reload()
  }, 300)
}

function plusInstallApk(url: string, onStatus: (t: string) => void): Promise<void> {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    onStatus('正在下载安装包…')
    const task = plus.downloader.createDownload(url, { filename: '_doc/update/homeai.apk' }, (d, status) => {
      if (status === 200) {
        onStatus('正在打开安装…')
        plus.runtime.install(
          d.filename,
          { force: true },
          () => resolve(),
          (err) => reject(err || new Error('安装失败')),
        )
        return
      }
      reject(new Error('下载失败'))
    })
    task.start()
    // #endif
    // #ifndef APP-PLUS
    reject(new Error('当前环境不支持安装 APK'))
    // #endif
  })
}

/**
 * 启动页调用。返回 continue 表示可以进登录/首页；updating 表示正在安装或即将重载。
 */
export async function checkAndApplyUpdate(hooks: {
  onStatus: (text: string) => void
  confirm: (info: { versionName: string; changelog: string; force: boolean; action: UpdateAction }) => Promise<boolean>
}): Promise<'continue' | 'updating'> {
  if (!isStandaloneApp()) return 'continue'
  const remote = await fetchPublicVersion()
  if (!remote) return 'continue'

  const shellCode = await readShellCode()
  let webCode = readWebCode(shellCode)
  if (shellCode > webCode) {
    uni.setStorageSync(WEB_VERSION_KEY, String(shellCode))
    webCode = shellCode
  }

  let action = resolveUpdateAction({
    enabled: remote.enabled === true,
    serverCode: Number(remote.versionCode || 0),
    localWebCode: webCode,
    shellCode,
    updateMode: remote.updateMode,
    minShellCode: Number(remote.minShellCode || 0),
  })
  if (action === 'resource' && !remote.resourceUrl) {
    action = remote.apkUrl ? 'apk' : 'none'
  }
  if (action === 'apk' && !remote.apkUrl) {
    action = 'none'
  }
  if (action === 'none') return 'continue'

  const ok = await hooks.confirm({
    versionName: remote.versionName || String(remote.versionCode || ''),
    changelog: remote.changelog || '有新版本可用',
    force: remote.forceUpdate === true,
    action,
  })
  if (!ok) {
    return remote.forceUpdate ? 'updating' : 'continue'
  }

  try {
    if (action === 'resource') {
      if (!isCapacitorNative()) {
        if (remote.apkUrl) {
          await plusInstallApk(remote.apkUrl, hooks.onStatus)
          return 'updating'
        }
        uni.showToast({ title: '请安装新版 APK', icon: 'none' })
        return 'continue'
      }
      await capacitorHotUpdate(remote.resourceUrl as string, remote.resourceSha256, Number(remote.versionCode), hooks.onStatus)
      return 'updating'
    }
    if (isCapacitorNative()) {
      await capacitorInstallApk(remote.apkUrl as string, remote.apkSha256, hooks.onStatus)
    } else {
      await plusInstallApk(remote.apkUrl as string, hooks.onStatus)
    }
    return 'updating'
  } catch (e: any) {
    uni.showToast({ title: e?.message || '更新失败', icon: 'none', duration: 3000 })
    return remote.forceUpdate ? 'updating' : 'continue'
  }
}
