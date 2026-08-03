/**
 * Token 管理
 */

const TOKEN_KEY = 'homeai_token'
const USER_KEY = 'homeai_user'

export function getToken(): string | null {
  return uni.getStorageSync(TOKEN_KEY) || null
}

export function setToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function removeToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

export function getUser(): any | null {
  const raw = uni.getStorageSync(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export function setUser(user: any) {
  uni.setStorageSync(USER_KEY, JSON.stringify(user))
}

export function removeUser() {
  uni.removeStorageSync(USER_KEY)
}

export function clearAuth() {
  removeToken()
  removeUser()
}
