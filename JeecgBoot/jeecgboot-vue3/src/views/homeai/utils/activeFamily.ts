/**
 * 家庭下拉过滤：status（已解散）与 delFlag（回收站）语义不同，勿混用。
 */

export function unwrapFamilyRecords(res: unknown): any[] {
  if (Array.isArray(res)) return res;
  const data = res as { records?: any[]; result?: { records?: any[] } } | null;
  return data?.records || data?.result?.records || [];
}

export function isActiveFamily(item: { status?: string; delFlag?: string | number } | null | undefined): boolean {
  if (!item) return false;
  if (Number(item.delFlag) === 1) return false;
  return item.status !== 'disbanded';
}

export function toFamilySelectOptions(res: unknown): { label: string; value: string }[] {
  return unwrapFamilyRecords(res)
    .filter(isActiveFamily)
    .filter((item) => item?.id)
    .map((item) => ({ label: item.name, value: item.id }));
}

export function toFamilyIdNameOptions(res: unknown): { id: string; name: string }[] {
  return unwrapFamilyRecords(res)
    .filter(isActiveFamily)
    .filter((item) => item?.id)
    .map((item) => ({ id: item.id, name: item.name }));
}
