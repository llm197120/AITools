<template>
  <a-modal
    :open="visible"
    :title="title"
    :footer="null"
    width="860px"
    destroyOnClose
    @cancel="close"
  >
    <a-spin :spinning="loading">
      <div v-if="error" class="hfp-empty">{{ error }}</div>
      <template v-else-if="preview">
        <div v-if="preview.kind === 'image' && mediaUrl" class="hfp-media">
          <a-image :src="mediaUrl" :preview="true" style="max-height: 70vh" />
        </div>
        <video v-else-if="preview.kind === 'video' && mediaUrl" :src="mediaUrl" controls class="hfp-video" />
        <audio v-else-if="preview.kind === 'audio' && mediaUrl" :src="mediaUrl" controls class="hfp-audio" />
        <pre v-else-if="preview.kind === 'text'" class="hfp-text">{{ textContent || '加载文本中...' }}</pre>
        <div v-else-if="preview.kind === 'pdf' || (preview.kind === 'office' && pdfUrl)" class="hfp-pdf">
          <iframe :src="pdfUrl" class="hfp-iframe" title="PDF 预览" />
        </div>
        <div v-else-if="preview.kind === 'office'" class="hfp-empty">
          <p v-if="converting">正在转换为 PDF，请稍候…</p>
          <p v-else-if="preview.convertStatus === 'FAILED'">无法预览：{{ preview.errorMessage || '转换失败' }}</p>
          <p v-else>Office 文档需先转为 PDF 再预览，也可直接下载原文件</p>
        </div>
        <div v-else-if="preview.kind === 'link' && mediaUrl" class="hfp-empty">
          <a :href="mediaUrl" target="_blank" rel="noopener">打开链接</a>
        </div>
        <div v-else class="hfp-empty">
          <p>该类型不支持页内预览</p>
        </div>
        <div v-if="source && preview.kind !== 'link'" class="hfp-actions">
          <a-button type="primary" :loading="downloading" @click="downloadRaw">下载原文件</a-button>
        </div>
      </template>
    </a-spin>
  </a-modal>
</template>

<script lang="ts" name="homeai-file-preview-modal" setup>
  import { computed, onBeforeUnmount, ref } from 'vue';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { downloadHomeaiContent, readHomeaiContentText } from '../utils/homeaiFileContent';

  export type HomeaiPreviewSource =
    | { module: 'storage'; id: string; title?: string }
    | { module: 'learn'; id: string; title?: string };

  interface HomeaiFilePreview {
    kind?: string;
    fileUrl?: string;
    previewPdfUrl?: string;
    fileName?: string;
    extension?: string;
    convertTaskId?: string;
    convertStatus?: string;
    errorMessage?: string;
  }

  const { createMessage } = useMessage();
  const visible = ref(false);
  const loading = ref(false);
  const converting = ref(false);
  const downloading = ref(false);
  const error = ref('');
  const textContent = ref('');
  const preview = ref<HomeaiFilePreview | null>(null);
  const source = ref<HomeaiPreviewSource | null>(null);
  let pollTimer: ReturnType<typeof setInterval> | null = null;

  const title = computed(() => preview.value?.fileName || source.value?.title || '文件预览');
  const mediaUrl = computed(() => preview.value?.fileUrl || '');
  const pdfUrl = computed(() => {
    if (preview.value?.kind === 'pdf') return preview.value.fileUrl || '';
    return preview.value?.previewPdfUrl || '';
  });

  function previewUrl() {
    if (!source.value) return '';
    return source.value.module === 'storage'
      ? `/homeai/storage/files/${source.value.id}/preview`
      : `/homeai/learn/materials/${source.value.id}/preview`;
  }

  function previewPdfUrl() {
    if (!source.value) return '';
    return source.value.module === 'storage'
      ? `/homeai/storage/files/${source.value.id}/preview-pdf`
      : `/homeai/learn/materials/${source.value.id}/preview-pdf`;
  }

  function stopPoll() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
    converting.value = false;
  }

  async function loadText() {
    if (!source.value) return;
    try {
      textContent.value = await readHomeaiContentText(source.value.module, source.value.id);
    } catch {
      textContent.value = '文本加载失败，请下载查看';
    }
  }

  async function applyPreview(data: HomeaiFilePreview) {
    preview.value = data;
    if (data.kind === 'text') {
      await loadText();
    }
    if (data.kind === 'office' && !data.previewPdfUrl) {
      await startOfficeConvert();
    }
  }

  async function startOfficeConvert() {
    converting.value = true;
    try {
      const data = await defHttp.post<HomeaiFilePreview>({ url: previewPdfUrl() });
      preview.value = data;
      if (data.previewPdfUrl || data.convertStatus === 'COMPLETED') {
        converting.value = false;
        return;
      }
      if (data.convertStatus === 'FAILED') {
        converting.value = false;
        return;
      }
      pollTimer = setInterval(async () => {
        try {
          const next = await defHttp.get<HomeaiFilePreview>({ url: previewUrl() });
          preview.value = next;
          if (next.previewPdfUrl || next.convertStatus === 'COMPLETED' || next.convertStatus === 'FAILED') {
            stopPoll();
          }
        } catch {
          stopPoll();
        }
      }, 3000);
    } catch (e: any) {
      converting.value = false;
      createMessage.error(e?.message || '无法开始转换');
    }
  }

  async function open(next: HomeaiPreviewSource) {
    source.value = next;
    visible.value = true;
    loading.value = true;
    error.value = '';
    preview.value = null;
    textContent.value = '';
    stopPoll();
    try {
      const data = await defHttp.get<HomeaiFilePreview>({ url: previewUrl() });
      await applyPreview(data);
    } catch (e: any) {
      error.value = e?.message || '预览失败';
    } finally {
      loading.value = false;
    }
  }

  async function downloadRaw() {
    if (!source.value) return;
    downloading.value = true;
    try {
      await downloadHomeaiContent(source.value.module, source.value.id, preview.value?.fileName || source.value.title);
    } catch (e: any) {
      createMessage.error(e?.message || '下载失败');
    } finally {
      downloading.value = false;
    }
  }

  function close() {
    visible.value = false;
    stopPoll();
  }

  onBeforeUnmount(stopPoll);

  defineExpose({ open, close });
</script>

<style scoped>
  .hfp-media,
  .hfp-pdf {
    text-align: center;
  }
  .hfp-video {
    width: 100%;
    max-height: 70vh;
    background: #000;
  }
  .hfp-audio {
    width: 100%;
    margin: 24px 0;
  }
  .hfp-text {
    max-height: 70vh;
    overflow: auto;
    background: #fafafa;
    padding: 16px;
    white-space: pre-wrap;
    word-break: break-all;
  }
  .hfp-iframe {
    width: 100%;
    height: 70vh;
    border: 0;
    background: #525252;
  }
  .hfp-empty {
    text-align: center;
    padding: 48px 16px;
    color: #666;
  }
  .hfp-actions {
    text-align: center;
    padding: 16px 0 8px;
  }
</style>
