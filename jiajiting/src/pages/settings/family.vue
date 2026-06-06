<template>
  <view class="page">
    <view v-if="!familyStore.isMember" class="card">
      <text class="title">创建家庭</text>
      <input v-model="familyName" class="input" placeholder="家庭名称，如：小两口" />
      <button class="btn primary" @tap="onCreate">创建并生成邀请码</button>

      <view class="divider">或</view>

      <text class="title">加入家庭（只读）</text>
      <input v-model="inviteCode" class="input" placeholder="输入 6 位邀请码" />
      <button class="btn ghost" @tap="onJoin">加入家庭</button>
      <text class="hint">v2.0：成员可协作记账，户主与成员各自记账、汇总查看</text>
    </view>

    <view v-else class="card">
      <text class="title">{{ familyStore.family?.name }}</text>
      <text class="meta">角色：{{ familyStore.isOwner ? '户主' : '成员（可记账）' }}</text>
      <text class="meta">成员 {{ familyStore.family?.members.length || 0 }} 人</text>

      <view v-if="familyStore.isOwner" class="invite-box">
        <text class="label">邀请码</text>
        <text class="code">{{ familyStore.family?.inviteCode }}</text>
        <button class="btn small" @tap="copyCode">复制邀请码</button>
      </view>

      <button class="btn primary" @tap="goSummary">查看家庭汇总</button>
      <button v-if="familyStore.isOwner" class="btn ghost" @tap="onRefresh">刷新汇总快照</button>
      <button class="btn danger" @tap="onLeave">退出家庭</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useFamilyStore } from '@/stores/family'
import { useAchievementStore } from '@/stores/achievement'
import { ensureLoggedIn } from '@/utils/auth-guard'

const familyStore = useFamilyStore()
const achievementStore = useAchievementStore()
const familyName = ref('')
const inviteCode = ref('')

function onCreate() {
  try {
    const fam = familyStore.createFamily(familyName.value)
    uni.showToast({ title: `邀请码：${fam.inviteCode}`, icon: 'none' })
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '创建失败', icon: 'none' })
  }
}

function onJoin() {
  try {
    familyStore.joinFamily(inviteCode.value)
    achievementStore.checkFamilyJoin()
    uni.showToast({ title: '已加入家庭', icon: 'success' })
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '加入失败', icon: 'none' })
  }
}

function copyCode() {
  const code = familyStore.copyInviteCode()
  if (!code) return
  uni.setClipboardData({
    data: code,
    success() {
      uni.showToast({ title: '已复制邀请码', icon: 'success' })
      achievementStore.checkFamilyJoin()
    },
  })
}

function onRefresh() {
  familyStore.refreshSnapshot()
  uni.showToast({ title: '已刷新', icon: 'success' })
}

function goSummary() {
  uni.navigateTo({ url: '/pages/family/summary' })
}

function onLeave() {
  uni.showModal({
    title: '退出家庭',
    content: '确定退出当前家庭？',
    success(res) {
      if (res.confirm) {
        familyStore.leaveFamily()
        uni.showToast({ title: '已退出', icon: 'none' })
      }
    },
  })
}

onShow(() => {
  if (!ensureLoggedIn()) return
  if (familyStore.isOwner) {
    familyStore.refreshSnapshot()
    const count = familyStore.family?.members.length || 0
    if (count > 1) achievementStore.checkFamilyJoin()
    achievementStore.checkFamilyTeam(count)
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.title { font-weight: 600; font-size: $font-size-md; display: block; margin-bottom: $spacing-sm; }
.meta { font-size: $font-size-sm; color: $color-text-secondary; display: block; margin-bottom: 4rpx; }
.input { background: $color-bg-page; padding: $spacing-sm; border-radius: $radius-sm; margin-bottom: $spacing-md; font-size: $font-size-sm; }
.btn { border-radius: $radius-full; font-size: $font-size-sm; margin-bottom: $spacing-sm; border: none; }
.btn.primary { background: $color-primary; color: #fff; }
.btn.ghost { background: $color-bg-card; border: 1rpx solid $color-border; }
.btn.danger { background: transparent; color: $color-danger; border: 1rpx solid $color-border; }
.btn.small { display: inline-block; padding: 0 $spacing-md; margin-top: $spacing-xs; }
.divider { text-align: center; color: $color-text-muted; margin: $spacing-lg 0; font-size: $font-size-sm; }
.hint { font-size: $font-size-xs; color: $color-text-muted; display: block; margin-top: $spacing-sm; }
.invite-box { background: $color-bg-page; padding: $spacing-md; border-radius: $radius-md; margin: $spacing-md 0; }
.label { font-size: $font-size-xs; color: $color-text-muted; display: block; }
.code { font-size: $font-size-xl; font-weight: 700; letter-spacing: 4rpx; display: block; margin: $spacing-xs 0; color: $color-primary; }
</style>
