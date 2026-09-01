import { describe, expect, it, beforeEach, vi, afterEach } from 'vitest'
import { stubUniStorage } from './testUtils'
import { readCache } from './cache'
import { readList, readDetail, mutate } from './dataAccess'

const getConnState = vi.fn<() => 'online' | 'offline' | 'unknown'>(() => 'online')
const enqueue = vi.fn()

vi.mock('./conn', () => ({
  getConnState: () => getConnState(),
  onConnChange: vi.fn(() => () => {}),
}))

vi.mock('./syncQueue', () => ({
  enqueue: (...a: any[]) => enqueue(...a),
}))

describe('offline/dataAccess.readList', () => {
  beforeEach(() => {
    stubUniStorage()
    getConnState.mockReturnValue('online')
    enqueue.mockClear()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.clearAllMocks()
  })

  it('离线且有缓存：返回缓存并标记 offline', async () => {
    const { writeCache } = await import('./cache')
    writeCache('plan', 'byDate:2026-09-01', [{ id: 'a' }], 'v1')
    getConnState.mockReturnValue('offline')
    const res = await readList('plan', 'byDate:2026-09-01', async () => {
      throw new Error('no network')
    })
    expect(res.offline).toBe(true)
    expect(res.fromCache).toBe(true)
    expect(res.data).toEqual([{ id: 'a' }])
  })

  it('离线且无缓存：返回空数组', async () => {
    getConnState.mockReturnValue('offline')
    const res = await readList('plan', 'nope', async () => [])
    expect(res.offline).toBe(true)
    expect(res.data).toEqual([])
  })

  it('在线且服务端版本无变化：沿用缓存不抖动', async () => {
    const { writeCache } = await import('./cache')
    writeCache('plan', 'byDate:2026-09-01', [{ id: 'a', updateTime: '2026-09-01 10:00:00' }], '2026-09-01 10:00:00')
    let fetchCount = 0
    const res = await readList(
      'plan',
      'byDate:2026-09-01',
      async () => {
        fetchCount++
        return [{ id: 'a', updateTime: '2026-09-01 10:00:00' }]
      },
      (d: any) => d,
    )
    expect(fetchCount).toBe(1)
    expect(res.fromCache).toBe(true)
    expect(res.offline).toBe(false)
  })

  it('在线且版本有变化：返回新数据并更新缓存', async () => {
    const res = await readList(
      'plan',
      'byDate:2026-09-02',
      async () => [{ id: 'b', updateTime: '2026-09-02 10:00:00' }],
      (d: any) => d,
    )
    expect(res.fromCache).toBe(false)
    expect(res.data).toEqual([{ id: 'b', updateTime: '2026-09-02 10:00:00' }])
    expect(readCache('plan', 'byDate:2026-09-02')?.version).toBe('2026-09-02 10:00:00')
  })

  it('在线请求失败且无缓存：抛错', async () => {
    await expect(
      readList('plan', 'boom', async () => {
        throw new Error('network')
      }),
    ).rejects.toThrow()
  })
})

describe('offline/dataAccess.mutate', () => {
  beforeEach(() => {
    stubUniStorage()
    getConnState.mockReturnValue('online')
    enqueue.mockClear()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.clearAllMocks()
  })

  it('离线：直接入队（乐观）', async () => {
    getConnState.mockReturnValue('offline')
    const res = await mutate('plan', 'toggle', { instanceId: 'x' }, async () => true)
    expect(res.queued).toBe(true)
    expect(enqueue).toHaveBeenCalled()
  })

  it('在线成功：直发并返回服务端结果', async () => {
    const res = await mutate(
      'bill',
      'create',
      { data: { amount: 1 } },
      async () => ({ id: 'new-1' }),
    )
    expect(res.queued).toBe(false)
    expect((res.result as any).id).toBe('new-1')
    expect(enqueue).not.toHaveBeenCalled()
  })

  it('在线 5xx/无状态（网络波动）：自动入队兜底', async () => {
    const res = await mutate('bill', 'create', { data: {} }, async () => {
      const e: any = new Error('bad gateway')
      e.status = 502
      throw e
    })
    expect(res.queued).toBe(true)
    expect(enqueue).toHaveBeenCalled()
  })

  it('在线 4xx 业务失败：直接抛出不入队', async () => {
    await expect(
      mutate('bill', 'create', { data: {} }, async () => {
        const e: any = new Error('格式错误')
        e.status = 400
        throw e
      }),
    ).rejects.toThrow('格式错误')
    expect(enqueue).not.toHaveBeenCalled()
  })
})

describe('offline/dataAccess.readDetail', () => {
  beforeEach(() => {
    stubUniStorage()
    getConnState.mockReturnValue('online')
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.clearAllMocks()
  })

  it('离线且有详情缓存：返回缓存', async () => {
    const { writeCache } = await import('./cache')
    writeCache('recipe', 'detail:r1', { id: 'r1', name: '番茄蛋' }, 'v1')
    getConnState.mockReturnValue('offline')
    const res = await readDetail('recipe', 'r1', async () => {
      throw new Error('off')
    })
    expect(res.data).toEqual({ id: 'r1', name: '番茄蛋' })
    expect(res.offline).toBe(true)
  })

  it('离线且无缓存：抛错', async () => {
    getConnState.mockReturnValue('offline')
    await expect(readDetail('recipe', 'nope', async () => ({}))).rejects.toThrow()
  })
})