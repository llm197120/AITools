/**
 * 本地时区日期工具（避免 toISOString() 的 UTC 偏移导致日期/月份错一天）
 */

/** 本地时区日期字符串 yyyy-MM-dd */
export function localDateStr(d: Date = new Date()): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 解析 yyyy-MM-dd 为本地 Date，避免 `new Date('yyyy-MM-dd')` 被当成 UTC */
export function parseLocalDate(dateStr: string): Date {
  const [y, m, d] = (dateStr || '').split('-').map(Number)
  if (!y || !m || !d) return new Date()
  return new Date(y, m - 1, d)
}

/** 本地时区时分字符串 HH:mm */
export function localTimeStr(d: Date = new Date()): string {
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

/** 将 HH:mm 落到指定日期的本地 Date */
export function parseLocalTime(timeStr: string, base: Date = new Date()): Date {
  const [h, m] = (timeStr || '').split(':').map(Number)
  return new Date(
    base.getFullYear(),
    base.getMonth(),
    base.getDate(),
    Number.isFinite(h) ? h : 0,
    Number.isFinite(m) ? m : 0,
    0,
    0,
  )
}
export function localMonthStr(d: Date = new Date()): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

/** 在 yyyy-MM 上增减月份，避免 UTC 偏移 */
export function addMonths(monthStr: string, delta: number): string {
  const [y, m] = monthStr.split('-').map(Number)
  return localMonthStr(new Date(y, (m || 1) - 1 + delta, 1))
}
