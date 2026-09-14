<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-alert
      type="info"
      show-icon
      style="margin-bottom: 16px"
      message="原 yml 中的运行时项可在此修改，保存后立即生效（无需重启）。计划与存储配额与原独立配置页共用同一套数据。微信 appid/secret、JWT 密钥仍走环境配置。"
    />
    <a-card :bordered="false">
      <a-spin :spinning="loading">
        <a-tabs v-model:activeKey="tab">
          <a-tab-pane key="upload" tab="上传体积">
            <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 12 }">
              <a-form-item v-for="item in uploadFields" :key="item.key" :label="item.label">
                <a-input-number v-model:value="uploadMb[item.key]" :min="1" :max="2048" :step="1" style="width: 160px" />
                <span class="hint">MB · yml 默认 {{ item.def }}MB</span>
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="learn" tab="学习提醒">
            <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 12 }">
              <a-form-item label="启用学习提醒">
                <a-switch v-model:checked="form.learn.remindEnabled" />
              </a-form-item>
              <a-form-item label="Cron">
                <a-input v-model:value="form.learn.remindCron" placeholder="0 0 20 * * ?" />
                <div class="hint">Spring 6 段表达式，默认每天 20:00。改完下一分钟起按新时刻触发。</div>
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="wechat" tab="微信订阅">
            <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 14 }">
              <a-form-item label="计划提醒模板 ID">
                <a-input v-model:value="form.wechat.planRemindTemplateId" />
              </a-form-item>
              <a-form-item label="学习提醒模板 ID">
                <a-input v-model:value="form.wechat.learnRemindTemplateId" />
              </a-form-item>
              <a-form-item label="标题字段">
                <a-input v-model:value="form.wechat.learnRemindTitleField" placeholder="thing1" />
              </a-form-item>
              <a-form-item label="已学分钟字段">
                <a-input v-model:value="form.wechat.learnRemindProgressField" placeholder="number2" />
              </a-form-item>
              <a-form-item label="目标分钟字段">
                <a-input v-model:value="form.wechat.learnRemindGoalField" placeholder="number3" />
              </a-form-item>
              <a-form-item label="日期字段">
                <a-input v-model:value="form.wechat.learnRemindDateField" placeholder="time4" />
              </a-form-item>
              <a-form-item label="标题文案">
                <a-input v-model:value="form.wechat.learnRemindTitleText" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="office" tab="Office 转换">
            <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 14 }">
              <a-form-item label="优先本机 Office">
                <a-switch v-model:checked="form.office.preferMsOffice" />
                <span class="hint">仅 Windows 生效，失败回退 LibreOffice</span>
              </a-form-item>
              <a-form-item label="LibreOffice 路径">
                <a-input v-model:value="form.office.sofficePath" />
              </a-form-item>
              <a-form-item label="PowerShell 路径">
                <a-input v-model:value="form.office.powershellPath" />
              </a-form-item>
              <a-form-item label="转换超时">
                <a-input-number v-model:value="form.office.convertTimeoutSeconds" :min="30" :max="600" style="width: 160px" />
                <span class="hint">秒</span>
              </a-form-item>
              <a-form-item label="Gotenberg 地址">
                <a-input v-model:value="form.office.gotenbergUrl" placeholder="留空则不用，例如 http://127.0.0.1:3000" />
                <div class="hint">仅本机/局域网。填了才启用；文档会 POST 到该地址，禁止公网。</div>
              </a-form-item>
              <a-form-item label="kkFileView 地址">
                <a-input v-model:value="form.office.kkFileViewUrl" placeholder="留空则不用，例如 http://127.0.0.1:8012" />
                <div class="hint">仅本机/局域网。管理端复杂格式预览；禁止公网。须能访问「文件外链」主机。</div>
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="oss" tab="OSS">
            <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 12 }">
              <a-form-item label="私有桶预签名">
                <a-switch v-model:checked="form.oss.privateBucket" />
              </a-form-item>
              <a-form-item label="预签名有效期">
                <a-input-number v-model:value="form.oss.presignExpireSeconds" :min="300" :max="86400" :step="300" style="width: 160px" />
                <span class="hint">秒（5 分钟～24 小时）</span>
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="file" tab="文件外链">
            <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 14 }">
              <a-form-item label="完整根地址">
                <a-input v-model:value="form.file.baseUrl" placeholder="留空则用协议+主机+端口+context-path" />
              </a-form-item>
              <a-form-item label="协议">
                <a-select v-model:value="form.file.scheme" style="width: 160px">
                  <a-select-option value="http">http</a-select-option>
                  <a-select-option value="https">https</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="主机">
                <a-input v-model:value="form.file.host" placeholder="127.0.0.1 或域名" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="plan" tab="计划">
            <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 12 }">
              <a-form-item label="重复计划窗口">
                <a-input-number v-model:value="form.plan.repeatHorizonDays" :min="7" :max="365" style="width: 160px" />
                <span class="hint">天</span>
              </a-form-item>
              <a-form-item label="实例清理保留">
                <a-input-number v-model:value="form.plan.instanceCleanupDays" :min="7" :max="180" style="width: 160px" />
                <span class="hint">天</span>
              </a-form-item>
              <a-form-item label="启用计划提醒">
                <a-switch v-model:checked="form.plan.remindEnabled" />
              </a-form-item>
              <a-form-item label="AI 文档润色">
                <a-switch v-model:checked="form.plan.aiDocPolishEnabled" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="storage" tab="存储配额">
            <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 12 }">
              <a-form-item label="默认用户配额">
                <a-input-number v-model:value="userLimitGb" :min="0.01" :max="100" :step="0.5" style="width: 160px" />
                <span class="hint">GB</span>
              </a-form-item>
              <a-form-item label="默认家庭配额">
                <a-input-number v-model:value="familyLimitGb" :min="0.01" :max="100" :step="0.5" style="width: 160px" />
                <span class="hint">GB</span>
              </a-form-item>
              <a-form-item label="告警阈值">
                <a-input-number v-model:value="form.storage.warnPercent" :min="50" :max="99" style="width: 160px" />
                <span class="hint">%</span>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-spin>
      <a-button type="primary" :loading="saving" @click="handleSave">保存全部</a-button>
    </a-card>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-sys-config" setup>
  import { PageWrapper } from '/@/components/Page';
  import { onMounted, reactive, ref } from 'vue';
  import { configApi } from '/@/api/homeai';
  import type { HomeaiSysConfig } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage } = useMessage();
  const MB = 1024 * 1024;
  const GB = 1024 * MB;
  const loading = ref(false);
  const saving = ref(false);
  const tab = ref('upload');
  const uploadFields = [
    { key: 'video', label: '视频', def: 200 },
    { key: 'audio', label: '音频', def: 50 },
    { key: 'image', label: '图片', def: 20 },
    { key: 'document', label: '文档', def: 50 },
    { key: 'archive', label: '压缩包', def: 100 },
    { key: 'text', label: '文本', def: 10 },
  ] as const;
  const uploadMb = reactive<Record<string, number>>({
    video: 200,
    audio: 50,
    image: 20,
    document: 50,
    archive: 100,
    text: 10,
  });
  const userLimitGb = ref(1);
  const familyLimitGb = ref(5);
  const form = reactive<Required<Pick<HomeaiSysConfig, 'learn' | 'wechat' | 'office' | 'oss' | 'file' | 'plan' | 'storage'>>>({
    learn: { remindEnabled: true, remindCron: '0 0 20 * * ?' },
    wechat: {
      planRemindTemplateId: '',
      learnRemindTemplateId: '',
      learnRemindTitleField: 'thing1',
      learnRemindProgressField: 'number2',
      learnRemindGoalField: 'number3',
      learnRemindDateField: 'time4',
      learnRemindTitleText: '每日学习目标',
    },
    office: { preferMsOffice: true, sofficePath: 'soffice', powershellPath: 'powershell', convertTimeoutSeconds: 120, gotenbergUrl: '', kkFileViewUrl: '' },
    oss: { privateBucket: true, presignExpireSeconds: 7200 },
    file: { baseUrl: '', scheme: 'http', host: '127.0.0.1' },
    plan: { repeatHorizonDays: 90, instanceCleanupDays: 30, remindEnabled: true, aiDocPolishEnabled: true },
    storage: { defaultUserLimitBytes: GB, defaultFamilyLimitBytes: 5 * GB, warnPercent: 80 },
  });

  function bytesToMb(v?: number, fallback = 1) {
    if (!v || v <= 0) return fallback;
    return Math.max(1, Math.round(v / MB));
  }

  function bytesToGb(v?: number, fallback = 1) {
    if (!v || v <= 0) return fallback;
    return Math.round((v / GB) * 100) / 100;
  }

  async function loadData() {
    loading.value = true;
    try {
      const res = await configApi.getSysConfig();
      if (!res) return;
      uploadMb.video = bytesToMb(res.upload?.video, 200);
      uploadMb.audio = bytesToMb(res.upload?.audio, 50);
      uploadMb.image = bytesToMb(res.upload?.image, 20);
      uploadMb.document = bytesToMb(res.upload?.document, 50);
      uploadMb.archive = bytesToMb(res.upload?.archive, 100);
      uploadMb.text = bytesToMb(res.upload?.text, 10);
      Object.assign(form.learn, res.learn || {});
      Object.assign(form.wechat, res.wechat || {});
      Object.assign(form.office, res.office || {});
      Object.assign(form.oss, res.oss || {});
      Object.assign(form.file, res.file || {});
      Object.assign(form.plan, res.plan || {});
      Object.assign(form.storage, res.storage || {});
      userLimitGb.value = bytesToGb(form.storage.defaultUserLimitBytes, 1);
      familyLimitGb.value = bytesToGb(form.storage.defaultFamilyLimitBytes, 5);
    } catch (e: any) {
      createMessage.error(e?.message || '系统配置加载失败');
    } finally {
      loading.value = false;
    }
  }

  async function handleSave() {
    saving.value = true;
    try {
      const payload: HomeaiSysConfig = {
        upload: {
          video: Math.round(uploadMb.video * MB),
          audio: Math.round(uploadMb.audio * MB),
          image: Math.round(uploadMb.image * MB),
          document: Math.round(uploadMb.document * MB),
          archive: Math.round(uploadMb.archive * MB),
          text: Math.round(uploadMb.text * MB),
        },
        learn: { ...form.learn },
        wechat: { ...form.wechat },
        office: { ...form.office },
        oss: { ...form.oss },
        file: { ...form.file },
        plan: { ...form.plan },
        storage: {
          defaultUserLimitBytes: Math.round(userLimitGb.value * GB),
          defaultFamilyLimitBytes: Math.round(familyLimitGb.value * GB),
          warnPercent: form.storage.warnPercent,
        },
      };
      await configApi.updateSysConfig(payload);
      createMessage.success('保存成功，已立即生效');
    } catch (e: any) {
      createMessage.error(e?.message || '保存失败');
    } finally {
      saving.value = false;
    }
  }

  onMounted(loadData);
</script>

<style scoped>
.hint {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
}
</style>
