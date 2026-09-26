<template>
  <div v-if="icpNumber" class="homeai-icp-beian">
    <a :href="queryUrl" target="_blank" rel="noopener noreferrer">{{ icpNumber }}</a>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { useGlobSetting } from '/@/hooks/setting';
  import { ICP_NUMBER, ICP_QUERY_URL } from '/@/settings/siteSetting';

  const glob = useGlobSetting();
  const icpNumber = computed(() => glob.icpNumber || ICP_NUMBER);
  const queryUrl = computed(() => glob.icpQueryUrl || ICP_QUERY_URL);
</script>

<style lang="less" scoped>
  .homeai-icp-beian {
    text-align: center;
    line-height: 1.5;
    font-size: 12px;

    a {
      color: rgba(0, 0, 0, 0.45);
      text-decoration: none;

      &:hover {
        color: rgba(0, 0, 0, 0.75);
        text-decoration: underline;
      }
    }
  }

  html[data-theme='dark'] .homeai-icp-beian a {
    color: rgba(255, 255, 255, 0.55);

    &:hover {
      color: rgba(255, 255, 255, 0.85);
    }
  }
</style>
