<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="isUpdate ? '编辑菜谱' : '新增菜谱'" width="45%">
    <BasicForm @register="registerForm" @submit="handleSubmit">
      <template #coverUrlSlot="{ model, field }">
        <div style="display: flex; gap: 8px; align-items: center">
          <a-input v-model:value="model[field]" placeholder="封面图片地址" style="flex: 1" />
          <input ref="coverInputRef" type="file" accept="image/*" style="display: none" @change="(e: any) => onFileChange(e, 'cover')" />
          <a-button size="small" @click="coverInputRef?.click()">上传</a-button>
          <a-image v-if="model[field]" :src="model[field]" width="48" height="48" style="border-radius: 4px" />
        </div>
      </template>
      <template #videoUrlSlot="{ model, field }">
        <div style="display: flex; gap: 8px; align-items: center">
          <a-input v-model:value="model[field]" placeholder="做菜视频地址" style="flex: 1" />
          <input ref="videoInputRef" type="file" accept="video/*" style="display: none" @change="(e: any) => onFileChange(e, 'video')" />
          <a-button size="small" @click="videoInputRef?.click()">上传</a-button>
        </div>
      </template>
    </BasicForm>

    <!-- 食材清单 -->
    <a-divider style="margin: 8px 0">食材清单</a-divider>
    <div v-for="(ing, i) in ingredients" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px">
      <a-input v-model:value="ing.name" placeholder="食材名" style="flex: 1" />
      <a-input v-model:value="ing.amount" placeholder="用量" style="flex: 1" />
      <a-button type="text" danger size="small" @click="ingredients.splice(i, 1)">删除</a-button>
    </div>
    <a-button type="dashed" block size="small" @click="ingredients.push({ name: '', amount: '' })">+ 添加食材</a-button>

    <!-- 烹饪步骤 -->
    <a-divider style="margin: 16px 0 8px">烹饪步骤</a-divider>
    <div v-for="(s, i) in steps" :key="i" style="border: 1px solid #f0f0f0; border-radius: 8px; padding: 8px; margin-bottom: 8px">
      <div style="display: flex; justify-content: space-between; margin-bottom: 6px">
        <span style="font-weight: 600">第 {{ i + 1 }} 步</span>
        <span>
          <a-button type="text" size="small" @click="moveStep(i, -1)">↑</a-button>
          <a-button type="text" size="small" @click="moveStep(i, 1)">↓</a-button>
          <a-button type="text" danger size="small" @click="steps.splice(i, 1)">删除</a-button>
        </span>
      </div>
      <a-textarea v-model:value="s.description" :rows="2" placeholder="步骤说明" style="margin-bottom: 6px" />
      <div style="display: flex; gap: 8px; align-items: center">
        <a-input v-model:value="s.imageUrl" placeholder="步骤图片地址" style="flex: 1" />
        <input type="file" accept="image/*" style="display: none" :id="`stepImg-${i}`" @change="(e: any) => onStepImageChange(e, i)" />
        <a-button size="small" @click="triggerStepImage(i)">上传图</a-button>
        <a-image v-if="s.imageUrl" :src="s.imageUrl" width="48" height="48" style="border-radius: 4px" />
      </div>
    </div>
    <a-button type="dashed" block size="small" @click="steps.push({ description: '', imageUrl: '' })">+ 添加步骤</a-button>

    <!-- 底部按钮 -->
    <template #footer>
      <a-button type="primary" :loading="saving" @click="submit">保存</a-button>
      <a-button style="margin-left: 8px" @click="closeDrawer()">取消</a-button>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" name="homeai-recipe-drawer" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { recipeApi } from '/@/api/homeai';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();

  const isUpdate = ref(false);
  const recordId = ref('');
  const saving = ref(false);
  const categoryOptions = ref<{ label: string; value: string }[]>([]);
  const coverInputRef = ref<HTMLInputElement | null>(null);
  const videoInputRef = ref<HTMLInputElement | null>(null);
  const ingredients = ref<any[]>([{ name: '', amount: '' }]);
  const steps = ref<any[]>([{ description: '', imageUrl: '' }]);

  const [registerDrawer, { closeDrawer }] = useDrawerInner(async (data: any) => {
    isUpdate.value = data.isUpdate || false;
    recordId.value = data.record?.id || '';
    resetFields();
    ingredients.value = [{ name: '', amount: '' }];
    steps.value = [{ description: '', imageUrl: '' }];
    try {
      const list: any[] = (await recipeApi.categories()) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.id }));
      updateSchema({
        field: 'categoryId',
        componentProps: { options: categoryOptions.value, placeholder: '请选择分类', showSearch: true },
      });
    } catch {
      categoryOptions.value = [];
    }
    if (isUpdate.value && data.record) {
      setFieldsValue({ ...data.record });
      // 加载食材/步骤
      try {
        const detail: any = await defHttp.get({ url: `/homeai/recipe/${data.record.id}` });
        const d = detail?.recipe || detail;
        if (d) setFieldsValue({ ...data.record, coverUrl: d.coverUrl, videoUrl: d.videoUrl, visibility: d.visibility });
        const ings = detail?.ingredients || [];
        ingredients.value = ings.length ? ings.map((x: any) => ({ name: x.name, amount: x.amount })) : [{ name: '', amount: '' }];
        const sts = detail?.steps || [];
        steps.value = sts.length ? sts.map((x: any) => ({ description: x.description, imageUrl: x.imageUrl })) : [{ description: '', imageUrl: '' }];
      } catch {
        // 加载失败时使用列表数据
      }
    }
  });

  const [registerForm, { setFieldsValue, resetFields, validate, submit, updateSchema }] = useForm({
    labelWidth: 100,
    schemas: [
      { field: 'name', label: '菜名', component: 'Input', required: true },
      {
        field: 'categoryId',
        label: '分类',
        component: 'Select',
        required: true,
        componentProps: { options: categoryOptions, placeholder: '请选择分类', showSearch: true },
      },
      {
        field: 'difficulty',
        label: '难度',
        component: 'Select',
        componentProps: { options: [{ label: '简单', value: 1 }, { label: '中等', value: 3 }, { label: '困难', value: 5 }] },
        defaultValue: 3,
      },
      { field: 'cookTime', label: '烹饪时间(分)', component: 'InputNumber' },
      { field: 'servings', label: '份数', component: 'InputNumber', defaultValue: 1 },
      {
        field: 'visibility',
        label: '可见性',
        component: 'Select',
        componentProps: { options: [{ label: '家庭共享', value: 'family' }, { label: '仅自己', value: 'private' }] },
        defaultValue: 'family',
      },
      { field: 'coverUrl', label: '封面', component: 'Input', slot: 'coverUrlSlot' },
      { field: 'videoUrl', label: '视频', component: 'Input', slot: 'videoUrlSlot' },
      { field: 'tips', label: '小贴士', component: 'InputTextArea' },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  function moveStep(i: number, dir: number) {
    const j = i + dir;
    if (j < 0 || j >= steps.value.length) return;
    const tmp = steps.value[i];
    steps.value[i] = steps.value[j];
    steps.value[j] = tmp;
  }

  function triggerStepImage(i: number) {
    (document.getElementById(`stepImg-${i}`) as HTMLInputElement | null)?.click();
  }

  async function uploadFile(file: File, url: string): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    const res: any = await defHttp.uploadFile({ url, data: formData });
    return typeof res === 'string' ? res : res?.url || '';
  }

  async function onFileChange(e: any, type: 'cover' | 'video') {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const url = type === 'cover'
        ? await uploadFile(file, '/homeai/recipe/cover')
        : await uploadFile(file, '/homeai/recipe/video');
      setFieldsValue({ [type === 'cover' ? 'coverUrl' : 'videoUrl']: url });
      createMessage.success('上传成功');
    } catch (err: any) {
      createMessage.error(err?.message || '上传失败');
    }
  }

  async function onStepImageChange(e: any, i: number) {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      steps.value[i].imageUrl = await uploadFile(file, '/homeai/recipe/step-image');
      createMessage.success('上传成功');
    } catch (err: any) {
      createMessage.error(err?.message || '上传失败');
    }
  }

  async function handleSubmit(values: any) {
    try {
      saving.value = true;
      const payload = {
        ...values,
        ingredients: ingredients.value.filter((x: any) => x.name),
        steps: steps.value.filter((x: any) => x.description).map((x: any, i: number) => ({
          description: x.description,
          imageUrl: x.imageUrl || null,
          stepNum: i + 1,
        })),
      };
      if (isUpdate.value) {
        await recipeApi.edit(recordId.value, payload);
        createMessage.success('编辑成功');
      } else {
        await recipeApi.add(payload);
        createMessage.success('新增成功');
      }
      closeDrawer();
      emit('success');
      return true;
    } catch (e: any) {
      createMessage.error(e?.message || '操作失败');
      return false;
    } finally {
      saving.value = false;
    }
  }
</script>
