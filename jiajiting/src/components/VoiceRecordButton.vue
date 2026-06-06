<template>
  <view
    class="voice-btn"
    :class="{ recording }"
    @touchstart.prevent="onStart"
    @touchend.prevent="onEnd"
    @touchcancel.prevent="onEnd"
    @tap="onTapFallback"
  >
    <text class="icon">🎤</text>
    <text class="label">{{ recording ? '松开识别' : '按住说话记账' }}</text>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  beginVoiceCapture,
  endVoiceCapture,
  promptVoiceFallback,
  isVoicePluginAvailable,
} from '@/services/voice'
import type { VoiceParseResult } from '@/utils/voice-parser'

const emit = defineEmits<{ parsed: [result: VoiceParseResult] }>()
const recording = ref(false)
let capturePromise: Promise<VoiceParseResult> | null = null

async function onStart() {
  if (recording.value) return
  recording.value = true
  uni.vibrateShort?.({ type: 'light' } as never)

  if (isVoicePluginAvailable()) {
    capturePromise = beginVoiceCapture()
    return
  }
}

async function onEnd() {
  if (!recording.value) return
  recording.value = false

  if (capturePromise) {
    endVoiceCapture()
    try {
      const result = await capturePromise
      emit('parsed', result)
    } catch (err) {
      const msg = err instanceof Error ? err.message : '识别失败'
      uni.showToast({ title: msg, icon: 'none' })
    }
    capturePromise = null
    return
  }
}

function onTapFallback() {
  if (isVoicePluginAvailable()) return
  promptVoiceFallback()
    .then((r) => emit('parsed', r))
    .catch((err) => {
      const msg = err instanceof Error ? err.message : ''
      if (msg && msg !== '已取消') uni.showToast({ title: msg, icon: 'none' })
    })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.voice-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  background: $color-bg-card;
  border-radius: $radius-full;
  border: 2rpx dashed $color-border;
  margin-bottom: $spacing-md;
}
.voice-btn.recording {
  border-color: $color-primary;
  background: rgba(108, 92, 231, 0.1);
}
.icon { font-size: 36rpx; }
.label { font-size: $font-size-sm; color: $color-text-secondary; }
</style>
