import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { get as getApi, post as postApi, put as putApi } from '../api/request'
import { logout as logoutApi, type AuthResult } from '../platform/auth'
import { scheduleTodayPlanReminds } from '../utils/push'

export const useUserStore = defineStore('homeai-user', () => {
  const cachedUser = uni.getStorageSync('homeai_user')
  let parsedUser: any = null
  if (cachedUser) {
    try {
      parsedUser = JSON.parse(cachedUser)
    } catch {
      parsedUser = null
    }
  }
  const userInfo = ref<any>(parsedUser)
  const token = ref<string>(uni.getStorageSync('homeai_token') || '')
  const isLogin = computed(() => !!token.value)

  async function login(code: string) {
    const result = await postApi('/user/login', { params: { code } })
    token.value = result.token
    userInfo.value = result.userInfo
    uni.setStorageSync('homeai_token', result.token)
    uni.setStorageSync('homeai_user', JSON.stringify(result.userInfo))
    // 登录成功后调度今日计划本地提醒（APP 端；小程序走微信订阅消息，内部为空实现）
    scheduleTodayPlanReminds()
    return result
  }

  /**
   * 手机号+密码登录/注册成功后写入登录态（与 login(code) 保持一致的存储约定）
   * @param auth 平台登录适配层返回的 AuthResult
   */
  function setAuth(auth: AuthResult) {
    token.value = auth.token
    const safeUser = auth.userInfo ? { ...auth.userInfo } : null
    if (safeUser) {
      delete safeUser.password
      delete safeUser.salt
    }
    userInfo.value = safeUser
    uni.setStorageSync('homeai_token', auth.token)
    uni.setStorageSync('homeai_user', JSON.stringify(safeUser))
    // 登录成功后调度今日计划本地提醒（APP 端；小程序走微信订阅消息，内部为空实现）
    scheduleTodayPlanReminds()
  }

  async function refreshUserInfo() {
    try {
      const info = await getApi('/user/info')
      if (info) {
        delete info.password
        delete info.salt
      }
      userInfo.value = info
      uni.setStorageSync('homeai_user', JSON.stringify(userInfo.value))
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
  }

  async function updateProfile(patch: { nickname?: string; avatarUrl?: string }) {
    const info = await putApi('/user/info', patch)
    if (info) {
      delete info.password
      delete info.salt
    }
    userInfo.value = info
    uni.setStorageSync('homeai_user', JSON.stringify(info))
    return info
  }

  /** 仅清本地登录态（401 过期时不要再打服务端，避免循环） */
  function clearLocalSession() {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('homeai_token')
    uni.removeStorageSync('homeai_user')
  }

  async function logout() {
    try {
      if (token.value) {
        await logoutApi()
      }
    } catch {
      // 服务端作废失败仍清本地
    }
    clearLocalSession()
  }

  return { userInfo, token, isLogin, login, setAuth, refreshUserInfo, updateProfile, logout, clearLocalSession }
})
