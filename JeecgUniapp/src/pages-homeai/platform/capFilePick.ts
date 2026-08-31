/**
 * Capacitor 原生选文件：走 HomeaiUpdate.pickFile（ACTION_OPEN_DOCUMENT）。
 * WebView `<input type=file accept=video/*>` 会被 Android 收成相册视频，不能当系统文件选择器。
 */
import { mimeTypesForPick } from './fileAccept'
import type { PickDocumentOptions, PickedDocument } from './filePicker'
import { isCapacitorNative } from './runtime'

type HomeaiNativePlugin = {
  pickFile: (o: { mime?: string; mimeTypes?: string[]; multiple?: boolean }) => Promise<{
    files?: Array<{ path: string; name?: string; size?: number; mimeType?: string }>
  }>
}

function nativePlugin() {
  return import('@capacitor/core').then(({ registerPlugin }) =>
    registerPlugin<HomeaiNativePlugin>('HomeaiUpdate'),
  )
}

export async function pickCapacitorDocuments(
  opts?: PickDocumentOptions,
): Promise<PickedDocument[]> {
  if (!isCapacitorNative()) {
    throw new Error('NOT_CAPACITOR')
  }
  const plugin = await nativePlugin()
  const count = opts?.count ?? 1
  const ret = await plugin.pickFile({
    mimeTypes: mimeTypesForPick(opts),
    multiple: count > 1,
  })
  return (ret.files || [])
    .filter((f) => f?.path)
    .slice(0, count)
    .map((f) => ({
      path: f.path,
      name: f.name,
      size: f.size,
      mimeType: f.mimeType,
    }))
}
