/**
 * 系统分享：Capacitor 走 Share 插件，旧 APP-PLUS 走 shareWithSystem，失败由调用方复制兜底。
 */
import { isCapacitorNative } from './runtime'

export async function shareText(summary: string, title?: string): Promise<boolean> {
  if (!summary) return false
  if (isCapacitorNative()) {
    try {
      const { Share } = await import('@capacitor/share')
      await Share.share({ title: title || '分享', text: summary, dialogTitle: title || '分享' })
      return true
    } catch {
      return false
    }
  }
  // #ifdef APP-PLUS
  return new Promise((resolve) => {
    uni.shareWithSystem({
      type: 'text',
      summary,
      success: () => resolve(true),
      fail: () => resolve(false),
    })
  })
  // #endif
  // #ifndef APP-PLUS
  // eslint-disable-next-line no-unreachable -- APP-PLUS 构建时上方 #ifdef 块保留
  return false
  // #endif
}
