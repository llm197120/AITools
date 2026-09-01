import { describe, expect, it, beforeEach, vi, afterEach } from 'vitest'
import { maxUpdatedAt, readCache, writeCache, clearCache } from './cache'

/** 内存版 uni storage，供 cache/syncQueue 等依赖 uni 的模块测试 */
export function stubUniStorage() {
  const store = new Map<string, any>()
  const uni = {
    getStorageSync: (k: string) => store.get(k),
    setStorageSync: (k: string, v: any) => {
      store.set(k, v)
    },
    removeStorageSync: (k: string) => {
      store.delete(k)
    },
    getStorageInfoSync: () => ({ keys: [...store.keys()] }),
  }
  vi.stubGlobal('uni', uni)
  return store
}

describe('offline/cache', () => {
  beforeEach(() => {
    stubUniStorage()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  describe('maxUpdatedAt', () => {
    it('取记录中最大 updateTime', () => {
      const records = [
        { id: 1, updateTime: '2026-08-30 10:00:00' },
        { id: 2, updateTime: '2026-08-31 09:00:00' },
        { id: 3, updateTime: '2026-08-29 08:00:00' },
      ]
      expect(maxUpdatedAt(records)).toBe('2026-08-31 09:00:00')
    })

    it('兼容 updatedAt / createTime，空列表返回空串', () => {
      expect(maxUpdatedAt([{ updatedAt: '2026-09-01' }, { createTime: '2026-09-02' }])).toBe(
        '2026-09-02',
      )
      expect(maxUpdatedAt([])).toBe('')
      expect(maxUpdatedAt(null)).toBe('')
    })
  })

  describe('readCache/writeCache', () => {
    it('写入后可读回，含版本与时间戳', () => {
      writeCache('plan', 'byDate:2026-09-01', [{ id: 'a' }], 'v1')
      const entry = readCache('plan', 'byDate:2026-09-01')
      expect(entry?.data).toEqual([{ id: 'a' }])
      expect(entry?.version).toBe('v1')
      expect(typeof entry?.fetchedAt).toBe('number')
    })

    it('不同 scope 隔离，clearCache 按模块清除', () => {
      writeCache('plan', 'a', 1, 'v')
      writeCache('bill', 'a', 2, 'v')
      expect(readCache('plan', 'a')?.data).toBe(1)
      clearCache('plan')
      expect(readCache('plan', 'a')).toBeNull()
      expect(readCache('bill', 'a')?.data).toBe(2)
    })

    it('未写入时返回 null', () => {
      expect(readCache('plan', 'nope')).toBeNull()
    })
  })
})