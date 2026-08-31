import { onPullDownRefresh } from '@dcloudio/uni-app'

/**
 * 主列表下拉刷新：等业务 reload 结束后再停转动画。
 */
export function useHomeaiPullRefresh(reload: () => Promise<unknown> | void) {
  onPullDownRefresh(async () => {
    try {
      await reload()
    } finally {
      uni.stopPullDownRefresh()
    }
  })
}
