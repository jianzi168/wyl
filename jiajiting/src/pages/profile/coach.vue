<template>
  <view class="page">
    <view class="actions card">
      <button class="btn" :disabled="!coachStore.canAskWeekly() || coachStore.loading" @tap="onWeekly">
        📊 周报解读{{ coachStore.canAskWeekly() ? '' : '（本周已用）' }}
      </button>
      <button class="btn" :disabled="!coachStore.canAskMonthly() || coachStore.loading" @tap="onMonthly">
        📅 月报解读{{ coachStore.canAskMonthly() ? '' : '（本月已用）' }}
      </button>
      <text class="hint">异常问答本周剩余 {{ coachStore.qaRemaining() }} 次 · 仅上传聚合统计</text>
    </view>

    <view class="ask card">
      <input v-model="question" class="input" placeholder="为什么本月花多了？怎么省钱？" />
      <button class="send" :disabled="coachStore.loading" @tap="onAsk">提问</button>
    </view>

    <view v-if="coachStore.loading" class="loading">分析中…</view>

    <view v-for="msg in coachStore.messages" :key="msg.id" class="msg card" :class="msg.role">
      <text class="role">{{ msg.role === 'coach' ? '🤖 AI 教练' : '我' }}</text>
      <text class="content">{{ msg.content }}</text>
    </view>

    <view v-if="!coachStore.messages.length" class="empty card">
      <text>暂无解读记录。可从周报/月报页快捷生成，或在此提问。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useCoachStore } from '@/stores/coach'
import { ensureLoggedIn } from '@/utils/auth-guard'
import { isAiEnabled } from '@/utils/ai-prefs'

const coachStore = useCoachStore()
const question = ref('')

async function run(fn: () => Promise<unknown>) {
  try {
    await fn()
  } catch (err) {
    const msg = err instanceof Error ? err.message : '生成失败'
    uni.showToast({ title: msg, icon: 'none' })
  }
}

function onWeekly() {
  run(() => coachStore.interpretWeekly())
}

function onMonthly() {
  run(() => coachStore.interpretMonthly())
}

function onAsk() {
  run(async () => {
    await coachStore.askQuestion(question.value)
    question.value = ''
  })
}

onShow(() => {
  if (!ensureLoggedIn()) return
  if (!isAiEnabled()) {
    uni.showModal({
      title: 'AI 功能已关闭',
      content: '可在「隐私与数据」中开启 AI 财务教练',
      showCancel: false,
    })
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; padding-bottom: 80rpx; }
.actions { margin-bottom: $spacing-md; }
.btn { margin-bottom: $spacing-sm; background: $color-primary; color: #fff; border: none; border-radius: $radius-full; font-size: $font-size-sm; }
.btn[disabled] { opacity: 0.5; }
.hint { font-size: $font-size-xs; color: $color-text-muted; display: block; }
.ask { display: flex; gap: $spacing-sm; margin-bottom: $spacing-md; align-items: center; }
.input { flex: 1; background: $color-bg-page; padding: $spacing-sm; border-radius: $radius-sm; font-size: $font-size-sm; }
.send { background: $color-primary; color: #fff; border: none; border-radius: $radius-full; font-size: $font-size-sm; padding: 0 $spacing-md; }
.loading { text-align: center; color: $color-text-muted; margin-bottom: $spacing-md; }
.msg { margin-bottom: $spacing-sm; }
.msg.coach { border-left: 6rpx solid $color-primary; }
.role { font-size: $font-size-xs; color: $color-text-muted; display: block; margin-bottom: $spacing-xs; }
.content { font-size: $font-size-sm; line-height: 1.6; white-space: pre-wrap; }
.empty { text-align: center; color: $color-text-muted; font-size: $font-size-sm; padding: $spacing-xl; }
</style>
