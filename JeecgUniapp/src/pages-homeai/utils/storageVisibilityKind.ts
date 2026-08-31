/**
 * 资料可见性枚举与 ActionSheet 下标映射（无网络依赖，便于单测）
 */

export type StorageVisibility = 'private' | 'family' | 'public'

/** ActionSheet 下标 → 可见性（0 私有 / 1 家庭 / 2 公开） */
export function visibilityFromTapIndex(tapIndex: number): StorageVisibility {
  return tapIndex === 2 ? 'public' : tapIndex === 1 ? 'family' : 'private'
}
