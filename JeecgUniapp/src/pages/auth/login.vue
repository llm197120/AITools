<route lang="json5">
{
  style: {
    navigationBarTitleText: '登录',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view class="login-page">
    <view class="brand">
      <view class="brand-logo">
        <wd-icon name="home" size="44px" color="#FFFFFF"></wd-icon>
      </view>
      <text class="brand-title">家庭AI小工具</text>
      <text class="brand-sub">
        {{ isRegisterMode ? '注册账号，开启智能家庭生活' : '登录后使用全部家庭功能' }}
      </text>
    </view>

    <view class="form-card">
      <view class="form-group">
        <view class="field-icon">
          <wd-icon name="phone" size="20px" color="#1B4F8A"></wd-icon>
        </view>
        <input
          class="field-input"
          v-model="phone"
          type="number"
          maxlength="11"
          placeholder="请输入手机号"
          placeholder-class="field-placeholder"
        />
      </view>

      <view class="form-group">
        <view class="field-icon">
          <wd-icon name="lock-on" size="20px" color="#1B4F8A"></wd-icon>
        </view>
        <input
          class="field-input"
          v-model="password"
          :password="!showPassword"
          maxlength="20"
          placeholder="请输入密码（至少6位）"
          placeholder-class="field-placeholder"
        />
        <text class="pwd-toggle" @tap="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</text>
      </view>

      <view class="form-group" v-if="isRegisterMode">
        <view class="field-icon">
          <wd-icon name="lock-on" size="20px" color="#1B4F8A"></wd-icon>
        </view>
        <input
          class="field-input"
          v-model="confirmPassword"
          :password="!showPassword"
          maxlength="20"
          placeholder="请再次输入密码"
          placeholder-class="field-placeholder"
        />
      </view>

      <view class="form-group" v-if="isRegisterMode">
        <view class="field-icon">
          <wd-icon name="user" size="20px" color="#1B4F8A"></wd-icon>
        </view>
        <input
          class="field-input"
          v-model="nickname"
          maxlength="20"
          placeholder="请输入昵称（可选）"
          placeholder-class="field-placeholder"
        />
      </view>

      <view class="agree-row" @tap="toggleAgreed">
        <view class="agree-box" :class="{ on: agreed }"></view>
        <text class="agree-text">我已阅读并同意</text>
        <text class="tip-link" @tap.stop="goAgreement">《用户协议》</text>
        <text class="agree-text">与</text>
        <text class="tip-link" @tap.stop="goPrivacy">《隐私政策》</text>
      </view>

      <wd-button
        class="submit-btn"
        size="large"
        type="primary"
        block
        round
        :loading="loading"
        @click="handleSubmit"
      >
        {{ isRegisterMode ? '注 册' : '登 录' }}
      </wd-button>

      <view v-if="!isRegisterMode" class="forgot-row" @click="onForgot">
        <text>忘记密码？请联系管理员在后台重置</text>
      </view>

      <view class="mode-toggle" @click="toggleMode">
        <text>{{ isRegisterMode ? '已有账号？去登录' : '没有账号？立即注册' }}</text>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { loginByPhone, registerByPhone, isPhoneValid } from '../../pages-homeai/platform/auth'
import type { AuthResult } from '../../pages-homeai/platform/auth'
import {
  afterLoginNavigate,
  HOMEAI_ONBOARD_FAMILY_KEY,
} from '../../pages-homeai/utils/homeaiAuth'

const userStore = useUserStore()

const isRegisterMode = ref(false)
const phone = ref('')
const password = ref('')
const confirmPassword = ref('')
const nickname = ref('')
const loading = ref(false)
const showPassword = ref(false)
const agreed = ref(!!uni.getStorageSync('homeai_privacy_agreed'))
const redirect = ref('')

onLoad((query?: Record<string, string>) => {
  if (query?.mode === 'register') {
    isRegisterMode.value = true
  }
  if (query?.redirect) {
    redirect.value = query.redirect
  }
  uni.setNavigationBarTitle({ title: isRegisterMode.value ? '注册' : '登录' })
})

function toggleAgreed() {
  agreed.value = !agreed.value
  if (agreed.value) uni.setStorageSync('homeai_privacy_agreed', 1)
  else uni.removeStorageSync('homeai_privacy_agreed')
}

function toggleMode() {
  isRegisterMode.value = !isRegisterMode.value
  uni.setNavigationBarTitle({ title: isRegisterMode.value ? '注册' : '登录' })
}

function onForgot() {
  uni.showToast({ title: '请联系管理员在后台重置密码', icon: 'none', duration: 2500 })
}

function goAgreement() {
  uni.navigateTo({ url: '/pages/agreement/index' })
}

function goPrivacy() {
  uni.navigateTo({ url: '/pages/privacy/index' })
}

function validate(): boolean {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议与隐私政策', icon: 'none' })
    return false
  }
  if (!isPhoneValid(phone.value)) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' })
    return false
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return false
  }
  if (password.value.length < 6) {
    uni.showToast({ title: '密码至少 6 位', icon: 'none' })
    return false
  }
  if (isRegisterMode.value && password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return false
  }
  return true
}

function leaveAfterAuth(isNew: boolean) {
  if (isNew) {
    uni.showActionSheet({
      itemList: ['创建家庭', '加入家庭', '稍后再说'],
      success: (res) => {
        if (res.tapIndex === 0) {
          uni.setStorageSync(HOMEAI_ONBOARD_FAMILY_KEY, 'create')
          uni.switchTab({ url: '/pages/homeai/family' })
          return
        }
        if (res.tapIndex === 1) {
          uni.setStorageSync(HOMEAI_ONBOARD_FAMILY_KEY, 'join')
          uni.switchTab({ url: '/pages/homeai/family' })
          return
        }
        afterLoginNavigate(redirect.value)
      },
      fail: () => afterLoginNavigate(redirect.value),
    })
    return
  }
  afterLoginNavigate(redirect.value)
}

function handleSuccess(auth: AuthResult) {
  if (!auth?.token) {
    uni.showToast({
      title: isRegisterMode.value ? '注册失败，请稍后重试' : '登录失败，请稍后重试',
      icon: 'none',
    })
    return
  }
  userStore.setAuth(auth)
  uni.setStorageSync('homeai_privacy_agreed', true)
  uni.showToast({ title: isRegisterMode.value ? '注册成功' : '登录成功', icon: 'success' })
  setTimeout(() => {
    leaveAfterAuth(!!(isRegisterMode.value || auth.isNewUser))
  }, 400)
}

async function handleSubmit() {
  if (loading.value) return
  if (!validate()) return
  loading.value = true
  try {
    const auth = isRegisterMode.value
      ? await registerByPhone(phone.value, password.value, nickname.value || undefined)
      : await loginByPhone(phone.value, password.value)
    handleSuccess(auth)
  } catch (err: any) {
    console.error(isRegisterMode.value ? '注册失败' : '登录失败', err)
    const msg = err instanceof Error ? err.message : (err?.errMsg || '')
    if (!msg) {
      uni.showToast({
        title: isRegisterMode.value ? '注册失败，请稍后重试' : '登录失败，请稍后重试',
        icon: 'none',
      })
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 64rpx 48rpx 48rpx;
  background: var(--hai-bg);
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 0 64rpx;
}

.brand-logo {
  width: 128rpx;
  height: 128rpx;
  border-radius: 36rpx;
  background: var(--hai-primary, #1b4f8a);
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-title {
  margin-top: 28rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 44rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.brand-sub {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: var(--hai-text-secondary);
}

.form-card {
  background: var(--hai-card);
  border-radius: 28rpx;
  box-shadow: var(--hai-shadow);
  padding: 40rpx 32rpx;
}

.form-group {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 28rpx;
  background: var(--hai-bg);
  border-radius: 20rpx;
  margin-bottom: 24rpx;
}

.field-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.field-input {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  color: var(--hai-text);
  height: 48rpx;
}

.field-placeholder {
  color: var(--hai-text-muted);
}

.pwd-toggle {
  flex-shrink: 0;
  font-size: 24rpx;
  color: var(--hai-primary, #1b4f8a);
}

.agree-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8rpx;
  margin: 8rpx 0 32rpx;
}

.agree-box {
  width: 28rpx;
  height: 28rpx;
  border-radius: 6rpx;
  border: 2rpx solid var(--hai-border, #d9d4cc);
  box-sizing: border-box;
}

.agree-box.on {
  background: var(--hai-primary, #1b4f8a);
  border-color: var(--hai-primary, #1b4f8a);
}

.agree-text {
  font-size: 22rpx;
  color: var(--hai-text-muted);
}

.submit-btn {
  margin-top: 8rpx;
}

.forgot-row {
  margin-top: 24rpx;
  text-align: center;
  font-size: 24rpx;
  color: var(--hai-text-muted, #8a857c);
}

.mode-toggle {
  margin-top: 32rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--hai-primary, #1b4f8a);
}

.tip-link {
  font-size: 22rpx;
  color: var(--hai-primary, #1b4f8a);
  text-decoration: underline;
}
</style>
