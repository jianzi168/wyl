<template>
  <view class="page">
    <view class="calendar card">
      <text class="section-title">📅 本月固定收支</text>
      <view v-for="item in recurringStore.thisMonthCalendar" :key="item.id" class="cal-row">
        <text>{{ item.dateLabel }}</text>
        <text>{{ item.type === 'income' ? '💰' : '📌' }} {{ item.name }}</text>
        <text :class="item.type">{{ item.type === 'income' ? '+' : '-' }}¥{{ item.amount }}</text>
      </view>
      <text v-if="!recurringStore.thisMonthCalendar.length" class="empty">暂无固定收支</text>
    </view>

    <view v-for="item in recurringStore.items" :key="item.id" class="item card">
      <view class="row">
        <text>{{ item.name }}</text>
        <switch :checked="item.isActive" @change="() => recurringStore.updateItem(item.id, { isActive: !item.isActive })" />
      </view>
      <text class="meta">每月{{ item.dayOfMonth }}日 · ¥{{ item.amount }}</text>
      <text class="del" @tap="recurringStore.removeItem(item.id)">删除</text>
    </view>

    <view class="form card">
      <text class="section-title">添加固定项</text>
      <input v-model="form.name" class="input" placeholder="名称，如：房租" />
      <input v-model="form.amount" class="input" type="digit" placeholder="金额" />
      <input v-model="form.day" class="input" type="number" placeholder="每月几号（1-28）" />
      <picker :range="types" :value="typeIndex" @change="onTypeChange">
        <view class="picker">{{ types[typeIndex] }}</view>
      </picker>
      <button class="add" @tap="add">添加</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRecurringStore, useBudgetStore } from '@/stores'
import type { RecordType } from '@/types/models'

const recurringStore = useRecurringStore()
const budgetStore = useBudgetStore()
const types = ['支出', '收入']
const typeIndex = ref(0)
const form = ref({ name: '', amount: '', day: '1' })

function onTypeChange(e: { detail: { value: string } }) {
  typeIndex.value = Number(e.detail.value)
}

function add() {
  const amount = parseFloat(form.value.amount)
  const day = Math.min(28, Math.max(1, parseInt(form.value.day) || 1))
  if (!form.value.name || !amount) {
    uni.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  recurringStore.addItem({
    name: form.value.name,
    amount,
    type: (typeIndex.value === 1 ? 'income' : 'expense') as RecordType,
    category: typeIndex.value === 1 ? '其他' : '账单',
    repeatRule: 'monthly',
    dayOfMonth: day,
    isActive: true,
    nextTrigger: Date.now(),
  })
  form.value = { name: '', amount: '', day: '1' }
  uni.showToast({ title: '已添加', icon: 'success' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; padding-bottom: 80rpx; }
.section-title { font-weight: 600; display: block; margin-bottom: $spacing-md; }
.calendar { margin-bottom: $spacing-md; }
.cal-row { display: flex; justify-content: space-between; font-size: $font-size-sm; margin-bottom: $spacing-xs; }
.empty { color: $color-text-muted; font-size: $font-size-sm; }
.item { margin-bottom: $spacing-sm; }
.row { display: flex; justify-content: space-between; }
.meta { font-size: $font-size-xs; color: $color-text-muted; display: block; margin-top: 4rpx; }
.del { color: $color-danger; font-size: $font-size-xs; margin-top: $spacing-xs; display: block; }
.income { color: $color-income; }
.expense { color: $color-expense; }
.form .input, .picker { background: $color-bg-page; padding: $spacing-sm; border-radius: $radius-sm; margin-bottom: $spacing-sm; }
.add { background: $color-primary; color: #fff; border: none; border-radius: $radius-full; }
</style>
