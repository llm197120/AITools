/**
 * HomeAI 管理端状态色 — 对齐 --hai-admin-* 语义令牌
 * Ant Design Tag/Progress 需要实色字符串，统一从此导出，避免散落 Flat UI hex。
 */
export const HAI_ADMIN = {
  primary: '#1b4f8a',
  danger: '#c45c4a',
  success: '#3e6e58',
  warning: '#d4a017',
  muted: '#8a857c',
  border: '#ece9e2',
} as const;

export function planPriorityColor(priority?: string): string {
  if (priority === 'urgent') return HAI_ADMIN.danger;
  if (priority === 'important') return HAI_ADMIN.warning;
  return HAI_ADMIN.muted;
}

export function recipeDifficultyColor(v: number | string | null | undefined): string {
  const n = Number(v);
  if (!Number.isFinite(n) || n < 1) return HAI_ADMIN.muted;
  if (n <= 2) return HAI_ADMIN.success;
  if (n === 3) return HAI_ADMIN.warning;
  return HAI_ADMIN.danger;
}

/** 配额进度条渐变：正常 / 预警 / 超限 */
export function quotaStrokeColor(pct: number): { '0%': string; '100%': string } {
  if (pct >= 90) return { '0%': HAI_ADMIN.danger, '100%': '#d97a6c' };
  if (pct >= 70) return { '0%': HAI_ADMIN.warning, '100%': '#e0b84a' };
  return { '0%': HAI_ADMIN.success, '100%': '#5a8f76' };
}
