import { describe, expect, it, beforeEach, vi, afterEach } from 'vitest'
import { normalizeImageKey, isIndexedDbSupported } from './imageCache'
import { stubUniStorage } from './testUtils'

describe('offline/imageCache', () => {
  beforeEach(() => {
    stubUniStorage()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  describe('normalizeImageKey', () => {
    it('去掉 OSS 预签名参数（Expires/Signature），保留路径', () => {
      const url =
        'https://llmaitools.oss-cn-beijing.aliyuncs.com/homeai/app-version/resource-1.zip?Expires=1788174493&OSSAccessKeyId=LTAI&Signature=abc%3D'
      expect(normalizeImageKey(url)).toBe(
        'https://llmaitools.oss-cn-beijing.aliyuncs.com/homeai/app-version/resource-1.zip',
      )
    })

    it('无参数 URL 原样', () => {
      expect(normalizeImageKey('https://x.com/a.png')).toBe('https://x.com/a.png')
    })

    it('blob/data/capacitor 原样（不误伤本地资源）', () => {
      expect(normalizeImageKey('blob:http://localhost/abc')).toBe('blob:http://localhost/abc')
      expect(normalizeImageKey('data:image/png;base64,xxx')).toBe('data:image/png;base64,xxx')
    })

    it('空串返回空串', () => {
      expect(normalizeImageKey('')).toBe('')
    })
  })

  it('IndexedDB 可用性检测不抛错', () => {
    expect(typeof isIndexedDbSupported()).toBe('boolean')
  })
})