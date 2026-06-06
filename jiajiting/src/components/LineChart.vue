<template>
  <view class="line-wrap">
    <canvas canvas-id="lineChart" class="canvas" :style="{ width: width + 'px', height: height + 'px' }" />
  </view>
</template>

<script setup lang="ts">
import { watch, onMounted, getCurrentInstance } from 'vue'
import { debounce } from '@/utils/perf'

const props = withDefaults(
  defineProps<{
    labels: string[]
    series: Array<{ name: string; color: string; data: number[] }>
    width?: number
    height?: number
  }>(),
  { width: 320, height: 160 }
)

function draw() {
  const ctx = uni.createCanvasContext('lineChart', getCurrentInstance()?.proxy)
  if (!ctx || !props.series.length) return
  const w = props.width
  const h = props.height
  const pad = 24
  const max = Math.max(...props.series.flatMap((s) => s.data), 1)
  const stepX = (w - pad * 2) / Math.max(props.labels.length - 1, 1)

  ctx.clearRect(0, 0, w, h)
  ctx.setStrokeStyle('#dfe6e9')
  ctx.setLineWidth(1)
  ctx.beginPath()
  ctx.moveTo(pad, h - pad)
  ctx.lineTo(w - pad, h - pad)
  ctx.stroke()

  props.series.forEach((s) => {
    ctx.beginPath()
    ctx.setStrokeStyle(s.color)
    ctx.setLineWidth(2)
    s.data.forEach((v, i) => {
      const x = pad + i * stepX
      const y = h - pad - (v / max) * (h - pad * 2)
      if (i === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    })
    ctx.stroke()
  })
  ctx.draw()
}

const debouncedDraw = debounce(draw)

onMounted(draw)
watch(() => [props.series, props.labels], debouncedDraw, { deep: true })
</script>

<style lang="scss" scoped>
.line-wrap {
  width: 100%;
  overflow: hidden;
}
.canvas {
  display: block;
  margin: 0 auto;
}
</style>
