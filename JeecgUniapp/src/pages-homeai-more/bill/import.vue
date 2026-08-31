<route lang="json5">{ style: { navigationBarTitleText: '导入账单', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view class="hai-page">
    <view class="section">
      <text class="label">导入来源</text>
      <view class="type-row">
        <view class="type-btn" :class="{ active: importType === 'wechat_csv' }" @click="switchType('wechat_csv')">微信 CSV</view>
        <view class="type-btn" :class="{ active: importType === 'excel' }" @click="switchType('excel')">Excel</view>
      </view>
    </view>
    <view class="upload-box" @click="chooseFile">
      <text class="upload-icon">📎</text>
      <text>{{ fileName || '点击选择文件' }}</text>
      <text class="tip">微信 CSV 或 .xlsx / .xls</text>
    </view>
    <wd-button size="large" type="primary" :disabled="!filePath" :loading="parsing" @click="preview">开始解析</wd-button>
  </view>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { useHomeaiFilePick } from '../../pages-homeai/utils/useHomeaiFilePick'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'

useHomeaiPageGuard()

const { pickFiles } = useHomeaiFilePick()

const importType = ref<'wechat_csv' | 'excel'>('wechat_csv')
const filePath = ref('')
const fileName = ref('')
const parsing = ref(false)

function switchType(next: 'wechat_csv' | 'excel') {
  if (importType.value === next) return
  importType.value = next
  filePath.value = ''
  fileName.value = ''
}

async function chooseFile() {
  const ext = importType.value === 'excel' ? ['xlsx', 'xls'] : ['csv']
  const files = await pickFiles({
    count: 1,
    type: 'file',
    extension: ext,
    allowedExt: ext,
  })
  if (files[0]) {
    filePath.value = files[0].path
    fileName.value = files[0].name
  }
}

async function preview() {
  if (!filePath.value || parsing.value) return
  const { billApi } = await import('../../pages-homeai/api/bill')
  parsing.value = true
  uni.showLoading({ title: '解析中...' })
  try {
    const rows = await billApi.importPreview(filePath.value, importType.value)
    uni.hideLoading()
    if (!Array.isArray(rows) || !rows.length) {
      uni.showToast({ title: '没有解析到账单', icon: 'none' })
      return
    }
    uni.setStorageSync('homeai_bill_import_preview', rows)
    uni.navigateTo({ url: '/pages-homeai-more/bill/import-confirm' })
  } catch (e: any) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '解析失败', icon: 'none' })
  } finally {
    parsing.value = false
  }
}
</script>
<style scoped>
/* page shell: .hai-page */
.label { font-size: 26rpx; color: var(--hai-text-secondary); margin-bottom: 16rpx; display: block; }
.type-row { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.type-btn { flex: 1; text-align: center; padding: 22rpx; background: var(--hai-card); border-radius: 999rpx; font-size: 28rpx; color: var(--hai-text-secondary); box-shadow: var(--hai-shadow); }
.type-btn.active { background: var(--hai-primary); color: var(--hai-on-primary); }
.upload-box { background: var(--hai-card); border-radius: 28rpx; padding: 60rpx 30rpx; text-align: center; margin-bottom: 40rpx; box-shadow: var(--hai-shadow); color: var(--hai-text); }
.upload-icon { font-size: 64rpx; display: block; margin-bottom: 16rpx; }
.tip { display: block; font-size: 22rpx; color: var(--hai-text-muted); margin-top: 12rpx; }
</style>
