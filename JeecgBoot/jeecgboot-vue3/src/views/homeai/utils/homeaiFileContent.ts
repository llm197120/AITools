/**
 * 管理端资料原文件下载：走鉴权 /content，带控制台 JWT，不依赖 OSS 预签名。
 */
import { defHttp } from '/@/utils/http/axios';
import { downloadByData } from '/@/utils/file/download';

export function homeaiContentPath(module: 'storage' | 'learn', id: string): string {
  return module === 'storage' ? `/homeai/storage/files/${id}/content` : `/homeai/learn/materials/${id}/content`;
}

function fileNameFromDisposition(header?: string): string {
  if (!header) return '';
  const star = /filename\*=UTF-8''([^;]+)/i.exec(header);
  if (star?.[1]) {
    try {
      return decodeURIComponent(star[1]);
    } catch {
      return star[1];
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(header);
  return plain?.[1] ? plain[1] : '';
}

function denyMessage(status?: number): string {
  if (status === 401) return '登录已过期，请重新登录';
  if (status === 403) return '无权访问该文件';
  if (status === 404) return '文件不存在或已删除';
  return '下载失败';
}

export async function downloadHomeaiContent(module: 'storage' | 'learn', id: string, fileName?: string): Promise<void> {
  try {
    const res = await defHttp.get(
      { url: homeaiContentPath(module, id), responseType: 'blob', timeout: 120000 },
      { isTransformResponse: false, isReturnNativeResponse: true },
    );
    const blob: Blob | undefined = res?.data;
    if (!blob) {
      throw new Error('下载失败');
    }
    if (blob.type && blob.type.includes('application/json')) {
      const text = await blob.text();
      try {
        const json = JSON.parse(text);
        throw new Error(json.message || '下载失败');
      } catch (e) {
        if (e instanceof Error && e.message !== '下载失败' && !e.message.includes('JSON')) throw e;
        throw new Error('下载失败');
      }
    }
    const name = fileNameFromDisposition(res.headers?.['content-disposition']) || fileName || 'file';
    downloadByData(blob, name, blob.type || 'application/octet-stream');
  } catch (e: any) {
    const status = e?.response?.status;
    if (status) {
      throw new Error(denyMessage(status));
    }
    throw new Error(e?.message || '下载失败');
  }
}

/** 文本预览：带 JWT 拉原文件，避免私有 OSS 预签名 fetch 失败 */
export async function readHomeaiContentText(module: 'storage' | 'learn', id: string): Promise<string> {
  const res = await defHttp.get(
    { url: homeaiContentPath(module, id), responseType: 'blob', timeout: 60000 },
    { isTransformResponse: false, isReturnNativeResponse: true },
  );
  const blob: Blob | undefined = res?.data;
  if (!blob) throw new Error('文本加载失败');
  return await blob.text();
}
