<template>
  <wd-select-picker
    v-model="inner"
    type="radio"
    :columns="columns"
    :label="label"
    :title="title || label"
    :placeholder="placeholder"
    :filterable="filterable"
    :show-confirm="false"
    align-right
    center
    label-width="180rpx"
    checked-color="#1B4F8A"
    value-key="value"
    label-key="label"
  />
</template>

<script lang="ts" setup>
/**
 * APP 底部列表选择（wot 单选弹层），替代微信原生 picker 滚轮。
 */
defineProps<{
  label: string
  title?: string
  placeholder?: string
  columns: { label: string; value: string }[]
  filterable?: boolean
}>()

const inner = defineModel<string>({ default: '' })
</script>
