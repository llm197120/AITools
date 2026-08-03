<route lang="json5">
{ style: { navigationBarTitleText: '新增菜谱' } }
</route>

<template>
  <view class="page">
    <view class="form-group"><text class="label">菜名</text><input class="input" v-model="form.name" placeholder="输入菜名"/></view>
    <view class="form-group"><text class="label">分类</text><input class="input" v-model="form.categoryId" placeholder="如：热菜"/></view>
    <view class="form-group"><text class="label">难度</text>
      <view class="radio-group">
        <view :class="'radio '+(form.difficulty==='easy'?'active':'')" @click="form.difficulty='easy'">简单</view>
        <view :class="'radio '+(form.difficulty==='medium'?'active':'')" @click="form.difficulty='medium'">中等</view>
        <view :class="'radio '+(form.difficulty==='hard'?'active':'')" @click="form.difficulty='hard'">困难</view>
      </view>
    </view>
    <view class="form-group"><text class="label">烹饪时间(分钟)</text><input class="input" v-model="form.cookTime" type="number"/></view>
    <view class="form-group">
      <text class="label">食材清单</text>
      <view class="ing-row" v-for="(ing,i) in ingredients" :key="i">
        <input class="input small" v-model="ing.name" placeholder="食材"/>
        <input class="input small" v-model="ing.amount" placeholder="用量"/>
        <text class="del-btn" @click="ingredients.splice(i,1)">✕</text>
      </view>
      <text class="add-btn" @click="ingredients.push({name:'',amount:''})">+ 添加食材</text>
    </view>
    <view class="form-group">
      <text class="label">烹饪步骤</text>
      <view class="step-row" v-for="(s,i) in steps" :key="i">
        <text class="step-no">{{ i+1 }}</text>
        <input class="input" v-model="s.description" placeholder="步骤说明"/>
        <text class="del-btn" @click="steps.splice(i,1)">✕</text>
      </view>
      <text class="add-btn" @click="steps.push({description:''})">+ 添加步骤</text>
    </view>
    <view class="form-group"><text class="label">小贴士</text><textarea class="input textarea" v-model="form.tips"/></view>
    <wd-button size="large" type="primary" @click="submit">保存</wd-button>
  </view>
</template>

<script lang="ts" setup>
import { ref, reactive } from '@dcloudio/uni-app'
import { post as postApi } from '../../pages-homeai/api/request'
const form = reactive({name:'',categoryId:'',difficulty:'medium',cookTime:30,tips:'',visibility:'family'})
const ingredients = ref([{name:'',amount:''}])
const steps = ref([{description:''}])
async function submit() {
  await postApi('/recipe', { data: { ...form, cookTime: parseInt(String(form.cookTime)), ingredients: ingredients.value.filter(i=>i.name), steps: steps.value.filter(s=>s.description) } })
  uni.showToast({title:'创建成功', icon:'success'})
  setTimeout(()=>uni.navigateBack(), 1000)
}
</script>

<style scoped>
.page{min-height:100vh;background:#f5f5f5;padding:20rpx}
.form-group{padding:20rpx 0}.label{font-size:26rpx;color:#666;margin-bottom:12rpx;display:block}
.input{height:72rpx;padding:0 20rpx;background:#fff;border-radius:10rpx;font-size:28rpx;width:100%}
.input.small{flex:1;height:60rpx}.textarea{height:120rpx;padding:12rpx 20rpx}
.radio-group{display:flex;gap:12rpx}.radio{flex:1;text-align:center;padding:16rpx;background:#fff;border-radius:10rpx;font-size:26rpx}.radio.active{background:#667eea;color:#fff}
.ing-row,.step-row{display:flex;align-items:center;gap:8rpx;margin-bottom:8rpx}
.step-no{width:40rpx;height:40rpx;line-height:40rpx;text-align:center;background:#667eea;color:#fff;border-radius:50%;font-size:22rpx}
.del-btn{color:#e74c3c;font-size:28rpx;cursor:pointer}.add-btn{color:#667eea;font-size:26rpx;padding:12rpx 0;display:block}
</style>