<route lang="json5">{ style: { navigationBarTitleText: '新增计划' } }</route>
<template><view class="plan-add">
  <input class="title-input" v-model="form.title" placeholder="计划标题..." />
  <textarea class="content-input" v-model="form.content" placeholder="详细内容（可选）" />
  <view class="row"><text>日期</text><picker mode="date" :value="form.planDate" @change="(e:any) => form.planDate = e.detail.value"><text>{{ form.planDate }}</text></picker></view>
  <view class="row"><text>优先级</text><picker :range="['普通','重要','紧急']" @change="(e:any) => form.priority = ['normal','important','urgent'][e.detail.value]"><text>{{ {normal:'普通',important:'重要',urgent:'紧急'}[form.priority] }}</text></picker></view>
  <view class="row"><text>分类</text><picker :range="['工作','学习','生活','运动','家庭']" @change="(e:any) => form.category = ['work','study','life','sport','family'][e.detail.value]"><text>{{ form.category }}</text></picker></view>
  <wd-button size="large" type="primary" @click="save">保存</wd-button>
</view></template>
<script lang="ts" setup>
import { ref } from 'vue'
import { post as postApi } from '../../pages-homeai/api/request';
const form = ref({ title: '', content: '', planDate: new Date().toISOString().substring(0, 10), priority: 'normal', category: 'life', remindBefore: 15 });
async function save() {
  if (!form.value.title) { uni.showToast({ title: '请输入标题', icon: 'none' }); return; }
  await postApi('/plan', { data: form.value }); uni.showToast({ title: '创建成功', icon: 'success' }); setTimeout(() => uni.navigateBack(), 1000);
}
</script>
<style scoped>.plan-add{padding:30rpx;min-height:100vh;background:#f5f5f5}.title-input{width:100%;font-size:32rpx;padding:20rpx;background:#fff;border-radius:12rpx;margin-bottom:20rpx}.content-input{width:100%;min-height:150rpx;font-size:28rpx;padding:20rpx;background:#fff;border-radius:12rpx;margin-bottom:20rpx}.row{display:flex;justify-content:space-between;padding:24rpx 20rpx;background:#fff;border-radius:12rpx;margin-bottom:16rpx;font-size:28rpx}</style>
