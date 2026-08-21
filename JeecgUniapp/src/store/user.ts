import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUserStore as useHomeaiUserStore } from '@/pages-homeai/stores/user'

const initState = {
  token: '',
  userid: '',
  username: '',
  realname: '',
  welcome: '',
  avatar: '',
  tenantId: 0,
  phone: '',
  email: '',
  sex: 1,
  birthday: '',
  loginTenantId: 0,
  // 本地存储时间
  localStorageTime: 0,
  // 组织编码名称
  orgCodeTxt: '',
}

/** 读取 HomeAI token，避免 Pinia 未就绪时抛错 */
function getHomeaiToken(): string {
  try {
    return useHomeaiUserStore().token || uni.getStorageSync('homeai_token') || ''
  } catch {
    return uni.getStorageSync('homeai_token') || ''
  }
}

/**
 * Jeecg 遗留 user store：token / isLogined 代理到 HomeAI store，
 * 避免 router、http、request 等仍读 @/store/user 时鉴权状态不一致。
 */
export const useUserStore = defineStore(
  'user',
  () => {
    // 仅此 ref 参与持久化；对外 userInfo 为带 HomeAI token 的计算属性
    const userInfoRaw = ref<IUserInfo>({ ...initState })

    /** 对外 userInfo：token 优先取 HomeAI */
    const userInfo = computed(() => {
      const homeaiToken = getHomeaiToken()
      if (homeaiToken) {
        return { ...userInfoRaw.value, token: homeaiToken }
      }
      return userInfoRaw.value
    })

    const setUserInfo = (val: IUserInfo) => {
      if (val?.loginTenantId) {
        val.tenantId = val.loginTenantId
      }
      userInfoRaw.value = val
    }
    const clearUserInfo = () => {
      userInfoRaw.value = { ...initState }
      // 同步清理 HomeAI 登录态
      try {
        useHomeaiUserStore().clearLocalSession()
      } catch {
        // ignore
      }
    }
    const getUserInfo = () => {
      return userInfo.value
    }
    const editUserInfo = (options) => {
      userInfoRaw.value = { ...userInfoRaw.value, ...options }
    }
    const setTenant = (tenantId) => {
      userInfoRaw.value.tenantId = tenantId
    }
    const getTenant = () => {
      return userInfoRaw.value.tenantId
    }
    const reset = () => {
      userInfoRaw.value = { ...initState }
    }
    /** 已登录：HomeAI token 或遗留 token 任一存在即可 */
    const isLogined = computed(() => !!getHomeaiToken() || !!userInfoRaw.value.token)

    return {
      userInfoRaw,
      userInfo,
      setUserInfo,
      getUserInfo,
      clearUserInfo,
      setTenant,
      getTenant,
      isLogined,
      editUserInfo,
      reset,
    }
  },
  {
    // 只持久化原始状态，避免把 computed userInfo 写入 storage
    persist: {
      paths: ['userInfoRaw'],
    },
  },
)
