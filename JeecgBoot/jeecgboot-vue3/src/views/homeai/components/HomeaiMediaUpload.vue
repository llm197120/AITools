<template>
  <div class="homeai-media-upload">
    <!-- 已上传内容预览 -->
    <div v-if="value" class="hmu-preview-wrap">
      <div v-if="mode === 'image'" class="hmu-image-preview">
        <a-image :src="value" :width="200" :height="140" />
        <div class="hmu-actions">
          <a-upload name="file" :accept="accept" :show-upload-list="false" :disabled="uploading || disabled" :custom-request="handleCustomRequest">
            <a-button size="small" :loading="uploading">更换</a-button>
          </a-upload>
          <a-button size="small" danger :disabled="uploading" @click="clearValue">删除</a-button>
        </div>
      </div>
      <div v-else-if="mode === 'video'" class="hmu-video-preview">
        <video :src="value" controls class="hmu-video" />
        <div class="hmu-actions">
          <a-upload name="file" :accept="accept" :show-upload-list="false" :disabled="uploading || disabled" :custom-request="handleCustomRequest">
            <a-button size="small" :loading="uploading">更换</a-button>
          </a-upload>
          <a-button size="small" danger :disabled="uploading" @click="clearValue">删除</a-button>
        </div>
      </div>
      <div v-else class="hmu-file-preview">
        <Icon icon="ant-design:file-outlined" :size="28" color="#52c41a" />
        <div class="hmu-file-info">
          <a :href="value" target="_blank" class="hmu-file-name" :title="value">{{ displayFileName }}</a>
          <span v-if="tip" class="hmu-file-tip">{{ tip }}</span>
        </div>
        <div class="hmu-actions">
          <a-button size="small" :href="value" target="_blank" :disabled="uploading">下载</a-button>
          <a-upload name="file" :accept="accept" :show-upload-list="false" :disabled="uploading || disabled" :custom-request="handleCustomRequest">
            <a-button size="small" :loading="uploading">更换</a-button>
          </a-upload>
          <a-button size="small" danger :disabled="uploading" @click="clearValue">删除</a-button>
        </div>
      </div>
    </div>

    <!-- 上传拖拽区 -->
    <a-upload
      v-else
      name="file"
      drag
      :accept="accept"
      :show-upload-list="false"
      :disabled="uploading || disabled"
      :before-upload="beforeUpload"
      :custom-request="handleCustomRequest"
    >
      <div class="hmu-drop">
        <a-spin :spinning="uploading">
          <Icon
            :icon="
              mode === 'image'
                ? 'ant-design:picture-outlined'
                : mode === 'video'
                  ? 'ant-design:video-camera-outlined'
                  : 'ant-design:cloud-upload-outlined'
            "
            :size="36"
            color="#a6adb4"
          />
          <p class="hmu-drop-text">{{ uploading ? '上传中...' : dropText }}</p>
          <p v-if="tip && !uploading" class="hmu-tip">{{ tip }}</p>
          <div v-if="uploading" class="hmu-progress">
            <a-progress :percent="progress" size="small" />
          </div>
        </a-spin>
      </div>
    </a-upload>
  </div>
</template>

<script lang="ts" name="homeai-media-upload" setup>
  import { computed, ref } from 'vue';
  import { Icon } from '/@/components/Icon';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';
  import type { PropType } from 'vue';

  const { createMessage } = useMessage();
  const emit = defineEmits(['change', 'update:value']);
  const props = defineProps({
    /** 当前媒体地址 */
    value: { type: String, default: '' },
    /** 上传模式：图片 / 视频 / 普通文件 */
    mode: { type: String as PropType<'image' | 'video' | 'file'>, default: 'file' },
    /** 上传接口地址 */
    uploadUrl: { type: String, required: true },
    /** 上传表单字段名 */
    uploadName: { type: String, default: 'file' },
    /** 附加上传参数（可为函数，上传时动态获取） */
    extraData: { type: [Object, Function] as PropType<Recordable | (() => Recordable)>, default: () => ({}) },
    /** 文件类型过滤，默认按模式自动匹配 */
    accept: { type: String, default: '' },
    /** 最大文件大小（MB） */
    maxSize: { type: Number, default: 50 },
    /** 辅助提示文案 */
    tip: { type: String, default: '' },
    /** 已上传文件名（file 模式展示用，缺省取地址末段） */
    fileName: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
  });

  const uploading = ref(false);
  const progress = ref(0);

  const accept = computed(() => {
    if (props.accept) return props.accept;
    if (props.mode === 'image') return 'image/*';
    if (props.mode === 'video') return 'video/*';
    return '';
  });

  const dropText = computed(() => {
    if (props.mode === 'image') return '点击或拖拽图片到此处上传';
    if (props.mode === 'video') return '点击或拖拽视频到此处上传';
    return '点击或拖拽文件到此处上传';
  });

  const displayFileName = computed(() => {
    if (props.fileName) return props.fileName;
    const url = props.value || '';
    const last = url.split('/').pop() || '';
    return last ? decodeURIComponent(last) : '已上传文件';
  });

  function getExtraData(): Recordable {
    if (typeof props.extraData === 'function') {
      return props.extraData();
    }
    return props.extraData || {};
  }

  function beforeUpload(file: File) {
    if (props.maxSize > 0 && file.size > props.maxSize * 1024 * 1024) {
      createMessage.warning(`文件大小不能超过 ${props.maxSize}MB`);
      return false;
    }
    return true;
  }

  async function handleCustomRequest(options: any) {
    const { file, onError } = options;
    if (!beforeUpload(file)) {
      onError(new Error('文件大小超限'));
      return;
    }
    uploading.value = true;
    progress.value = 0;
    try {
      const res: any = await defHttp.uploadFile(
        {
          url: props.uploadUrl,
          onUploadProgress: (e: ProgressEvent) => {
            if (e.total) {
              progress.value = Math.round((e.loaded / e.total) * 100);
            }
          },
        },
        { file, name: props.uploadName, data: getExtraData() },
        { isReturnResponse: true }
      );
      const url = res?.result || '';
      if (!url || (res?.code && res.code !== 200)) {
        throw new Error(res?.message || '上传失败，未获取到文件地址');
      }
      emit('change', url);
      emit('update:value', url);
      createMessage.success('上传成功');
    } catch (err: any) {
      createMessage.error(err?.message || '上传失败');
      onError?.(err);
    } finally {
      uploading.value = false;
      progress.value = 0;
    }
  }

  function clearValue() {
    emit('change', '');
    emit('update:value', '');
  }
</script>

<style scoped lang="less">
  .homeai-media-upload {
    width: 100%;
  }

  .hmu-drop {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    min-height: 104px;
    padding: 16px;
    cursor: pointer;

    .hmu-drop-text {
      margin: 0;
      font-size: 13px;
      color: #666;
    }

    .hmu-tip {
      margin: 0;
      font-size: 12px;
      color: #999;
    }

    .hmu-progress {
      width: 100%;
      margin-top: 4px;
    }
  }

  :deep(.ant-upload) {
    width: 100%;
  }

  :deep(.ant-upload-drag) {
    border: 1px dashed var(--hai-admin-border, #d9d9d9);
    border-radius: 8px;
    background: var(--hai-admin-bg, #fafafa);
    padding: 0;
    transition: border-color 0.2s;

    &:hover {
      border-color: var(--hai-admin-primary, #1677ff);
    }
  }

  .hmu-preview-wrap {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .hmu-image-preview {
      position: relative;
      width: 200px;
      border: 1px solid var(--hai-admin-border, #ece9e2);
      border-radius: 8px;
      overflow: hidden;
    }

    .hmu-video-preview {
      .hmu-video {
        width: 100%;
        max-height: 240px;
        border-radius: 8px;
        background: #000;
      }
    }

    .hmu-file-preview {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      border: 1px solid var(--hai-admin-border, #ece9e2);
      border-radius: 8px;
      background: var(--hai-admin-bg, #fafafa);

      .hmu-file-info {
        flex: 1;
        min-width: 0;

        .hmu-file-name {
          display: inline-block;
          max-width: 100%;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          font-size: 13px;
        }

        .hmu-file-tip {
          display: block;
          margin-top: 2px;
          font-size: 12px;
          color: #999;
        }
      }
    }

    .hmu-actions {
      display: flex;
      gap: 8px;
      margin-top: 4px;
    }
  }
</style>
