import { ref } from 'vue';

/**
 * 主列表加载失败与空表区分：失败时横幅 + 重试，避免运营误以为没数据。
 */
export function useHomeaiListLoad() {
  const listFailed = ref(false);

  function wrapListApi<T>(fn: (params: T) => Promise<unknown>) {
    return async (params: T) => {
      try {
        const res = await fn(params);
        listFailed.value = false;
        return res;
      } catch (e) {
        listFailed.value = true;
        throw e;
      }
    };
  }

  return { listFailed, wrapListApi };
}
