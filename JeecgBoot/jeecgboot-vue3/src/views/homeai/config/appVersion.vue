<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-alert
      type="info"
      show-icon
      style="margin-bottom: 16px"
      message="控制 APP 是否更新、更新方式与安装包。本地 versionCode 小于此处才更新；种子记录默认关闭，避免内测包误触发。"
    />
    <a-card title="当前发布版本" :bordered="false">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item label="对 APP 生效">
          <a-switch :checked="form.enabled === 1" @update:checked="(v) => (form.enabled = v ? 1 : 0)" />
          <span style="margin-left: 8px; color: #999">关闭时启动页不提示更新</span>
        </a-form-item>
        <a-form-item label="版本号">
          <a-input v-model:value="form.versionName" placeholder="如 1.0.1" style="width: 200px" />
        </a-form-item>
        <a-form-item label="versionCode">
          <a-input-number v-model:value="form.versionCode" :min="1" :precision="0" style="width: 200px" />
          <span style="margin-left: 8px; color: #999">须大于已安装 APP 的整数版本</span>
        </a-form-item>
        <a-form-item label="更新方式">
          <a-radio-group v-model:value="form.updateMode">
            <a-radio value="apk">覆盖安装 APK</a-radio>
            <a-radio value="resource">热更新（只更 H5）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="强制更新">
          <a-switch :checked="form.forceUpdate === 1" @update:checked="(v) => (form.forceUpdate = v ? 1 : 0)" />
        </a-form-item>
        <a-form-item label="最低壳 versionCode">
          <a-input-number v-model:value="form.minShellCode" :min="1" :precision="0" style="width: 200px" />
          <span style="margin-left: 8px; color: #999">热更新时原生壳低于此值会改走 APK</span>
        </a-form-item>
        <a-form-item label="更新说明">
          <a-textarea v-model:value="form.changelog" :rows="4" placeholder="展示在 APP 更新弹窗" />
        </a-form-item>
        <a-form-item label="APK">
          <a-space direction="vertical" style="width: 100%">
            <a-upload :show-upload-list="false" :before-upload="(file) => onUpload(file, 'apk')" accept=".apk">
              <a-button :loading="uploading === 'apk'" preIcon="ant-design:upload-outlined">上传 APK</a-button>
            </a-upload>
            <a-input v-model:value="form.apkUrl" placeholder="上传后自动填入，也可粘贴 URL" />
            <span v-if="form.apkSha256" style="color: #999">SHA-256：{{ form.apkSha256 }}</span>
          </a-space>
        </a-form-item>
        <a-form-item label="H5 zip">
          <a-space direction="vertical" style="width: 100%">
            <a-upload :show-upload-list="false" :before-upload="(file) => onUpload(file, 'resource')" accept=".zip">
              <a-button :loading="uploading === 'resource'" preIcon="ant-design:upload-outlined">上传 zip</a-button>
            </a-upload>
            <a-input v-model:value="form.resourceUrl" placeholder="zip 根目录须有 index.html" />
            <span v-if="form.resourceSha256" style="color: #999">SHA-256：{{ form.resourceSha256 }}</span>
          </a-space>
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 6, span: 14 }">
          <a-button type="primary" :loading="saving" @click="handleSave">保存配置</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-app-version" setup>
  import { PageWrapper } from '/@/components/Page';
  import { onMounted, reactive, ref } from 'vue';
  import { configApi } from '/@/api/homeai';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage, createConfirm } = useMessage();
  const saving = ref(false);
  const uploading = ref<'apk' | 'resource' | ''>('');
  const form = reactive({
    versionName: '1.0.0',
    versionCode: 100,
    updateMode: 'apk',
    forceUpdate: 0,
    apkUrl: '',
    resourceUrl: '',
    apkSha256: '',
    resourceSha256: '',
    minShellCode: 100,
    changelog: '',
    enabled: 0,
  });

  async function loadData() {
    try {
      const res = await configApi.getAppVersion();
      if (!res) return;
      form.versionName = res.versionName || '1.0.0';
      form.versionCode = res.versionCode ?? 100;
      form.updateMode = res.updateMode === 'resource' ? 'resource' : 'apk';
      form.forceUpdate = res.forceUpdate === 1 ? 1 : 0;
      form.apkUrl = res.apkUrl || '';
      form.resourceUrl = res.resourceUrl || '';
      form.apkSha256 = res.apkSha256 || '';
      form.resourceSha256 = res.resourceSha256 || '';
      form.minShellCode = res.minShellCode ?? form.versionCode;
      form.changelog = res.changelog || '';
      form.enabled = res.enabled === 1 ? 1 : 0;
    } catch (e: any) {
      createMessage.error(e?.message || 'APP 版本加载失败');
    }
  }

  function onUpload(file: File, kind: 'apk' | 'resource') {
    void doUpload(file, kind);
    return false;
  }

  async function doUpload(file: File, kind: 'apk' | 'resource') {
    uploading.value = kind;
    try {
      const res: any = await defHttp.uploadFile(
        { url: configApi.uploadAppPackage, timeout: 300 * 1000 },
        { file, name: 'file', data: { kind } },
        { isReturnResponse: true }
      );
      if (!res || res.success !== true || res.code !== 200) {
        throw new Error(res?.message || '上传失败');
      }
      const r = res.result || {};
      if (kind === 'apk') {
        form.apkUrl = r.url || '';
        form.apkSha256 = r.sha256 || '';
      } else {
        form.resourceUrl = r.url || '';
        form.resourceSha256 = r.sha256 || '';
      }
      createMessage.success('上传成功，请再点保存');
    } catch (e: any) {
      createMessage.error(e?.message || '上传失败');
    } finally {
      uploading.value = '';
    }
  }

  async function doSave() {
    saving.value = true;
    try {
      await configApi.updateAppVersion({ ...form });
      createMessage.success('保存成功');
    } catch (e: any) {
      createMessage.error(e?.message || '保存失败');
    } finally {
      saving.value = false;
    }
  }

  async function handleSave() {
    if (form.enabled === 1 && form.forceUpdate === 1) {
      createConfirm({
        iconType: 'warning',
        title: '强制更新',
        content: '开启后旧版本用户必须更新才能继续使用，确定保存？',
        onOk: doSave,
      });
      return;
    }
    await doSave();
  }

  onMounted(loadData);
</script>
