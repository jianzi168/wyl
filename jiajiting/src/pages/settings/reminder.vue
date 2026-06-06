<template>
  <view class="page">
    <view class="card row">
      <view>
        <text class="title">支付后提醒补记</text>
        <text class="desc">消费后通过微信订阅消息提醒你记账（需授权）</text>
      </view>
      <switch :checked="enabled" @change="onToggle" />
    </view>

    <view class="card tips">
      <text class="section-title">说明</text>
      <text class="p">1. 需在公众平台配置订阅消息模板，并填入 `.env.local` 的 `VITE_SUBSCRIBE_RECORD_TMPL`</text>
      <text class="p">2. 每次记账完成后可请求一次订阅授权</text>
      <text class="p">3. 关闭后不再弹出订阅请求</text>
    </view>

    <button class="test-btn" @tap="testSubscribe">测试订阅授权</button>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  isRemindAfterPayEnabled,
  setRemindAfterPayEnabled,
  requestRecordRemindSubscribe,
} from '@/utils/subscribe-message'

const enabled = ref(true)

onMounted(() => {
  enabled.value = isRemindAfterPayEnabled()
})

function onToggle(e: Event) {
  const checked = (e as unknown as { detail: { value: boolean } }).detail.value
  enabled.value = checked
  setRemindAfterPayEnabled(checked)
}

async function testSubscribe() {
  const ok = await requestRecordRemindSubscribe()
  uni.showToast({
    title: ok ? '订阅成功' : '未配置模板或用户拒绝',
    icon: 'none',
  })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.row { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-md; }
.title { font-weight: 600; display: block; }
.desc { font-size: $font-size-xs; color: $color-text-muted; margin-top: 4rpx; display: block; max-width: 480rpx; }
.tips { font-size: $font-size-sm; color: $color-text-secondary; }
.section-title { font-weight: 600; display: block; margin-bottom: $spacing-sm; }
.p { display: block; margin-bottom: $spacing-xs; line-height: 1.6; }
.test-btn { background: $color-bg-card; border: 1rpx solid $color-border; border-radius: $radius-full; font-size: $font-size-sm; margin-top: $spacing-lg; }
</style>
