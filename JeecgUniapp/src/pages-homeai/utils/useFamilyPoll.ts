import { onHide, onUnload } from '@dcloudio/uni-app'
import { useFamilyStore } from '../stores/family'
import { useUserStore } from '../stores/user'

const FAMILY_POLL_MS = 45000

/** 首页 / 个人中心等 Tab 页停留时轻量刷新家庭信息（被移出家庭时及时清空） */
export function useFamilyPoll() {
  const familyStore = useFamilyStore()
  const userStore = useUserStore()
  let poll: ReturnType<typeof setInterval> | null = null

  function stop() {
    if (poll) {
      clearInterval(poll)
      poll = null
    }
  }

  function start() {
    stop()
    if (!userStore.isLogin) return
    poll = setInterval(() => {
      if (!userStore.isLogin) return
      familyStore.fetchFamilyInfo()
    }, FAMILY_POLL_MS)
  }

  onHide(stop)
  onUnload(stop)

  return { start, stop }
}
