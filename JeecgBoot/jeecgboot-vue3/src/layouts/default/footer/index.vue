<template>
  <Footer :class="prefixCls" v-if="getShowLayoutFooter" ref="footerRef">
    <HomeaiIcpBeian />
  </Footer>
</template>

<script lang="ts">
  import { computed, defineComponent, unref, ref } from 'vue';
  import { Layout } from 'ant-design-vue';

  import { useRouter } from 'vue-router';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { useLayoutHeight } from '../content/useContentViewHeight';
  import HomeaiIcpBeian from '/@/views/homeai/components/HomeaiIcpBeian.vue';

  export default defineComponent({
    name: 'LayoutFooter',
    components: { Footer: Layout.Footer, HomeaiIcpBeian },
    setup() {
      const { currentRoute } = useRouter();
      const { prefixCls } = useDesign('layout-footer');

      const footerRef = ref<ComponentRef>(null);
      const { setFooterHeight } = useLayoutHeight();

      const getShowLayoutFooter = computed(() => {
        // 备案号必须展示，不跟本地缓存的 showFooter 走
        const visible = !unref(currentRoute).meta?.hiddenFooter;
        if (visible) {
          const footerEl = unref(footerRef)?.$el;
          setFooterHeight(footerEl?.offsetHeight || 0);
        } else {
          setFooterHeight(0);
        }
        return visible;
      });
      return {
        getShowLayoutFooter,
        prefixCls,
        footerRef,
      };
    },
  });
</script>
<style lang="less" scoped>
  @prefix-cls: ~'@{namespace}-layout-footer';

  .@{prefix-cls} {
    padding: 12px 16px;
    text-align: center;
  }
</style>
