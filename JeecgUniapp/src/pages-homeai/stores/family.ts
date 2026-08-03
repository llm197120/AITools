import { defineStore } from 'pinia'
import { ref } from 'vue'
import { get as getApi, del as delApi } from '../api/request'

export const useFamilyStore = defineStore('homeai-family', () => {
  const familyInfo = ref<any>(null)
  const hasFamily = ref(false)

  async function fetchFamilyInfo() {
    try {
      const result = await getApi('/family/info')
      if (result.hasFamily) {
        familyInfo.value = result.family
        hasFamily.value = true
      } else {
        familyInfo.value = null
        hasFamily.value = false
      }
    } catch (e) {
      console.error('获取家庭信息失败', e)
    }
  }

  async function createFamily(name: string) {
    const result = await getApi('/family', { name })
    familyInfo.value = result
    hasFamily.value = true
    return result
  }

  async function disbandFamily() {
    await delApi('/family/disband')
    familyInfo.value = null
    hasFamily.value = false
  }

  async function leaveFamily() {
    await delApi('/family/leave')
    familyInfo.value = null
    hasFamily.value = false
  }

  return { familyInfo, hasFamily, fetchFamilyInfo, createFamily, disbandFamily, leaveFamily }
})
