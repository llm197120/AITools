<route lang="json5">
{ style: { navigationBarTitleText: '添加学习资料', navigationBarBackgroundColor: '#F3F2EE' } }
</route>
<template>
  <HomeFormCard>
    <input class="home-form-input" v-model="form.title" placeholder="资料标题" />
    <view class="home-form-row">
      <text>类型</text>
      <picker :range="typeLabels" @change="onTypeChange">
        <text class="home-form-value">{{ typeLabels[typeIndex] }}</text>
      </picker>
    </view>
    <view class="home-form-row">
      <text>分类</text>
      <picker :range="categoryNames" @change="onCategoryChange">
        <text class="home-form-value">{{ selectedCategoryName || '请选择' }}</text>
      </picker>
    </view>
    <view class="home-form-row">
      <text>资料文件</text>
      <text class="home-form-value">
        {{ form.fileUrl ? '已上传文件' : '点击选择文件（可选）' }}
      </text>
    </view>
    <HomeMediaUpload
      v-model="form.fileUrl"
      mode="file"
      url="/homeai/learn/upload"
      :form-data="{ type: form.type }"
      placeholder="点击选择文件上传"
      tip="格式需与资料类型匹配"
    />
    <wd-button size="large" type="primary" block :loading="saving" @click="save">保存</wd-button>
  </HomeFormCard>
</template>
<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { learnApi } from '../../pages-homeai/api/index'
import HomeFormCard from '../../components/HomeFormCard.vue'
import HomeMediaUpload from '../../pages-homeai/components/HomeMediaUpload.vue'

const typeLabels = ['视频', '图片', 'PDF', '文档', '笔记']
const typeValues = ['video', 'image', 'pdf', 'doc', 'note']
const typeIndex = ref(4)
const categoryNames = ref<string[]>([])
const categoryIds = ref<string[]>([])
const selectedCategoryName = ref('')
const saving = ref(false)

const form = ref({
  title: '',
  type: 'note',
  categoryId: '',
  fileUrl: '',
  visibility: 'private',
})

onMounted(async () => {
  const list = (await learnApi.categories()) || []
  categoryNames.value = list.map((c: any) => c.name)
  categoryIds.value = list.map((c: any) => c.id)
  if (categoryIds.value.length) {
    form.value.categoryId = categoryIds.value[0]
    selectedCategoryName.value = categoryNames.value[0]
  }
})

function onTypeChange(e: any) {
  typeIndex.value = Number(e.detail.value)
  form.value.type = typeValues[typeIndex.value]
}

function onCategoryChange(e: any) {
  const idx = Number(e.detail.value)
  form.value.categoryId = categoryIds.value[idx] || ''
  selectedCategoryName.value = categoryNames.value[idx] || ''
}

async function save() {
  if (!form.value.title) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await learnApi.create({ ...form.value })
    uni.showToast({ title: '创建成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>
