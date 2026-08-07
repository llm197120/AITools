import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { get as getApi, post as postApi } from '../api/request'

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
    return result
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

  return { userInfo, token, isLogin, login, refreshUserInfo, logout }
})
