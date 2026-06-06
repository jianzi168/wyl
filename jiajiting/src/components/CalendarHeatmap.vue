<template>
  <view class="heatmap">
    <view class="weekdays">
      <text v-for="w in weekdays" :key="w">{{ w }}</text>
    </view>
    <view v-for="(row, ri) in rows" :key="ri" class="week-row">
      <view
        v-for="(cell, ci) in row"
        :key="ci"
        class="cell"
        :class="[`level-${cell.level}`, { empty: cell.empty }]"
      >
        <text v-if="!cell.empty" class="day">{{ cell.day }}</text>
      </view>
    </view>
    <view class="legend">
      <text>少</text>
      <view class="level-1 sample" />
      <view class="level-2 sample" />
      <view class="level-3 sample" />
      <text>多</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import type { HeatmapCell } from '@/utils/report'

defineProps<{ rows: HeatmapCell[][] }>()
const weekdays = ['日', '一', '二', '三', '四', '五', '六']
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: $font-size-xs;
  color: $color-text-muted;
  margin-bottom: $spacing-xs;
}
.week-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6rpx;
  margin-bottom: 6rpx;
}
.cell {
  aspect-ratio: 1;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $color-bg-page;
  font-size: 20rpx;
}
.cell.empty { background: transparent; }
.cell.level-1 { background: rgba(108, 92, 231, 0.2); }
.cell.level-2 { background: rgba(108, 92, 231, 0.45); }
.cell.level-3 { background: rgba(108, 92, 231, 0.75); color: #fff; }
.legend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8rpx;
  margin-top: $spacing-sm;
  font-size: $font-size-xs;
  color: $color-text-muted;
}
.sample {
  width: 24rpx;
  height: 24rpx;
  border-radius: 4rpx;
}
</style>
