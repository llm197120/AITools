<route lang="json5">
{
  style: {
    navigationBarTitleText: 'AI对话',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view class="conversations-page">
    <view v-if="!loading && list.length === 0" class="empty-wrap">
      <HomeEmpty
        icon-name="chat"
        title="暂无对话"
        hint="点下方示例快速开聊，或点右下角新建"
        :card="false"
      >
        <template #actions>
          <view class="empty-actions">
            <view class="example-topic hai-press" @click="createAndGo('帮我写一份食谱')">帮我写一份食谱</view>
            <view class="example-topic hai-press" @click="createAndGo('今天晚餐推荐')">今天晚餐推荐</view>
            <view class="example-topic hai-press" @click="createAndGo('帮我解释一下什么是量子计算')">帮我解释一下量子计算</view>
          </view>
        </template>
      </HomeEmpty>
    </view>

    <view v-else class="list">
      <view class="swipe-wrap" v-for="item in list" :key="item.id">
        <view class="swipe-actions">
          <view class="swipe-btn rename" @click.stop="renameItem(item)">重命名</view>
          <view class="swipe-btn delete" @click.stop="deleteItem(item)">删除</view>
        </view>
        <view
          class="list-item hai-press"
          :style="{ transform: `translateX(${offsets[item.id] || 0}px)` }"
          @touchstart="onTouchStart($event, item.id)"
          @touchmove="onTouchMove($event, item.id)"
          @touchend="onTouchEnd(item.id)"
          @click="onItemClick(item)"
        >
          <view class="item-content">
            <text class="item-title">{{ item.title }}</text>
            <text class="item-meta">{{ item.messageCount }} 条消息 · {{ formatTime(item.updateTime) }}</text>
          </view>
          <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
        </view>
      </view>
    </view>

    <view class="hai-fab" @click="createAndGo()">
      <wd-icon name="add" size="24px" color="#fff"></wd-icon>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get as getApi, post as postApi, put as putApi, del as delApi } from '../../pages-homeai/api/request'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import HomeEmpty from '../../components/HomeEmpty.vue'

useHomeaiPageGuard()

const list = ref<any[]>([])
const loading = ref(false)
const offsets = reactive<Record<string, number>>({})
const ACTION_WIDTH = 160
const startX = ref(0)
const startOffset = ref(0)
const activeId = ref('')
const moved = ref(false)

onShow(loadList)

async function loadList() {
  loading.value = true
  try {
    list.value = await getApi('/ai/conversations/mine')
    for (const key of Object.keys(offsets)) delete offsets[key]
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
  const url = `/pages-homeai-ai/ai/chat?id=${id}${initialMsg ? '&initial=' + encodeURIComponent(initialMsg) : ''}`
  uni.navigateTo({ url })
}

function closeOthers(exceptId: string) {
  for (const id of Object.keys(offsets)) {
    if (id !== exceptId) offsets[id] = 0
  }
}

function onTouchStart(e: any, id: string) {
  const touch = e.touches?.[0]
  if (!touch) return
  activeId.value = id
  startX.value = touch.clientX
  startOffset.value = offsets[id] || 0
  moved.value = false
  closeOthers(id)
}

function onTouchMove(e: any, id: string) {
  if (activeId.value !== id) return
  const touch = e.touches?.[0]
  if (!touch) return
  const dx = touch.clientX - startX.value
  if (Math.abs(dx) > 8) moved.value = true
  let next = startOffset.value + dx
  if (next > 0) next = 0
  if (next < -ACTION_WIDTH) next = -ACTION_WIDTH
  offsets[id] = next
}

function onTouchEnd(id: string) {
  const cur = offsets[id] || 0
  offsets[id] = cur < -ACTION_WIDTH / 2 ? -ACTION_WIDTH : 0
  activeId.value = ''
}

function onItemClick(item: any) {
  if (moved.value) return
  if ((offsets[item.id] || 0) < 0) {
    offsets[item.id] = 0
    return
  }
  goChat(item.id)
}

function renameItem(item: any) {
  offsets[item.id] = 0
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
}

function deleteItem(item: any) {
  offsets[item.id] = 0
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
</script>

<style scoped>
.conversations-page {
  min-height: 100vh;
  background: var(--hai-bg);
}
.list {
  padding: 24rpx 32rpx 120rpx;
}
.swipe-wrap {
  position: relative;
  margin-bottom: 16rpx;
  border-radius: var(--hai-radius);
  overflow: hidden;
}
.swipe-actions {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  display: flex;
  width: 320rpx;
}
.swipe-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  color: #fff;
}
.swipe-btn.rename {
  background: var(--hai-primary);
}
.swipe-btn.delete {
  background: var(--hai-danger);
}
.list-item {
  display: flex;
  align-items: center;
  padding: 28rpx 30rpx;
  background: var(--hai-card);
  border-radius: var(--hai-radius);
  gap: 12rpx;
  box-shadow: var(--hai-shadow);
  position: relative;
  z-index: 1;
  transition: transform 0.18s ease;
}
.item-content {
  flex: 1;
}
.item-title {
  font-size: 28rpx;
  color: var(--hai-text);
  font-weight: 500;
}
.item-meta {
  font-size: 22rpx;
  color: var(--hai-text-muted);
  margin-top: 8rpx;
  display: block;
}
.empty-wrap {
  padding: 48rpx 32rpx 120rpx;
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
</style>
