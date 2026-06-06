<template>
  <view class="page">
    <view v-for="cat in budgetStore.categories" :key="cat.id" class="row card">
      <view class="left">
        <text>{{ cat.emoji }} {{ cat.name }}</text>
        <text class="sub">子类：{{ cat.subCategories.join('、') || '无' }}</text>
      </view>
      <switch :checked="cat.isVisible" @change="() => budgetStore.toggleCategoryVisible(cat.id)" />
    </view>
    <text class="hint">拖动排序将在后续版本支持，当前可显示/隐藏分类</text>
  </view>
</template>

<script setup lang="ts">
import { useBudgetStore } from '@/stores'
const budgetStore = useBudgetStore()
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.row { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-sm; }
.left { display: flex; flex-direction: column; }
.sub { font-size: $font-size-xs; color: $color-text-muted; margin-top: 4rpx; }
.hint { font-size: $font-size-xs; color: $color-text-muted; text-align: center; margin-top: $spacing-lg; display: block; }
</style>
