<template>
  <text class="home-file-icon">{{ icon }}</text>
</template>
<script lang="ts" setup>
import { computed } from 'vue'

const props = defineProps<{ ext?: string; name?: string }>()

const icon = computed(() => {
  const ext = (props.ext || props.name?.split('.').pop() || '').toLowerCase()
  const map: Record<string, string> = {
    pdf: '📄', doc: '📝', docx: '📝', xls: '📊', xlsx: '📊',
    ppt: '📽️', pptx: '📽️', jpg: '🖼️', jpeg: '🖼️', png: '🖼️', gif: '🖼️',
    mp4: '🎬', mov: '🎬', zip: '📦', rar: '📦', txt: '📃', md: '📃',
  }
  return map[ext] || '📎'
})
</script>
<style scoped>
.home-file-icon { font-size: 36rpx; }
</style>
