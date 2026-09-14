import { describe, expect, it } from 'vitest'
import { withTimeout } from './withTimeout'

describe('withTimeout', () => {
  it('resolves before deadline', async () => {
    await expect(withTimeout(Promise.resolve(1), 200, 'timeout')).resolves.toBe(1)
  })

  it('rejects when hung', async () => {
    await expect(withTimeout(new Promise(() => undefined), 20, '下载超时')).rejects.toThrow('下载超时')
  })
})
