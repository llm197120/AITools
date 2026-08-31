import { describe, expect, it } from 'vitest'
import { visibilityFromTapIndex } from './storageVisibilityKind'

describe('visibilityFromTapIndex', () => {
  it('0 私有 / 1 家庭 / 2 公开', () => {
    expect(visibilityFromTapIndex(0)).toBe('private')
    expect(visibilityFromTapIndex(1)).toBe('family')
    expect(visibilityFromTapIndex(2)).toBe('public')
  })

  it('未知下标回落私有', () => {
    expect(visibilityFromTapIndex(-1)).toBe('private')
    expect(visibilityFromTapIndex(9)).toBe('private')
  })
})
