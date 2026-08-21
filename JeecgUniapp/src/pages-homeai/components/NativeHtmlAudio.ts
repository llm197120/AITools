import { defineComponent, h } from 'vue'

/**
 * uni-h5 生产构建不导出 Audio 组件；用原生 audio 做预览。
 */
export default defineComponent({
  name: 'NativeHtmlAudio',
  props: {
    src: { type: String, default: '' },
  },
  setup(props) {
    return () =>
      h('audio', {
        src: props.src,
        controls: true,
        class: 'hai-native-audio',
        style: { width: '100%' },
      })
  },
})
