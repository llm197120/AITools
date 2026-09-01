<template>
  <div class="sync-config-page">
    <a-card title="APP 离线同步与缓存配置" :bordered="false">
      <a-alert
        type="info"
        show-icon
        message="APP 断网恢复后的缓慢同步参数：每批条数、批间隔、单条每日最大尝试次数、图片缓存上限。保存后 APP 下次启动生效。"
        style="margin-bottom: 16px"
      />
      <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item label="每批同步条数">
          <a-input-number v-model:value="form.batchSize" :min="1" :max="50" style="width: 100%" />
        </a-form-item>
        <a-form-item label="批间隔（毫秒）">
          <a-input-number v-model:value="form.intervalMs" :min="1000" :max="600000" :step="1000" style="width: 100%" />
        </a-form-item>
        <a-form-item label="单条每日最大尝试次数">
          <a-input-number v-model:value="form.maxRetriesPerDay" :min="1" :max="500" style="width: 100%" />
        </a-form-item>
        <a-form-item label="图片缓存上限（MB）">
          <a-input-number v-model:value="form.imageCacheLimitMb" :min="64" :max="16384" :step="128" style="width: 100%" />
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 6, span: 14 }">
          <a-button type="primary" :loading="saving" @click="save">保存</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { message } from 'ant-design-vue';

const BASE = '/homeai/config/sync';

const form = ref({
  batchSize: 1,
  intervalMs: 5000,
  maxRetriesPerDay: 20,
  imageCacheLimitMb: 4096,
});
const saving = ref(false);

async function load() {
  try {
    const data: any = await defHttp.get({ url: BASE });
    if (data) {
      form.value.batchSize = Number(data.batchSize) || 1;
      form.value.intervalMs = Number(data.intervalMs) || 5000;
      form.value.maxRetriesPerDay = Number(data.maxRetriesPerDay) || 20;
      form.value.imageCacheLimitMb = Number(data.imageCacheLimitMb) || 4096;
    }
  } catch {
    /* ignore */
  }
}

async function save() {
  saving.value = true;
  try {
    await defHttp.put({ url: `${BASE}/admin`, data: { ...form.value } });
    message.success('保存成功');
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.sync-config-page {
  padding: 16px;
  max-width: 640px;
}
</style>