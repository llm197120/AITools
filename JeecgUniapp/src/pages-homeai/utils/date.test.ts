import { describe, expect, it } from 'vitest'
import { addMonths, localDateStr, localMonthStr, localTimeStr, parseLocalDate, toDateStr, toDateTimeStr } from './date'

describe('localDateStr', () => {
  it('按本地时区格式化为 yyyy-MM-dd', () => {
    expect(localDateStr(new Date(2026, 7, 22, 23, 30))).toBe('2026-08-22')
  })
})

describe('parseLocalDate', () => {
  it('按本地日历解析，不当成 UTC', () => {
    const d = parseLocalDate('2026-08-22')
    expect(d.getFullYear()).toBe(2026)
    expect(d.getMonth()).toBe(7)
    expect(d.getDate()).toBe(22)
  })
})

describe('toDateStr', () => {
  it('已经是日历日则原样返回', () => {
    expect(toDateStr('2026-08-23')).toBe('2026-08-23')
  })

  it('带时区的 ISO 按本地时区换算', () => {
    expect(toDateStr('2026-08-22T16:00:00.000Z')).toBe(localDateStr(new Date('2026-08-22T16:00:00.000Z')))
  })
})

describe('toDateTimeStr', () => {
  it('已是本地时分则截到分钟', () => {
    expect(toDateTimeStr('2026-08-23 21:05:00')).toBe('2026-08-23 21:05')
  })

  it('纯日历日不当成 UTC 午夜', () => {
    expect(toDateTimeStr('2026-08-23')).toBe('2026-08-23 00:00')
  })

  it('带时区的 ISO 按本地时区换算', () => {
    const iso = '2026-08-22T16:00:00.000Z'
    const d = new Date(iso)
    expect(toDateTimeStr(iso)).toBe(`${localDateStr(d)} ${localTimeStr(d)}`)
  })
})

describe('addMonths', () => {
  it('跨年前进一个月', () => {
    expect(addMonths('2026-12', 1)).toBe('2027-01')
  })

  it('回退一个月', () => {
    expect(addMonths('2026-01', -1)).toBe('2025-12')
  })

  it('与 localMonthStr 一致', () => {
    expect(addMonths('2026-08', 0)).toBe(localMonthStr(new Date(2026, 7, 1)))
  })
})
