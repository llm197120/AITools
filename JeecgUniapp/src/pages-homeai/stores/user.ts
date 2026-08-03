import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { get as getApi } from '../api/request'

export const useUserStore = defineStore('homeai-user', () => {
  const userInfo = ref<any>(null)
  const token = ref<string>('')
  const isLogin = computed(() => !!token.value)

  async function login(code: string) {
    const result = await getApi('/user/login', { code })
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
