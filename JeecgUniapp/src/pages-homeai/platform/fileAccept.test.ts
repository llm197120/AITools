import { describe, expect, it } from 'vitest'
import { mimeTypesForPick } from './fileAccept'

describe('mimeTypesForPick', () => {
  it('type=all 打开完整文件管理', () => {
    expect(mimeTypesForPick({ type: 'all' })).toEqual(['*/*'])
  })

  it('仅视频扩展名时带上 video/*', () => {
    const mimes = mimeTypesForPick({ extension: ['mp4', 'mov'] })
    expect(mimes[0]).toBe('video/*')
    expect(mimes).toContain('video/mp4')
  })

  it('音频白名单带上 audio/*', () => {
    const mimes = mimeTypesForPick({ extension: ['mp3', 'wav'] })
    expect(mimes[0]).toBe('audio/*')
  })
})
