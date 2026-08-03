<route lang="json5">
{ style: { navigationBarTitleText: '日常计划' } }
</route>

<template>
  <view class="page">
    <view class="header-card"><text class="greeting">{{ today }} 有 {{ plans.length }} 项计划</text></view>
    <view class="plan-item" v-for="p in plans" :key="p.id" @click="detail(p)">
      <view class="plan-left">
        <view class="priority-dot" :class="p.priority || 'normal'"></view>
        <view>
          <text class="plan-title">{{ p.title }}</text>
          <text class="plan-cat">{{ p.category }} · {{ p.isAllDay ? '全天' : '定时' }}</text>
        </view>
      </view>
      <wd-icon name="arrow-right" size="14px" color="#ccc"></wd-icon>
    </view>
    <view class="fab" @click="showAdd">+</view>
  </view>
</template>

<script lang="ts" setup>
import { ref, onShow } from '@dcloudio/uni-app'
import { get as getApi, post as postApi } from '../../pages-homeai/api/request'
const today = ref(new Date().toISOString().substring(0,10))
const plans = ref<any[]>([])
onShow(async () => {
  const cal = await getApi('/plan/calendar', {params:{month:today.value.substring(0,7)}})
  plans.value = cal[today.value] || []
})
function detail(p:any) { uni.showToast({title:p.title, icon:'none'}) }
function showAdd() {
  uni.showModal({
    title:'新增计划', editable:true, placeholderText:'计划标题',
    success: async (res) => {
      if(res.confirm && res.content){
        await postApi('/plan', { data: { title: res.content, planDate: today.value, priority: 'normal', category: '生活', isAllDay: 1 } })
        onShow()
      }
    }
  })
}
</script>

<style scoped>
.page{min-height:100vh;background:#f5f5f5;padding:20rpx}
.header-card{background:linear-gradient(135deg,#667eea,#764ba2);padding:30rpx;border-radius:16rpx;margin-bottom:20rpx}
.greeting{color:#fff;font-size:30rpx;font-weight:600}
.plan-item{display:flex;align-items:center;justify-content:space-between;padding:24rpx;background:#fff;border-radius:12rpx;margin-bottom:12rpx}
.plan-left{display:flex;align-items:center;gap:16rpx}
.priority-dot{width:16rpx;height:16rpx;border-radius:50%}
.priority-dot.normal{background:#999}.priority-dot.important{background:#f39c12}.priority-dot.urgent{background:#e74c3c}
.plan-title{font-size:28rpx;color:#333;display:block}.plan-cat{font-size:22rpx;color:#999}
.fab{position:fixed;right:40rpx;bottom:100rpx;width:100rpx;height:100rpx;background:#667eea;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:48rpx;color:#fff}
</style>