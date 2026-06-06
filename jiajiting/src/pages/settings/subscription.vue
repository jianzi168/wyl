<template>
  <view class="page">
    <view class="hero card" :class="{ active: premium }">
      <text class="plan">{{ premium ? '⭐ 高级版' : '免费版' }}</text>
      <text class="desc" v-if="premium">有效期至 {{ expireLabel }}</text>
      <text class="desc" v-else>升级解锁更多能力</text>
    </view>

    <view class="features card">
      <text class="section-title">高级版权益</text>
      <text class="feat">✓ AI 教练问答不限次数</text>
      <text class="feat">✓ 报表大数据加速渲染</text>
      <text class="feat">✓ 专属成就徽章</text>
      <text class="feat">✓ 优先同步（云端模式）</text>
    </view>

    <view class="compare card">
      <view class="row head">
        <text>功能</text><text>免费</text><text>高级</text>
      </view>
      <view class="row"><text>AI 问答</text><text>3次/周</text><text>不限</text></view>
      <view class="row"><text>记账/报表</text><text>✓</text><text>✓</text></view>
      <view class="row"><text>家庭协作</text><text>✓</text><text>✓</text></view>
    </view>

    <button v-if="!premium" class="buy" @tap="subscribe">开通高级版（演示）</button>
    <button v-else class="ghost" @tap="cancel">取消订阅（演示）</button>
    <text class="hint">演示环境本地模拟，正式版对接微信支付</text>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { isPremium, setPremium, premiumExpireLabel } from '@/utils/subscription'
import { useAchievementStore } from '@/stores/achievement'
import { track } from '@/utils/analytics'

const premium = ref(false)
const expireLabel = ref('')

onMounted(() => {
  premium.value = isPremium()
  expireLabel.value = premiumExpireLabel()
})

function subscribe() {
  setPremium(true)
  premium.value = true
  expireLabel.value = premiumExpireLabel()
  useAchievementStore().checkPremium()
  track('premium_subscribe', {})
  uni.showToast({ title: '已开通高级版', icon: 'success' })
}

function cancel() {
  uni.showModal({
    title: '取消订阅',
    content: '确定取消高级版？',
    success(res) {
      if (!res.confirm) return
      setPremium(false)
      premium.value = false
      uni.showToast({ title: '已取消', icon: 'none' })
    },
  })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.hero { text-align: center; padding: $spacing-xl; margin-bottom: $spacing-md; background: $color-bg-page; }
.hero.active { background: linear-gradient(135deg, rgba(108,92,231,0.15), rgba(255,193,7,0.12)); }
.plan { font-size: $font-size-xl; font-weight: 700; display: block; }
.desc { font-size: $font-size-sm; color: $color-text-secondary; margin-top: $spacing-xs; display: block; }
.features { margin-bottom: $spacing-md; }
.section-title { font-weight: 600; display: block; margin-bottom: $spacing-sm; }
.feat { display: block; font-size: $font-size-sm; margin-bottom: $spacing-xs; color: $color-text-secondary; }
.compare { margin-bottom: $spacing-lg; font-size: $font-size-sm; }
.row { display: flex; justify-content: space-between; padding: $spacing-xs 0; border-bottom: 1rpx solid $color-border; }
.row.head { font-weight: 600; }
.buy { background: $color-primary-gradient; color: #fff; border: none; border-radius: $radius-full; margin-bottom: $spacing-sm; }
.ghost { background: $color-bg-card; border: 1rpx solid $color-border; border-radius: $radius-full; font-size: $font-size-sm; }
.hint { display: block; text-align: center; font-size: $font-size-xs; color: $color-text-muted; }
</style>
