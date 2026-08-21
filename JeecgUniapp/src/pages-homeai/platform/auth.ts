/**
 * 平台登录适配层（Android app-plus 手机号+密码登录）
 *
 * 供 APP 端（app-plus）使用账号密码登录，微信小程序端仍走原有微信登录逻辑，不受影响。
 * 后端接口 /homeai/auth/login/password 与 /homeai/auth/register 由后端并行开发中，
 * 此处仅按约定契约调用，无需等待后端就绪。
 */
import { post } from '../api/request'

/** 登录/注册统一返回结构（与后端契约一致） */
export interface AuthResult {
  token: string
  refreshToken: string
  /** 是否新注册用户（登录接口通常为 false） */
  isNewUser: boolean
  /** 用户信息（字段以后端返回为准，避免过度约束） */
  userInfo: any
}

/**
 * 手机号 + 密码登录
 * @param phone 手机号
 * @param password 密码
 */
export function loginByPhone(phone: string, password: string): Promise<AuthResult> {
  return post<AuthResult>('/auth/login/password', { data: { phone, password } })
}

/**
 * 手机号 + 密码注册（注册成功即自动登录）
 * @param phone 手机号
 * @param password 密码
 * @param nickname 昵称（可选）
 */
export function registerByPhone(
  phone: string,
  password: string,
  nickname?: string
): Promise<AuthResult> {
  return post<AuthResult>('/auth/register', { data: { phone, password, nickname } })
}

/**
 * 已登录修改密码
 */
export function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  return post('/auth/change-password', { data: { oldPassword, newPassword } })
}

/**
 * 退出登录（服务端作废 Redis token，需携带当前 JWT）
 */
export function logout(): Promise<void> {
  return post('/auth/logout')
}

/**
 * 校验中国大陆手机号格式
 * @param phone 手机号
 */
export function isPhoneValid(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone)
}
