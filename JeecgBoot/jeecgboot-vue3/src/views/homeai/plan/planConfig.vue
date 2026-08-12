<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-alert
      type="info"
      show-icon
      style="margin-bottom: 16px"
      message="计划模块运行时参数。修改后立即生效（Redis 缓存）；yml 中为默认值。"
    />
    <a-card title="计划与提醒配置" :bordered="false">
      <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 10 }">
        <a-form-item label="重复计划窗口(天)">
          <a-input-number v-model:value="form.repeatHorizonDays" :min="7" :max="365" style="width: 160px" />
          <span style="margin-left: 8px; color: #999">预生成与每日滚动的未来天数</span>
        </a-form-item>
        <a-form-item label="实例清理保留(天)">
          <a-input-number v-model:value="form.instanceCleanupDays" :min="7" :max="180" style="width: 160px" />
          <span style="margin-left: 8px; color: #999">早于该日期的实例将被物理删除</span>
        </a-form-item>
        <a-form-item label="启用计划提醒">
          <a-switch v-model:checked="form.remindEnabled" />
        </a-form-item>
        <a-form-item label="AI 文档润色">
          <a-switch v-model:checked="form.aiDocPolishEnabled" />
          <span style="margin-left: 8px; color: #999">Office AI 生成时调用默认大模型</span>
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 8, span: 10 }">
          <a-button type="primary" :loading="saving" @click="handleSave">保存配置</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-plan-config" setup>
  import { PageWrapper } from '/@/components/Page';
  import { onMounted, reactive, ref } from 'vue';
  import { configApi } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage } = useMessage();
  const saving = ref(false);
  const form = reactive({
    repeatHorizonDays: 90,
    instanceCleanupDays: 30,
    remindEnabled: true,
    aiDocPolishEnabled: true,
  });

  async function loadData() {
    const res: any = await configApi.getPlanConfig();
    if (res) {
      form.repeatHorizonDays = res.repeatHorizonDays ?? 90;
      form.instanceCleanupDays = res.instanceCleanupDays ?? 30;
      form.remindEnabled = res.remindEnabled !== false;
      form.aiDocPolishEnabled = res.aiDocPolishEnabled !== false;
    }
  }

  async function handleSave() {
    saving.value = true;
    try {
      await configApi.updatePlanConfig({ ...form });
      createMessage.success('保存成功');
    } finally {
      saving.value = false;
    }
  }

  onMounted(loadData);
</script>
