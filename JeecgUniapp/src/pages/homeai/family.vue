<route lang="json5">
{
  style: {
    navigationBarTitleText: '我的家庭',
    navigationBarBackgroundColor: '#F3F2EE',
    enablePullDownRefresh: true,
  },
}
</route>

<template>
  <view class="family-page">
    <!-- 有家庭状态（含加载失败时保留缓存） -->
    <view class="has-family" v-if="familyStore.hasFamily">
      <view class="family-header">
        <text class="family-name">{{ familyStore.familyInfo?.name }}</text>
        <text class="member-count">{{ displayedMemberCount }} 位成员</text>
        <view class="edit-name" @click="showEditName" v-if="isAdmin">编辑名称</view>
      </view>

      <view class="section">
        <view class="section-title">
          <text class="section-title-text">家庭成员</text>
          <text class="invite-btn hai-press" @click="generateInviteCode" v-if="isAdmin">邀请成员</text>
        </view>
        <text v-if="membersLoadFailed" class="members-fail" @click="fetchMembers">成员加载失败，点此重试</text>
        <view v-else class="member-list">
          <view class="member-item" v-for="member in members" :key="member.memberId">
            <image class="member-avatar" :src="member.avatarUrl || '/static/default-avatar.png'" mode="aspectFill" lazy-load />
            <view class="member-info">
              <text class="member-name">{{ member.nickname }}</text>
              <text class="member-role">{{ roleLabel(member.role) }}</text>
            </view>
            <view
              v-if="isAdmin && member.userId !== currentUserId"
              class="remove-btn"
              @click="removeMember(member)"
            >移除</view>
          </view>
        </view>
      </view>

      <view class="admin-actions" v-if="isAdmin">
        <view class="btn-ghost block" @click="showTransfer">转让管理员</view>
        <view class="btn-danger block" @click="confirmDisband">解散家庭</view>
      </view>

      <view class="leave-btn" @click="confirmLeave" v-if="!isAdmin">退出家庭</view>
    </view>

    <!-- 加载失败 -->
    <view class="no-family" v-else-if="familyStore.familyLoadFailed">
      <HomeEmpty
        icon-name="warning"
        icon-color="#1B4F8A"
        icon-size="40px"
        title="家庭信息加载失败"
        hint="请检查网络后重试"
      >
        <template #actions>
          <view class="actions">
            <view class="btn-primary" @click="retryFamilyInfo">重试</view>
          </view>
        </template>
      </HomeEmpty>
    </view>

    <!-- 无家庭状态 -->
    <view class="no-family" v-else-if="familyStore.familyInfoLoaded">
      <HomeEmpty
        icon-name="home"
        icon-color="#1B4F8A"
        icon-size="40px"
        title="还没有家庭"
        hint="创建或加入家庭后，资料与计划可共享使用"
      >
        <template #actions>
          <view class="actions">
            <view class="btn-primary" @click="showCreate">创建家庭</view>
            <view class="btn-ghost" @click="showJoin">加入家庭</view>
          </view>
        </template>
      </HomeEmpty>
    </view>

    <view class="no-family loading-hint" v-else>
      <text class="loading-text">加载中…</text>
    </view>

    <wd-popup v-model="createVisible" position="center" custom-style="width:80%;border-radius:28rpx;overflow:hidden">
      <view class="dialog-title">创建家庭</view>
      <view class="dialog-body">
        <wd-input v-model="createName" placeholder="请输入家庭名称" />
      </view>
      <view class="dialog-footer">
        <wd-button block @click="createVisible = false">取消</wd-button>
        <wd-button type="primary" block :loading="familyBusy" @click="createFamily">确认创建</wd-button>
      </view>
    </wd-popup>

    <wd-popup v-model="joinVisible" position="center" custom-style="width:80%;border-radius:28rpx;overflow:hidden">
      <view class="dialog-title">加入家庭</view>
      <view class="dialog-body">
        <wd-input v-model="inviteCode" placeholder="请输入6位邀请码" :maxlength="8" />
        <text class="dialog-hint">邀请码 24 小时内有效，且只能使用一次</text>
      </view>
      <view class="dialog-footer">
        <wd-button block @click="joinVisible = false">取消</wd-button>
        <wd-button type="primary" block :loading="familyBusy" @click="joinFamily">确认加入</wd-button>
      </view>
    </wd-popup>

    <wd-popup v-model="editNameVisible" position="center" custom-style="width:80%;border-radius:28rpx;overflow:hidden">
      <view class="dialog-title">编辑家庭名称</view>
      <view class="dialog-body">
        <wd-input v-model="editName" placeholder="请输入家庭名称" />
      </view>
      <view class="dialog-footer">
        <wd-button block @click="editNameVisible = false">取消</wd-button>
        <wd-button type="primary" block :loading="familyBusy" @click="saveFamilyName">保存</wd-button>
      </view>
    </wd-popup>

    <wd-popup v-model="disbandVisible" position="center" custom-style="width:80%;border-radius:28rpx;overflow:hidden">
      <view class="dialog-title">解散家庭</view>
      <view class="dialog-body">
        <text class="dialog-hint">解散后数据保留，所有成员将退出。请输入「确认解散」继续。</text>
        <wd-input v-model="disbandConfirm" placeholder="请输入确认解散" />
      </view>
      <view class="dialog-footer">
        <wd-button block @click="disbandVisible = false">取消</wd-button>
        <wd-button type="error" block :loading="familyBusy" @click="doDisband">确认解散</wd-button>
      </view>
    </wd-popup>

    <wd-message-box />
    <wd-action-sheet
      v-model="transferVisible"
      :actions="transferActions"
      cancel-text="取消"
      @select="onTransferSelect"
    />
    <wd-action-sheet
      v-model="inviteSheetVisible"
      :actions="inviteSheetActions"
      cancel-text="取消"
      @select="onInviteSelect"
    />
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { useFamilyStore } from '../../pages-homeai/stores/family'
import { get as getApi, post as postApi, del as delApi, put as putApi } from '../../pages-homeai/api'
import { ensureProfileWhenGuest, HOMEAI_ONBOARD_FAMILY_KEY } from '../../pages-homeai/utils/homeaiAuth'
import { shareText } from '../../pages-homeai/platform/share'
import { useMessage } from 'wot-design-uni'
import HomeEmpty from '../../components/HomeEmpty.vue'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import { useFamilyPoll } from '../../pages-homeai/utils/useFamilyPoll'

const userStore = useUserStore()
const familyStore = useFamilyStore()
const message = useMessage()

const members = ref<any[]>([])
const membersLoadFailed = ref(false)
const currentUserId = computed(() => userStore.userInfo?.id)
const isAdmin = computed(() => familyStore.myRole === 'admin')
const displayedMemberCount = computed(() => {
  if (!membersLoadFailed.value) return members.value.length
  return Number(familyStore.familyInfo?.memberCount || 0)
})

// 创建家庭
const createVisible = ref(false)
const createName = ref('')
const familyBusy = ref(false)

// 加入家庭
const joinVisible = ref(false)
const inviteCode = ref('')
const editNameVisible = ref(false)
const editName = ref('')
const disbandVisible = ref(false)
const disbandConfirm = ref('')
const transferVisible = ref(false)
const transferCandidates = ref<any[]>([])
const transferActions = computed(() =>
  transferCandidates.value.map((m) => ({ name: m.nickname || '未命名' })),
)
const inviteSheetVisible = ref(false)
const inviteSheetActions = [
  { name: '复制邀请码' },
  { name: '系统分享' },
]
const pendingInviteCode = ref('')

const { start: startFamilyPoll, stop: stopFamilyPoll } = useFamilyPoll()

useHomeaiPullRefresh(async () => {
  await familyStore.fetchFamilyInfo()
  if (familyStore.hasFamily) await fetchMembers()
})

onShow(async () => {
  stopFamilyPoll()
  if (!ensureProfileWhenGuest()) {
    return
  }
  await familyStore.fetchFamilyInfo()
  if (familyStore.hasFamily) {
    await fetchMembers()
    uni.removeStorageSync(HOMEAI_ONBOARD_FAMILY_KEY)
    startFamilyPoll()
    return
  }
  const onboard = uni.getStorageSync(HOMEAI_ONBOARD_FAMILY_KEY)
  uni.removeStorageSync(HOMEAI_ONBOARD_FAMILY_KEY)
  if (onboard === 'create') {
    showCreate()
  } else if (onboard === 'join') {
    showJoin()
  }
})

async function retryFamilyInfo() {
  await familyStore.fetchFamilyInfo()
  if (familyStore.hasFamily) {
    await fetchMembers()
    startFamilyPoll()
  }
}

function roleLabel(role: string) {
  const map: Record<string, string> = { admin: '管理员', member: '成员', restricted: '受限成员' }
  return map[role] || role
}

async function fetchMembers() {
  membersLoadFailed.value = false
  try {
    members.value = await getApi('/family/members')
  } catch {
    members.value = []
    membersLoadFailed.value = true
  }
}

// 创建家庭
function showCreate() {
  createName.value = ''
  createVisible.value = true
}
async function createFamily() {
  if (familyBusy.value) return
  if (!createName.value.trim()) {
    uni.showToast({ title: '请输入家庭名称', icon: 'none' })
    return
  }
  familyBusy.value = true
  try {
    await familyStore.createFamily(createName.value.trim())
    createVisible.value = false
    await fetchMembers()
  } finally {
    familyBusy.value = false
  }
}

// 加入家庭
function showJoin() {
  inviteCode.value = ''
  joinVisible.value = true
}
async function joinFamily() {
  if (familyBusy.value) return
  const code = inviteCode.value.replace(/\s/g, '').toUpperCase()
  if (code.length !== 6) {
    uni.showToast({ title: '请输入6位邀请码', icon: 'none' })
    return
  }
  familyBusy.value = true
  try {
    await postApi('/family/members', { params: { code } })
    joinVisible.value = false
    await familyStore.fetchFamilyInfo()
    await fetchMembers()
  } finally {
    familyBusy.value = false
  }
}

// 生成邀请码
async function generateInviteCode() {
  if (familyBusy.value) return
  familyBusy.value = true
  try {
    const code = String(await postApi('/family/invite-code') || '')
    if (!code) {
      uni.showToast({ title: '生成失败', icon: 'none' })
      return
    }
    pendingInviteCode.value = code
    inviteSheetVisible.value = true
  } finally {
    familyBusy.value = false
  }
}

function copyInviteCode(code: string) {
  uni.setClipboardData({
    data: code,
    success: () => {
      uni.showToast({ title: '已复制，24小时内有效', icon: 'none' })
    },
  })
}

async function shareInviteCode(code: string) {
  const name = familyStore.familyInfo?.name || '家庭'
  const summary = `邀请你加入「${name}」，邀请码：${code}（24小时内有效，只能使用一次）`
  const ok = await shareText(summary, '邀请加入家庭')
  if (ok) {
    uni.showToast({ title: '已打开系统分享', icon: 'none' })
    return
  }
  copyInviteCode(code)
}

function onInviteSelect({ index }: { index: number }) {
  const code = pendingInviteCode.value
  if (!code) return
  if (index === 0) copyInviteCode(code)
  else shareInviteCode(code)
}

// 移除成员
async function removeMember(member: any) {
  if (familyBusy.value) return
  try {
    await message.confirm({
      title: '移除成员',
      msg: `确定移除 ${member.nickname} 吗？`,
    })
  } catch {
    return
  }
  familyBusy.value = true
  try {
    await delApi(`/family/member/${member.memberId}`)
    await fetchMembers()
    await familyStore.fetchFamilyInfo()
  } finally {
    familyBusy.value = false
  }
}

// 编辑名称
function showEditName() {
  editName.value = familyStore.familyInfo?.name || ''
  editNameVisible.value = true
}

async function saveFamilyName() {
  if (familyBusy.value) return
  if (!editName.value.trim()) {
    uni.showToast({ title: '请输入家庭名称', icon: 'none' })
    return
  }
  familyBusy.value = true
  try {
    await putApi('/family', { id: familyStore.familyInfo?.id, name: editName.value.trim() })
    editNameVisible.value = false
    await familyStore.fetchFamilyInfo()
  } finally {
    familyBusy.value = false
  }
}

// 转让管理员
function showTransfer() {
  const candidates = members.value.filter((m) => m.role !== 'admin')
  if (candidates.length === 0) {
    uni.showToast({ title: '没有可转让的成员', icon: 'none' })
    return
  }
  transferCandidates.value = candidates
  transferVisible.value = true
}

async function onTransferSelect({ index }: { index: number }) {
  if (familyBusy.value) return
  const target = transferCandidates.value[index]
  if (!target) return
  try {
    await message.confirm({
      title: '转让管理员',
      msg: `确定将管理员转让给 ${target.nickname || '该成员'}？转让后您将变为普通成员。`,
    })
  } catch {
    return
  }
  familyBusy.value = true
  try {
    await postApi('/family/transfer', { params: { targetUserId: target.userId } })
    uni.showToast({ title: '转让成功' })
    await familyStore.fetchFamilyInfo()
    await fetchMembers()
  } finally {
    familyBusy.value = false
  }
}

// 解散家庭
function confirmDisband() {
  disbandConfirm.value = ''
  disbandVisible.value = true
}

async function doDisband() {
  if (familyBusy.value) return
  if (disbandConfirm.value !== '确认解散') {
    uni.showToast({ title: '请输入「确认解散」', icon: 'none' })
    return
  }
  familyBusy.value = true
  try {
    await familyStore.disbandFamily()
    disbandVisible.value = false
    uni.showToast({ title: '家庭已解散' })
  } finally {
    familyBusy.value = false
  }
}

// 退出家庭
async function confirmLeave() {
  if (familyBusy.value) return
  try {
    await message.confirm({
      title: '退出家庭',
      msg: '确定退出当前家庭吗？',
    })
  } catch {
    return
  }
  familyBusy.value = true
  try {
    await familyStore.leaveFamily()
    uni.showToast({ title: '已退出家庭' })
  } finally {
    familyBusy.value = false
  }
}
</script>

<style scoped>
.family-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 24rpx 32rpx 48rpx;
  background: var(--hai-bg);
}

.no-family {
  padding-top: 40rpx;
}

.loading-hint {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 320rpx;
}

.loading-text {
  font-size: 28rpx;
  color: var(--hai-text-secondary, #888);
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-top: 28rpx;
  padding: 0 12rpx;
}

.btn-primary,
.btn-ghost,
.btn-danger {
  text-align: center;
  padding: 24rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 500;
}

.btn-primary {
  background: var(--hai-primary);
  color: var(--hai-on-primary);
}

.btn-ghost {
  background: var(--hai-card);
  color: var(--hai-primary);
  border: 1rpx solid rgba(27, 79, 138, 0.25);
}

.btn-danger {
  background: var(--hai-card);
  color: var(--hai-danger);
  border: 1rpx solid rgba(196, 92, 74, 0.25);
}

.btn-primary.block,
.btn-ghost.block,
.btn-danger.block {
  width: 100%;
  box-sizing: border-box;
}

.family-header {
  padding: 40rpx 32rpx;
  background: var(--hai-card);
  border-radius: 28rpx;
  box-shadow: var(--hai-shadow);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.family-name {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 40rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.member-count {
  font-size: 24rpx;
  color: var(--hai-text-secondary);
}

.edit-name {
  margin-top: 8rpx;
  padding: 10rpx 28rpx;
  border-radius: 999rpx;
  background: var(--hai-primary-soft);
  color: var(--hai-primary);
  font-size: 22rpx;
}

.section {
  margin-top: 24rpx;
  background: var(--hai-card);
  border-radius: 28rpx;
  box-shadow: var(--hai-shadow);
  overflow: hidden;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 30rpx;
  border-bottom: 1rpx solid var(--hai-border);
}
.members-fail {
  display: block;
  padding: 32rpx 30rpx;
  font-size: 26rpx;
  color: var(--hai-danger, #c45c4a);
  text-align: center;
}

.section-title-text {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.invite-btn {
  color: var(--hai-primary);
  font-size: 24rpx;
  padding: 8rpx 22rpx;
  border-radius: 999rpx;
  background: var(--hai-primary-soft);
}

.member-list {
  padding: 0 30rpx;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  gap: 20rpx;
  border-bottom: 1rpx solid var(--hai-border);
}

.member-item:last-child {
  border-bottom: none;
}

.member-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: var(--hai-bg);
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 28rpx;
  color: var(--hai-text);
}

.member-role {
  font-size: 22rpx;
  color: var(--hai-text-muted);
  margin-left: 8rpx;
}

.remove-btn {
  min-height: 64rpx;
  min-width: 88rpx;
  padding: 14rpx 28rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  font-size: 24rpx;
  color: var(--hai-danger);
  background: var(--hai-danger-soft, rgba(196, 92, 74, 0.08));
}

.admin-actions {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.leave-btn {
  margin-top: 40rpx;
  padding: 28rpx;
  text-align: center;
  background: var(--hai-card);
  border-radius: 28rpx;
  box-shadow: var(--hai-shadow);
  color: var(--hai-danger);
  font-size: 28rpx;
}

.dialog-title {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  padding: 36rpx 24rpx 10rpx;
  color: var(--hai-text);
}

.dialog-body {
  padding: 20rpx 30rpx;
}

.dialog-hint {
  display: block;
  font-size: 24rpx;
  color: var(--hai-text-secondary);
  line-height: 1.5;
  margin-bottom: 16rpx;
}

.dialog-footer {
  display: flex;
  gap: 20rpx;
  padding: 0 30rpx 30rpx;
  justify-content: center;
}
</style>
