<template>
  <BasicModal
    v-bind="forwardedAttrs"
    :width="modalWidth"
    :centered="true"
    :maskClosable="false"
    :canFullscreen="false"
    :destroyOnClose="true"
    :okText="okLabel"
    :cancelText="cancelLabel"
    :wrapClassName="wrapClass"
    @register="onRegister"
  >
    <slot />
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </BasicModal>
</template>

<script lang="ts" name="homeai-form-modal" setup>
  import { computed, useAttrs } from 'vue';
  import { BasicModal } from '/@/components/Modal';
  import { HOMEAI_FORM_MODAL_WIDTH, type HomeaiFormModalSize } from '../utils/formLayout';

  defineOptions({ inheritAttrs: false });

  const props = withDefaults(
    defineProps<{
      size?: HomeaiFormModalSize;
      okText?: string;
      cancelText?: string;
    }>(),
    {
      size: 'medium',
      okText: '保存并关闭',
      cancelText: '取消',
    },
  );

  const emit = defineEmits(['register']);
  const attrs = useAttrs();

  const modalWidth = computed(() => attrs.width ?? HOMEAI_FORM_MODAL_WIDTH[props.size]);
  const okLabel = computed(() => (attrs.okText as string) || props.okText);
  const cancelLabel = computed(() => (attrs.cancelText as string) || props.cancelText);
  const wrapClass = computed(() => {
    const extra = attrs.wrapClassName ? String(attrs.wrapClassName) : '';
    return ['homeai-form-modal', extra].filter(Boolean).join(' ');
  });
  const forwardedAttrs = computed(() => {
    const rest = { ...attrs } as Record<string, unknown>;
    delete rest.width;
    delete rest.okText;
    delete rest.cancelText;
    delete rest.wrapClassName;
    delete rest.maskClosable;
    delete rest.centered;
    delete rest.canFullscreen;
    delete rest.destroyOnClose;
    return rest;
  });

  function onRegister(modal: unknown, uid: string) {
    emit('register', modal, uid);
  }
</script>
