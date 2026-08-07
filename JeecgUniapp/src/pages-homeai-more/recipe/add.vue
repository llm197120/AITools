<route lang="json5">
{
  style: {
    navigationBarTitleText: '新增菜谱',
  },
}
</route>

<template>
  <view class="page">
    <!-- 封面 -->
    <view class="cover-block" @click="chooseCover">
      <image v-if="form.coverUrl" class="cover-img" :src="form.coverUrl" mode="aspectFill" />
      <view v-else class="cover-empty">
        <text class="cover-icon">🖼️</text>
        <text class="cover-tip">点击上传封面</text>
      </view>
    </view>

    <!-- 基本信息 -->
    <view class="form-card">
      <view class="form-group">
        <text class="label">菜名</text>
        <input class="input" v-model="form.name" placeholder="输入菜名" />
      </view>
      <view class="form-group">
        <text class="label">分类</text>
        <picker :range="categories" range-key="name" @change="onCategoryChange">
          <view class="picker-value">{{ currentCategoryName || '选择分类' }}</view>
        </picker>
      </view>
      <view class="form-group">
        <text class="label">难度</text>
        <view class="radio-group">
          <view :class="'radio '+(form.difficulty==='1'?'active':'')" @click="form.difficulty='1'">简单</view>
          <view :class="'radio '+(form.difficulty==='3'?'active':'')" @click="form.difficulty='3'">中等</view>
          <view :class="'radio '+(form.difficulty==='5'?'active':'')" @click="form.difficulty='5'">困难</view>
        </view>
      </view>
      <view class="form-group row">
        <view class="half">
          <text class="label">烹饪时间(分钟)</text>
          <input class="input" type="number" v-model="form.cookTime" />
        </view>
        <view class="half">
          <text class="label">份数</text>
          <input class="input" type="number" v-model="form.servings" />
        </view>
      </view>
      <view class="form-group">
        <text class="label">可见性</text>
        <view class="radio-group">
          <view :class="'radio '+(form.visibility==='private'?'active':'')" @click="form.visibility='private'">仅自己</view>
          <view :class="'radio '+(form.visibility==='family'?'active':'')" @click="form.visibility='family'">家庭共享</view>
        </view>
      </view>
    </view>

    <!-- 食材清单 -->
    <view class="form-card">
      <view class="card-title">食材清单</view>
      <view class="ing-row" v-for="(ing, i) in ingredients" :key="i">
        <input class="input small" v-model="ing.name" placeholder="食材名" />
        <input class="input small" v-model="ing.amount" placeholder="用量" />
        <text class="del-btn" @click="ingredients.splice(i, 1)">✕</text>
      </view>
      <text class="add-btn" @click="ingredients.push({ name: '', amount: '' })">+ 添加食材</text>
    </view>

    <!-- 烹饪步骤 -->
    <view class="form-card">
      <view class="card-title">烹饪步骤</view>
      <view class="step-item" v-for="(s, i) in steps" :key="i">
        <view class="step-head">
          <text class="step-no">{{ i + 1 }}</text>
          <view class="step-ops">
            <text class="op-btn" @click="moveStep(i, -1)">↑</text>
            <text class="op-btn" @click="moveStep(i, 1)">↓</text>
            <text class="op-btn danger" @click="steps.splice(i, 1)">✕</text>
          </view>
        </view>
        <view class="step-img-block" @click="chooseStepImage(i)">
          <image v-if="s.imageUrl" class="step-img" :src="s.imageUrl" mode="aspectFill" />
          <view v-else class="step-img-empty">📷 添加步骤图</view>
        </view>
        <textarea class="step-input" v-model="s.description" placeholder="步骤说明..." :maxlength="300" />
      </view>
      <text class="add-btn" @click="addStep">+ 添加步骤</text>
    </view>

    <!-- 做菜视频 -->
    <view class="form-card">
      <view class="card-title">做菜视频（可选）</view>
      <view v-if="form.videoUrl" class="video-block">
        <video class="video-preview" :src="form.videoUrl" controls></video>
        <text class="del-btn center" @click="form.videoUrl = ''">删除视频</text>
      </view>
      <view v-else class="video-upload" @click="chooseVideo">🎬 选择视频上传</view>
    </view>

    <!-- 小贴士 -->
    <view class="form-card">
      <view class="card-title">小贴士</view>
      <textarea class="tips-input" v-model="form.tips" placeholder="添加小贴士（可选）" />
    </view>

    <wd-button size="large" type="primary" :loading="saving" @click="submit">保存</wd-button>
    <view style="height: 40rpx"></view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get as getApi, post as postApi, put as putApi, getServerBaseUrl } from '../../pages-homeai/api/request'

const editId = ref('')
const saving = ref(false)
const categories = ref<any[]>([])

const form = ref<any>({
  name: '',
  categoryId: '',
  difficulty: '3',
  cookTime: 30,
  servings: 2,
  coverUrl: '',
  videoUrl: '',
  visibility: 'family',
  tips: '',
})
const ingredients = ref<any[]>([{ name: '', amount: '' }])
const steps = ref<any[]>([{ description: '', imageUrl: '' }])

const currentCategoryName = ref('')

async function loadCategories() {
  try {
    const res: any = await getApi('/recipe/category/all')
    categories.value = Array.isArray(res) ? res : []
  } catch {
    categories.value = []
  }
}

function onCategoryChange(e: any) {
  const c = categories.value[e.detail.value]
  if (c) {
    form.value.categoryId = c.id
    currentCategoryName.value = c.name
  }
}

async function loadDetail(id: string) {
  const res: any = await getApi(`/recipe/${id}`)
  if (!res || !res.recipe) return
  const r = res.recipe
  form.value = {
    name: r.name || '',
    categoryId: r.categoryId || '',
    difficulty: String(r.difficulty || 3),
    cookTime: r.cookTime || 30,
    servings: r.servings || 2,
    coverUrl: r.coverUrl || '',
    videoUrl: r.videoUrl || '',
    visibility: r.visibility || 'family',
    tips: r.tips || '',
  }
  const cat = categories.value.find((c: any) => c.id === r.categoryId)
  currentCategoryName.value = cat?.name || ''
  ingredients.value = (res.ingredients || []).map((x: any) => ({ name: x.name, amount: x.amount }))
  ingredients.value.push({ name: '', amount: '' })
  steps.value = (res.steps || []).map((x: any) => ({ description: x.description, imageUrl: x.imageUrl }))
}

onLoad(async (opts: any) => {
  await loadCategories()
  if (opts?.id) {
    editId.value = opts.id
    uni.setNavigationBarTitle({ title: '编辑菜谱' })
    await loadDetail(opts.id)
  }
})

function addStep() {
  steps.value.push({ description: '', imageUrl: '' })
}

function moveStep(i: number, dir: number) {
  const j = i + dir
  if (j < 0 || j >= steps.value.length) return
  const tmp = steps.value[i]
  steps.value[i] = steps.value[j]
  steps.value[j] = tmp
}

function uploadFile(filePath: string, url: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('homeai_token')
    uni.uploadFile({
      url: getServerBaseUrl() + url,
      filePath,
      name: 'file',
      header: { 'X-Access-Token': token },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.success && data.result) resolve(data.result)
          else reject(new Error(data.message || '上传失败'))
        } catch (e) {
          reject(e)
        }
      },
      fail: reject,
    })
  })
}

function chooseCover() {
  uni.chooseImage({
    count: 1,
    success: async (r) => {
      if (!r.tempFilePaths || !r.tempFilePaths[0]) return
      uni.showLoading({ title: '上传中...' })
      try {
        form.value.coverUrl = await uploadFile(r.tempFilePaths[0], '/homeai/recipe/cover')
        uni.hideLoading()
      } catch (e: any) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '封面上传失败', icon: 'none' })
      }
    },
  })
}

function chooseVideo() {
  uni.chooseVideo({
    sourceType: ['album', 'camera'],
    maxDuration: 30,
    success: async (r) => {
      if (!r.tempFilePath) return
      uni.showLoading({ title: '上传中...' })
      try {
        form.value.videoUrl = await uploadFile(r.tempFilePath, '/homeai/recipe/video')
        uni.hideLoading()
      } catch (e: any) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '视频上传失败', icon: 'none' })
      }
    },
  })
}

function chooseStepImage(i: number) {
  uni.chooseImage({
    count: 1,
    success: async (r) => {
      if (!r.tempFilePaths || !r.tempFilePaths[0]) return
      uni.showLoading({ title: '上传中...' })
      try {
        steps.value[i].imageUrl = await uploadFile(r.tempFilePaths[0], '/homeai/recipe/step-image')
        uni.hideLoading()
      } catch (e: any) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '图片上传失败', icon: 'none' })
      }
    },
  })
}

async function submit() {
  if (!form.value.name.trim()) {
    uni.showToast({ title: '请输入菜名', icon: 'none' })
    return
  }
  saving.value = true
  const data = {
    ...form.value,
    cookTime: parseInt(String(form.value.cookTime)) || 30,
    servings: parseInt(String(form.value.servings)) || 2,
    ingredients: ingredients.value.filter((x: any) => x.name),
    steps: steps.value
      .filter((x: any) => x.description)
      .map((x: any, i: number) => ({ description: x.description, imageUrl: x.imageUrl || null, stepNum: i + 1 })),
  }
  try {
    if (editId.value) {
      await putApi('/recipe', { data: { id: editId.value, ...data } })
    } else {
      const created: any = await postApi('/recipe', { data })
      editId.value = created.id
    }
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: `/pages-homeai-more/recipe/detail?id=${editId.value}` })
    }, 800)
  } catch (e: any) {
    uni.showToast({ title: e.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20rpx 20rpx 40rpx;
}
.cover-block {
  height: 320rpx;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
  background: #fff;
}
.cover-img {
  width: 100%;
  height: 100%;
}
.cover-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.cover-icon {
  font-size: 64rpx;
}
.cover-tip {
  font-size: 24rpx;
  color: #999;
  margin-top: 12rpx;
}
.form-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
}
.form-group {
  margin-bottom: 20rpx;
}
.form-group.row {
  display: flex;
  gap: 20rpx;
}
.half {
  flex: 1;
}
.label {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
  display: block;
}
.input {
  height: 72rpx;
  padding: 0 20rpx;
  background: #f8f8f8;
  border-radius: 10rpx;
  font-size: 28rpx;
  width: 100%;
}
.input.small {
  flex: 1;
  height: 60rpx;
}
.picker-value {
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 20rpx;
  background: #f8f8f8;
  border-radius: 10rpx;
  font-size: 28rpx;
  color: #333;
}
.radio-group {
  display: flex;
  gap: 12rpx;
}
.radio {
  flex: 1;
  text-align: center;
  padding: 16rpx;
  background: #f8f8f8;
  border-radius: 10rpx;
  font-size: 26rpx;
}
.radio.active {
  background: #667eea;
  color: #fff;
}
.ing-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 12rpx;
}
.del-btn {
  color: #e74c3c;
  font-size: 28rpx;
  padding: 8rpx;
}
.del-btn.center {
  display: block;
  text-align: center;
  margin-top: 12rpx;
}
.add-btn {
  color: #667eea;
  font-size: 26rpx;
  padding: 12rpx 0;
  display: block;
}
.step-item {
  border: 1rpx solid #f0f0f0;
  border-radius: 12rpx;
  padding: 16rpx;
  margin-bottom: 16rpx;
}
.step-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}
.step-no {
  width: 44rpx;
  height: 44rpx;
  line-height: 44rpx;
  text-align: center;
  background: #667eea;
  color: #fff;
  border-radius: 50%;
  font-size: 22rpx;
}
.step-ops {
  display: flex;
  gap: 16rpx;
}
.op-btn {
  color: #667eea;
  font-size: 28rpx;
}
.op-btn.danger {
  color: #e74c3c;
}
.step-img-block {
  margin-bottom: 12rpx;
}
.step-img {
  width: 100%;
  height: 220rpx;
  border-radius: 10rpx;
}
.step-img-empty {
  height: 120rpx;
  background: #f8f8f8;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 26rpx;
}
.step-input {
  width: 100%;
  min-height: 100rpx;
  font-size: 28rpx;
  padding: 12rpx;
  background: #f8f8f8;
  border-radius: 10rpx;
  box-sizing: border-box;
}
.video-upload {
  height: 140rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
  font-size: 28rpx;
}
.video-preview {
  width: 100%;
  height: 360rpx;
  border-radius: 12rpx;
}
.tips-input {
  width: 100%;
  min-height: 120rpx;
  font-size: 28rpx;
  padding: 12rpx;
  background: #f8f8f8;
  border-radius: 10rpx;
  box-sizing: border-box;
}
</style>
