<template>
  <image :src="displaySrc" :mode="mode" v-bind="$attrs" @load="onLoad" @error="onError" />
</template>

<script lang="ts" setup>
/**
 * 离线图片组件：已浏览图片优先用本地 blob 缓存（磁盘 IndexedDB），
 * 首次加载成功后异步写入缓存；加载失败（离线/过期）时回退本地缓存。
 * 注意：OSS 预签名 URL 未配置 CORS，写缓存走后端同域代理接口。
 */
import { computed, ref, watch } from 'vue'
import { getImageBlob, putImageBlob, fetchImageBlobViaProxy, normalizeImageKey } from './imageCache'
import { getConnState } from './conn'

const props = defineProps({
  src: { type: String, default: '' },
  mode: { type: String, default: 'aspectFill' },
})

const emit = defineEmits(['load', 'error'])

const displaySrc = ref('')
const loadedUrl = ref('')

function toBlobUrl(url: string): string {
  try {
    return URL.createObjectURL(url as unknown as Blob) || ''
  } catch {
    return ''
  }
}

async function apply() {
  const src = props.src || ''
  if (!src) {
    displaySrc.value = ''
    return
  }
  if (/^(blob:|data:)/i.test(src)) {
    displaySrc.value = src
    return
  }
  // 1) 本地已有 blob → 直接渲染
  const cached = await getImageBlob(src)
  if (cached && cached.blob) {
    const url = toBlobUrl(cached.blob)
    if (url) {
      displaySrc.value = url
      return
    }
  }
  // 2) 无缓存 → 渲染原 URL
  displaySrc.value = src
}

/** 加载成功：异步写缓存（同域代理取 blob，CORS 安全） */
function onLoad() {
  const src = props.src || ''
  if (loadedUrl.value === src) return
  loadedUrl.value = src
  emit('load')
  if (/^https?:\/\//i.test(src)) {
    fetchImageBlobViaProxy(src).then((res) => {
      if (res) putImageBlob(src, res.blob, { contentType: res.contentType })
    })
  }
}

/** 加载失败：回退本地缓存；无缓存则保持原样（占位由外层处理） */
async function onError() {
  const src = props.src || ''
  if (/^(blob:|data:)/i.test(src)) {
    emit('error')
    return
  }
  const cached = await getImageBlob(src)
  if (cached && cached.blob) {
    const url = toBlobUrl(cached.blob)
    if (url) {
      displaySrc.value = url
      return
    }
  }
  emit('error')
}

watch(
  () => props.src,
  () => {
    loadedUrl.value = ''
    apply()
  },
  { immediate: true },
)
</script>
