<route lang="json5">
{
  style: {
    navigationBarTitleText: '新增菜谱',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view v-if="editId && loading" class="hai-page"><HomeSkeleton variant="card" /></view>
  <HomeEmpty
    v-else-if="editId && loadFailed"
    title="菜谱加载失败"
    hint="请检查网络后重试"
    action-text="重试"
    :card="true"
    @action="retryEdit"
  />
  <HomeFormCard v-else>
    <!-- 封面 -->
    <HomeMediaUpload
      v-model="form.coverUrl"
      mode="image"
      url="/homeai/recipe/cover"
      placeholder="点击上传封面"
      tip="支持 jpg/png/webp，建议比例 16:9"
      :height="320"
    />

    <!-- 基本信息 -->
    <view class="home-form-group">
      <wd-cell-group border>
        <wd-cell title="菜名" title-width="180rpx" center>
          <input class="home-form-cell-input" v-model="form.name" placeholder="输入菜名" />
        </wd-cell>
        <HomePickerCell
          v-model="form.categoryId"
          label="分类"
          title="选择分类"
          placeholder="请选择分类"
          :columns="categoryColumns"
          :filterable="categoryColumns.length > 8"
        />
        <text v-if="catFailed" class="cat-fail" @click="loadCategories">分类加载失败，点此重试</text>
        <wd-cell title="难度" title-width="180rpx">
          <view class="radio-group">
            <view :class="'radio '+(form.difficulty==='1'?'active':'')" @click="form.difficulty='1'">入门</view>
            <view :class="'radio '+(form.difficulty==='2'?'active':'')" @click="form.difficulty='2'">简单</view>
            <view :class="'radio '+(form.difficulty==='3'?'active':'')" @click="form.difficulty='3'">中等</view>
            <view :class="'radio '+(form.difficulty==='4'?'active':'')" @click="form.difficulty='4'">较难</view>
            <view :class="'radio '+(form.difficulty==='5'?'active':'')" @click="form.difficulty='5'">困难</view>
          </view>
        </wd-cell>
        <wd-cell title="烹饪时间" title-width="180rpx" center>
          <input class="home-form-cell-input" type="number" v-model="form.cookTime" placeholder="分钟" />
        </wd-cell>
        <wd-cell title="份数" title-width="180rpx" center>
          <input class="home-form-cell-input" type="number" v-model="form.servings" />
        </wd-cell>
        <wd-cell title="可见性" title-width="180rpx">
          <view class="radio-group">
            <view :class="'radio '+(form.visibility==='private'?'active':'')" @click="setVisibility('private')">仅自己</view>
            <view :class="'radio '+(form.visibility==='family'?'active':'')" @click="setVisibility('family')">家庭共享</view>
            <view :class="'radio '+(form.visibility==='public'?'active':'')" @click="setVisibility('public')">公开</view>
          </view>
        </wd-cell>
      </wd-cell-group>
    </view>

    <!-- 食材清单 -->
    <view class="form-card">
      <view class="card-title">食材清单</view>
      <view class="ing-row" v-for="(ing, i) in ingredients" :key="i">
        <input class="input small" v-model="ing.name" placeholder="食材名" />
        <input class="input small" v-model="ing.amount" placeholder="用量" />
        <text class="del-btn hai-press" @click="ingredients.splice(i, 1)">删除</text>
      </view>
      <view class="add-btn hai-press" @click="ingredients.push({ name: '', amount: '' })">+ 添加食材</view>
    </view>

    <!-- 烹饪步骤 -->
    <view class="form-card">
      <view class="card-title">烹饪步骤</view>
      <view class="step-item" v-for="(s, i) in steps" :key="i">
        <view class="step-head">
          <text class="step-no">{{ i + 1 }}</text>
          <view class="step-ops">
            <text class="op-btn hai-press" @click="moveStep(i, -1)">上移</text>
            <text class="op-btn hai-press" @click="moveStep(i, 1)">下移</text>
            <text class="op-btn danger hai-press" @click="steps.splice(i, 1)">删除</text>
          </view>
        </view>
        <HomeMediaUpload
          v-model="s.imageUrl"
          mode="image"
          url="/homeai/recipe/step-image"
          placeholder="📷 添加步骤图"
          :height="140"
        />
        <textarea class="step-input" v-model="s.description" placeholder="步骤说明..." :maxlength="300" />
      </view>
      <view class="add-btn hai-press" @click="addStep">+ 添加步骤</view>
    </view>

    <!-- 做菜视频 -->
    <view class="form-card">
      <view class="card-title">做菜视频（可选）</view>
      <HomeMediaUpload
        v-model="form.videoUrl"
        mode="video"
        url="/homeai/recipe/video"
        placeholder="选择视频上传"
        tip="支持 mp4/webm/mov 等常见视频格式"
        :max-size="200"
        :max-video-duration="30"
      />
    </view>

    <!-- 小贴士 -->
    <view class="form-card">
      <view class="card-title">小贴士</view>
      <textarea class="tips-input" v-model="form.tips" placeholder="添加小贴士（可选）" />
    </view>

    <wd-button class="home-form-save" size="large" type="primary" block round :loading="saving" @click="submit">保存</wd-button>
    <view style="height: 40rpx"></view>
  </HomeFormCard>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { recipeApi } from '../../pages-homeai/api/recipe'
import { mutate } from '../../pages-homeai/offline/dataAccess'
import { useFamilyStore } from '../../pages-homeai/stores/family'
import { formatQuantityUnit, parseAmountToQuantityUnit } from '../../pages-homeai/utils/recipeIngredient'
import HomeFormCard from '../../components/HomeFormCard.vue'
import HomeMediaUpload from '../../pages-homeai/components/HomeMediaUpload.vue'
import HomePickerCell from '../../pages-homeai/components/HomePickerCell.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'

useHomeaiPageGuard()

const editId = ref('')
const saving = ref(false)
const loadFailed = ref(false)
const loading = ref(false)
const catFailed = ref(false)
const categories = ref<any[]>([])
const familyStore = useFamilyStore()

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

const categoryColumns = computed(() =>
  categories.value
    .filter((c: any) => c?.id && c?.name)
    .map((c: any) => ({ label: String(c.name), value: String(c.id) })),
)

function setVisibility(v: string) {
  if (v === 'family' && !familyStore.hasFamily) {
    uni.showToast({ title: '加入家庭后才能共享菜谱', icon: 'none' })
    return
  }
  if (v === 'public' && form.value.visibility !== 'public') {
    uni.showModal({
      title: '设为公开',
      content: '公开后所有登录用户都能看到这道菜，确定？',
      success: (r) => {
        if (r.confirm) form.value.visibility = 'public'
      },
    })
    return
  }
  form.value.visibility = v
}

async function loadCategories() {
  catFailed.value = false
  try {
    const res: any = await recipeApi.categories()
    categories.value = Array.isArray(res) ? res : []
  } catch {
    catFailed.value = categories.value.length === 0
    uni.showToast({ title: '分类加载失败', icon: 'none' })
  }
}

async function retryEdit() {
  if (editId.value) await loadDetail(editId.value)
}

async function loadDetail(id: string) {
  loading.value = true
  loadFailed.value = false
  try {
  const res: any = await recipeApi.detail(id)
  if (!res || !res.recipe) {
    loadFailed.value = true
    return
  }
  if (res.canModify !== true) {
    uni.showToast({ title: '无权编辑该菜谱', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 400)
    return
  }
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
  ingredients.value = (res.ingredients || []).map((x: any) => ({
    name: x.name,
    amount: formatQuantityUnit(x.quantity, x.unit, x.amount),
  }))
  ingredients.value.push({ name: '', amount: '' })
  steps.value = (res.steps || []).map((x: any) => ({ description: x.description, imageUrl: x.imageUrl }))
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

onLoad(async (opts: any) => {
  await familyStore.fetchFamilyInfo()
  if (!opts?.id && !familyStore.hasFamily) {
    form.value.visibility = 'private'
  }
  await loadCategories()
  if (opts?.categoryId && !opts?.id) form.value.categoryId = opts.categoryId
  if (opts?.id) {
    editId.value = opts.id
    loading.value = true
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

async function submit() {
  if (saving.value) return
  if (!form.value.name.trim()) {
    uni.showToast({ title: '请输入菜名', icon: 'none' })
    return
  }
  if (!form.value.categoryId) {
    uni.showToast({ title: '请选择分类', icon: 'none' })
    return
  }
  if (form.value.visibility === 'family' && !familyStore.hasFamily) {
    uni.showToast({ title: '加入家庭后才能共享菜谱', icon: 'none' })
    return
  }
  const cookTime = parseInt(String(form.value.cookTime), 10)
  const servings = parseInt(String(form.value.servings), 10)
  if (!Number.isFinite(cookTime) || cookTime <= 0) {
    uni.showToast({ title: '烹饪时间请填写大于 0 的分钟数', icon: 'none' })
    return
  }
  if (!Number.isFinite(servings) || servings <= 0) {
    uni.showToast({ title: '份数请填写大于 0 的数字', icon: 'none' })
    return
  }
  saving.value = true
  const data = {
    ...form.value,
    name: form.value.name.trim(),
    difficulty: Number(form.value.difficulty) || 3,
    cookTime,
    servings,
    ingredients: ingredients.value
      .filter((x: any) => String(x.name || '').trim())
      .map((x: any) => {
        const { quantity, unit } = parseAmountToQuantityUnit(x.amount || '')
        return { name: String(x.name).trim(), quantity, unit }
      }),
    steps: steps.value
      .filter((x: any) => String(x.description || '').trim())
      .map((x: any, i: number) => ({
        description: String(x.description).trim(),
        imageUrl: x.imageUrl || null,
        stepNum: i + 1,
      })),
  }
  try {
    const payload = editId.value ? { id: editId.value, ...data } : data
    const res = editId.value
      ? await mutate('recipe', 'update', { data: payload }, (p) => recipeApi.update(p.data))
      : await mutate('recipe', 'create', { data: payload }, (p) => recipeApi.create(p.data))
    if (!editId.value && (res.result as any)?.id) editId.value = (res.result as any).id
    uni.showToast({
      title: res.queued ? '已离线保存，联网后同步' : '保存成功',
      icon: res.queued ? 'none' : 'success',
    })
    setTimeout(() => {
      if (res.queued || !editId.value) {
        uni.navigateBack()
      } else {
        uni.redirectTo({ url: `/pages-homeai-more/recipe/detail?id=${editId.value}` })
      }
    }, 800)
  } catch (e: any) {
    uni.showToast({ title: e.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.cat-fail {
  display: block;
  padding: 8rpx 24rpx 16rpx;
  font-size: 22rpx;
  color: var(--hai-danger);
}
.form-card {
  background: var(--hai-card);
  border-radius: var(--hai-radius);
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: var(--hai-shadow);
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
  color: var(--hai-text);
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
  color: var(--hai-text-secondary);
  margin-bottom: 12rpx;
  display: block;
}
.input {
  height: 72rpx;
  padding: 0 20rpx;
  background: var(--hai-bg);
  border-radius: 12rpx;
  font-size: 28rpx;
  width: 100%;
  color: var(--hai-text);
}
.input.small {
  flex: 1;
  height: 60rpx;
}
.picker-value {
  height: 72rpx;
  padding: 0 20rpx;
  background: var(--hai-bg);
  border-radius: 16rpx;
  font-size: 28rpx;
  color: var(--hai-text);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.picker-value .muted {
  color: var(--hai-text-muted);
}
.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  justify-content: flex-end;
}
.radio {
  flex: none;
  min-width: 0;
  text-align: center;
  padding: 10rpx 16rpx;
  background: var(--hai-bg);
  border-radius: 999rpx;
  font-size: 22rpx;
  color: var(--hai-text-secondary);
}
.radio.active {
  background: var(--hai-primary);
  color: var(--hai-on-primary);
}
.ing-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 12rpx;
}
.del-btn {
  color: var(--hai-danger);
  font-size: 24rpx;
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  background: var(--hai-danger-soft);
}
.add-btn {
  color: var(--hai-primary);
  font-size: 26rpx;
  padding: 16rpx 0;
  text-align: center;
  display: block;
  border-radius: 999rpx;
  background: var(--hai-primary-soft);
}
.step-item {
  border: 1rpx solid var(--hai-border);
  border-radius: var(--hai-radius-md);
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
  background: var(--hai-primary);
  color: var(--hai-on-primary);
  border-radius: 50%;
  font-size: 22rpx;
}
.step-ops {
  display: flex;
  gap: 16rpx;
}
.op-btn {
  color: var(--hai-primary);
  font-size: 24rpx;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: var(--hai-primary-soft);
}
.op-btn.danger {
  color: var(--hai-danger);
  background: var(--hai-danger-soft);
}
.step-input {
  width: 100%;
  min-height: 100rpx;
  font-size: 28rpx;
  padding: 12rpx;
  background: var(--hai-bg);
  border-radius: 12rpx;
  box-sizing: border-box;
  color: var(--hai-text);
}
.tips-input {
  width: 100%;
  min-height: 120rpx;
  font-size: 28rpx;
  padding: 12rpx;
  background: var(--hai-bg);
  border-radius: 12rpx;
  box-sizing: border-box;
  color: var(--hai-text);
}
</style>
