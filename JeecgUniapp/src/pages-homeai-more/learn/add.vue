<route lang="json5">
{ style: { navigationBarTitleText: '添加学习资料', navigationBarBackgroundColor: '#F3F2EE' } }
</route>
<template>
  <HomeFormCard>
    <input class="home-form-input" v-model="form.title" placeholder="资料标题" />

    <view class="home-form-group">
      <wd-cell-group border>
        <HomePickerCell
          v-model="form.type"
          label="类型"
          title="资料类型"
          :columns="typeColumns"
        />
        <HomePickerCell
          v-model="form.categoryId"
          label="分类"
          title="选择分类"
          placeholder="请选择分类"
          :columns="categoryColumns"
        />
      </wd-cell-group>
    </view>

    <view v-if="form.type === 'link'" class="home-form-group">
      <wd-cell-group border>
        <wd-input v-model="form.fileUrl" label="链接" placeholder="https://..." />
      </wd-cell-group>
    </view>
    <HomeMediaUpload
      v-else
      v-model="form.fileUrl"
      :mode="uploadMode"
      url="/homeai/learn/upload"
      :form-data="{ type: form.type }"
      :allowed-ext="allowedExt"
      :max-size="maxSize"
      :placeholder="uploadPlaceholder"
      tip="格式需与资料类型匹配"
    />
    <wd-button class="home-form-save" size="large" type="primary" block round :loading="saving" @click="save">
      {{ editing ? '保存修改' : '保存' }}
    </wd-button>
  </HomeFormCard>
</template>
<script lang="ts" setup>
import { computed, ref, onMounted, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { learnApi } from '../../pages-homeai/api/index'
import HomeFormCard from '../../components/HomeFormCard.vue'
import HomeMediaUpload from '../../pages-homeai/components/HomeMediaUpload.vue'
import HomePickerCell from '../../pages-homeai/components/HomePickerCell.vue'

const typeColumns = [
  { label: '视频', value: 'video' },
  { label: '音频', value: 'audio' },
  { label: '图片', value: 'image' },
  { label: 'PDF', value: 'pdf' },
  { label: '文档', value: 'doc' },
  { label: '表格', value: 'xls' },
  { label: 'PPT', value: 'ppt' },
  { label: '链接', value: 'link' },
  { label: '笔记', value: 'note' },
]
const categoryColumns = ref<{ label: string; value: string }[]>([])
const saving = ref(false)
const editing = ref(false)
const materialId = ref('')

const form = ref({
  title: '',
  type: 'note',
  categoryId: '',
  fileUrl: '',
  visibility: 'private',
})

const uploadMode = computed(() => {
  if (form.value.type === 'image') return 'image'
  if (form.value.type === 'video') return 'video'
  if (form.value.type === 'audio') return 'audio'
  return 'file'
})

const allowedExt = computed(() => {
  const map: Record<string, string[]> = {
    video: ['mp4', 'mov', 'webm', 'mkv', 'avi'],
    audio: ['mp3', 'wav', 'm4a', 'aac'],
    image: ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'],
    pdf: ['pdf'],
    doc: ['doc', 'docx'],
    xls: ['xls', 'xlsx'],
    ppt: ['ppt', 'pptx'],
    note: ['txt', 'md'],
  }
  return map[form.value.type] || []
})

const maxSize = computed(() => {
  if (form.value.type === 'video') return 200
  if (form.value.type === 'image') return 20
  if (['audio', 'pdf', 'doc', 'xls', 'ppt'].includes(form.value.type)) return 50
  return 10
})

const uploadPlaceholder = computed(() => {
  if (form.value.type === 'video') return '点击选择视频'
  if (form.value.type === 'audio') return '点击选择音频'
  if (form.value.type === 'image') return '点击选择图片'
  return '点击选择文件上传'
})

onLoad((opts: any) => {
  if (opts?.id) {
    editing.value = true
    materialId.value = opts.id
    uni.setNavigationBarTitle({ title: '编辑学习资料' })
  }
})

watch(
  () => form.value.type,
  (next, prev) => {
    if (prev && next !== prev) {
      form.value.fileUrl = ''
    }
  },
  { flush: 'sync' },
)

onMounted(async () => {
  const list = (await learnApi.categories()) || []
  categoryColumns.value = list.map((c: any) => ({ label: c.name, value: c.id }))
  if (editing.value && materialId.value) {
    await loadDetail(materialId.value)
    return
  }
  if (categoryColumns.value.length && !form.value.categoryId) {
    form.value.categoryId = categoryColumns.value[0].value
  }
})

async function loadDetail(id: string) {
  const m = await learnApi.materialById(id)
  if (!m) {
    uni.showToast({ title: '资料不存在', icon: 'none' })
    return
  }
  form.value.title = m.title || ''
  form.value.type = m.type || 'note'
  form.value.categoryId = m.categoryId || categoryColumns.value[0]?.value || ''
  form.value.fileUrl = m.fileUrl || ''
}

async function save() {
  if (!form.value.title) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  if (form.value.type === 'link' && !form.value.fileUrl) {
    uni.showToast({ title: '请填写链接地址', icon: 'none' })
    return
  }
  if (!['link', 'note'].includes(form.value.type) && !form.value.fileUrl) {
    uni.showToast({ title: '请上传与类型匹配的资料文件', icon: 'none' })
    return
  }
  saving.value = true
  try {
    if (editing.value && materialId.value) {
      await learnApi.update({ id: materialId.value, ...form.value })
      uni.showToast({ title: '已保存', icon: 'success' })
    } else {
      await learnApi.create({ ...form.value })
      uni.showToast({ title: '创建成功', icon: 'success' })
    }
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>
