<route lang="json5">
{
  style: {
    navigationBarTitleText: '修改密码',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view class="page">
    <view class="form-card">
      <view class="form-group">
        <input class="field-input" v-model="oldPassword" :password="true" maxlength="20" placeholder="请输入原密码" />
      </view>
      <view class="form-group">
        <input class="field-input" v-model="newPassword" :password="true" maxlength="20" placeholder="请输入新密码（至少6位）" />
      </view>
      <view class="form-group">
        <input class="field-input" v-model="confirmPassword" :password="true" maxlength="20" placeholder="请再次输入新密码" />
      </view>
      <wd-button size="large" type="primary" block round :loading="loading" @click="submit">保存</wd-button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { changePassword } from '../../pages-homeai/platform/auth'
import { useUserStore } from '../../pages-homeai/stores/user'

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)

async function submit() {
  if (loading.value) return
  if (!oldPassword.value) {
    uni.showToast({ title: '请输入原密码', icon: 'none' })
    return
  }
  if (!newPassword.value || newPassword.value.length < 6) {
    uni.showToast({ title: '新密码至少 6 位', icon: 'none' })
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
    return
  }
  if (newPassword.value === oldPassword.value) {
    uni.showToast({ title: '新密码不能与原密码相同', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await changePassword(oldPassword.value, newPassword.value)
    const userStore = useUserStore()
    userStore.clearLocalSession()
    uni.showToast({ title: '密码已修改，请重新登录', icon: 'success' })
    setTimeout(() => uni.reLaunch({ url: '/pages/auth/login' }), 800)
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
  padding: 24rpx 28rpx;
  background: var(--hai-bg);
  border-radius: 20rpx;
  margin-bottom: 24rpx;
}
.field-input {
  font-size: 30rpx;
  color: var(--hai-text);
  height: 48rpx;
}
</style>
