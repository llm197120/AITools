<template>
  <div style="padding: 16px">
    <BasicTable @register="registerTable">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag :color="record.type === 'expense' ? 'red' : 'green'">{{ record.type === 'expense' ? '支出' : '收入' }}</a-tag>
        </template>
        <template v-if="column.key === 'color'">
          <span class="color-dot" :style="{background: record.color}"></span>
        </template>
      </template>
    </BasicTable>
  </div>
</template>
<script lang="ts" name="homeai-bill-category" setup>
  import { BasicTable, useTable } from '/@/components/Table';
  import { defHttp } from '/@/utils/http/axios';
  const [registerTable] = useTable({
    title: '消费分类管理',
    api: (params: any) => defHttp.get({ url: '/homeai/bill/category-list', params }),
    columns: [
      { title: '分类名称', dataIndex: 'name', width: 150 },
      { title: '图标', dataIndex: 'icon', width: 60 },
      { title: '颜色', dataIndex: 'color', key: 'color', width: 60 },
      { title: '类型', dataIndex: 'type', key: 'type', width: 60 },
      { title: '系统默认', dataIndex: 'isDefault', width: 70 },
      { title: '排序', dataIndex: 'sortOrder', width: 60 },
    ],
    useSearchForm: false, showTableSetting: true, showIndexColumn: true,
  });
</script>
<style>.color-dot{display:inline-block;width:16px;height:16px;border-radius:50%}</style>
