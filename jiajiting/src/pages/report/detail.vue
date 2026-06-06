<template>
  <view class="page">
    <view class="header card">
      <text class="title">{{ emoji }} {{ category }} 详情</text>
      <text>已花 ¥{{ formatMoney(spent) }} / 预算 ¥{{ formatMoney(budget) }}</text>
      <view class="bar"><view class="fill" :style="{ width: Math.min(status.percent, 100) + '%' }" /></view>
      <text v-if="largeData" class="perf-hint">共 {{ totalCount }} 条，分页加载中</text>
    </view>

    <view class="filters">
      <text :class="{ active: filter === 'all' }" @tap="filter = 'all'">全部</text>
      <text :class="{ active: filter === '刚需' }" @tap="filter = '刚需'">刚需</text>
      <text :class="{ active: filter === '可选' }" @tap="filter = '可选'">可选</text>
      <text :class="{ active: filter === '奢侈' }" @tap="filter = '奢侈'">奢侈</text>
    </view>

    <view
      v-for="item in visible"
      :key="item.id"
      class="row card"
      @tap="editRecord(item.id)"
      @longpress="deleteRecord(item.id)"
    >
      <view class="left">
        <text class="time">{{ formatDateTime(item.timestamp) }}</text>
        <text class="remark">{{ item.remark || '无备注' }}</text>
      </view>
      <view class="right">
        <text class="amount">¥{{ formatMoney(item.amount) }}</text>
        <text class="nature">{{ item.nature }}</text>
      </view>
    </view>

    <button v-if="hasMore" class="load-more" @tap="loadMore">加载更多</button>
    <text v-if="!filtered.length" class="empty">暂无记录</text>
    <text class="hint">点击编辑 · 长按删除</text>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useRecordStore, useBudgetStore } from '@/stores'
import { getMonthRangeBy, formatMoney, formatDateTime } from '@/utils/date'
import { budgetStatus } from '@/utils/budget'
import { isLargeDataset, paginate, PAGE_SIZE } from '@/utils/perf'
import type { RecordNature } from '@/types/models'

const recordStore = useRecordStore()
const budgetStore = useBudgetStore()

const category = ref('')
const year = ref(new Date().getFullYear())
const month = ref(new Date().getMonth() + 1)
const filter = ref<'all' | RecordNature>('all')
const page = ref(1)

const emoji = computed(() => budgetStore.categories.find((c) => c.name === category.value)?.emoji || '···')
const spent = computed(() => budgetStore.getCategorySpent(category.value, year.value, month.value))
const budget = computed(() => budgetStore.categories.find((c) => c.name === category.value)?.budget || 0)
const status = computed(() => budgetStatus(spent.value, budget.value))

const filtered = computed(() => {
  const { start, end } = getMonthRangeBy(year.value, month.value)
  return recordStore.records
    .filter(
      (r) =>
        r.type === 'expense' &&
        r.category === category.value &&
        r.timestamp >= start &&
        r.timestamp <= end &&
        (filter.value === 'all' || r.nature === filter.value)
    )
    .sort((a, b) => b.timestamp - a.timestamp)
})

const totalCount = computed(() => filtered.value.length)
const largeData = computed(() => isLargeDataset(totalCount.value))
const visible = computed(() => paginate(filtered.value, page.value))
const hasMore = computed(() => visible.value.length < filtered.value.length)

function loadMore() {
  page.value += 1
}

function editRecord(id: string) {
  uni.navigateTo({ url: `/pages/record/quick?id=${id}` })
}

function deleteRecord(id: string) {
  uni.showModal({
    title: '删除记录',
    content: '确定删除这条记录？',
    confirmColor: '#d63031',
    success(res) {
      if (res.confirm) {
        recordStore.removeRecord(id)
        uni.showToast({ title: '已删除', icon: 'none' })
      }
    },
  })
}

onLoad((query) => {
  category.value = decodeURIComponent((query?.category as string) || '')
  year.value = Number(query?.year) || year.value
  month.value = Number(query?.month) || month.value
  uni.setNavigationBarTitle({ title: `${category.value}明细` })
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; padding-bottom: 80rpx; }
.header { margin-bottom: $spacing-md; }
.title { font-size: $font-size-lg; font-weight: 600; display: block; margin-bottom: $spacing-xs; }
.bar { height: 12rpx; background: $color-border; border-radius: $radius-full; margin-top: $spacing-sm; overflow: hidden; }
.fill { height: 100%; background: $color-primary; }
.perf-hint { font-size: $font-size-xs; color: $color-warning; display: block; margin-top: $spacing-xs; }
.filters { display: flex; gap: $spacing-md; margin-bottom: $spacing-md; font-size: $font-size-sm; }
.filters .active { color: $color-primary; font-weight: 600; }
.row { display: flex; justify-content: space-between; margin-bottom: $spacing-sm; }
.time { font-size: $font-size-xs; color: $color-text-muted; display: block; }
.remark { font-size: $font-size-sm; display: block; margin-top: 4rpx; }
.amount { font-weight: 600; color: $color-expense; display: block; text-align: right; }
.nature { font-size: $font-size-xs; color: $color-text-muted; display: block; text-align: right; }
.load-more { background: $color-bg-card; border: 1rpx solid $color-border; border-radius: $radius-full; font-size: $font-size-sm; margin: $spacing-md 0; }
.empty { text-align: center; color: $color-text-muted; font-size: $font-size-sm; display: block; }
.hint { text-align: center; color: $color-text-muted; font-size: $font-size-xs; display: block; margin-top: $spacing-md; }
</style>
