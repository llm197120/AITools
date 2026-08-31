import { defineStore } from 'pinia'

import { ref } from 'vue'

import { get as getApi, post as postApi, del as delApi } from '../api/request'



export const useFamilyStore = defineStore('homeai-family', () => {

  const familyInfo = ref<any>(null)

  const hasFamily = ref(false)

  /** 是否已成功拉取过家庭信息（区分「未加载」与「确认无家庭」） */

  const familyInfoLoaded = ref(false)

  /** 最近一次拉取失败（网络/服务异常时不应误显「无家庭」） */

  const familyLoadFailed = ref(false)

  /** 当前用户在家庭中的角色，不依赖成员列表是否加载成功 */

  const myRole = ref('')



  function clearFamily() {

    familyInfo.value = null

    hasFamily.value = false

    myRole.value = ''

  }



  async function fetchFamilyInfo() {

    try {

      const result = await getApi('/family/info')

      familyLoadFailed.value = false

      familyInfoLoaded.value = true

      if (result.hasFamily) {

        familyInfo.value = result.family

        hasFamily.value = true

        myRole.value = result.myRole || 'member'

      } else {

        clearFamily()

      }

    } catch (e) {

      console.error('获取家庭信息失败', e)

      familyLoadFailed.value = true

    }

  }



  async function createFamily(name: string) {

    const result = await postApi('/family', { name })

    familyInfo.value = result

    hasFamily.value = true

    myRole.value = 'admin'

    familyInfoLoaded.value = true

    familyLoadFailed.value = false

    return result

  }



  async function disbandFamily() {

    await delApi('/family/disband')

    clearFamily()

    familyInfoLoaded.value = true

    familyLoadFailed.value = false

  }



  async function leaveFamily() {

    await delApi('/family/leave')

    clearFamily()

    familyInfoLoaded.value = true

    familyLoadFailed.value = false

  }



  return {

    familyInfo,

    hasFamily,

    familyInfoLoaded,

    familyLoadFailed,

    myRole,

    fetchFamilyInfo,

    createFamily,

    disbandFamily,

    leaveFamily,

  }

})


