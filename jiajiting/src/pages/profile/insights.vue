<template>
  <view class="page">
    <view class="header">
      <text class="title">洞察中心</text>
      <text class="action" @tap="insightStore.markAllRead">全部已读</text>
    </view>

    <view v-if="!visibleItems.length" class="empty card">
      <text>暂无洞察，继续记账后会自动生成建议</text>
      <button class="refresh" @tap="refresh">刷新洞察</button>
    </view>

    <view
      v-for="item in visibleItems"
      :key="item.id"
      class="item card"
      :class="{ unread: !item.readAt }"
      @tap="open(item.id)"
    >
      <view class="top">
        <text class="priority" :class="item.priority">{{ priorityLabel(item.priority) }}</text>
        <text class="time">{{ formatDate(item.createdAt) }}</text>
      </view>
      <text class="item-title">{{ item.title }}</text>
      <text class="content">{{ item.content }}</text>
      <text class="dismiss" @tap.stop="insightStore.dismiss(item.id)">关闭</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useInsightStore } from '@/stores/insight'
import { formatDate } from '@/utils/date'
import { ensureLoggedIn } from '@/utils/auth-guard'
import type { InsightPriority } from '@/types/auth'

const insightStore = useInsightStore()

const visibleItems = computed(() =>
  insightStore.items.filter((i) => !i.dismissedAt).slice(0, 50)
)

function priorityLabel(p: InsightPriority) {
  const map = { critical: '紧急', high: '重要', medium: '提示', low: '鼓励' }
  return map[p]
}

function open(id: string) {
  insightStore.markRead(id)
}

function refresh() {
  insightStore.generateFromRules()
  uni.showToast({ title: '已刷新', icon: 'success' })
}

onShow(() => {
  ensureLoggedIn()
  insightStore.generateFromRules()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-md; }
.title { font-size: $font-size-lg; font-weight: 700; }
.action { font-size: $font-size-sm; color: $color-primary; }
.empty { text-align: center; padding: $spacing-xl; color: $color-text-muted; font-size: $font-size-sm; }
.refresh { margin-top: $spacing-md; background: $color-primary; color: #fff; border: none; border-radius: $radius-full; font-size: $font-size-sm; }
.item { margin-bottom: $spacing-sm; position: relative; }
.item.unread { border-left: 6rpx solid $color-primary; }
.top { display: flex; justify-content: space-between; margin-bottom: $spacing-xs; }
.priority { font-size: $font-size-xs; padding: 2rpx 12rpx; border-radius: $radius-full; background: $color-bg-page; }
.priority.critical, .priority.high { background: rgba(214,48,49,0.12); color: $color-danger; }
.time { font-size: $font-size-xs; color: $color-text-muted; }
.item-title { font-weight: 600; font-size: $font-size-md; display: block; }
.content { font-size: $font-size-sm; color: $color-text-secondary; margin-top: $spacing-xs; display: block; }
.dismiss { position: absolute; right: $spacing-md; bottom: $spacing-md; font-size: $font-size-xs; color: $color-text-muted; }
</style>
