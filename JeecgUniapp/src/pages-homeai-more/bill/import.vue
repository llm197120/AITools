<route lang="json5">{ style: { navigationBarTitleText: '导入账单' } }</route>
<template>
  <view class="page">
    <view class="section">
      <text class="label">导入来源</text>
      <view class="type-row">
        <view class="type-btn" :class="{ active: importType === 'wechat_csv' }" @click="importType = 'wechat_csv'">微信 CSV</view>
        <view class="type-btn" :class="{ active: importType === 'excel' }" @click="importType = 'excel'">Excel</view>
      </view>
    </view>
    <view class="upload-box" @click="chooseFile">
      <text class="upload-icon">📎</text>
      <text>{{ fileName || '点击选择文件' }}</text>
      <text class="tip">微信 CSV 或 .xlsx / .xls</text>
    </view>
    <wd-button size="large" type="primary" :disabled="!filePath" @click="preview">开始解析</wd-button>
  </view>
</template>
<script lang="ts" setup>
import { ref } from 'vue'

const importType = ref<'wechat_csv' | 'excel'>('wechat_csv')
const filePath = ref('')
const fileName = ref('')

function chooseFile() {
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    extension: importType.value === 'excel' ? ['xlsx', 'xls'] : ['csv'],
    success: (r) => {
      const f = r.tempFiles?.[0]
      if (f) {
        filePath.value = f.path
        fileName.value = f.name
      }
    },
  })
}

async function preview() {
  if (!filePath.value) return
  const { billApi } = await import('../../pages-homeai/api/bill')
  uni.showLoading({ title: '解析中...' })
  try {
    const rows = await billApi.importPreview(filePath.value, importType.value)
    uni.hideLoading()
    uni.navigateTo({
      url: `/pages-homeai-more/bill/import-confirm?rows=${encodeURIComponent(JSON.stringify(rows))}`,
    })
  } catch (e: any) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '解析失败', icon: 'none' })
  }
}
</script>
<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding: 30rpx; }
.label { font-size: 26rpx; color: #666; margin-bottom: 16rpx; display: block; }
.type-row { display: flex; gap: 16rpx; margin-bottom: 30rpx; }
.type-btn { flex: 1; text-align: center; padding: 20rpx; background: #fff; border-radius: 12rpx; font-size: 28rpx; }
.type-btn.active { background: #667eea; color: #fff; }
.upload-box { background: #fff; border-radius: 16rpx; padding: 60rpx 30rpx; text-align: center; margin-bottom: 40rpx; }
.upload-icon { font-size: 64rpx; display: block; margin-bottom: 16rpx; }
.tip { display: block; font-size: 22rpx; color: #999; margin-top: 12rpx; }
</style>
