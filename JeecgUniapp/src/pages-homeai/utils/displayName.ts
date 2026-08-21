/**
 * 用户展示名：去掉历史「微信用户」兜底
 */
export function displayNickname(user: any): string {
  if (!user) return '未登录'
  const n = String(user.nickname || '').trim()
  if (n && n !== '微信用户') return n
  const phone = String(user.phone || '')
  if (phone.length >= 4) return `用户${phone.slice(-4)}`
  return '用户'
}
