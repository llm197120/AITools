<route lang="json5">
{
  style: {
    navigationBarTitleText: 'AI对话',
  },
}
</route>

<template>
  <view class="conversations-page">
    <!-- 空状态 -->
    <wd-empty v-if="!loading && list.length === 0" description="暂无对话">
      <template #image>
        <wd-icon name="chat" size="80px" color="#ccc"></wd-icon>
      </template>
      <template #footer>
        <view class="empty-actions">
          <view class="example-topic" @click="createAndGo('帮我写一份食谱')">
            <text>🍳 帮我写一份食谱</text>
          </view>
          <view class="example-topic" @click="createAndGo('今天晚餐推荐')">
            <text>🍽️ 今天晚餐推荐</text>
          </view>
          <view class="example-topic" @click="createAndGo('帮我解释一下什么是量子计算')">
            <text>🔬 帮我解释一下量子计算</text>
          </view>
        </view>
      </template>
    </wd-empty>

    <!-- 对话列表 -->
    <view v-else class="list">
      <view class="list-item" v-for="item in list" :key="item.id"
        @click="goChat(item.id)"
        @longpress="showAction(item)">
        <view class="item-content">
          <text class="item-title">{{ item.title }}</text>
          <text class="item-meta">{{ item.messageCount }} 条消息 · {{ formatTime(item.updateTime) }}</text>
        </view>
        <wd-icon name="arrow-right" size="14px" color="#ccc"></wd-icon>
      </view>
    </view>

    <!-- 新建对话 -->
    <view class="fab" @click="createAndGo()">
      <wd-icon name="chat-new" size="24px" color="#fff"></wd-icon>
    </view>

    <!-- 操作菜单 -->
    <wd-action-sheet v-model="actionShow" :actions="actions" @select="onActionSelect"></wd-action-sheet>
  </view>
</template>

<script lang="ts" setup>
import { ref, onShow } from '@dcloudio/uni-app'
import { get as getApi, post as postApi, put as putApi, del as delApi } from '../../pages-homeai/api/request'

const list = ref<any[]>([])
const loading = ref(false)
const actionShow = ref(false)
const selectedItem = ref<any>(null)
const actions = ref([
  { name: '重命名', key: 'rename' },
  { name: '删除', key: 'delete', color: '#e74c3c' },
])

onShow(loadList)

async function loadList() {
  loading.value = true
  try {
    list.value = await getApi('/ai/conversations/mine')
  } catch (e) {
    console.error('加载对话列表失败', e)
  } finally {
    loading.value = false
  }
}

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 86400000) return '今天'
  if (diff < 172800000) return '昨天'
  return `${d.getMonth() + 1}/${d.getDate()}`
}

async function createAndGo(tip?: string) {
  try {
    const conv = await postApi('/ai/conversations', {})
    goChat(conv.id, tip)
  } catch (e) {
    console.error('创建对话失败', e)
  }
}

function goChat(id: string, initialMsg?: string) {
  const url = `/pages/homeai-ai/ai/chat?id=${id}${initialMsg ? '&initial=' + encodeURIComponent(initialMsg) : ''}`
  uni.navigateTo({ url })
}

function showAction(item: any) {
  selectedItem.value = item
  actionShow.value = true
}

async function onActionSelect(e: any) {
  const item = selectedItem.value
  if (!item) return
  if (e.key === 'rename') {
    uni.showModal({
      title: '重命名',
      editable: true,
      content: item.title,
      success: async (res) => {
        if (res.confirm && res.content) {
          await putApi(`/ai/conversations/${item.id}/rename`, { params: { title: res.content } })
          await loadList()
        }
      },
    })
  } else if (e.key === 'delete') {
    uni.showModal({
      title: '删除对话',
      content: '确定删除此对话吗？',
      success: async (res) => {
        if (res.confirm) {
          await delApi(`/ai/conversations/${item.id}`)
          await loadList()
        }
      },
    })
  }
}
</script>

<style scoped>
.conversations-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.list {
  padding: 20rpx;
}
.list-item {
  display: flex;
  align-items: center;
  padding: 28rpx 30rpx;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  gap: 12rpx;
}
.item-content {
  flex: 1;
}
.item-title {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}
.item-meta {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
  display: block;
}
.empty-actions {
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.example-topic {
  padding: 24rpx 30rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #666;
}
.fab {
  position: fixed;
  right: 40rpx;
  bottom: 100rpx;
  width: 100rpx;
  height: 100rpx;
  background: #667eea;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 20rpx rgba(102, 126, 234, 0.4);
}
</style>
