<template>
  <view class="page">
    <text class="summary">已解锁 {{ achievementStore.unlockedCount }} / {{ achievementStore.list.length }}</text>
    <view class="grid">
      <view
        v-for="item in achievementStore.list"
        :key="item.code"
        class="badge card"
        :class="{ unlocked: item.unlocked }"
      >
        <text class="emoji">{{ item.emoji }}</text>
        <text class="name">{{ item.name }}</text>
        <text class="desc">{{ item.description }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useAchievementStore } from '@/stores'
const achievementStore = useAchievementStore()
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.summary { display: block; margin-bottom: $spacing-md; font-size: $font-size-sm; color: $color-text-secondary; }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: $spacing-sm; }
.badge { text-align: center; padding: $spacing-md; opacity: 0.45; }
.badge.unlocked { opacity: 1; border: 2rpx solid $color-primary; }
.emoji { font-size: 48rpx; display: block; }
.name { font-weight: 600; font-size: $font-size-sm; display: block; margin-top: $spacing-xs; }
.desc { font-size: $font-size-xs; color: $color-text-muted; display: block; margin-top: 4rpx; }
</style>
