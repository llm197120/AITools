import type { FormSchema } from '/@/components/Form';

export const HOMEAI_FORM_MODAL_WIDTH = {
  short: 520,
  medium: 720,
  long: 880,
  wide: 800,
} as const;

export type HomeaiFormModalSize = keyof typeof HOMEAI_FORM_MODAL_WIDTH;

export const COL_HALF = { span: 12 };
export const COL_FULL = { span: 24 };

export function omitHomeaiFormMeta<T extends Record<string, any>>(values: T): T {
  const out = { ...values };
  Object.keys(out).forEach((k) => {
    if (k.startsWith('_g_')) delete out[k];
  });
  return out;
}

export function homeaiGroup(label: string, field: string): FormSchema {
  return {
    field,
    label,
    component: 'Divider',
    colProps: COL_FULL,
    componentProps: { orientation: 'left', plain: true },
  };
}
