<template>
  <view class="keypad">
    <view
      v-for="key in keys"
      :key="key"
      class="key"
      :class="{ confirm: key === confirmKey }"
      @tap="$emit('press', key)"
    >
      <text>{{ key }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{ confirmKey?: string }>()
defineEmits<{ press: [key: string] }>()
const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '0', '✓']
</script>

<script lang="ts">
export default { options: { virtualHost: true } }
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-sm;
}
.key {
  background: $color-bg-card;
  border-radius: $radius-md;
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-size-xl;
}
.key.confirm {
  background: $color-primary;
  color: #fff;
}
</style>
