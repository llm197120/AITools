<route lang="json5">
{
  style: {
    navigationBarTitleText: 'AI对话',
  },
}
</route>

<template>
  <view class="chat-page">
    <!-- 消息列表 -->
    <scroll-view class="message-list" scroll-y :scroll-into-view="scrollToId" scroll-with-animation>
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
        <wd-icon name="attachment" size="22px" color="#999" class="attach-btn" @click="showAttachmentPicker"></wd-icon>
        <input class="text-input" v-model="inputText" type="text" placeholder="输入消息..."
          :disabled="quotaExhausted" confirm-type="send" @confirm="sendMessage" />
        <wd-button v-if="!quotaExhausted" size="small" :disabled="!inputText.trim() || isStreaming"
          @click="sendMessage">发送</wd-button>
        <wd-button v-else size="small" disabled>已用完</wd-button>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref, nextTick } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { get as getApi, post as postApi, del as delApi, getServerBaseUrl } from '../../pages-homeai/api/request'
import { preloadWhitelist, validateUploadFile } from '../../pages-homeai/utils/fileWhitelist'

const conversationId = ref('')
const messages = ref<any[]>([])
const inputText = ref('')
const isStreaming = ref(false)
const scrollToId = ref('')
const quotaExhausted = ref(false)
const showReconnect = ref(false)

const selectedImages = ref<string[]>([])
const selectedFiles = ref<any[]>([])

onLoad(async (options: any) => {
  conversationId.value = options?.id || ''
  if (conversationId.value) {
    await loadMessages()
  }
  // 如果有初始消息
  if (options?.initial) {
    inputText.value = decodeURIComponent(options.initial)
    sendMessage()
  }
})

onShow(() => {
  preloadWhitelist()
})

async function loadMessages() {
  messages.value = await getApi(`/ai/conversations/${conversationId.value}/messages`)
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

async function sendMessage() {
  const content = inputText.value.trim()
  if (!content) return

  // Token 预检
  const quota = await getApi('/ai/chat/quota')
  if (!quota.allowed) {
    uni.showToast({ title: quota.message || 'Token 不足', icon: 'none' })
    quotaExhausted.value = true
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
  scrollToBottom()

  try {
    // 使用 uni.request enableChunked 接收 SSE 流式响应
    const token = uni.getStorageSync('homeai_token')
    let sseBuffer = '' // SSE 行缓冲区，处理跨 chunk 断行

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
        })
        const d = JSON.parse(up.data)
        if (d && d.result) {
          const stored = d.result.storedUrl || d.result.url
          if (stored) imageUrls.push(stored)
        }
      } catch (e) {
        console.error('图片上传失败', e)
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

    const requestTask = uni.request({
      url: getAppBaseUrl() + '/homeai/ai/chat/send',
      method: 'POST',
      header: {
        'X-Access-Token': token,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: requestData,
      enableChunked: true,
      responseType: 'text',
      // 实时流式接收
      onChunkReceived: (res) => {
        try {
          const chunk = chunkToString((res as any).data)
          if (!chunk) return
          sseBuffer += chunk
          sseBuffer = consumeSseBuffer(sseBuffer, aiMsgIdx)
          scrollToBottom()
        } catch (_) {
          // 忽略单次解析错误
        }
      },
      success: async (res) => {
        const statusCode = (res as any).statusCode
        if (statusCode && statusCode >= 400) {
          messages.value[aiMsgIdx].isStreaming = false
          isStreaming.value = false
          messages.value[aiMsgIdx].content = '请求失败，请检查登录状态或稍后重试'
          return
        }
        const body = chunkToString((res as any).data)
        if (body) {
          // 若返回的是普通 JSON 错误而非 SSE
          if (body.trim().startsWith('{') && body.includes('"success"')) {
            try {
              const json = JSON.parse(body)
              if (!json.success) {
                messages.value[aiMsgIdx].content = json.message || '请求失败'
                messages.value[aiMsgIdx].isStreaming = false
                isStreaming.value = false
                return
              }
            } catch (_) {
              // 继续按 SSE 解析
            }
          }
          sseBuffer += body
        }
        sseBuffer = consumeSseBuffer(sseBuffer + '\n\n', aiMsgIdx)
        sseBuffer = ''
        messages.value[aiMsgIdx].isStreaming = false
        isStreaming.value = false
        if (!messages.value[aiMsgIdx].content) {
          const loaded = await reloadAssistantFromServer(aiMsgIdx)
          if (!loaded) {
            messages.value[aiMsgIdx].content = '未收到 AI 回复，请检查 AI 密钥配置或稍后重试'
          }
        }
        scrollToBottom()
      },
      fail: (err) => {
        console.error('请求失败', err)
        showReconnect.value = true
        messages.value[aiMsgIdx].isStreaming = false
        isStreaming.value = false
      },
    })

    // 暂存 requestTask 用于停止
    currentTask.value = requestTask

  } catch (e) {
    console.error('发送消息失败', e)
    messages.value[aiMsgIdx].isStreaming = false
    isStreaming.value = false
  }

  selectedImages.value = []
  selectedFiles.value = []
}

// 停止生成
const currentTask = ref<any>(null)

async function stopGeneration() {
  try {
    await postApi('/ai/chat/stop', { params: { conversationId: conversationId.value } })
  } catch (e) { /* ignore */ }
  if (currentTask.value) {
    currentTask.value.abort()
    currentTask.value = null
  }
  // 标记最后一条 AI 消息停止流式
  const last = messages.value[messages.value.length - 1]
  if (last && last.isStreaming) {
    last.isStreaming = false
  }
  isStreaming.value = false
}

function resendLastMessage() {
  const lastUserMsg = [...messages.value].reverse().find(m => m.role === 'user')
  if (lastUserMsg) {
    inputText.value = lastUserMsg.content
    // 移除最后两条（用户 + AI占位）
    messages.value = messages.value.slice(0, -2)
    sendMessage()
  }
}

// 附件选择
function showAttachmentPicker() {
  uni.showActionSheet({
    itemList: ['拍照', '从相册选择', '选择文件'],
    success: (res) => {
      if (res.tapIndex === 0) {
        const sourceType: any = ['camera']
        uni.chooseImage({
          count: 9,
          sourceType,
          success: (r) => {
            selectedImages.value.push(...r.tempFilePaths)
          },
        })
      } else if (res.tapIndex === 1) {
        const sourceType: any = ['album']
        uni.chooseImage({
          count: 9,
          sourceType,
          success: (r) => {
            selectedImages.value.push(...r.tempFilePaths)
          },
        })
      } else if (res.tapIndex === 2) {
        // 选择文件
        uni.chooseMessageFile({
          count: 5,
          type: 'all',
          success: async (r) => {
            for (const file of r.tempFiles) {
              if (!(await validateUploadFile(file.path))) continue
              // 上传文件到服务器
              try {
                const token = uni.getStorageSync('homeai_token')
                const uploadRes = await uni.uploadFile({
                  url: getAppBaseUrl() + '/homeai/ai/chat/upload',
                  filePath: file.path,
                  name: 'file',
                  header: {
                    'X-Access-Token': token,
                  },
                })
                const data = JSON.parse(uploadRes.data)
                if (data && data.result) {
                  selectedFiles.value.push({
                    name: file.name,
                    url: data.result.storedUrl || data.result.url,
                    size: file.size,
                  })
                }
              } catch (e) {
                console.error('文件上传失败', e)
                uni.showToast({ title: '文件上传失败', icon: 'none' })
              }
            }
          },
        })
      }
    },
  })
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
  background: #f5f5f5;
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
  background: linear-gradient(135deg, #667eea, #764ba2);
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
  border-radius: 16rpx;
  font-size: 28rpx;
  line-height: 1.6;
  word-break: break-word;
}
.bubble.user {
  background: #667eea;
  color: #fff;
  border-bottom-right-radius: 4rpx;
}
.bubble.assistant {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);
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
  color: #667eea;
  text-decoration: underline;
}
.input-area {
  padding: 16rpx 20rpx;
  background: #fff;
  border-top: 1rpx solid #eee;
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
  background: #f0f0f0;
  border-radius: 8rpx;
  max-width: 100%;
}
.file-name {
  font-size: 24rpx;
  color: #666;
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
}
.text-input {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
  background: #f5f5f5;
  border-radius: 36rpx;
  font-size: 28rpx;
}
</style>
