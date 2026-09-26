<template>
  <div class="aui-image">
    <div class="aui-image-text homeai-login-hero">
      <div class="homeai-login-hero__title">{{ title }}</div>
      <p class="homeai-login-hero__desc">{{ desc }}</p>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { useGlobSetting } from '/@/hooks/setting';
  import { useI18n } from '/@/hooks/web/useI18n';

  const glob = useGlobSetting();
  const { t } = useI18n();
  const title = computed(() => glob.title || '家庭AI小工具');
  const desc = computed(() => t('sys.login.signInDesc'));
</script>

<style lang="less" scoped>
  .homeai-login-hero {
    color: #fff;
    text-shadow: 0 1px 12px rgba(0, 0, 0, 0.18);
  }

  .homeai-login-hero__title {
    font-size: 42px;
    font-weight: 700;
    letter-spacing: 0.04em;
    line-height: 1.25;
  }

  .homeai-login-hero__desc {
    margin-top: 16px;
    font-size: 16px;
    line-height: 1.7;
    max-width: 22em;
    opacity: 0.95;
  }
</style>
