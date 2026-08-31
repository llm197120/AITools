<route lang="json5">
{ style: { navigationBarTitleText: 'AI文件生成', navigationBarBackgroundColor: '#F3F2EE' } }
</route>

<template>
  <view class="generate-page">
    <view v-if="quotaHint" class="quota-bar">{{ quotaHint }}</view>
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
      <textarea
        class="desc-input"
        v-model="instruction"
        placeholder="比如：生成一份家庭月度开支汇总表，包含食品、交通、住房等分类..."
        @blur="refreshQuota"
      />
    </view>
    <view class="template-area">
      <text class="section-title">选择模板（可选）：</text>
      <scroll-view scroll-x class="template-scroll">
        <text v-if="!templates.length" class="tpl-empty">暂无模板，可不选直接生成</text>
        <view class="template-item" v-for="tpl in templates" :key="tpl.id" @click="selectedTemplate = tpl.id">
          <text :class="{ selected: selectedTemplate === tpl.id }">{{ tpl.name }}</text>
        </view>
      </scroll-view>
    </view>
    <wd-button size="large" type="primary" :disabled="quotaBlocked" :loading="generating" @click="generate">开始生成</wd-button>
  </view>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get as getApi, post as postApi } from '../../pages-homeai/api/request'
import { storageApi } from '../../pages-homeai/api/index'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'

useHomeaiPageGuard()

const docType = ref('word')
const instruction = ref('')
const templates = ref<any[]>([])
const selectedTemplate = ref('')
const sourceFileId = ref('')
const quotaHint = ref('')
const quotaBlocked = ref(false)
const generating = ref(false)

async function loadTemplates() {
  try {
    templates.value = (await getApi('/storage/template/enabled', { params: { type: docType.value } })) || []
  } catch {
    templates.value = []
    uni.showToast({ title: '模板加载失败', icon: 'none' })
  }
}

watch(docType, () => {
  selectedTemplate.value = ''
  loadTemplates()
})

onLoad(async (opts: any) => {
  sourceFileId.value = opts?.fileId || ''
  await loadTemplates()
  await refreshQuota()
})

async function refreshQuota() {
  try {
    const q: any = await storageApi.generateQuotaCheck(instruction.value.trim() || undefined)
    if (!q) return
    if (q.aiDocPolishEnabled === false) {
      quotaHint.value = '当前未启用 AI 润色，将直接按指令生成'
      quotaBlocked.value = false
      return
    }
    const daily = q.remainingDaily
    const monthly = q.remainingMonthly
    quotaHint.value = `预估消耗约 ${q.estimatedInputTokens || 0}+${q.estimatedOutputTokens || 0} Token · 今日剩余 ${daily ?? '-'} · 本月剩余 ${monthly ?? '-'}`
    quotaBlocked.value = q.allowed === false
    if (quotaBlocked.value && q.message) {
      quotaHint.value = q.message
    }
  } catch {
    quotaHint.value = '额度查询失败，提交时将再校验'
  }
}

async function generate() {
  if (generating.value) return
  if (!instruction.value.trim()) {
    uni.showToast({ title: '请描述需求', icon: 'none' })
    return
  }
  await refreshQuota()
  if (quotaBlocked.value) {
    uni.showToast({ title: quotaHint.value || 'Token 额度不足', icon: 'none' })
    return
  }
  generating.value = true
  uni.showLoading({ title: '提交中...' })
  try {
    await postApi('/storage/office/generate', {
      params: {
        fileId: sourceFileId.value || '',
        instruction: instruction.value,
        docType: docType.value,
        templateId: selectedTemplate.value || '',
      },
    })
    uni.hideLoading()
    uni.showToast({ title: '生成任务已提交', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: '/pages-homeai-more/storage/office-history' })
    }, 400)
  } catch (e: any) {
    uni.hideLoading()
    uni.showToast({ title: e?.message || '提交失败', icon: 'none' })
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.generate-page { min-height: 100vh; background: var(--hai-bg); padding: 24rpx 32rpx 48rpx; box-sizing: border-box; }
.quota-bar { font-size: 24rpx; color: var(--hai-text-secondary); background: var(--hai-card); padding: 16rpx 20rpx; border-radius: 24rpx; margin-bottom: 20rpx; line-height: 1.5; box-shadow: var(--hai-shadow); }
.section-title { font-size: 26rpx; color: var(--hai-text-secondary); margin-bottom: 16rpx; display: block; }
.type-row { display: flex; gap: 16rpx; margin-bottom: 30rpx; }
.type-btn { flex: 1; text-align: center; padding: 22rpx; background: var(--hai-card); border-radius: 999rpx; font-size: 28rpx; color: var(--hai-text-secondary); box-shadow: var(--hai-shadow); }
.type-btn.active { background: var(--hai-primary); color: var(--hai-on-primary); }
.desc-input { width: 100%; min-height: 200rpx; padding: 24rpx; background: var(--hai-card); border-radius: 28rpx; font-size: 28rpx; margin-bottom: 30rpx; color: var(--hai-text); box-shadow: var(--hai-shadow); box-sizing: border-box; }
.template-scroll { white-space: nowrap; margin-bottom: 40rpx; }
.tpl-empty { font-size: 24rpx; color: var(--hai-text-muted); }
.template-item { display: inline-block; padding: 16rpx 30rpx; background: var(--hai-card); border-radius: 24rpx; margin-right: 16rpx; font-size: 26rpx; color: var(--hai-text-secondary); box-shadow: var(--hai-shadow); }
.template-item .selected { color: var(--hai-primary); font-weight: 600; }
</style>
