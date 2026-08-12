<route lang="json5">{ style: { navigationBarTitleText: '导入确认', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view class="hai-page">
    <text class="summary">共 {{ rows.length }} 条，已选 {{ selectedCount }} 条</text>
    <view class="row" v-for="(r, i) in rows" :key="i" @click="toggle(i)">
      <text class="check">{{ r._checked !== false ? '☑' : '☐' }}</text>
      <view class="info">
        <text class="line">{{ r.billDate }} · {{ r.remark || r.categoryName || '-' }}</text>
        <text class="amount" :class="r.type === 'income' ? 'green' : 'red'">
          {{ r.type === 'income' ? '+' : '-' }}¥{{ r.amount }}
        </text>
      </view>
    </view>
    <wd-button size="large" type="primary" @click="confirm">确认导入</wd-button>
  </view>
</template>
<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { billApi } from '../../pages-homeai/api/bill'

const rows = ref<any[]>([])

const selectedCount = computed(() => rows.value.filter((r) => r._checked !== false).length)

function toggle(i: number) {
  rows.value[i]._checked = rows.value[i]._checked === false
}

onLoad((opts: any) => {
  if (opts?.rows) {
    try {
      rows.value = JSON.parse(decodeURIComponent(opts.rows)).map((r: any) => ({ ...r, _checked: true }))
    } catch {
      uni.showToast({ title: '数据错误', icon: 'none' })
    }
  }
})

async function confirm() {
  const entries = rows.value
    .filter((r) => r._checked !== false)
    .map(({ billDate, type, categoryId, amount, remark, paymentMethod }) => ({
      billDate, type, categoryId, amount, remark, paymentMethod: paymentMethod || '微信',
    }))
  if (!entries.length) {
    uni.showToast({ title: '请至少选择一条', icon: 'none' })
    return
  }
  uni.showLoading({ title: '导入中...' })
  try {
    await billApi.importConfirm(entries)
    uni.hideLoading()
    uni.showToast({ title: '导入成功', icon: 'success' })
    setTimeout(() => uni.navigateBack({ delta: 2 }), 800)
  } catch (e: any) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '导入失败', icon: 'none' })
  }
}
</script>
<style scoped>
/* page shell: .hai-page */
.summary { display: block; font-size: 26rpx; color: var(--hai-text-secondary); margin-bottom: 16rpx; padding: 0 8rpx; }
.row { display: flex; align-items: center; gap: 16rpx; padding: 24rpx; background: var(--hai-card); border-radius: 24rpx; margin-bottom: 12rpx; box-shadow: var(--hai-shadow); }
.check { font-size: 32rpx; color: var(--hai-primary); }
.info { flex: 1; }
.line { font-size: 26rpx; color: var(--hai-text); display: block; }
.amount { font-size: 30rpx; font-weight: 600; display: block; margin-top: 4rpx; }
.red { color: var(--hai-danger); }
.green { color: var(--hai-success); }
</style>
