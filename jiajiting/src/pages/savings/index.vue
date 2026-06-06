<template>
  <view class="page">
    <view class="header card">
      <text class="title">💡 省钱洞察</text>
      <text class="subtitle">基于你的账单习惯，无爬虫、无价格承诺</text>
    </view>

    <view class="section">
      <text class="section-title">消费习惯提醒</text>
      <view v-for="tip in tips" :key="tip.id" class="tip card">
        <text class="tip-title">{{ tip.title }}</text>
        <text class="tip-content">{{ tip.content }}</text>
        <button
          v-if="tip.dealId && dealMap[tip.dealId]"
          class="deal-btn"
          @tap="openDeal(dealMap[tip.dealId])"
        >
          查看官方优惠 ›
        </button>
      </view>
      <view v-if="!tips.length" class="empty card">暂无提醒，多记账可获得更准洞察</view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">联盟优惠</text>
        <text class="link" @tap="goCoupons">优惠券中心 ›</text>
      </view>
      <view v-for="deal in deals" :key="deal.id" class="deal card" @tap="openDeal(deal)">
        <text class="emoji">{{ deal.emoji }}</text>
        <view class="deal-body">
          <text class="deal-title">{{ deal.title }}</text>
          <text class="deal-sub">{{ deal.subtitle }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="links card">
      <view class="link-row" @tap="goInbox">
        <text>📬 洞察中心 Inbox</text>
        <text v-if="insightStore.unreadCount" class="badge">{{ insightStore.unreadCount }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="link-row" @tap="goCoach">
        <text>🤖 AI 财务教练</text>
        <text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useRecordStore } from '@/stores/record'
import { useInsightStore } from '@/stores/insight'
import { evaluateSavingsTips } from '@/constants/savings-rules'
import { getDeals, openAllianceLink } from '@/services/alliance'
import type { AllianceDeal } from '@/constants/alliance-deals'
import { ensureLoggedIn } from '@/utils/auth-guard'
import { track } from '@/utils/analytics'

const recordStore = useRecordStore()
const insightStore = useInsightStore()
const deals = getDeals()

const tips = computed(() => evaluateSavingsTips(recordStore.records))

const dealMap = computed(() => {
  const map: Record<string, AllianceDeal> = {}
  deals.forEach((d) => {
    map[d.id] = d
  })
  return map
})

function openDeal(deal: AllianceDeal) {
  track('alliance_open', { id: deal.id })
  openAllianceLink(deal)
}

function goCoupons() {
  uni.navigateTo({ url: '/pages/coupons/index' })
}

function goInbox() {
  uni.navigateTo({ url: '/pages/profile/insights' })
}

function goCoach() {
  uni.navigateTo({ url: '/pages/profile/coach' })
}

onShow(() => {
  if (!ensureLoggedIn()) return
  insightStore.generateFromRules()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; padding-bottom: 40rpx; }
.header { margin-bottom: $spacing-md; }
.title { font-size: $font-size-xl; font-weight: 700; display: block; }
.subtitle { font-size: $font-size-xs; color: $color-text-muted; margin-top: 4rpx; display: block; }
.section { margin-bottom: $spacing-lg; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-sm; }
.section-title { font-size: $font-size-lg; font-weight: 600; display: block; margin-bottom: $spacing-sm; }
.link { font-size: $font-size-sm; color: $color-primary; }
.tip { margin-bottom: $spacing-sm; }
.tip-title { font-weight: 600; font-size: $font-size-md; display: block; }
.tip-content { font-size: $font-size-sm; color: $color-text-secondary; margin-top: $spacing-xs; display: block; line-height: 1.6; }
.deal-btn { margin-top: $spacing-sm; background: transparent; color: $color-primary; font-size: $font-size-xs; border: none; padding: 0; text-align: left; }
.deal { display: flex; align-items: center; gap: $spacing-md; margin-bottom: $spacing-sm; }
.emoji { font-size: 40rpx; }
.deal-body { flex: 1; }
.deal-title { font-weight: 600; font-size: $font-size-sm; display: block; }
.deal-sub { font-size: $font-size-xs; color: $color-text-muted; display: block; margin-top: 2rpx; }
.arrow { color: $color-text-muted; }
.empty { text-align: center; color: $color-text-muted; font-size: $font-size-sm; padding: $spacing-lg; }
.links { padding: 0; }
.link-row { display: flex; align-items: center; padding: $spacing-md; border-bottom: 1rpx solid $color-border; font-size: $font-size-md; }
.link-row:last-child { border-bottom: none; }
.badge { background: $color-danger; color: #fff; font-size: $font-size-xs; padding: 2rpx 10rpx; border-radius: $radius-full; margin-left: auto; margin-right: $spacing-sm; }
</style>
