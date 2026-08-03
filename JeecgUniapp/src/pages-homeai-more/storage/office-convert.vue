<route lang="json5">
{ style: { navigationBarTitleText: '格式转换' } }
</route>

<template>
  <view class="convert-page">
    <view class="file-info-card">
      <text class="label">源文件：</text>
      <text>{{ sourceFormat }}</text>
    </view>
    <view class="format-list">
      <text class="section-title">选择目标格式：</text>
      <view class="format-item" v-for="rule in targets" :key="rule.id" @click="startConvert(rule)">
        <text>{{ rule.sourceFormat }} → {{ rule.targetFormat }}</text>
        <wd-icon name="arrow-right" size="14px" color="#ccc"></wd-icon>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { get as getApi, post as postApi } from '../../pages-homeai/api/request'

const sourceFormat = ref('')
const targets = ref<any[]>([])
const fileId = ref('')

onLoad((options: any) => {
  fileId.value = options?.fileId || ''
  sourceFormat.value = options?.format || ''
  loadTargets()
})

async function loadTargets() {
  targets.value = await getApi('/storage/rule/targets', { params: { sourceFormat: sourceFormat.value } })
}

async function startConvert(rule: any) {
  uni.showLoading({ title: '提交中...' })
  try {
    await postApi('/storage/office/convert', {
      params: { fileId: fileId.value, sourceFormat: sourceFormat.value, targetFormat: rule.targetFormat },
    })
    uni.hideLoading()
    uni.showToast({ title: '转换任务已提交', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) {
    uni.hideLoading()
  }
}
</script>

<style scoped>
.convert-page { min-height: 100vh; background: #f5f5f5; padding: 30rpx; }
.file-info-card { padding: 30rpx; background: #fff; border-radius: 12rpx; font-size: 28rpx; margin-bottom: 30rpx; }
.section-title { font-size: 26rpx; color: #666; margin-bottom: 16rpx; display: block; }
.format-item { display: flex; justify-content: space-between; padding: 28rpx; background: #fff; border-radius: 12rpx; margin-bottom: 12rpx; font-size: 28rpx; }
</style>
