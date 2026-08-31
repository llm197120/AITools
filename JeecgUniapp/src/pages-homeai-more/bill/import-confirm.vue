<route lang="json5">{ style: { navigationBarTitleText: '导入确认', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view class="hai-page hai-page--import">
    <text class="summary">
      共 {{ rows.length }} 条，已选 {{ selectedCount }} 条
      <text v-if="totalPages > 1"> · 第 {{ page }}/{{ totalPages }} 页</text>
    </text>
    <view class="row" v-for="(r, i) in pageRows" :key="pageStart + i" @click="toggle(pageStart + i)">
      <text class="check">{{ r._checked !== false ? '☑' : '☐' }}</text>
      <view class="info">
        <text class="line">{{ r.billDate }} · {{ r.remark || r.categoryName || '-' }}</text>
        <text class="amount" :class="r.type === 'income' ? 'green' : 'red'">
          {{ r.type === 'income' ? '+' : '-' }}¥{{ r.amount }}
        </text>
      </view>
    </view>
    <view v-if="totalPages > 1" class="pager">
      <text class="pager-btn" :class="{ disabled: page <= 1 }" @click="prevPage">上一页</text>
      <text class="pager-btn" :class="{ disabled: page >= totalPages }" @click="nextPage">下一页</text>
    </view>
    <view class="footer">
      <text v-if="rows.length" class="select-page" @click="togglePage">{{ pageAllSelected ? '取消本页' : '本页全选' }}</text>
      <wd-button size="large" type="primary" :loading="importing" @click="confirm">确认导入</wd-button>
    </view>
  </view>
</template>
<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { billApi } from '../../pages-homeai/api/bill'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'

useHomeaiPageGuard()

const PAGE_SIZE = 50
const rows = ref<any[]>([])
const importing = ref(false)
const page = ref(1)

const selectedCount = computed(() => rows.value.filter((r) => r._checked !== false).length)
const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / PAGE_SIZE)))
const pageStart = computed(() => (page.value - 1) * PAGE_SIZE)
const pageRows = computed(() => rows.value.slice(pageStart.value, pageStart.value + PAGE_SIZE))
const pageAllSelected = computed(() =>
  pageRows.value.length > 0 && pageRows.value.every((r) => r._checked !== false),
)

function toggle(i: number) {
  rows.value[i]._checked = rows.value[i]._checked === false
}

function togglePage() {
  const allOn = pageAllSelected.value
  pageRows.value.forEach((r) => {
    r._checked = !allOn
  })
}

function prevPage() {
  if (page.value <= 1) return
  page.value -= 1
}

function nextPage() {
  if (page.value >= totalPages.value) return
  page.value += 1
}

onLoad((opts: any) => {
  try {
    const cached = uni.getStorageSync('homeai_bill_import_preview')
    const raw = Array.isArray(cached)
      ? cached
      : opts?.rows
        ? JSON.parse(decodeURIComponent(opts.rows))
        : []
    rows.value = raw.map((r: any) => ({ ...r, _checked: true }))
    if (!rows.value.length) {
      uni.showToast({ title: '没有可导入的账单', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '数据错误', icon: 'none' })
  }
})

async function confirm() {
  if (importing.value) return
  const entries = rows.value
    .filter((r) => r._checked !== false)
    .map(({ billDate, type, categoryId, amount, remark, paymentMethod }) => ({
      billDate, type, categoryId, amount, remark, paymentMethod: paymentMethod || '微信',
    }))
  if (!entries.length) {
    uni.showToast({ title: '请至少选择一条', icon: 'none' })
    return
  }
  importing.value = true
  uni.showLoading({ title: '导入中...' })
  try {
    await billApi.importConfirm(entries)
    uni.removeStorageSync('homeai_bill_import_preview')
    uni.hideLoading()
    uni.showToast({ title: '导入成功', icon: 'success' })
    setTimeout(() => uni.navigateBack({ delta: 2 }), 800)
  } catch (e: any) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '导入失败', icon: 'none' })
  } finally {
    importing.value = false
  }
}
</script>
<style scoped>
/* page shell: .hai-page */
.hai-page--import { padding-bottom: calc(200rpx + env(safe-area-inset-bottom)); }
.summary { display: block; font-size: 26rpx; color: var(--hai-text-secondary); margin-bottom: 16rpx; padding: 0 8rpx; }
.row { display: flex; align-items: center; gap: 16rpx; padding: 24rpx; background: var(--hai-card); border-radius: 24rpx; margin-bottom: 12rpx; box-shadow: var(--hai-shadow); }
.check { font-size: 32rpx; color: var(--hai-primary); }
.info { flex: 1; }
.line { font-size: 26rpx; color: var(--hai-text); display: block; }
.amount { font-size: 30rpx; font-weight: 600; display: block; margin-top: 4rpx; }
.red { color: var(--hai-danger); }
.green { color: var(--hai-success); }
.pager { display: flex; justify-content: center; gap: 32rpx; padding: 16rpx 0 24rpx; }
.pager-btn { font-size: 26rpx; color: var(--hai-primary); }
.pager-btn.disabled { color: var(--hai-text-muted); }
.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 32rpx calc(16rpx + env(safe-area-inset-bottom));
  background: var(--hai-bg);
  box-shadow: 0 -8rpx 24rpx rgba(0, 0, 0, 0.04);
}
.select-page { display: block; text-align: center; font-size: 24rpx; color: var(--hai-primary); margin-bottom: 12rpx; }
</style>
