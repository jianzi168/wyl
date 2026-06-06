<template>
  <view class="page">
    <view class="user-card card">
      <text class="nickname">{{ userStore.displayName }}</text>
      <text class="meta">{{ phoneLabel }} · 连续 {{ achievementStore.streak.days }} 天</text>
      <view class="sync-row">
        <text class="sync-status" :class="syncStore.status">同步：{{ syncStatusLabel }}</text>
        <text v-if="syncStore.pending > 0" class="pending">待同步 {{ syncStore.pending }} 条</text>
      </view>
    </view>

    <view class="menu card">
      <view class="menu-item" @tap="nav('/pages/settings/budget')">
        <text>⚙️ 预算设置</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="nav('/pages/settings/categories')">
        <text>📂 分类管理</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="nav('/pages/settings/recurring')">
        <text>📌 固定收支</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="nav('/pages/record/import')">
        <text>📥 导入账单</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="nav('/pages/profile/insights')">
        <text>💡 洞察中心</text>
        <view class="right">
          <text v-if="insightStore.unreadCount" class="badge">{{ insightStore.unreadCount }}</text>
          <text class="arrow">›</text>
        </view>
      </view>
      <view class="menu-item" @tap="nav('/pages/settings/reminder')">
        <text>🔔 记账提醒</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="nav('/pages/profile/coach')">
        <text>🤖 AI 财务教练</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="nav('/pages/settings/family')">
        <text>👨‍👩‍👧 家庭共享</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="nav('/pages/settings/subscription')">
        <text>⭐ 高级版</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="nav('/pages/settings/privacy')">
        <text>🔒 隐私与数据</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="handleSync">
        <text>🔄 立即同步</text><text class="arrow">›</text>
      </view>
      <view v-if="userStore.needsPhoneBind" class="menu-item" @tap="nav('/pages/auth/bind-phone')">
        <text>📱 绑定手机号</text><text class="arrow">›</text>
      </view>
    </view>

    <view class="achievements card" @tap="nav('/pages/profile/achievements')">
      <text class="section-title">🏆 成就徽章（{{ achievementStore.unlockedCount }}/{{ achievementStore.list.length }}）</text>
      <text class="link">查看全部 ›</text>
    </view>

    <button class="logout-btn" @tap="handleLogout">退出登录</button>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore, useSyncStore, useAchievementStore } from '@/stores'
import { useInsightStore } from '@/stores/insight'
import { ensureLoggedIn } from '@/utils/auth-guard'
import { formatDate } from '@/utils/date'

const userStore = useUserStore()
const syncStore = useSyncStore()
const achievementStore = useAchievementStore()
const insightStore = useInsightStore()

const phoneLabel = computed(() =>
  userStore.hasPhone ? maskPhone(userStore.user!.phone!) : '未绑定手机'
)

const syncStatusLabel = computed(() => {
  const map = {
    idle: syncStore.lastSyncedAt ? `已同步 ${formatDate(syncStore.lastSyncedAt)}` : '空闲',
    syncing: '同步中…',
    offline: '离线',
    error: '失败',
  }
  return map[syncStore.status]
})

function maskPhone(phone: string) {
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

function nav(url: string) {
  uni.navigateTo({ url })
}

async function handleSync() {
  const ok = await syncStore.syncNow()
  uni.showToast({ title: ok ? '同步完成' : syncStore.errorMessage || '同步失败', icon: ok ? 'success' : 'none' })
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '本地数据将保留，下次登录可继续同步',
    success(res) {
      if (res.confirm) {
        userStore.logout()
        uni.reLaunch({ url: '/pages/auth/login' })
      }
    },
  })
}

onShow(() => {
  if (!ensureLoggedIn()) return
  insightStore.generateFromRules()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.user-card { margin-bottom: $spacing-md; }
.nickname { font-size: $font-size-xl; font-weight: 700; display: block; }
.meta { font-size: $font-size-sm; color: $color-text-muted; margin-top: $spacing-xs; display: block; }
.sync-row { margin-top: $spacing-sm; display: flex; justify-content: space-between; font-size: $font-size-xs; }
.sync-status.error { color: $color-danger; }
.sync-status.offline { color: $color-warning; }
.pending { color: $color-primary; }
.menu { margin-bottom: $spacing-md; padding: 0; }
.menu-item { display: flex; justify-content: space-between; padding: $spacing-md; border-bottom: 1rpx solid $color-border; font-size: $font-size-md; }
.menu-item:last-child { border-bottom: none; }
.right { display: flex; align-items: center; gap: $spacing-xs; }
.badge { background: $color-danger; color: #fff; font-size: $font-size-xs; padding: 2rpx 12rpx; border-radius: $radius-full; min-width: 32rpx; text-align: center; }
.arrow { color: $color-text-muted; }
.achievements { margin-bottom: $spacing-lg; }
.section-title { font-weight: 600; display: block; }
.link { font-size: $font-size-sm; color: $color-primary; margin-top: $spacing-xs; display: block; }
.logout-btn { background: $color-bg-card; color: $color-danger; font-size: $font-size-sm; border: 1rpx solid $color-border; }
</style>
