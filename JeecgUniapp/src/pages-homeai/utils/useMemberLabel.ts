/**
 * 家庭成员 userId → 昵称缓存（小程序端）
 * 用于资料等模块展示「谁创建」而不暴露裸 ID。
 */
import { ref } from 'vue'
import { get as getApi } from '../api/request'

const labelMap = ref<Map<string, string>>(new Map())
let loading: Promise<void> | null = null

export function useMemberLabel() {
  async function loadMemberLabels(force = false) {
    if (!force && labelMap.value.size > 0) return
    if (!force && loading) {
      await loading
      return
    }
    loading = (async () => {
      try {
        const list: any[] = (await getApi('/family/members')) || []
        const map = new Map<string, string>()
        for (const m of list) {
          const id = String(m.userId || '')
          if (!id) continue
          map.set(id, m.nickname || m.nickName || m.name || id)
        }
        labelMap.value = map
      } catch {
        // 无家庭或未登录时保持空表，回退文案由调用方处理
        labelMap.value = new Map()
      } finally {
        loading = null
      }
    })()
    await loading
  }

  function resolveMemberLabel(userId?: string | null, fallback = '他人'): string {
    if (!userId) return fallback
    return labelMap.value.get(String(userId)) || fallback
  }

  return { labelMap, loadMemberLabels, resolveMemberLabel }
}
