/**
 * 菜谱食材用量：展示字符串 ↔ 后端 quantity/unit
 */

/** 将展示用用量字符串解析为后端 quantity + unit */
export function parseAmountToQuantityUnit(amount: string): { quantity?: number; unit?: string } {
  const trimmed = (amount || '').trim()
  if (!trimmed) return {}
  const match = trimmed.match(/^(\d+(?:\.\d+)?)(.*)$/)
  if (!match) {
    return { unit: trimmed }
  }
  const quantity = Number(match[1])
  const unit = (match[2] || '').trim()
  const result: { quantity?: number; unit?: string } = {}
  if (!Number.isNaN(quantity)) {
    result.quantity = quantity
  }
  if (unit) {
    result.unit = unit
  }
  return result
}

/** 将后端 quantity/unit 合成为展示字符串 */
export function formatQuantityUnit(
  quantity?: number | string | null,
  unit?: string | null,
  amountFallback?: string,
): string {
  const hasQuantity = quantity !== undefined && quantity !== null && quantity !== ''
  if (hasQuantity) {
    return `${quantity}${unit || ''}`
  }
  if (unit) {
    return String(unit)
  }
  return amountFallback || ''
}
