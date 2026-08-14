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

/** 本地时区年月字符串 yyyy-MM */
export function localMonthStr(d: Date = new Date()): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}
