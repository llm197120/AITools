<route lang="json5">
{
  style: {
    navigationBarTitleText: '我的家庭',
  },
}
</route>

<template>
  <view class="family-page">
    <!-- 无家庭状态 -->
    <view class="no-family" v-if="!familyStore.hasFamily">
      <wd-status-tip tip="您还没有加入任何家庭" image="content">
        <template #image>
          <wd-icon name="home" size="80px" color="#ccc"></wd-icon>
        </template>
      </wd-status-tip>
      <view class="actions">
        <wd-button type="primary" size="large" @click="showCreate">创建家庭</wd-button>
        <wd-button plain size="large" @click="showJoin">加入家庭</wd-button>
      </view>
    </view>

    <!-- 有家庭状态 -->
    <view class="has-family" v-else>
      <!-- 家庭信息 -->
      <view class="family-header">
        <text class="family-name">{{ familyStore.familyInfo?.name }}</text>
        <text class="member-count">{{ familyStore.familyInfo?.memberCount || 0 }} 位成员</text>
        <!-- 管理员可见：编辑名称 -->
        <wd-button size="small" plain @click="showEditName" v-if="isAdmin">编辑名称</wd-button>
      </view>

      <!-- 成员列表 -->
      <view class="section">
        <view class="section-title">
          <text>家庭成员</text>
          <text class="invite-btn" @click="generateInviteCode" v-if="isAdmin">+ 邀请</text>
        </view>
        <view class="member-list">
          <view class="member-item" v-for="member in members" :key="member.memberId">
            <image class="member-avatar" :src="member.avatarUrl || '/static/default-avatar.png'" mode="aspectFill" />
            <view class="member-info">
              <text class="member-name">{{ member.nickname }}</text>
              <text class="member-role">{{ roleLabel(member.role) }}</text>
            </view>
            <wd-button v-if="isAdmin && member.userId !== currentUserId" size="small" plain type="danger" @click="removeMember(member)">移除</wd-button>
          </view>
        </view>
      </view>

      <!-- 管理员操作 -->
      <view class="admin-actions" v-if="isAdmin">
        <wd-button block plain @click="showTransfer">转让管理员</wd-button>
        <wd-button block plain type="danger" @click="confirmDisband">解散家庭</wd-button>
      </view>

      <!-- 退出家庭 -->
      <view class="leave-btn" @click="confirmLeave" v-if="!isAdmin">
        退出家庭
      </view>
    </view>

    <!-- 创建家庭弹窗 -->
    <wd-popup v-model="createVisible" position="center" custom-style="width:80%;border-radius:16rpx;overflow:hidden">
      <view class="dialog-title">创建家庭</view>
      <view class="dialog-body">
        <wd-input v-model="createName" placeholder="请输入家庭名称" />
      </view>
      <view class="dialog-footer">
        <wd-button @click="createVisible = false">取消</wd-button>
        <wd-button type="primary" @click="createFamily">确认创建</wd-button>
      </view>
    </wd-popup>

    <!-- 加入家庭弹窗 -->
    <wd-popup v-model="joinVisible" position="center" custom-style="width:80%;border-radius:16rpx;overflow:hidden">
      <view class="dialog-title">加入家庭</view>
      <view class="dialog-body">
        <wd-input v-model="inviteCode" placeholder="请输入6位邀请码" maxlength="6" />
      </view>
      <view class="dialog-footer">
        <wd-button @click="joinVisible = false">取消</wd-button>
        <wd-button type="primary" @click="joinFamily">确认加入</wd-button>
      </view>
    </wd-popup>
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { useFamilyStore } from '../../pages-homeai/stores/family'
import { get as getApi, post as postApi, del as delApi, put as putApi } from '../../pages-homeai/api'
import { ensureProfileWhenGuest, ensureLoginForAction } from '../../pages-homeai/utils/homeaiAuth'

const userStore = useUserStore()
const familyStore = useFamilyStore()

const members = ref<any[]>([])
const currentUserId = computed(() => userStore.userInfo?.id)
const isAdmin = computed(() => members.value.some(m => m.userId === currentUserId.value && m.role === 'admin'))

// 创建家庭
const createVisible = ref(false)
const createName = ref('')

// 加入家庭
const joinVisible = ref(false)
const inviteCode = ref('')

onShow(async () => {
  if (!ensureProfileWhenGuest()) {
    return
  }
  await familyStore.fetchFamilyInfo()
  if (familyStore.hasFamily) {
    await fetchMembers()
  }
})

function roleLabel(role: string) {
  const map: Record<string, string> = { admin: '管理员', member: '成员', restricted: '受限成员' }
  return map[role] || role
}

async function fetchMembers() {
  members.value = await getApi('/family/members')
}

// 创建家庭
function showCreate() {
  createName.value = ''
  createVisible.value = true
}
async function createFamily() {
  if (!createName.value.trim()) {
    uni.showToast({ title: '请输入家庭名称', icon: 'none' })
    return
  }
  await familyStore.createFamily(createName.value.trim())
  createVisible.value = false
  await fetchMembers()
}

// 加入家庭
function showJoin() {
  inviteCode.value = ''
  joinVisible.value = true
}
async function joinFamily() {
  if (inviteCode.value.length !== 6) {
    uni.showToast({ title: '请输入6位邀请码', icon: 'none' })
    return
  }
  await postApi('/family/members', { params: { code: inviteCode.value.toUpperCase() } })
  joinVisible.value = false
  await familyStore.fetchFamilyInfo()
  await fetchMembers()
}

// 生成邀请码
async function generateInviteCode() {
  const code = await postApi('/family/invite-code')
  uni.setClipboardData({
    data: code,
    success: () => {
      uni.showToast({ title: '邀请码已复制: ' + code, icon: 'none' })
    },
  })
}

// 移除成员
function removeMember(member: any) {
  uni.showModal({
    title: '提示',
    content: `确定移除 ${member.nickname} 吗？`,
    success: async (res) => {
      if (res.confirm) {
        await delApi(`/family/member/${member.memberId}`)
        await fetchMembers()
      }
    },
  })
}

// 编辑名称
function showEditName() {
  uni.showModal({
    title: '编辑家庭名称',
    editable: true,
    content: familyStore.familyInfo?.name || '',
    success: async (res) => {
      if (res.confirm && res.content) {
        await putApi('/family', { id: familyStore.familyInfo?.id, name: res.content })
        await familyStore.fetchFamilyInfo()
      }
    },
  })
}

// 转让管理员
function showTransfer() {
  const candidates = members.value.filter(m => m.role !== 'admin')
  if (candidates.length === 0) {
    uni.showToast({ title: '没有可转让的成员', icon: 'none' })
    return
  }
  uni.showActionSheet({
    itemList: candidates.map(m => m.nickname),
    success: async (res) => {
      const target = candidates[res.tapIndex]
      await postApi('/family/transfer', { params: { targetUserId: target.userId } })
      uni.showToast({ title: '转让成功' })
      await fetchMembers()
    },
  })
}

// 解散家庭
function confirmDisband() {
  uni.showModal({
    title: '解散家庭',
    content: '解散后家庭将被标记为已解散（数据保留），所有成员将退出家庭。请输入"确认解散"继续。',
    editable: true,
    success: async (res) => {
      if (res.confirm && res.content === '确认解散') {
        await familyStore.disbandFamily()
        uni.showToast({ title: '家庭已解散' })
      } else if (res.confirm) {
        uni.showToast({ title: '请输入"确认解散"', icon: 'none' })
      }
    },
  })
}

// 退出家庭
function confirmLeave() {
  uni.showModal({
    title: '退出家庭',
    content: '确定退出当前家庭吗？',
    success: async (res) => {
      if (res.confirm) {
        await familyStore.leaveFamily()
        uni.showToast({ title: '已退出家庭' })
      }
    },
  })
}
</script>

<style scoped>
.family-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.no-family {
  padding-top: 120rpx;
}
.actions {
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}
.family-header {
  padding: 40rpx 30rpx;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.family-name {
  font-size: 40rpx;
  font-weight: bold;
}
.member-count {
  font-size: 26rpx;
  opacity: 0.8;
}
.section {
  margin: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 30rpx;
  font-size: 28rpx;
  font-weight: bold;
  border-bottom: 1rpx solid #f0f0f0;
}
.invite-btn {
  color: #667eea;
  font-weight: normal;
}
.member-list {
  padding: 0 30rpx;
}
.member-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  gap: 20rpx;
  border-bottom: 1rpx solid #f5f5f5;
}
.member-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
}
.member-info {
  flex: 1;
}
.member-name {
  font-size: 28rpx;
  color: #333;
}
.member-role {
  font-size: 22rpx;
  color: #999;
  margin-left: 8rpx;
}
.admin-actions {
  margin: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.leave-btn {
  margin: 40rpx 20rpx;
  padding: 28rpx;
  text-align: center;
  background: #fff;
  border-radius: 16rpx;
  color: #e74c3c;
  font-size: 28rpx;
}
.dialog-title {
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  padding: 30rpx 24rpx 10rpx;
}
.dialog-body {
  padding: 20rpx 30rpx;
}
.dialog-footer {
  display: flex;
  gap: 20rpx;
  padding: 0 30rpx 30rpx;
  justify-content: center;
}
</style>
