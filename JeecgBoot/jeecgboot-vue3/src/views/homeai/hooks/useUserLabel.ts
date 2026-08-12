/**
 * userId → 昵称解析（来自 userApi.options）
 * 解析失败时回退显示原始 userId
 */
import { ref, computed } from 'vue';
import { userApi } from '/@/api/homeai';

type UserOptionItem = { label?: string; value?: string; nickname?: string; id?: string };

/** 模块级缓存，避免多页面重复请求 */
let cachedMap: Map<string, string> | null = null;
let loadingPromise: Promise<void> | null = null;

export function useUserLabel() {
  const userLabelMap = ref<Map<string, string>>(cachedMap ?? new Map());

  const userOptions = computed(() =>
    Array.from(userLabelMap.value.entries()).map(([value, label]) => ({ label, value })),
  );

  async function loadUserOptions(force = false) {
    if (!force && cachedMap && cachedMap.size > 0) {
      userLabelMap.value = cachedMap;
      return;
    }
    if (!force && loadingPromise) {
      await loadingPromise;
      userLabelMap.value = cachedMap ?? new Map();
      return;
    }
    loadingPromise = (async () => {
      try {
        const list = ((await userApi.options()) as UserOptionItem[]) || [];
        const map = new Map<string, string>();
        for (const u of list) {
          const id = String(u.value ?? u.id ?? '');
          if (!id) continue;
          map.set(id, u.label || u.nickname || id);
        }
        cachedMap = map;
        userLabelMap.value = map;
      } catch {
        cachedMap = new Map();
        userLabelMap.value = new Map();
      } finally {
        loadingPromise = null;
      }
    })();
    await loadingPromise;
  }

  function resolveUserLabel(userId?: string | null): string {
    if (!userId) return '-';
    const id = String(userId);
    return userLabelMap.value.get(id) || id;
  }

  return { userLabelMap, userOptions, loadUserOptions, resolveUserLabel };
}
