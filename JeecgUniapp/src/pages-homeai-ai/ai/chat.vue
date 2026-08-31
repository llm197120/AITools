<route lang="json5">
{
  style: {
    navigationBarTitleText: 'AI对话',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view class="chat-page">
    <!-- 消息列表 -->
    <scroll-view class="message-list" scroll-y :scroll-into-view="scrollToId" scroll-with-animation>
      <view v-if="msgFailed && messages.length === 0 && !isStreaming" class="empty-chat">
        <HomeEmpty
          icon-name="chat"
          title="消息加载失败"
          hint="请检查网络后重试"
          action-text="重试"
          :card="false"
          @action="loadMessages"
        />
      </view>
      <view v-else-if="messages.length === 0 && !isStreaming" class="empty-chat">
        <HomeEmpty
          icon-name="chat"
          title="开始对话"
          hint="点下方示例填入，或直接输入问题"
          :card="false"
        >
          <template #actions>
            <view class="empty-actions">
              <view class="example-topic hai-press" @click="fillExample('帮我写一份食谱')">帮我写一份食谱</view>
              <view class="example-topic hai-press" @click="fillExample('今天晚餐推荐')">今天晚餐推荐</view>
              <view class="example-topic hai-press" @click="fillExample('帮我解释一下什么是量子计算')">帮我解释一下量子计算</view>
            </view>
          </template>
        </HomeEmpty>
      </view>
      <view class="message-item" v-for="(msg, i) in messages" :key="msg.id || i"
        :id="'msg-' + i"
        :class="msg.role === 'user' ? 'user-msg' : 'assistant-msg'">
        <view v-if="msg.role === 'assistant'" class="ai-avatar">
          <text class="ai-avatar-text">AI</text>
        </view>
        <view class="bubble" :class="msg.role">
          <!-- 图片附件 -->
          <image v-if="msg.contentType === 'image' && msg.fileUrl" class="msg-image" :src="msg.fileUrl" mode="widthFix" />
          <!-- 文本内容（Markdown） -->
          <mp-html v-if="msg.content && msg.role === 'assistant'" class="msg-text" :content="msg.content" selectable />
          <text v-else-if="msg.content" class="msg-text" selectable>{{ msg.content }}</text>
          <!-- 流式加载动画 -->
          <view class="streaming-dots" v-if="msg.role === 'assistant' && msg.isStreaming">
            <text class="dot">.</text><text class="dot">.</text><text class="dot">.</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 停止生成按钮 -->
    <view class="stop-bar" v-if="isStreaming">
      <wd-button size="small" round @click="stopGeneration">⏹ 停止生成</wd-button>
    </view>

    <!-- 重连提示 -->
    <view class="reconnect-bar" v-if="showReconnect">
      <text>连接中断，</text>
      <text class="reconnect-link" @click="resendLastMessage">点击继续</text>
    </view>

    <view v-if="quotaHint" class="quota-bar">{{ quotaHint }}</view>
    <!-- 输入区域 -->
    <view class="input-area">
      <view class="preview-images" v-if="selectedImages.length > 0">
        <view class="preview-item" v-for="(img, i) in selectedImages" :key="i">
          <image :src="img" mode="aspectFill" />
          <wd-icon name="close" size="14px" color="#fff" class="img-remove" @click="removeImage(i)"></wd-icon>
        </view>
      </view>
      <view class="preview-files" v-if="selectedFiles.length > 0">
        <view class="file-item" v-for="(f, i) in selectedFiles" :key="i">
          <wd-icon name="file" size="16px" color="#666"></wd-icon>
          <text class="file-name">{{ f.name }}</text>
          <wd-icon name="close" size="14px" color="#999" class="file-remove" @click="removeFile(i)"></wd-icon>
        </view>
      </view>
      <view class="input-row">
        <wd-icon name="attachment" size="22px" color="#8A857C" class="attach-btn" @click="showAttachmentPicker"></wd-icon>
        <input class="text-input" v-model="inputText" type="text" placeholder="输入消息..."
          :disabled="quotaExhausted" confirm-type="send" @confirm="sendMessage" @blur="refreshQuota" />
        <wd-button v-if="!quotaExhausted" size="medium" type="primary" :disabled="!canSend || isStreaming"
          @click="sendMessage">发送</wd-button>
        <wd-button v-else size="medium" disabled>已用完</wd-button>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { computed, ref, nextTick } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { get as getApi, post as postApi, del as delApi, getServerBaseUrl } from '../../pages-homeai/api/request'
import { validateUploadFile } from '../../pages-homeai/utils/fileWhitelist'
import { useHomeaiFilePick } from '../../pages-homeai/utils/useHomeaiFilePick'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { consumeHomeaiUnauthorized } from '../../pages-homeai/utils/homeaiAuth'
import HomeEmpty from '../../components/HomeEmpty.vue'

useHomeaiPageGuard()

const { preload: preloadFilePick, showPickMenu } = useHomeaiFilePick()

const conversationId = ref('')
const hasQueryTitle = ref(false)
const messages = ref<any[]>([])
const inputText = ref('')
const isStreaming = ref(false)
const scrollToId = ref('')
const quotaExhausted = ref(false)
const quotaHint = ref('')
const showReconnect = ref(false)

const selectedImages = ref<string[]>([])
const selectedFiles = ref<any[]>([])
const msgFailed = ref(false)
const canSend = computed(() =>
  !!inputText.value.trim() || selectedImages.value.length > 0 || selectedFiles.value.length > 0,
)

function applyNavTitle(raw?: string) {
  const clean = String(raw || '').replace(/[\n\r]+/g, ' ').trim()
  if (!clean || clean === '新对话' || clean === '[图片]' || clean === '[附件]') return
  const title = clean.length > 16 ? `${clean.slice(0, 16)}…` : clean
  uni.setNavigationBarTitle({ title })
}

onLoad(async (options: any) => {
  conversationId.value = options?.id || ''
  if (options?.title) {
    hasQueryTitle.value = true
    try {
      applyNavTitle(decodeURIComponent(options.title))
    } catch {
      applyNavTitle(options.title)
    }
  }
  if (conversationId.value) {
    await loadMessages()
  }
  // 如果有初始消息
  if (options?.initial) {
    inputText.value = decodeURIComponent(options.initial)
    sendMessage()
  }
})

const UNFINISHED_KEY = 'homeai_chat_unfinished'

onShow(() => {
  preloadFilePick()
  refreshQuota()
  const unfinished = uni.getStorageSync(UNFINISHED_KEY)
  if (unfinished && unfinished === conversationId.value) {
    showReconnect.value = true
    uni.removeStorageSync(UNFINISHED_KEY)
  }
})

function fillExample(text: string) {
  inputText.value = text
}

async function refreshQuota() {
  try {
    const text = inputText.value.trim()
    const quota: any = await getApi('/ai/quota/precheck', {
      scene: 'chat',
      ...(text ? { text } : {}),
    })
    if (!quota) return
    const daily = quota.remainingDaily
    const monthly = quota.remainingMonthly
    quotaHint.value = `今日剩余 ${daily ?? '-'} Token · 本月剩余 ${monthly ?? '-'}`
    quotaExhausted.value = quota.allowed === false
    if (quotaExhausted.value && quota.message) {
      quotaHint.value = quota.message
    }
  } catch {
    if (!quotaHint.value) quotaHint.value = '额度查询失败，提交时将再校验'
  }
}

async function loadMessages() {
  if (!conversationId.value) return
  try {
    messages.value = await getApi(`/ai/conversations/${conversationId.value}/messages`)
    msgFailed.value = false
    if (!hasQueryTitle.value) {
      const firstUser = messages.value.find((m: any) => m.role === 'user' && m.content)
      if (firstUser) applyNavTitle(firstUser.content)
    }
  } catch {
    msgFailed.value = messages.value.length === 0
    if (!msgFailed.value) uni.showToast({ title: '消息刷新失败', icon: 'none' })
  }
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    scrollToId.value = 'msg-' + (messages.value.length - 1)
  })
}

/** 将 SSE chunk 转为字符串 */
function chunkToString(chunk: unknown): string {
  if (!chunk) return ''
  if (typeof chunk === 'string') return chunk
  if (chunk instanceof ArrayBuffer) {
    return new TextDecoder('utf-8').decode(chunk)
  }
  if (ArrayBuffer.isView(chunk)) {
    return new TextDecoder('utf-8').decode(chunk)
  }
  return String(chunk)
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** 解析单条 SSE JSON 事件 */
function tryParseSseJson(raw: string, aiMsgIdx: number) {
  const text = raw.trim()
  if (!text || text === '[DONE]') return
  try {
    appendAiragEvent(JSON.parse(text), aiMsgIdx)
  } catch (_) {
    // 非完整 JSON，忽略
  }
}

/** 解析 airag EventData 格式，累加 AI 回复内容 */
function appendAiragEvent(event: any, aiMsgIdx: number) {
  if (!event || aiMsgIdx < 0 || aiMsgIdx >= messages.value.length) return
  const type = event.event || event.eventType
  if (type === 'MESSAGE' || type === 'THINKING' || type === 'THINKING_END') {
    const piece = event.data?.message
    if (piece) {
      messages.value[aiMsgIdx].content += piece
    }
  } else if (type === 'ERROR' || type === 'FLOW_ERROR') {
    const errMsg = event.data?.message || '请求出错，请稍后重试'
    if (!messages.value[aiMsgIdx].content) {
      messages.value[aiMsgIdx].content = errMsg
    }
  }
  if (event.conversationId) {
    conversationId.value = event.conversationId
  }
}

/** 按 SSE 协议解析缓冲区，返回未处理完的剩余内容 */
function consumeSseBuffer(buffer: string, aiMsgIdx: number): string {
  const normalized = buffer.replace(/\r\n/g, '\n')
  const parts = normalized.split('\n\n')
  const remaining = parts.pop() || ''

  for (const part of parts) {
    if (!part.trim()) continue
    for (const line of part.split('\n')) {
      const trimmed = line.trim()
      if (!trimmed) continue
      if (trimmed.startsWith('data:')) {
        tryParseSseJson(trimmed.substring(5), aiMsgIdx)
      } else if (trimmed.startsWith('{')) {
        tryParseSseJson(trimmed, aiMsgIdx)
      }
    }
  }

  // 保留可能未以空行结尾的最后一行
  const lines = remaining.split('\n')
  const kept: string[] = []
  for (const line of lines) {
    const trimmed = line.trim()
    if (trimmed.startsWith('data:')) {
      const payload = trimmed.substring(5).trim()
      if (payload.startsWith('{') && payload.endsWith('}')) {
        tryParseSseJson(payload, aiMsgIdx)
        continue
      }
    } else if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      tryParseSseJson(trimmed, aiMsgIdx)
      continue
    }
    kept.push(line)
  }
  return kept.join('\n')
}

/** SSE 未解析到内容时，从服务端拉取最新 assistant 消息 */
async function reloadAssistantFromServer(aiMsgIdx: number): Promise<boolean> {
  if (!conversationId.value) return false
  for (let i = 0; i < 8; i++) {
    await sleep(400)
    try {
      const list = (await getApi(`/ai/conversations/${conversationId.value}/messages`)) || []
      const assistants = list.filter((m: any) => m.role === 'assistant')
      const last = assistants[assistants.length - 1]
      if (last?.content) {
        messages.value[aiMsgIdx].content = last.content
        messages.value[aiMsgIdx].id = last.id
        return true
      }
    } catch (_) {
      // 继续重试
    }
  }
  return false
}

function encodeChatForm(data: Record<string, unknown>): string {
  const parts: string[] = []
  const append = (key: string, value: unknown) => {
    if (value == null || value === '') return
    parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
  }
  append('conversationId', data.conversationId)
  append('content', data.content)
  const images = data.images
  if (Array.isArray(images)) images.forEach((u) => append('images', u))
  const files = data.files
  if (Array.isArray(files)) files.forEach((u) => append('files', u))
  return parts.join('&')
}

function supportsFetchStream(): boolean {
  // #ifdef MP-WEIXIN
  return false
  // #endif
  try {
    return typeof fetch === 'function' && typeof ReadableStream !== 'undefined'
  } catch {
    return false
  }
}

/**
 * SSE 降级策略：
 * 1. H5 / Capacitor 用 fetch + ReadableStream 读 SSE，边到边出字；
 * 2. 小程序优先 RequestTask.onChunkReceived；
 * 3. 都不支持则 enableChunked=false，依赖 success 整包解析。
 */
function supportsChunkedTransfer(): boolean {
  try {
    if (typeof uni !== 'undefined' && typeof (uni as any).canIUse === 'function') {
      if ((uni as any).canIUse('RequestTask.onChunkReceived')) return true
    }
  } catch (_) {
    // ignore
  }
  try {
    // #ifdef MP-WEIXIN
    if (typeof wx !== 'undefined' && typeof wx.canIUse === 'function') {
      if (wx.canIUse('RequestTask.onChunkReceived')) return true
    }
    // #endif
  } catch (_) {
    // ignore
  }
  // H5 / App 等多数端对 enableChunked 支持不稳定，默认走非流式整包
  return false
}

function isChunkedUnsupportedError(err: any): boolean {
  const msg = String(err?.errMsg || err?.message || err || '').toLowerCase()
  return (
    msg.includes('chunk') ||
    msg.includes('enablechunked') ||
    msg.includes('onchunkreceived') ||
    msg.includes('not support') ||
    msg.includes('不支持')
  )
}

function finishAssistantMessage(aiMsgIdx: number) {
  if (aiMsgIdx >= 0 && aiMsgIdx < messages.value.length) {
    messages.value[aiMsgIdx].isStreaming = false
  }
  isStreaming.value = false
  refreshQuota()
}

async function handleChatSuccess(
  res: any,
  aiMsgIdx: number,
  sseBufferRef: { value: string },
  usedChunked: boolean,
) {
  const statusCode = res?.statusCode
  if (consumeHomeaiUnauthorized(statusCode, res?.data)) {
    finishAssistantMessage(aiMsgIdx)
    messages.value[aiMsgIdx].content = '登录已过期，请重新登录'
    return
  }
  if (statusCode && statusCode >= 400) {
    finishAssistantMessage(aiMsgIdx)
    messages.value[aiMsgIdx].content = '请求失败，请检查登录状态或稍后重试'
    return
  }
  // 流式已拼过内容时，success 整包再解析会导致重复；只做收尾
  const alreadyHasContent = !!messages.value[aiMsgIdx]?.content
  if (!(usedChunked && alreadyHasContent)) {
    const body = chunkToString(res?.data)
    if (body) {
      // 若返回的是普通 JSON 错误而非 SSE
      if (body.trim().startsWith('{') && body.includes('"success"')) {
        try {
          const json = JSON.parse(body)
          if (!json.success) {
            messages.value[aiMsgIdx].content = json.message || '请求失败'
            finishAssistantMessage(aiMsgIdx)
            return
          }
        } catch (_) {
          // 继续按 SSE 解析
        }
      }
      sseBufferRef.value += body
    }
    sseBufferRef.value = consumeSseBuffer(sseBufferRef.value + '\n\n', aiMsgIdx)
    sseBufferRef.value = ''
  }
  finishAssistantMessage(aiMsgIdx)
  if (!messages.value[aiMsgIdx].content) {
    const loaded = await reloadAssistantFromServer(aiMsgIdx)
    if (!loaded) {
      messages.value[aiMsgIdx].content = '未收到 AI 回复，请检查 AI 密钥配置或稍后重试'
    }
  }
  scrollToBottom()
}

// 停止生成用的请求句柄（需在 startChatRequest 之前声明，供降级重试更新）
const currentTask = ref<any>(null)

function startFetchChatRequest(
  requestData: Record<string, unknown>,
  token: string,
  aiMsgIdx: number,
) {
  const sseBufferRef = { value: '' }
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), 180000)

  fetch(getAppBaseUrl() + '/homeai/ai/chat/send', {
    method: 'POST',
    headers: {
      'X-Access-Token': token,
      'Content-Type': 'application/x-www-form-urlencoded',
      Accept: 'text/event-stream',
    },
    body: encodeChatForm(requestData),
    signal: controller.signal,
  })
    .then(async (res) => {
      if (res.status === 401 || !res.ok) {
        const peek = await res.text().catch(() => '')
        if (consumeHomeaiUnauthorized(res.status, peek || undefined)) {
          finishAssistantMessage(aiMsgIdx)
          messages.value[aiMsgIdx].content = '登录已过期，请重新登录'
          return
        }
        if (!res.ok) {
          finishAssistantMessage(aiMsgIdx)
          messages.value[aiMsgIdx].content = '请求失败，请检查登录状态或稍后重试'
          return
        }
      }
      const reader = res.body?.getReader()
      if (!reader) {
        const body = await res.text().catch(() => '')
        await handleChatSuccess({ statusCode: res.status, data: body }, aiMsgIdx, sseBufferRef, false)
        return
      }
      const decoder = new TextDecoder()
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const chunk = decoder.decode(value, { stream: true })
        if (!chunk) continue
        sseBufferRef.value += chunk
        sseBufferRef.value = consumeSseBuffer(sseBufferRef.value, aiMsgIdx)
        scrollToBottom()
      }
      sseBufferRef.value = consumeSseBuffer(sseBufferRef.value + '\n\n', aiMsgIdx)
      finishAssistantMessage(aiMsgIdx)
      if (!messages.value[aiMsgIdx].content) {
        const loaded = await reloadAssistantFromServer(aiMsgIdx)
        if (!loaded) {
          messages.value[aiMsgIdx].content = '未收到 AI 回复，请检查 AI 密钥配置或稍后重试'
        }
      }
      scrollToBottom()
    })
    .catch((err) => {
      if (controller.signal.aborted) {
        finishAssistantMessage(aiMsgIdx)
        return
      }
      console.error('请求失败', err)
      showReconnect.value = true
      finishAssistantMessage(aiMsgIdx)
    })
    .finally(() => {
      clearTimeout(timeoutId)
    })

  return {
    abort() {
      controller.abort()
    },
  }
}

/** 发起一次聊天请求；allowRetryNonChunked 为 true 时，流式失败可降级重试一次 */
function startChatRequest(
  requestData: Record<string, unknown>,
  token: string,
  aiMsgIdx: number,
  enableChunked: boolean,
  allowRetryNonChunked: boolean,
) {
  if (supportsFetchStream()) {
    return startFetchChatRequest(requestData, token, aiMsgIdx)
  }
  const sseBufferRef = { value: '' }
  let retriedNonChunked = false

  const requestTask = uni.request({
    url: getAppBaseUrl() + '/homeai/ai/chat/send',
    method: 'POST',
    header: {
      'X-Access-Token': token,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: requestData,
    timeout: 180000,
    enableChunked,
    responseType: 'text',
    ...(enableChunked
      ? {
          onChunkReceived: (res: any) => {
            try {
              const chunk = chunkToString(res?.data)
              if (!chunk) return
              sseBufferRef.value += chunk
              sseBufferRef.value = consumeSseBuffer(sseBufferRef.value, aiMsgIdx)
              scrollToBottom()
            } catch (_) {
              // 忽略单次解析错误
            }
          },
        }
      : {}),
    success: async (res) => {
      await handleChatSuccess(res, aiMsgIdx, sseBufferRef, enableChunked)
    },
    fail: (err) => {
      console.error('请求失败', err)
      // 流式失败且像不支持 chunked：清空占位内容后同一 aiMsgIdx 重试非流式，不新增消息
      if (
        enableChunked &&
        allowRetryNonChunked &&
        !retriedNonChunked &&
        isChunkedUnsupportedError(err)
      ) {
        retriedNonChunked = true
        if (messages.value[aiMsgIdx]) {
          messages.value[aiMsgIdx].content = ''
          messages.value[aiMsgIdx].isStreaming = true
        }
        isStreaming.value = true
        const fallbackTask = startChatRequest(requestData, token, aiMsgIdx, false, false)
        currentTask.value = fallbackTask
        return
      }
      showReconnect.value = true
      finishAssistantMessage(aiMsgIdx)
    },
  })

  return requestTask
}

async function sendMessage() {
  const content = inputText.value.trim()
    || (selectedImages.value.length ? '[图片]' : '')
    || (selectedFiles.value.length ? '[附件]' : '')
  if (!content) return
  const isFirstMessage = messages.value.length === 0

  // Token 预检（R25：按场景 + 文本长度估算）
  const quota = await getApi('/ai/quota/precheck', { scene: 'chat', text: content })
  if (!quota.allowed) {
    uni.showToast({ title: quota.message || 'Token 不足', icon: 'none' })
    quotaExhausted.value = true
    if (quota.message) quotaHint.value = quota.message
    return
  }

  // 添加用户消息到列表
  messages.value.push({
    id: 'temp-' + Date.now(),
    role: 'user',
    content: content,
    contentType: 'text',
  })

  // 添加占位AI消息
  const aiMsgIdx = messages.value.length
  messages.value.push({
    id: 'temp-ai-' + Date.now(),
    role: 'assistant',
    content: '',
    contentType: 'text',
    isStreaming: true,
  })

  inputText.value = ''
  isStreaming.value = true
  showReconnect.value = false
  if (isFirstMessage) applyNavTitle(content)
  scrollToBottom()

  try {
    const token = uni.getStorageSync('homeai_token')

    // 本地图片先上传，换取服务器可访问地址
    const imageUrls: string[] = []
    for (const img of selectedImages.value) {
      if (!img) continue
      if (img.startsWith('http')) {
        imageUrls.push(img)
        continue
      }
      if (!(await validateUploadFile(img))) continue
      try {
        const up = await uni.uploadFile({
          url: getAppBaseUrl() + '/homeai/ai/chat/upload',
          filePath: img,
          name: 'file',
          header: { 'X-Access-Token': token },
          timeout: 120000,
        })
        if (consumeHomeaiUnauthorized(up.statusCode, up.data)) {
          inputText.value = content
          messages.value.splice(messages.value.length - 2, 2)
          isStreaming.value = false
          return
        }
        const d = JSON.parse(up.data)
        if (d && d.result) {
          const stored = d.result.storedUrl || d.result.url
          if (stored) imageUrls.push(stored)
        }
      } catch (e) {
        // 图片上传失败：明确提示并中止发送，避免消息"少图"静默发出
        uni.showToast({ title: '图片上传失败，请重试', icon: 'none' })
        inputText.value = content
        messages.value.splice(messages.value.length - 2, 2)
        isStreaming.value = false
        return
      }
    }

    const requestData: Record<string, unknown> = {
      conversationId: conversationId.value,
      content: content,
    }
    if (imageUrls.length > 0) {
      requestData.images = imageUrls
    }
    const fileUrls = selectedFiles.value.map((f: any) => f.url).filter((u: string) => u && u !== 'undefined')
    if (fileUrls.length > 0) {
      requestData.files = fileUrls
    }

    // 按端能力决定是否启用 chunked；不支持时走 success 整包解析
    const useChunked = supportsChunkedTransfer()
    const requestTask = startChatRequest(requestData, token, aiMsgIdx, useChunked, useChunked)
    currentTask.value = requestTask
  } catch (e) {
    console.error('发送消息失败', e)
    finishAssistantMessage(aiMsgIdx)
  }

  selectedImages.value = []
  selectedFiles.value = []
}

function abortLocalStream(opts?: { discardEmpty?: boolean }) {
  if (currentTask.value) {
    try {
      currentTask.value.abort()
    } catch {
      /* ignore */
    }
    currentTask.value = null
  }
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant' && last.isStreaming) {
    if (opts?.discardEmpty && !String(last.content || '').trim()) {
      messages.value.pop()
    } else {
      last.isStreaming = false
    }
  }
  isStreaming.value = false
}

function notifyServerStop() {
  if (!conversationId.value) return
  postApi('/ai/chat/stop', { params: { conversationId: conversationId.value } }).catch(() => {})
}

function stopOnLeave() {
  if (!isStreaming.value && !currentTask.value) return
  if (conversationId.value) {
    uni.setStorageSync(UNFINISHED_KEY, conversationId.value)
  }
  notifyServerStop()
  abortLocalStream()
}

onUnload(() => {
  stopOnLeave()
})

async function stopGeneration() {
  try {
    await postApi('/ai/chat/stop', { params: { conversationId: conversationId.value } })
  } catch (e) { /* ignore */ }
  abortLocalStream({ discardEmpty: true })
  uni.removeStorageSync(UNFINISHED_KEY)
}

function resendLastMessage() {
  const lastMsg = messages.value[messages.value.length - 1]
  const isLastStreaming = !!lastMsg?.isStreaming
  const lastUserMsg = [...messages.value].reverse().find((m) => m.role === 'user')
  if (!lastUserMsg) return
  inputText.value = lastUserMsg.content
  if (isLastStreaming) {
    // 仅当最后一条是未完成的 AI 占位时，移除其与对应的用户消息后重发，避免误删已完成回复
    const userIdx = messages.value.lastIndexOf(lastUserMsg)
    messages.value = messages.value.slice(0, userIdx)
  }
  sendMessage()
}

// 附件选择（R25：统一 useHomeaiFilePick）
function showAttachmentPicker() {
  showPickMenu(
    async (files, source) => {
      if (source === 'file') {
        for (const file of files) {
          if (!(await validateUploadFile(file.path, file.name))) continue
          try {
            const token = uni.getStorageSync('homeai_token')
            const uploadRes = await uni.uploadFile({
              url: getAppBaseUrl() + '/homeai/ai/chat/upload',
              filePath: file.path,
              name: 'file',
              header: { 'X-Access-Token': token },
              timeout: 120000,
            })
            if (consumeHomeaiUnauthorized(uploadRes.statusCode, uploadRes.data)) {
              return
            }
            const data = JSON.parse(uploadRes.data)
            const url = data && data.result ? data.result.storedUrl || data.result.url : ''
            if (url) {
              selectedFiles.value.push({ name: file.name, url, size: file.size })
            } else {
              uni.showToast({ title: '文件上传失败', icon: 'none' })
            }
          } catch (e) {
            console.error('文件上传失败', e)
            uni.showToast({ title: '文件上传失败', icon: 'none' })
          }
        }
      } else {
        selectedImages.value.push(...files.map((f) => f.path))
      }
    },
    { allowFile: true, imageCount: 9, fileCount: 5 },
  )
}

function removeImage(index: number) {
  selectedImages.value.splice(index, 1)
}

function removeFile(index: number) {
  selectedFiles.value.splice(index, 1)
}

function getAppBaseUrl(): string {
  return getServerBaseUrl()
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--hai-bg);
}
.message-list {
  flex: 1;
  padding: 20rpx;
}
.message-item {
  display: flex;
  margin-bottom: 24rpx;
  gap: 16rpx;
}
.user-msg {
  justify-content: flex-end;
}
.assistant-msg {
  justify-content: flex-start;
}
.ai-avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--hai-primary), var(--hai-primary-mid));
  display: flex;
  align-items: center;
  justify-content: center;
}
.ai-avatar-text {
  font-size: 22rpx;
  font-weight: 600;
  color: #fff;
  line-height: 1;
}
.bubble {
  max-width: 70%;
  padding: 20rpx 24rpx;
  border-radius: var(--hai-radius-sm);
  font-size: 28rpx;
  line-height: 1.6;
  word-break: break-word;
}
.bubble.user {
  background: var(--hai-primary);
  color: #fff;
  border-bottom-right-radius: 4rpx;
}
.bubble.assistant {
  background: var(--hai-card);
  color: var(--hai-text);
  border: 1rpx solid var(--hai-border);
  border-bottom-left-radius: 4rpx;
  box-shadow: var(--hai-shadow);
}
.msg-image {
  width: 200rpx;
  border-radius: 8rpx;
  margin-bottom: 8rpx;
}
.msg-text {
  white-space: pre-wrap;
}
.streaming-dots {
  display: inline-flex;
  gap: 4rpx;
}
.dot {
  animation: blink 1.4s infinite;
  font-size: 40rpx;
  line-height: 1;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 20% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}
.stop-bar, .reconnect-bar {
  text-align: center;
  padding: 12rpx;
}
.reconnect-link {
  color: var(--hai-primary);
  text-decoration: underline;
}
.input-area {
  padding: 16rpx 20rpx;
  background: var(--hai-card);
  border-top: 1rpx solid var(--hai-border);
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}
.preview-images {
  display: flex;
  gap: 12rpx;
  padding: 12rpx 0;
  overflow-x: auto;
}
.preview-item {
  position: relative;
  width: 120rpx;
  height: 120rpx;
}
.preview-item image {
  width: 100%;
  height: 100%;
  border-radius: 8rpx;
}
.img-remove {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  background: rgba(0,0,0,0.5);
  border-radius: 50%;
  padding: 4rpx;
}
.preview-files {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  padding: 12rpx 0;
}
.file-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  background: var(--hai-bg);
  border-radius: 8rpx;
  max-width: 100%;
}
.file-name {
  font-size: 24rpx;
  color: var(--hai-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200rpx;
}
.file-remove {
  flex-shrink: 0;
}
.input-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.attach-btn {
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.text-input {
  flex: 1;
  min-height: 80rpx;
  height: 80rpx;
  padding: 0 24rpx;
  background: var(--hai-bg);
  border-radius: 40rpx;
  font-size: 28rpx;
  color: var(--hai-text);
}
.empty-chat {
  padding: 48rpx 16rpx 24rpx;
}
.empty-actions {
  padding-top: 28rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.example-topic {
  padding: 24rpx 30rpx;
  background: var(--hai-card);
  border-radius: var(--hai-radius-md);
  font-size: 26rpx;
  color: var(--hai-text);
  box-shadow: var(--hai-shadow);
  text-align: left;
}
.quota-bar {
  font-size: 22rpx;
  color: var(--hai-text-muted);
  text-align: center;
  padding: 10rpx 20rpx;
  background: var(--hai-card);
  border-top: 1rpx solid var(--hai-border);
}
</style>
