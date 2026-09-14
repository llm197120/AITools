/**
 * APP / H5 页内 PDF：打包 pdf.js，disableWorker 避免 file:// worker 失败
 */
import { withTimeout } from './withTimeout'

let pdfjsLoading: Promise<any> | null = null

function loadPdfJs(): Promise<any> {
  if (typeof window !== 'undefined' && (window as any).pdfjsLib) {
    return Promise.resolve((window as any).pdfjsLib)
  }
  if (!pdfjsLoading) {
    pdfjsLoading = import('pdfjs-dist/build/pdf').then((mod) => {
      const lib = (mod as any).default || mod
      if (!lib?.getDocument) throw new Error('PDF 引擎加载失败')
      lib.disableWorker = true
      if (lib.GlobalWorkerOptions) lib.GlobalWorkerOptions.workerSrc = ''
      return lib
    })
  }
  return pdfjsLoading
}

export async function renderPdfPage(data: ArrayBuffer, canvas: HTMLCanvasElement, pageNum: number) {
  const pdfjs = await loadPdfJs()
  const loadingTask = pdfjs.getDocument({ data, disableWorker: true, isEvalSupported: false })
  const pdf = await loadingTask.promise
  const total = pdf.numPages || 1
  const page = await pdf.getPage(Math.min(Math.max(1, pageNum), total))
  const unscaled = page.getViewport({ scale: 1 })
  const width = canvas.parentElement?.clientWidth || 360
  const scale = Math.max(1, width / unscaled.width)
  const viewport = page.getViewport({ scale })
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('无法绘制 PDF')
  canvas.width = viewport.width
  canvas.height = viewport.height
  await page.render({ canvasContext: ctx, viewport }).promise
  return total
}

export async function fetchPreviewBuffer(url: string, fileName = `preview-${Date.now()}.bin`): Promise<ArrayBuffer> {
  const { isCapacitorNative } = await import('../platform/runtime')
  if (isCapacitorNative()) {
    const { capacitorDownloadToTemp, capacitorReadBase64, base64ToArrayBuffer } = await import(
      '../platform/capDownload'
    )
    const path = await withTimeout(
      capacitorDownloadToTemp(url, fileName),
      90000,
      '文件下载超时',
    )
    return base64ToArrayBuffer(await capacitorReadBase64(path))
  }
  const { accessTokenHeaders } = await import('../platform/accessToken')
  const headers = accessTokenHeaders(url)
  if (typeof fetch === 'function') {
    try {
      const res = await fetch(url, headers ? { headers } : undefined)
      if (res.ok) return await res.arrayBuffer()
    } catch {
      // CORS 时回退 uni.downloadFile
    }
  }
  const temp = await new Promise<string>((resolve, reject) => {
    uni.downloadFile({
      url,
      header: headers,
      success: (res) => {
        if (res.statusCode === 200 && res.tempFilePath) resolve(res.tempFilePath)
        else reject(new Error('文件下载失败'))
      },
      fail: reject,
    })
  })
  if (typeof fetch === 'function') {
    const res = await fetch(temp)
    return await res.arrayBuffer()
  }
  return new Promise((resolve, reject) => {
    const fsm: any = uni.getFileSystemManager?.()
    if (!fsm) {
      reject(new Error('无法读取文件'))
      return
    }
    fsm.readFile({
      filePath: temp,
      success: (r: any) => resolve(r.data),
      fail: reject,
    })
  })
}

export async function fetchPdfBuffer(url: string): Promise<ArrayBuffer> {
  return fetchPreviewBuffer(url, `preview-${Date.now()}.pdf`)
}
