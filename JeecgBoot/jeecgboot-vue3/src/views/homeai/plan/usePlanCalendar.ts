/**
 * 计划日历 Tab 逻辑
 * 从 planList.vue 抽出，供 PlanCalendarTab / 列表页复用
 */
import { computed, ref } from 'vue';
import dayjs, { Dayjs } from 'dayjs';
import { planApi } from '/@/api/homeai';
import { useMessage } from '/@/hooks/web/useMessage';
import { useUserLabel } from '../hooks/useUserLabel';
import type { HomeaiPlan } from '/@/api/homeai/types';

export function usePlanCalendar() {
  const { createMessage, createConfirm } = useMessage();
  const { userOptions, loadUserOptions, resolveUserLabel } = useUserLabel();

  const calendarUserId = ref<string | undefined>(undefined);
  const rollLogs = ref<Record<string, unknown>[]>([]);
  const rollLogPagination = ref({ current: 1, pageSize: 5, total: 0 });
  const rollLogColumns = computed(() => [
    { title: '操作时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作人',
      dataIndex: 'userId',
      width: 140,
      customRender: ({ text }: { text: string }) => resolveUserLabel(text),
    },
    { title: '摘要', dataIndex: 'targetSummary' },
    { title: '结果', dataIndex: 'result', width: 80 },
  ]);
  const calendarValue = ref<Dayjs>(dayjs());
  const calendarDates = ref<string[]>([]);
  const expiredDates = ref<string[]>([]);
  const pendingDates = ref<string[]>([]);
  const dayPlans = ref<HomeaiPlan[]>([]);
  const selectedDateLabel = computed(() => calendarValue.value.format('YYYY-MM-DD'));

  function calendarCellStatus(current: Dayjs): string | null {
    const d = current.format('YYYY-MM-DD');
    const hasPending = pendingDates.value.includes(d);
    const hasExpired = expiredDates.value.includes(d);
    if (hasPending && hasExpired) return 'mixed';
    if (hasPending) return 'pending';
    if (hasExpired) return 'expired';
    if (calendarDates.value.includes(d)) return 'pending';
    return null;
  }

  function statusLabel(status: string) {
    return (
      { pending: '待完成', completed: '已完成', expired: '已过期', cancelled: '已取消' }[status] ||
      status ||
      '-'
    );
  }

  function repeatLabel(rule: string) {
    return (
      { none: '不重复', daily: '每天', weekly: '每周', monthly: '每月' }[rule] || rule || '-'
    );
  }

  async function loadCalendarDates(yearMonth?: string) {
    const ym = yearMonth || calendarValue.value.format('YYYY-MM');
    try {
      const res: unknown = await planApi.adminCalendar({
        yearMonth: ym,
        userId: calendarUserId.value,
      });
      if (Array.isArray(res)) {
        calendarDates.value = res.map((d) =>
          typeof d === 'string' ? d : dayjs(d as string | number | Date).format('YYYY-MM-DD'),
        );
        expiredDates.value = [];
        pendingDates.value = [];
        return;
      }
      const data = res as {
        dates?: string[];
        expiredDates?: string[];
        pendingDates?: string[];
      };
      calendarDates.value = (data?.dates || []).map((d: string) => dayjs(d).format('YYYY-MM-DD'));
      expiredDates.value = (data?.expiredDates || []).map((d: string) => dayjs(d).format('YYYY-MM-DD'));
      pendingDates.value = (data?.pendingDates || []).map((d: string) => dayjs(d).format('YYYY-MM-DD'));
    } catch {
      calendarDates.value = [];
      expiredDates.value = [];
      pendingDates.value = [];
    }
  }

  async function loadDayPlans(date?: string) {
    const d = date || calendarValue.value.format('YYYY-MM-DD');
    try {
      dayPlans.value = ((await planApi.adminPlansByDate(d, calendarUserId.value)) as HomeaiPlan[]) || [];
    } catch {
      dayPlans.value = [];
    }
  }

  function onCalendarUserChange() {
    loadCalendarDates();
    loadDayPlans();
  }

  async function loadRollLogs(pageNo = 1) {
    try {
      const res = (await planApi.rollForwardLogs({
        pageNo,
        pageSize: rollLogPagination.value.pageSize,
      })) as { records?: Record<string, unknown>[]; current?: number; total?: number };
      rollLogs.value = res?.records || [];
      rollLogPagination.value = {
        ...rollLogPagination.value,
        current: res?.current || pageNo,
        total: res?.total || 0,
      };
    } catch {
      rollLogs.value = [];
    }
  }

  function onRollLogTableChange(pagination: { current?: number }) {
    loadRollLogs(pagination.current);
  }

  function onCalendarPanelChange(date: Dayjs) {
    calendarValue.value = date;
    loadCalendarDates(date.format('YYYY-MM'));
  }

  function onCalendarSelect(date: Dayjs) {
    calendarValue.value = date;
    loadDayPlans(date.format('YYYY-MM-DD'));
  }

  /** 进入日历 Tab 时刷新全部数据 */
  function refreshCalendarTab() {
    loadUserOptions();
    loadCalendarDates();
    loadDayPlans();
    loadRollLogs();
  }

  function handleRollForwardAll() {
    createConfirm({
      iconType: 'info',
      title: '补跑重复计划实例',
      content: '将为所有重复计划补齐至配置窗口天数，是否继续？',
      onOk: async () => {
        const res = (await planApi.rollForwardRepeat()) as { created?: number };
        createMessage.success(`已新建 ${res?.created ?? 0} 条实例`);
        loadRollLogs();
        loadCalendarDates();
        loadDayPlans();
      },
    });
  }

  return {
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
    loadCalendarDates,
    loadDayPlans,
    onCalendarUserChange,
    loadUserOptions,
    loadRollLogs,
    onRollLogTableChange,
    onCalendarPanelChange,
    onCalendarSelect,
    refreshCalendarTab,
    handleRollForwardAll,
  };
}
