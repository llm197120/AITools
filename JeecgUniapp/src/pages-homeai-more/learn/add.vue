<route lang="json5">{ style: { navigationBarTitleText: '添加学习资料' } }</route>
<template>
  <view class="learn-add">
    <input class="input" v-model="form.title" placeholder="资料标题" />
    <view class="row">
      <text>类型</text>
      <picker :range="typeLabels" @change="onTypeChange">
        <text>{{ typeLabels[typeIndex] }}</text>
      </picker>
    </view>
    <view class="row">
      <text>分类</text>
      <picker :range="categoryNames" @change="onCategoryChange">
        <text>{{ selectedCategoryName || '请选择' }}</text>
      </picker>
    </view>
    <view class="row file-row" @click="chooseFile">
      <text>资料文件</text>
      <text class="file-name">{{ fileName || '点击选择文件（可选）' }}</text>
    </view>
    <wd-button size="large" type="primary" :loading="saving" @click="save">保存</wd-button>
  </view>
</template>
<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { learnApi } from '../../pages-homeai/api/index'
import { getServerBaseUrl } from '../../pages-homeai/api/request'
import { validateUploadFile } from '../../pages-homeai/utils/fileWhitelist'

const typeLabels = ['视频', '图片', 'PDF', '文档', '笔记']
const typeValues = ['video', 'image', 'pdf', 'doc', 'note']
const typeIndex = ref(4)
const categoryNames = ref<string[]>([])
const categoryIds = ref<string[]>([])
const selectedCategoryName = ref('')
const filePath = ref('')
const fileName = ref('')
const saving = ref(false)

const form = ref({
  title: '',
  type: 'note',
  categoryId: '',
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

function chooseFile() {
  uni.chooseMessageFile({
    count: 1,
    type: 'all',
    success: (r) => {
      if (r.tempFiles?.[0]) {
        filePath.value = r.tempFiles[0].path
        fileName.value = r.tempFiles[0].name
      }
    },
  })
}

async function save() {
  if (!form.value.title) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const material: any = await learnApi.create(form.value)
    if (filePath.value && material?.id) {
      if (!(await validateUploadFile(filePath.value))) return
      const token = uni.getStorageSync('homeai_token')
      const up: any = await uni.uploadFile({
        url: getServerBaseUrl() + `/homeai/learn/materials/${material.id}/upload`,
        filePath: filePath.value,
        name: 'file',
        header: { 'X-Access-Token': token },
      })
      const data = typeof up.data === 'string' ? JSON.parse(up.data) : up.data
      if (!data.success) throw new Error(data.message || '文件上传失败')
    }
    uni.showToast({ title: '创建成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>
<style scoped>
.learn-add { padding: 30rpx; min-height: 100vh; background: #f5f5f5; }
.input { width: 100%; font-size: 32rpx; padding: 20rpx; background: #fff; border-radius: 12rpx; margin-bottom: 20rpx; box-sizing: border-box; }
.row { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 20rpx; background: #fff; border-radius: 12rpx; margin-bottom: 16rpx; font-size: 28rpx; }
.file-row .file-name { max-width: 360rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #666; font-size: 24rpx; }
</style>
