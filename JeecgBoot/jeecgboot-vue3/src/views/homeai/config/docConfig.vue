<template>
  <div class="doc-config-page">
    <a-card title="协议与隐私政策" :bordered="false">
      <a-alert
        type="info"
        show-icon
        message="保存后 APP 登录页与个人中心会优先展示此处内容。留空则 APP 回退内置静态文案。请勿粘贴脚本或外链脚本。"
        style="margin-bottom: 16px"
      />
      <a-spin :spinning="loading">
        <a-tabs v-model:activeKey="activeKey">
          <a-tab-pane key="agreement" tab="用户协议">
            <JEditor v-model:value="agreementHtml" :height="420" :autoFocus="false" />
          </a-tab-pane>
          <a-tab-pane key="privacy" tab="隐私政策">
            <JEditor v-model:value="privacyHtml" :height="420" :autoFocus="false" />
          </a-tab-pane>
        </a-tabs>
      </a-spin>
      <div class="doc-config-actions">
        <a-button type="primary" :loading="saving" @click="save">保存当前页</a-button>
        <a-button :loading="saving" @click="saveAll">保存全部</a-button>
      </div>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted } from 'vue';
  import { defHttp } from '/@/utils/http/axios';
  import { message } from 'ant-design-vue';
  import JEditor from '/@/components/Form/src/jeecg/components/JEditor.vue';

  const BASE = '/homeai/config/doc';
  const activeKey = ref<'agreement' | 'privacy'>('agreement');
  const agreementHtml = ref('');
  const privacyHtml = ref('');
  const loading = ref(false);
  const saving = ref(false);

  async function load(type: 'agreement' | 'privacy') {
    const html: any = await defHttp.get({ url: `${BASE}/${type}` });
    const text = html == null ? '' : String(html);
    if (type === 'agreement') agreementHtml.value = text;
    else privacyHtml.value = text;
  }

  async function loadAll() {
    loading.value = true;
    try {
      await Promise.all([load('agreement'), load('privacy')]);
    } catch {
      message.error('加载失败，请刷新重试');
    } finally {
      loading.value = false;
    }
  }

  async function saveOne(type: 'agreement' | 'privacy') {
    const html = type === 'agreement' ? agreementHtml.value : privacyHtml.value;
    await defHttp.put({ url: `${BASE}/${type}/admin`, data: { content: html } }, { successMessageMode: 'none' });
  }

  async function save() {
    saving.value = true;
    try {
      await saveOne(activeKey.value);
      message.success('保存成功');
    } catch {
      message.error('保存失败');
    } finally {
      saving.value = false;
    }
  }

  async function saveAll() {
    saving.value = true;
    try {
      await saveOne('agreement');
      await saveOne('privacy');
      message.success('全部保存成功');
    } catch {
      message.error('保存失败');
    } finally {
      saving.value = false;
    }
  }

  onMounted(loadAll);
</script>

<style scoped>
  .doc-config-page {
    padding: 16px;
    max-width: 960px;
  }

  .doc-config-actions {
    margin-top: 16px;
    text-align: right;
  }

  .doc-config-actions > .ant-btn + .ant-btn {
    margin-left: 8px;
  }
</style>
