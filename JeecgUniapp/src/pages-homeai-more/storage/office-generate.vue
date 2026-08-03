<route lang="json5">
{ style: { navigationBarTitleText: 'AI文件生成' } }
</route>

<template>
  <view class="generate-page">
    <view class="type-select">
      <text class="section-title">选择文档类型：</text>
      <view class="type-row">
        <view class="type-btn" :class="{ active: docType === 'word' }" @click="docType = 'word'">Word</view>
        <view class="type-btn" :class="{ active: docType === 'excel' }" @click="docType = 'excel'">Excel</view>
        <view class="type-btn" :class="{ active: docType === 'ppt' }" @click="docType = 'ppt'">PPT</view>
      </view>
    </view>
    <view class="input-area">
      <text class="section-title">描述你的需求：</text>
      <textarea class="desc-input" v-model="instruction" placeholder="比如：生成一份家庭月度开支汇总表，包含食品、交通、住房等分类..." />
    </view>
    <view class="template-area">
      <text class="section-title">选择模板（可选）：</text>
      <scroll-view scroll-x class="template-scroll">
        <view class="template-item" v-for="tpl in templates" :key="tpl.id" @click="selectedTemplate = tpl.id">
          <text :class="{ selected: selectedTemplate === tpl.id }">{{ tpl.name }}</text>
        </view>
      </scroll-view>
    </view>
    <wd-button size="large" type="primary" @click="generate">开始生成</wd-button>
  </view>
</template>

<script lang="ts" setup>
import { ref } from '@dcloudio/uni-app'
import { get as getApi, post as postApi } from '../../pages-homeai/api/request'

const docType = ref('word')
const instruction = ref('')
const templates = ref<any[]>([])
const selectedTemplate = ref('')

onLoad(async () => {
  templates.value = await getApi('/storage/template/enabled', { params: { type: docType.value } })
})

async function generate() {
  if (!instruction.value.trim()) { uni.showToast({ title: '请描述需求', icon: 'none' }); return }
  uni.showLoading({ title: '提交中...' })
  try {
    await postApi('/storage/office/generate', { params: { fileId: '', instruction: instruction.value } })
    uni.hideLoading()
    uni.showToast({ title: '生成任务已提交', icon: 'success' })
  } catch (e) { uni.hideLoading() }
}
</script>

<style scoped>
.generate-page { min-height: 100vh; background: #f5f5f5; padding: 30rpx; }
.section-title { font-size: 26rpx; color: #666; margin-bottom: 16rpx; display: block; }
.type-row { display: flex; gap: 20rpx; margin-bottom: 30rpx; }
.type-btn { flex: 1; text-align: center; padding: 20rpx; background: #fff; border-radius: 12rpx; font-size: 28rpx; color: #666; }
.type-btn.active { background: #667eea; color: #fff; }
.desc-input { width: 100%; min-height: 200rpx; padding: 20rpx; background: #fff; border-radius: 12rpx; font-size: 28rpx; margin-bottom: 30rpx; }
.template-scroll { white-space: nowrap; margin-bottom: 40rpx; }
.template-item { display: inline-block; padding: 16rpx 30rpx; background: #fff; border-radius: 10rpx; margin-right: 16rpx; font-size: 26rpx; }
.template-item .selected { color: #667eea; font-weight: 600; }
</style>
