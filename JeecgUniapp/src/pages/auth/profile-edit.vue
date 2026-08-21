<route lang="json5">
{
  style: {
    navigationBarTitleText: '编辑资料',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view class="page">
    <view class="form-card">
      <HomeMediaUpload
        v-model="avatarUrl"
        mode="image"
        url="/homeai/user/info/avatar"
        placeholder="点击上传头像"
        tip="支持 jpg/png/webp，不超过 2MB"
        :max-size="2"
        :height="200"
      />
      <view class="form-group">
        <input class="field-input" v-model="nickname" maxlength="20" placeholder="请输入昵称" />
      </view>
      <wd-button size="large" type="primary" block round :loading="loading" @click="submit">保存</wd-button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useUserStore } from '../../pages-homeai/stores/user'
import { displayNickname } from '../../pages-homeai/utils/displayName'
import HomeMediaUpload from '../../pages-homeai/components/HomeMediaUpload.vue'

const userStore = useUserStore()
const nickname = ref(displayNickname(userStore.userInfo) === '未登录' ? '' : (userStore.userInfo?.nickname && userStore.userInfo.nickname !== '微信用户' ? userStore.userInfo.nickname : ''))
const avatarUrl = ref(userStore.userInfo?.avatarUrl || '')
const loading = ref(false)

async function submit() {
  const name = nickname.value.trim()
  if (!name) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await userStore.updateProfile({ nickname: name, avatarUrl: avatarUrl.value || undefined })
    uni.showToast({ title: '已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 400)
  } catch {
    // request.ts 已 toast
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 32rpx;
  background: var(--hai-bg);
  box-sizing: border-box;
}
.form-card {
  background: var(--hai-card);
  border-radius: 28rpx;
  padding: 40rpx 32rpx;
  box-shadow: var(--hai-shadow);
}
.form-group {
  margin-top: 32rpx;
  padding: 24rpx 28rpx;
  background: var(--hai-bg);
  border-radius: 20rpx;
  margin-bottom: 32rpx;
}
.field-input {
  font-size: 30rpx;
  color: var(--hai-text);
  height: 48rpx;
}
</style>
