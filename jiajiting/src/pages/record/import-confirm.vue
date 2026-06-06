<template>
  <view class="page">
    <text class="summary">共 {{ rows.length }} 条，去重后 {{ validCount }} 条可导入</text>
    <view v-for="row in rows" :key="row.tempId" class="row card" :class="{ dup: row.duplicate }">
      <view class="top">
        <text>{{ row.remark }} ¥{{ row.amount.toFixed(2) }}</text>
        <text v-if="row.duplicate" class="tag">重复</text>
      </view>
      <picker :range="categories" :value="catIndex(row)" @change="(e: any) => onCatChange(row, e)">
        <view class="picker">{{ row.selectedCategory }} ›</view>
      </picker>
    </view>
    <button class="confirm" @tap="confirm">确认导入 {{ validCount }} 条</button>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { markDuplicates, rowsToRecords } from '@/utils/import-parser'
import type { ParsedImportRow } from '@/utils/import-parser'
import { useRecordStore, useBudgetStore, useUserStore, useAchievementStore } from '@/stores'
import { createId } from '@/utils/id'
import { getStorage } from '@/utils/storage'
import { track } from '@/utils/analytics'

const recordStore = useRecordStore()
const budgetStore = useBudgetStore()
const userStore = useUserStore()
const rows = ref<ParsedImportRow[]>([])
const categories = computed(() => budgetStore.categories.map((c) => c.name))
const validCount = computed(() => rows.value.filter((r) => !r.duplicate).length)

onMounted(() => {
  const preview = getStorage<{ rows: ParsedImportRow[] } | null>('import_preview', null)
  if (!preview) {
    uni.navigateBack()
    return
  }
  rows.value = markDuplicates(preview.rows, recordStore.records)
})

function catIndex(row: ParsedImportRow) {
  return Math.max(0, categories.value.indexOf(row.selectedCategory))
}

function onCatChange(row: ParsedImportRow, e: { detail: { value: string } }) {
  row.selectedCategory = categories.value[Number(e.detail.value)]
}

function confirm() {
  const batchId = createId('batch')
  const items = rowsToRecords(rows.value, userStore.user?.id || 'guest', batchId)
  recordStore.addRecordsBatch(items)
  if (items.length > 0) useAchievementStore().checkImport()
  track('import_confirm', { count: items.length })
  uni.showToast({ title: `已导入 ${items.length} 条`, icon: 'success' })
  setTimeout(() => uni.switchTab({ url: '/pages/record/index' }), 600)
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; padding-bottom: 120rpx; }
.summary { display: block; margin-bottom: $spacing-md; font-size: $font-size-sm; color: $color-text-secondary; }
.row { margin-bottom: $spacing-sm; opacity: 1; }
.row.dup { opacity: 0.5; }
.top { display: flex; justify-content: space-between; font-size: $font-size-sm; }
.tag { color: $color-warning; font-size: $font-size-xs; }
.picker { font-size: $font-size-xs; color: $color-primary; margin-top: $spacing-xs; }
.confirm { position: fixed; bottom: 40rpx; left: $spacing-md; right: $spacing-md; background: $color-primary; color: #fff; border-radius: $radius-full; border: none; }
</style>
