<template>
  <view class="page">
    <view class="card policy">
      <text class="title">隐私政策</text>
      <text class="p">家计通收集您的微信登录信息、记账数据，用于提供预算分析与多设备同步服务。</text>
      <text class="p">数据存储于国内云服务，不会出售给第三方。AI 功能（如有）仅上传聚合统计数据。</text>
      <text class="p">您可随时导出或删除本地及云端数据。</text>
    </view>

    <view class="card row">
      <view>
        <text class="title">AI 财务教练</text>
        <text class="desc">开启后可使用周报/月报解读与异常问答（仅上传聚合统计）</text>
      </view>
      <switch :checked="aiEnabled" @change="onAiToggle" />
    </view>

    <view class="menu card">
      <view class="menu-item" @tap="exportData">
        <text>📋 导出账单 CSV</text>
        <text>›</text>
      </view>
      <view class="menu-item danger" @tap="deleteAccount">
        <text>🗑️ 注销账号并清除数据</text>
        <text>›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRecordStore, useUserStore } from '@/stores'
import { isAiEnabled, setAiEnabled } from '@/utils/ai-prefs'
import { exportRecords } from '@/utils/export'
import { removeStorage } from '@/utils/storage'
import { track } from '@/utils/analytics'
import { clearQueue } from '@/services/sync'

const recordStore = useRecordStore()
const userStore = useUserStore()
const aiEnabled = ref(true)

onMounted(() => {
  aiEnabled.value = isAiEnabled()
})

function onAiToggle(e: Event) {
  const checked = (e as unknown as { detail: { value: boolean } }).detail.value
  aiEnabled.value = checked
  setAiEnabled(checked)
}

function exportData() {
  exportRecords(recordStore.records)
  track('export', { count: recordStore.records.length })
}

function deleteAccount() {
  uni.showModal({
    title: '注销账号',
    content: '将清除本地所有记账数据且无法恢复，确定继续？',
    confirmColor: '#d63031',
    success(res) {
      if (!res.confirm) return
      recordStore.clearAll()
      clearQueue()
      removeStorage('categories')
      removeStorage('optional_budget')
      removeStorage('recurring_items')
      removeStorage('achievements_unlocked')
      removeStorage('privacy_accepted')
      userStore.logout()
      track('account_delete')
      uni.reLaunch({ url: '/pages/auth/login' })
    },
  })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.policy { margin-bottom: $spacing-md; }
.title { font-weight: 700; font-size: $font-size-lg; display: block; margin-bottom: $spacing-md; }
.p { font-size: $font-size-sm; color: $color-text-secondary; line-height: 1.7; display: block; margin-bottom: $spacing-sm; }
.row { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-md; }
.desc { font-size: $font-size-xs; color: $color-text-muted; margin-top: 4rpx; display: block; max-width: 480rpx; }
.menu { padding: 0; }
.menu-item { display: flex; justify-content: space-between; padding: $spacing-md; border-bottom: 1rpx solid $color-border; }
.menu-item.danger { color: $color-danger; }
</style>
