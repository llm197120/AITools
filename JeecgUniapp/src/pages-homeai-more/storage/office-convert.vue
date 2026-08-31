<route lang="json5">
{ style: { navigationBarTitleText: '格式转换', navigationBarBackgroundColor: '#F3F2EE' } }
</route>

<template>
  <view class="convert-page">
    <view class="file-info-card">
      <text class="label">源文件：</text>
      <text>{{ fileName || sourceFormat }}</text>
      <text v-if="fileName && sourceFormat" class="fmt">.{{ sourceFormat }}</text>
    </view>
    <view class="format-list">
      <text class="section-title">选择目标格式：</text>
      <HomeEmpty
        v-if="loadFailed"
        title="规则加载失败"
        hint="请检查网络后重试"
        action-text="重试"
        :card="true"
        @action="loadTargets"
      />
      <HomeEmpty
        v-else-if="!targets.length"
        title="暂无可用转换规则"
        hint="请联系管理员在后台配置该格式的转换规则"
        :card="true"
      />
      <view class="format-item" v-for="rule in targets" :key="rule.id" @click="startConvert(rule)">
        <text>{{ rule.sourceFormat }} → {{ rule.targetFormat }}</text>
        <wd-icon name="arrow-right" size="14px" color="#A39E94"></wd-icon>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get as getApi, post as postApi } from '../../pages-homeai/api/request'
import HomeEmpty from '../../components/HomeEmpty.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'

useHomeaiPageGuard()

const sourceFormat = ref('')
const fileName = ref('')
const targets = ref<any[]>([])
const fileId = ref('')
const converting = ref(false)
const loadFailed = ref(false)

onLoad((options: any) => {
  fileId.value = options?.fileId || ''
  sourceFormat.value = options?.format || ''
  try {
    fileName.value = options?.name ? decodeURIComponent(options.name) : ''
  } catch {
    fileName.value = options?.name || ''
  }
  loadTargets()
})

async function loadTargets() {
  loadFailed.value = false
  try {
    targets.value = (await getApi('/storage/rule/targets', { params: { sourceFormat: sourceFormat.value } })) || []
  } catch {
    targets.value = []
    loadFailed.value = true
  }
}

async function startConvert(rule: any) {
  if (converting.value) return
  converting.value = true
  uni.showLoading({ title: '提交中...' })
  try {
    await postApi('/storage/office/convert', {
      params: { fileId: fileId.value, sourceFormat: sourceFormat.value, targetFormat: rule.targetFormat },
    })
    uni.hideLoading()
    uni.showToast({ title: '转换任务已提交', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: '/pages-homeai-more/storage/office-history' })
    }, 400)
  } catch (e: any) {
    uni.hideLoading()
    uni.showToast({ title: e?.message || '提交失败，请重试', icon: 'none' })
  } finally {
    converting.value = false
  }
}
</script>

<style scoped>
.convert-page { min-height: 100vh; background: var(--hai-bg); padding: 24rpx 32rpx 48rpx; box-sizing: border-box; }
.file-info-card { padding: 30rpx; background: var(--hai-card); border-radius: 28rpx; font-size: 28rpx; margin-bottom: 30rpx; color: var(--hai-text); box-shadow: var(--hai-shadow); }
.fmt { margin-left: 8rpx; color: var(--hai-text-muted); font-size: 24rpx; }
.section-title { font-size: 26rpx; color: var(--hai-text-secondary); margin-bottom: 16rpx; display: block; }
.format-item { display: flex; justify-content: space-between; padding: 28rpx; background: var(--hai-card); border-radius: 24rpx; margin-bottom: 12rpx; font-size: 28rpx; color: var(--hai-text); box-shadow: var(--hai-shadow); }
</style>
