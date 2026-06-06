<template>
  <view class="page">
    <view class="tabs card">
      <text
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{ active: activeTab === t.key }"
        @tap="activeTab = t.key"
      >
        {{ t.label }} ({{ couponStore.byStatus(t.key).length }})
      </text>
    </view>

    <view v-if="couponStore.loading" class="loading">加载中…</view>

    <view
      v-for="cp in couponStore.byStatus(activeTab)"
      :key="cp.id"
      class="coupon card"
      :class="{ dim: activeTab !== 'available' }"
    >
      <text class="emoji">{{ cp.emoji }}</text>
      <view class="body">
        <text class="title">{{ cp.title }}</text>
        <text class="desc">{{ cp.description }}</text>
        <text class="expire">有效期至 {{ formatDate(cp.expireAt) }}</text>
      </view>
      <view class="right">
        <text class="amount">¥{{ cp.amount }}</text>
        <text class="min">满{{ cp.minSpend }}可用</text>
        <button
          v-if="activeTab === 'available'"
          class="use-btn"
          @tap="onUse(cp)"
        >
          去使用
        </button>
      </view>
    </view>

    <view v-if="!couponStore.loading && !couponStore.byStatus(activeTab).length" class="empty card">
      暂无{{ tabLabel }}优惠券
    </view>

    <text class="disclaimer">优惠券来自官方联盟 API，家计通不承诺价格与可用性</text>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useCouponStore } from '@/stores/coupon'
import { openAllianceLink } from '@/services/alliance'
import type { Coupon, CouponStatus } from '@/types/coupon'
import { formatDate } from '@/utils/date'
import { ensureLoggedIn } from '@/utils/auth-guard'

const couponStore = useCouponStore()
const activeTab = ref<CouponStatus>('available')

const tabs: { key: CouponStatus; label: string }[] = [
  { key: 'available', label: '未使用' },
  { key: 'used', label: '已用' },
  { key: 'expired', label: '过期' },
]

const tabLabel = computed(() => tabs.find((t) => t.key === activeTab.value)?.label || '')

async function onUse(cp: Coupon) {
  openAllianceLink(cp)
  await couponStore.useCoupon(cp.id)
}

onShow(() => {
  if (!ensureLoggedIn()) return
  couponStore.load()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; padding-bottom: 60rpx; }
.tabs { display: flex; margin-bottom: $spacing-md; padding: $spacing-xs; }
.tab { flex: 1; text-align: center; font-size: $font-size-xs; padding: $spacing-sm 4rpx; border-radius: $radius-full; color: $color-text-secondary; }
.tab.active { background: $color-primary; color: #fff; }
.loading { text-align: center; color: $color-text-muted; margin-bottom: $spacing-md; }
.coupon { display: flex; gap: $spacing-sm; margin-bottom: $spacing-sm; align-items: stretch; }
.coupon.dim { opacity: 0.55; }
.emoji { font-size: 40rpx; align-self: center; }
.body { flex: 1; }
.title { font-weight: 600; font-size: $font-size-sm; display: block; }
.desc { font-size: $font-size-xs; color: $color-text-muted; display: block; margin-top: 2rpx; }
.expire { font-size: $font-size-xs; color: $color-text-muted; display: block; margin-top: 4rpx; }
.right { text-align: right; min-width: 120rpx; }
.amount { font-size: $font-size-lg; font-weight: 700; color: $color-expense; display: block; }
.min { font-size: $font-size-xs; color: $color-text-muted; display: block; }
.use-btn { margin-top: $spacing-xs; background: $color-primary; color: #fff; border: none; border-radius: $radius-full; font-size: $font-size-xs; padding: 0 16rpx; }
.empty { text-align: center; color: $color-text-muted; padding: $spacing-xl; font-size: $font-size-sm; }
.disclaimer { display: block; text-align: center; font-size: $font-size-xs; color: $color-text-muted; margin-top: $spacing-lg; line-height: 1.6; }
</style>
