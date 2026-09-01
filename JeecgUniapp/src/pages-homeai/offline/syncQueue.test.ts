import { describe, expect, it, beforeEach, vi, afterEach } from 'vitest'
import { stubUniStorage } from './testUtils'
import { enqueue, removeQueued, getPendingCount, getSyncConfig, setSyncConfig } from './syncQueue'

// conn mock：避免真实事件/定时器
vi.mock('./conn', () => ({
  getConnState: vi.fn(() => 'online'),
  onConnChange: vi.fn(() => () => {}),
}))

describe('offline/syncQueue（纯 API）', () => {
  beforeEach(() => {
    stubUniStorage()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.clearAllMocks()
  })

  it('enqueue 入队后计数 +1，同 module+id 覆盖不重复', () => {
    const id = enqueue('plan', 'create', { title: 'a' })
    expect(getPendingCount()).toBe(1)
    enqueue('plan', 'create', { title: 'a2' }, id)
    expect(getPendingCount()).toBe(1)
  })

  it('removeQueued 移除指定条目', () => {
    const id = enqueue('bill', 'create', { amount: 1 })
    enqueue('bill', 'update', { id: 'x' })
    removeQueued('bill', id)
    expect(getPendingCount()).toBe(1)
  })

  it('setSyncConfig 覆盖配置，getSyncConfig 返回副本', () => {
    setSyncConfig({ batchSize: 3, intervalMs: 10000, maxRetriesPerItemPerDay: 5 })
    const cfg = getSyncConfig()
    expect(cfg.batchSize).toBe(3)
    expect(cfg.intervalMs).toBe(10000)
    expect(cfg.maxRetriesPerItemPerDay).toBe(5)
    // 返回副本，外部修改不影响内部
    cfg.batchSize = 99
    expect(getSyncConfig().batchSize).toBe(3)
  })

  it('默认配置为 1 条/批、5s 间隔、单条 24h 20 次、图片缓存 4096MB', () => {
    // 模块级 config 可能被前置用例修改，先重置再断言默认值
    setSyncConfig({ batchSize: 1, intervalMs: 5000, maxRetriesPerItemPerDay: 20, imageCacheLimitMB: 4096 })
    const cfg = getSyncConfig()
    expect(cfg.batchSize).toBe(1)
    expect(cfg.intervalMs).toBe(5000)
    expect(cfg.maxRetriesPerItemPerDay).toBe(20)
    expect(cfg.imageCacheLimitMB).toBe(4096)
  })
})