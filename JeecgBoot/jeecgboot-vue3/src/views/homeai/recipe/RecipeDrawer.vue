<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="isUpdate ? '编辑菜谱' : '新增菜谱'" width="40%">
    <BasicForm @register="registerForm" @submit="handleSubmit">
      <template #coverUrlSlot="{ model, field }">
        <HomeaiMediaUpload v-model:value="model[field]" mode="image" :upload-url="`${BASE}/recipe/cover`" tip="支持 jpg/png/webp，建议比例 16:9" />
      </template>
      <template #videoUrlSlot="{ model, field }">
        <HomeaiMediaUpload
          v-model:value="model[field]"
          mode="video"
          :upload-url="`${BASE}/recipe/video`"
          :max-size="200"
          tip="支持 mp4/webm/mov 等常见视频格式"
        />
      </template>
    </BasicForm>

    <a-card size="small" title="食材清单" :bordered="false" style="margin-top: 12px; background: var(--hai-admin-bg, #f3f2ee)">
      <div v-for="(ing, i) in ingredients" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px">
        <a-input v-model:value="ing.name" placeholder="食材名" style="flex: 1" />
        <a-input v-model:value="ing.amount" placeholder="用量" style="flex: 1" />
        <a-button type="text" danger size="small" @click="ingredients.splice(i, 1)">删除</a-button>
      </div>
      <a-button type="dashed" block size="small" @click="ingredients.push({ name: '', amount: '' })">+ 添加食材</a-button>
    </a-card>

    <a-card size="small" title="烹饪步骤" :bordered="false" style="margin-top: 12px; background: var(--hai-admin-bg, #f3f2ee)">
      <div
        v-for="(s, i) in steps"
        :key="i"
        style="border: 1px solid var(--hai-admin-border, #ece9e2); border-radius: 8px; padding: 8px; margin-bottom: 8px; background: #fff"
      >
        <div style="display: flex; justify-content: space-between; margin-bottom: 6px">
          <span style="font-weight: 600">第 {{ i + 1 }} 步</span>
          <span>
            <a-button type="text" size="small" @click="moveStep(i, -1)">↑</a-button>
            <a-button type="text" size="small" @click="moveStep(i, 1)">↓</a-button>
            <a-button type="text" danger size="small" @click="steps.splice(i, 1)">删除</a-button>
          </span>
        </div>
        <a-textarea v-model:value="s.description" :rows="2" placeholder="步骤说明" style="margin-bottom: 6px" />
        <HomeaiMediaUpload v-model:value="s.imageUrl" mode="image" :upload-url="`${BASE}/recipe/step-image`" tip="可选：上传该步骤图片" />
      </div>
      <a-button type="dashed" block size="small" @click="steps.push({ description: '', imageUrl: '' })">+ 添加步骤</a-button>
    </a-card>

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
  import { recipeApi, familyApi } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';
  import type { HomeaiCategory, HomeaiRecipe, HomeaiRecipeStep } from '/@/api/homeai';
  import type { HomeaiRecipeIngredient } from '/@/api/homeai/types';
  import HomeaiMediaUpload from '/@/views/homeai/components/HomeaiMediaUpload.vue';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const BASE = '/homeai';

  /** 展示用量 → quantity/unit（与小程序 recipeIngredient 对齐，跨项目内联） */
  function parseAmountToQuantityUnit(amount: string): { quantity?: number; unit?: string } {
    const trimmed = (amount || '').trim();
    if (!trimmed) return {};
    const match = trimmed.match(/^(\d+(?:\.\d+)?)(.*)$/);
    if (!match) return { unit: trimmed };
    const quantity = Number(match[1]);
    const unit = (match[2] || '').trim();
    const result: { quantity?: number; unit?: string } = {};
    if (!Number.isNaN(quantity)) result.quantity = quantity;
    if (unit) result.unit = unit;
    return result;
  }

  /** quantity/unit → 展示用量 */
  function formatQuantityUnit(quantity?: number | string | null, unit?: string | null, amountFallback?: string): string {
    const hasQuantity = quantity !== undefined && quantity !== null && quantity !== '';
    if (hasQuantity) return `${quantity}${unit || ''}`;
    if (unit) return String(unit);
    return amountFallback || '';
  }

  const isUpdate = ref(false);
  const recordId = ref('');
  const saving = ref(false);
  const categoryOptions = ref<{ label: string; value: string }[]>([]);
  const familyOptions = ref<{ label: string; value: string }[]>([]);
  const ingredients = ref<HomeaiRecipeIngredient[]>([{ name: '', amount: '' }]);
  const steps = ref<Array<{ description: string; imageUrl?: string }>>([{ description: '', imageUrl: '' }]);

  async function loadFamilyOptions() {
    try {
      const res: any = await familyApi.list({ pageNo: 1, pageSize: 500 });
      const records = res?.records || res?.result?.records || [];
      familyOptions.value = records.map((f: any) => ({ label: f.name, value: f.id }));
    } catch {
      familyOptions.value = [];
    }
  }

  const [registerDrawer, { closeDrawer }] = useDrawerInner(async (data: { isUpdate?: boolean; record?: HomeaiRecipe }) => {
    isUpdate.value = data.isUpdate || false;
    recordId.value = data.record?.id || '';
    resetFields();
    ingredients.value = [{ name: '', amount: '' }];
    steps.value = [{ description: '', imageUrl: '' }];
    await loadFamilyOptions();
    try {
      const list: HomeaiCategory[] = (await recipeApi.categories()) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.id }));
      updateSchema([
        {
          field: 'categoryId',
          componentProps: { options: categoryOptions.value, placeholder: '请选择分类', showSearch: true },
        },
        {
          field: 'familyId',
          componentProps: { options: familyOptions.value, placeholder: '请选择家庭', showSearch: true, allowClear: true },
        },
      ]);
    } catch {
      categoryOptions.value = [];
    }
    if (isUpdate.value && data.record) {
      setFieldsValue({ ...data.record });
      // 加载食材/步骤
      try {
        const detail = await recipeApi.getById(data.record.id);
        const d = detail?.recipe || detail;
        if (d) {
          setFieldsValue({
            ...data.record,
            coverUrl: d.coverUrl,
            videoUrl: d.videoUrl,
            visibility: d.visibility || 'public',
            familyId: d.familyId,
          });
        }
        const ings: HomeaiRecipeIngredient[] = detail?.ingredients || [];
        ingredients.value = ings.length
          ? ings.map((x) => ({ name: x.name, amount: formatQuantityUnit(x.quantity, x.unit, x.amount) }))
          : [{ name: '', amount: '' }];
        const sts: HomeaiRecipeStep[] = detail?.steps || [];
        steps.value = sts.length
          ? sts.map((x) => ({
              description: x.description || '',
              imageUrl: x.imageUrl,
            }))
          : [{ description: '', imageUrl: '' }];
      } catch {
        createMessage.warning('菜谱详情加载失败，已使用列表数据');
      }
    } else {
      setFieldsValue({ visibility: 'public' });
    }
  });

  const [registerForm, { setFieldsValue, resetFields, submit, updateSchema }] = useForm({
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
        componentProps: {
          options: [
            { label: '入门', value: 1 },
            { label: '简单', value: 2 },
            { label: '中等', value: 3 },
            { label: '较难', value: 4 },
            { label: '困难', value: 5 },
          ],
        },
        defaultValue: 3,
      },
      { field: 'cookTime', label: '烹饪时间(分)', component: 'InputNumber' },
      { field: 'servings', label: '份数', component: 'InputNumber', defaultValue: 1 },
      {
        field: 'visibility',
        label: '可见性',
        component: 'Select',
        componentProps: {
          options: [
            { label: '公开（所有小程序用户）', value: 'public' },
            { label: '家庭共享', value: 'family' },
            { label: '仅自己', value: 'private' },
          ],
        },
        defaultValue: 'public',
      },
      {
        field: 'familyId',
        label: '所属家庭',
        component: 'Select',
        componentProps: { options: familyOptions, placeholder: '请选择家庭', showSearch: true, allowClear: true },
        ifShow: ({ values }) => values.visibility === 'family',
        required: true,
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

  async function handleSubmit(values: any) {
    try {
      if (values.visibility === 'family' && !values.familyId) {
        createMessage.warning('家庭共享菜谱请选择所属家庭');
        return false;
      }
      saving.value = true;
      const payload = {
        ...values,
        familyId: values.visibility === 'family' ? values.familyId : null,
        ingredients: ingredients.value
          .filter((x: any) => x.name)
          .map((x: any) => {
            const { quantity, unit } = parseAmountToQuantityUnit(x.amount || '');
            return { name: x.name, quantity, unit };
          }),
        steps: steps.value
          .filter((x: any) => x.description)
          .map((x: any, i: number) => ({
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
