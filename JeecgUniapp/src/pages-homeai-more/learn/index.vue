<route lang="json5">
{ style: { navigationBarTitleText: '学习模块' } }
</route>

<template>
  <view class="page">
    <view class="stats-card">
      <text class="stats-num">{{ stats.totalRecords }}</text>
      <text class="stats-label">次学习记录</text>
      <text class="stats-num">{{ stats.totalDuration }}分钟</text>
      <text class="stats-label">总时长</text>
    </view>
    <view class="material-item" v-for="m in materials" :key="m.id" @click="startLearn(m)">
      <view class="mat-icon">{{ getTypeIcon(m.type) }}</view>
      <view class="mat-info">
        <text class="mat-title">{{ m.title }}</text>
        <text class="mat-cat">{{ m.category }} · {{ m.type }}</text>
      </view>
      <wd-icon name="arrow-right" size="14px" color="#ccc"></wd-icon>
    </view>
    <view class="fab" @click="showAdd">+</view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get as getApi, post as postApi } from '../../pages-homeai/api/request'
const materials = ref<any[]>([])
const stats = ref({totalRecords:0,totalDuration:0})
onShow(async () => {
  materials.value = await getApi('/learn/materials')
  stats.value = await getApi('/learn/statistics')
})
function getTypeIcon(t:string) { const m:any={pdf:'📄',video:'🎬',link:'🔗',note:'📝',image:'🖼️'}; return m[t]||'📎' }
function startLearn(m:any) {
  uni.showModal({
    title:m.title, content:'开始学习？',
    success: async (res)=>{
      if(res.confirm){ await postApi('/learn/record',{data:{materialId:m.id,duration:30,progress:50,recordType:'timer'}}); onShow() }
    }
  })
}
function showAdd() {
  uni.showModal({title:'添加资料',editable:true,placeholderText:'资料标题',
    success: async (res)=>{ if(res.confirm&&res.content){ await postApi('/learn/material',{data:{title:res.content,category:'其他',type:'note',visibility:'private'}}); onShow() } }
  })
}
</script>

<style scoped>
.page{min-height:100vh;background:#f5f5f5;padding:20rpx}
.stats-card{display:flex;flex-wrap:wrap;background:#fff;border-radius:12rpx;padding:24rpx;margin-bottom:20rpx}
.stats-num{width:50%;font-size:36rpx;font-weight:700;text-align:center}.stats-label{width:50%;font-size:22rpx;color:#999;text-align:center;margin-bottom:12rpx}
.material-item{display:flex;align-items:center;padding:24rpx;background:#fff;border-radius:12rpx;margin-bottom:12rpx;gap:16rpx}
.mat-icon{font-size:32rpx}.mat-info{flex:1}.mat-title{font-size:28rpx}.mat-cat{font-size:22rpx;color:#999;display:block}
.fab{position:fixed;right:40rpx;bottom:100rpx;width:100rpx;height:100rpx;background:#27ae60;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:48rpx;color:#fff}
</style>
