<route lang="json5">
{ style: { navigationBarTitleText: '格式转换', navigationBarBackgroundColor: '#F3F2EE' } }
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
        <wd-icon name="arrow-right" size="14px" color="#A39E94"></wd-icon>
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
.convert-page { min-height: 100vh; background: var(--hai-bg); padding: 24rpx 32rpx 48rpx; box-sizing: border-box; }
.file-info-card { padding: 30rpx; background: var(--hai-card); border-radius: 28rpx; font-size: 28rpx; margin-bottom: 30rpx; color: var(--hai-text); box-shadow: var(--hai-shadow); }
.section-title { font-size: 26rpx; color: var(--hai-text-secondary); margin-bottom: 16rpx; display: block; }
.format-item { display: flex; justify-content: space-between; padding: 28rpx; background: var(--hai-card); border-radius: 24rpx; margin-bottom: 12rpx; font-size: 28rpx; color: var(--hai-text); box-shadow: var(--hai-shadow); }
</style>
