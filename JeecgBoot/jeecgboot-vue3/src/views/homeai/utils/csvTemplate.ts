/**
 * 通用 CSV 模板下载（BOM 头 + Blob 链接，兼容 Excel 中文）
 */
export function downloadCsvTemplate(headers: string[], filename: string) {
  const blob = new Blob(['\uFEFF' + headers.join(',') + '\n'], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}
