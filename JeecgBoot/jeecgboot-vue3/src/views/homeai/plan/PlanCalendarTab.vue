<template>
  <div class="plan-calendar-tab">
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="24" style="margin-bottom: 12px">
        <a-space>
          <span>筛选用户：</span>
          <a-select
            v-model:value="calendarUserId"
            allow-clear
            placeholder="全部用户"
            style="width: 220px"
            :options="userOptions"
            @change="onCalendarUserChange"
          />
        </a-space>
      </a-col>
      <a-col :span="14">
        <a-card :bordered="false" title="计划日历">
          <template #extra>
            <a-button size="small" type="link" @click="handleRollForwardAll">全量补跑重复实例</a-button>
          </template>
          <a-calendar v-model:value="calendarValue" @panelChange="onCalendarPanelChange" @select="onCalendarSelect">
            <template #dateCellRender="{ current }">
              <div class="cal-cell" :class="cellClass(current)">
                <span v-if="calendarCellStatus(current) === 'pending'" class="cal-dot pending" title="有待办" />
                <span v-else-if="calendarCellStatus(current) === 'expired'" class="cal-dot expired" title="有过期" />
                <span v-else-if="calendarCellStatus(current) === 'mixed'" class="cal-dot mixed" title="待办+过期" />
                <span v-if="calendarCellStatus(current)" class="cal-label">{{ cellLabel(current) }}</span>
              </div>
            </template>
          </a-calendar>
          <div class="cal-legend">
            <span><i class="cal-dot pending" />待办</span>
            <span><i class="cal-dot expired" />过期</span>
            <span><i class="cal-dot mixed" />混合</span>
          </div>
        </a-card>
      </a-col>
      <a-col :span="10">
        <a-card :bordered="false" :title="selectedDateLabel + ' 的计划'">
          <a-list v-if="dayPlans.length" :data-source="dayPlans" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta
                  :title="item.title"
                  :description="`${item.category || '-'} · ${resolveUserLabel(item.userId)} · ${statusLabel(item.status)}${item.repeatRule && item.repeatRule !== 'none' ? ' · ' + repeatLabel(item.repeatRule) : ''}`"
                />
              </a-list-item>
            </template>
          </a-list>
          <a-empty v-else description="当日暂无计划" />
        </a-card>
      </a-col>
    </a-row>
    <a-card title="补跑操作日志" :bordered="false" style="margin-bottom: 16px">
      <a-table
        :data-source="rollLogs"
        :columns="rollLogColumns"
        :pagination="rollLogPagination"
        size="small"
        row-key="id"
        @change="onRollLogTableChange"
      />
    </a-card>
  </div>
</template>

<script lang="ts" name="homeai-plan-calendar-tab" setup>
  import { onMounted } from 'vue';
  import type { Dayjs } from 'dayjs';
  import dayjs from 'dayjs';
  import { usePlanCalendar } from './usePlanCalendar';

  const {
    calendarUserId,
    userOptions,
    rollLogs,
    rollLogPagination,
    rollLogColumns,
    calendarValue,
    dayPlans,
    selectedDateLabel,
    calendarCellStatus,
    statusLabel,
    repeatLabel,
    resolveUserLabel,
    onCalendarUserChange,
    onCalendarPanelChange,
    onCalendarSelect,
    onRollLogTableChange,
    refreshCalendarTab,
    handleRollForwardAll,
  } = usePlanCalendar();

  function cellClass(current: Dayjs) {
    const status = calendarCellStatus(current);
    const today = current.isSame(dayjs(), 'day');
    return {
      'is-today': today,
      [`is-${status}`]: !!status,
    };
  }

  function cellLabel(current: Dayjs) {
    const status = calendarCellStatus(current);
    if (status === 'pending') return '待办';
    if (status === 'expired') return '过期';
    if (status === 'mixed') return '混合';
    return '';
  }

  /** 供父组件在切到日历 Tab 时刷新 */
  defineExpose({ refreshCalendarTab, handleRollForwardAll, loadUserOptions: refreshCalendarTab });

  onMounted(() => {
    refreshCalendarTab();
  });
</script>

<style scoped>
  .cal-cell {
    min-height: 28px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    padding-top: 2px;
  }
  .cal-cell.is-today {
    background: rgba(27, 79, 138, 0.08);
    border-radius: 6px;
  }
  .cal-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
  .cal-dot.pending {
    background: #1b4f8a;
  }
  .cal-dot.expired {
    background: #a39e94;
  }
  .cal-dot.mixed {
    background: #c45c4a;
  }
  .cal-label {
    font-size: 11px;
    line-height: 1.2;
    color: #8a857c;
  }
  .cal-cell.is-pending .cal-label {
    color: #1b4f8a;
  }
  .cal-cell.is-expired .cal-label {
    color: #a39e94;
  }
  .cal-cell.is-mixed .cal-label {
    color: #c45c4a;
  }
  .cal-legend {
    display: flex;
    gap: 16px;
    margin-top: 8px;
    font-size: 12px;
    color: #8a857c;
  }
  .cal-legend span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
</style>
