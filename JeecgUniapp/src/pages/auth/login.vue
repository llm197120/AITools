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
    <!-- 品牌区 -->
    <view class="brand">
      <view class="brand-logo">
        <wd-icon name="home" size="44px" color="#FFFFFF"></wd-icon>
      </view>
      <text class="brand-title">家庭AI小工具</text>
      <text class="brand-sub">
        {{ isRegisterMode ? '注册账号，开启智能家庭生活' : '登录后使用全部家庭功能' }}
      </text>
    </view>

    <!-- 表单卡片 -->
    <view class="form-card">
      <!-- 手机号 -->
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

      <!-- 密码 -->
      <view class="form-group">
        <view class="field-icon">
          <wd-icon name="lock-on" size="20px" color="#1B4F8A"></wd-icon>
        </view>
        <input
          class="field-input"
          v-model="password"
          :password="true"
          maxlength="20"
          placeholder="请输入密码"
          placeholder-class="field-placeholder"
        />
      </view>

      <!-- 昵称（仅注册模式，可选） -->
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

      <!-- 提交按钮 -->
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

      <!-- 模式切换 -->
      <view class="mode-toggle" @click="isRegisterMode = !isRegisterMode">
        <text>{{ isRegisterMode ? '已有账号？去登录' : '没有账号？立即注册' }}</text>
      </view>
    </view>

    <view class="footer-tip">
      <text>登录即代表同意</text>
      <text class="tip-link" @tap="goAgreement">《用户协议》</text>
      <text>与</text>
      <text class="tip-link" @tap="goPrivacy">《隐私政策》</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useUserStore } from '../../pages-homeai/stores/user'
import { loginByPhone, registerByPhone, isPhoneValid } from '../../pages-homeai/platform/auth'
import type { AuthResult } from '../../pages-homeai/platform/auth'

const userStore = useUserStore()

/** 当前是否为注册模式 */
const isRegisterMode = ref(false)
const phone = ref('')
const password = ref('')
const nickname = ref('')
const loading = ref(false)

/** 跳转用户协议 */
function goAgreement() {
  uni.navigateTo({ url: '/pages/agreement/index' })
}

/** 跳转隐私政策 */
function goPrivacy() {
  uni.navigateTo({ url: '/pages/privacy/index' })
}

/**
 * 提交前本地校验，失败时提示并阻止请求
 * @returns 校验是否通过
 */
function validate(): boolean {
  if (!isPhoneValid(phone.value)) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' })
    return false
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return false
  }
  return true
}

/**
 * 登录/注册成功后的统一处理：写入登录态 -> 提示 -> 跳转首页
 * @param auth 平台返回的登录结果
 */
function handleSuccess(auth: AuthResult) {
  userStore.setAuth(auth)
  uni.showToast({ title: '登录成功', icon: 'success' })
  // 延迟跳转，避免 toast 被切换页面打断
  setTimeout(() => {
    uni.switchTab({ url: '/pages/homeai/index' })
  }, 500)
}

/** 提交登录或注册 */
async function handleSubmit() {
  if (loading.value) return
  if (!validate()) return
  loading.value = true
  try {
    const auth = isRegisterMode.value
      ? await registerByPhone(phone.value, password.value, nickname.value || undefined)
      : await loginByPhone(phone.value, password.value)
    handleSuccess(auth)
  } catch (err) {
    // request.ts 已对后端失败统一 toast，这里仅 console 记录，避免重复提示
    console.error(isRegisterMode.value ? '注册失败' : '登录失败', err)
    // 兜底：仅当异常没有 message（如网络层原始错误且未弹 toast）时才补充提示
    if (!(err instanceof Error && err.message)) {
      uni.showToast({ title: '登录失败，请稍后重试', icon: 'none' })
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

/* 品牌区 */
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
  box-shadow: 0 12rpx 32rpx rgba(27, 79, 138, 0.35);
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

/* 表单卡片 */
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

.form-group:last-of-type {
  margin-bottom: 40rpx;
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

.submit-btn {
  margin-top: 8rpx;
}

.mode-toggle {
  margin-top: 32rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--hai-primary, #1b4f8a);
}

.footer-tip {
  margin-top: auto;
  padding-top: 48rpx;
  text-align: center;
  font-size: 22rpx;
  color: var(--hai-text-muted);
}

.tip-link {
  color: var(--hai-primary, #1b4f8a);
  text-decoration: underline;
}
</style>
