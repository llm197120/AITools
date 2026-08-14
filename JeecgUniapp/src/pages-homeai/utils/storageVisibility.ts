/**
 * 资料存储可见性：private / family / public
 */
import { storageApi } from '../api/storage'

export type StorageVisibility = 'private' | 'family' | 'public'

export type AssignableFamily = { id: string; name: string }

export type VisibilityPickResult = {
  visibility: StorageVisibility
  familyIds?: string[]
}

const VIS_LABELS = ['仅自己可见（私有）', '家庭可见', '公开（所有人可见）']

export async function loadAssignableFamilies(): Promise<AssignableFamily[]> {
  try {
    return (await storageApi.assignableFamilies()) || []
  } catch {
    return []
  }
}

/** 选择可见性；家庭可见时选择可分配家庭（可多选） */
export function pickStorageVisibility(
  defaultVis: StorageVisibility = 'private',
): Promise<VisibilityPickResult | null> {
  return new Promise((resolve) => {
    uni.showActionSheet({
      itemList: VIS_LABELS,
      success: async (res) => {
        const visibility: StorageVisibility =
          res.tapIndex === 2 ? 'public' : res.tapIndex === 1 ? 'family' : 'private'
        if (visibility !== 'family') {
          resolve({ visibility })
          return
        }
        const families = await loadAssignableFamilies()
        if (families.length === 0) {
          uni.showToast({ title: '暂无可分配家庭', icon: 'none' })
          resolve(null)
          return
        }
        if (families.length === 1) {
          resolve({ visibility, familyIds: [families[0].id] })
          return
        }
        uni.showActionSheet({
          itemList: families.map((f) => f.name),
          success: (r2) => {
            const picked = families[r2.tapIndex]
            if (!picked) {
              resolve(null)
              return
            }
            resolve({ visibility, familyIds: [picked.id] })
          },
          fail: () => resolve(null),
        })
      },
      fail: () => resolve({ visibility: defaultVis }),
    })
  })
}

/** 修改已有资源的可见性 */
export function pickStorageVisibilityChange(
  current?: StorageVisibility,
): Promise<VisibilityPickResult | null> {
  return pickStorageVisibility(current || 'private')
}
