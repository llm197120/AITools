import { vi } from 'vitest'

/** 内存版 uni storage，供离线层测试使用（避免依赖真实 uni 运行时） */
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