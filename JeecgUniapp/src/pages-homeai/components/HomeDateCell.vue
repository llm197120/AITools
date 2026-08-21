<template>
  <wd-datetime-picker
    v-model="timestamp"
    type="date"
    :label="label"
    :title="title || label"
    :placeholder="placeholder || '请选择日期'"
    align-right
    center
    label-width="180rpx"
  />
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { localDateStr, parseLocalDate } from '../utils/date'

/**
 * APP 底部日期选择，v-model 为本地时区 yyyy-MM-dd。
 */
defineProps<{
  label: string
  title?: string
  placeholder?: string
}>()

const model = defineModel<string>({ default: '' })

const timestamp = computed({
  get() {
    return parseLocalDate(model.value || localDateStr()).getTime()
  },
  set(value: number | string) {
    const ts = typeof value === 'number' ? value : Number(value)
    if (!Number.isFinite(ts)) return
    model.value = localDateStr(new Date(ts))
  },
})
</script>
