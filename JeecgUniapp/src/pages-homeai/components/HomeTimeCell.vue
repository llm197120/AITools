<template>
  <wd-datetime-picker
    v-model="model"
    type="time"
    :label="label"
    :title="title || label"
    :placeholder="placeholder || '请选择时间'"
    align-right
    center
    label-width="180rpx"
  />
</template>

<script lang="ts" setup>
/**
 * APP 底部时间选择，v-model 为本地 HH:mm（wot type=time 用字符串）。
 */
defineProps<{
  label: string
  title?: string
  placeholder?: string
}>()

const model = defineModel<string>({ default: '09:00' })
</script>
