import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { get as getApi, post as postApi } from '../api/request'
import type { AuthResult } from '../platform/auth'
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
    userInfo.value = auth.userInfo
    uni.setStorageSync('homeai_token', auth.token)
    uni.setStorageSync('homeai_user', JSON.stringify(auth.userInfo))
    // 登录成功后调度今日计划本地提醒（APP 端；小程序走微信订阅消息，内部为空实现）
    scheduleTodayPlanReminds()
  }

  async function refreshUserInfo() {
    try {
      userInfo.value = await getApi('/user/info')
      uni.setStorageSync('homeai_user', JSON.stringify(userInfo.value))
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('homeai_token')
    uni.removeStorageSync('homeai_user')
  }

  return { userInfo, token, isLogin, login, setAuth, refreshUserInfo, logout }
})
