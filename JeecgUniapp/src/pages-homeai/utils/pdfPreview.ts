/**
 * APP / H5 页内 PDF 渲染：CDN 加载 pdf.js，disableWorker 避免 file:// worker 失败
 */
const PDFJS_CDN = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js'

let pdfjsLoading: Promise<any> | null = null

function loadPdfJs(): Promise<any> {
  const w = typeof window !== 'undefined' ? (window as any) : null
  if (w?.pdfjsLib) return Promise.resolve(w.pdfjsLib)
  if (typeof document === 'undefined') {
    return Promise.reject(new Error('当前平台不支持页内 PDF'))
  }
  if (!pdfjsLoading) {
    pdfjsLoading = new Promise((resolve, reject) => {
      const s = document.createElement('script')
      s.src = PDFJS_CDN
      s.onload = () => {
        const lib = (window as any).pdfjsLib
        if (!lib) {
          reject(new Error('PDF 引擎加载失败'))
          return
        }
        lib.disableWorker = true
        resolve(lib)
      }
      s.onerror = () => reject(new Error('PDF 引擎加载失败'))
      document.head.appendChild(s)
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

export async function fetchPdfBuffer(url: string): Promise<ArrayBuffer> {
  const { isCapacitorNative } = await import('../platform/runtime')
  if (isCapacitorNative()) {
    const { capacitorDownloadToTemp, capacitorReadBase64, base64ToArrayBuffer } = await import(
      '../platform/capDownload'
    )
    const path = await capacitorDownloadToTemp(url, `preview-${Date.now()}.pdf`)
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
        else reject(new Error('PDF 下载失败'))
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
      reject(new Error('无法读取 PDF'))
      return
    }
    fsm.readFile({
      filePath: temp,
      success: (r: any) => resolve(r.data),
      fail: reject,
    })
  })
}
