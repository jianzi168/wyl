<template>
  <view class="ring-wrap">
    <canvas canvas-id="ringChart" class="canvas" :style="{ width: size + 'px', height: size + 'px' }" />
    <view class="center">
      <text class="total">¥{{ total.toFixed(0) }}</text>
      <text class="label">总支出</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { watch, onMounted, getCurrentInstance } from 'vue'
import { debounce } from '@/utils/perf'

const props = defineProps<{
  segments: Array<{ color: string; percent: number }>
  total: number
  size?: number
}>()

const size = props.size || 200
const emit = defineEmits<{ segmentTap: [index: number] }>()

function draw() {
  const ctx = uni.createCanvasContext('ringChart', getCurrentInstance()?.proxy)
  if (!ctx) return
  const s = size
  const cx = s / 2
  const cy = s / 2
  const outer = s / 2 - 4
  const inner = outer * 0.62

  ctx.clearRect(0, 0, s, s)
  let start = -Math.PI / 2
  props.segments.forEach((seg) => {
    const angle = (seg.percent / 100) * Math.PI * 2
    if (angle <= 0) return
    ctx.beginPath()
    ctx.arc(cx, cy, outer, start, start + angle)
    ctx.arc(cx, cy, inner, start + angle, start, true)
    ctx.closePath()
    ctx.setFillStyle(seg.color)
    ctx.fill()
    start += angle
  })
  ctx.draw()
}

const debouncedDraw = debounce(draw)

onMounted(draw)
watch(() => props.segments, debouncedDraw, { deep: true })
</script>

<style lang="scss" scoped>
.ring-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}
.canvas {
  display: block;
}
.center {
  position: absolute;
  text-align: center;
}
.total {
  font-size: 36rpx;
  font-weight: 700;
  display: block;
}
.label {
  font-size: 22rpx;
  color: #636e72;
}
</style>
