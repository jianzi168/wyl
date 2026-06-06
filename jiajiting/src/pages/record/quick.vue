<template>
  <view class="page">
    <view class="nav">
      <text @tap="goBack">← 返回</text>
      <text class="title">{{ editId ? '编辑记录' : '记一笔' }}</text>
      <text class="link" @tap="goImport">导入</text>
    </view>

    <view class="amount-display">
      <text class="currency">¥</text>
      <text class="amount">{{ displayAmount }}</text>
    </view>
    <text class="category-hint">{{ selectedCategory }} · 上次</text>

    <scroll-view scroll-x class="category-row">
      <view
        v-for="cat in primaryCategories"
        :key="cat.id"
        class="category-chip"
        :class="{ active: selectedCategory === cat.name }"
        @tap="selectedCategory = cat.name"
      >
        <text>{{ cat.emoji }} {{ cat.name }}</text>
      </view>
      <view class="category-chip" @tap="showAllCategories = !showAllCategories">
        <text>··· {{ showAllCategories ? '收起' : '更多' }}</text>
      </view>
    </scroll-view>

    <view v-if="showAllCategories" class="category-expand">
      <view
        v-for="cat in extraCategories"
        :key="cat.id"
        class="category-chip small"
        :class="{ active: selectedCategory === cat.name }"
        @tap="selectedCategory = cat.name"
      >
        <text>{{ cat.emoji }} {{ cat.name }}</text>
      </view>
    </view>

    <view v-if="showMore" class="more-panel card">
      <text class="panel-label">性质</text>
      <view class="nature-row">
        <view
          v-for="n in natures"
          :key="n.value"
          class="nature-chip"
          :class="{ active: selectedNature === n.value }"
          @tap="selectedNature = n.value"
        >
          <text>{{ n.icon }} {{ n.value }}</text>
        </view>
      </view>
    </view>

    <view class="remark-row card">
      <input v-model="remark" class="remark-input" placeholder="备注（可选，如：永辉超市）" @input="onRemarkInput" />
      <view v-if="predictedCategory" class="predict">
        <text>💡 预判：{{ predictedCategory }}</text>
        <text class="predict-btn" @tap="applyPredict">采用</text>
      </view>
    </view>

    <text class="more-toggle" @tap="showMore = !showMore">{{ showMore ? '收起更多' : '更多选项' }}</text>

    <VoiceRecordButton @parsed="onVoiceParsed" />

    <NumberKeypad @press="onKey" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import NumberKeypad from '@/components/NumberKeypad.vue'
import VoiceRecordButton from '@/components/VoiceRecordButton.vue'
import type { VoiceParseResult } from '@/utils/voice-parser'
import { useRecordStore, useBudgetStore, useAchievementStore } from '@/stores'
import { ensureLoggedIn } from '@/utils/auth-guard'
import { predictCategory } from '@/constants/keywords'
import { DEFAULT_NATURE } from '@/constants/categories'
import type { RecordNature } from '@/types/models'

const recordStore = useRecordStore()
const budgetStore = useBudgetStore()

const amountStr = ref('')
const selectedCategory = ref(recordStore.lastCategory)
const selectedNature = ref<RecordNature>(recordStore.lastNature || DEFAULT_NATURE)
const remark = ref('')
const predictedCategory = ref<string | null>(null)
const showAllCategories = ref(false)
const showMore = ref(false)
const editId = ref('')

const natures = [
  { value: '刚需' as const, icon: '🔴' },
  { value: '可选' as const, icon: '🟡' },
  { value: '奢侈' as const, icon: '🟢' },
]

const visibleCategories = computed(() => budgetStore.categories.filter((c) => c.isVisible))
const primaryCategories = computed(() => visibleCategories.value.slice(0, 6))
const extraCategories = computed(() => visibleCategories.value.slice(6))

const displayAmount = computed(() => {
  if (!amountStr.value) return '0.00'
  return amountStr.value.includes('.')
    ? amountStr.value.padEnd(amountStr.value.indexOf('.') + 3, '0').slice(0, amountStr.value.indexOf('.') + 3)
    : `${amountStr.value}.00`
})

onLoad((query) => {
  if (!ensureLoggedIn()) return
  if (query?.category) selectedCategory.value = decodeURIComponent(query.category as string)
  if (query?.id) {
    editId.value = query.id as string
    const item = recordStore.getById(editId.value)
    if (!item) {
      uni.showToast({ title: '记录不存在', icon: 'none' })
      return
    }
    amountStr.value = String(item.amount)
    selectedCategory.value = item.category
    selectedNature.value = item.nature
    remark.value = item.remark || ''
  }
})

function onRemarkInput() {
  predictedCategory.value = predictCategory(remark.value)
}

function applyPredict() {
  if (predictedCategory.value) selectedCategory.value = predictedCategory.value
}

function onKey(key: string) {
  if (key === '✓') {
    submit()
    return
  }
  if (key === '.' && amountStr.value.includes('.')) return
  if (amountStr.value.length >= 10) return
  amountStr.value += key
}

function submit() {
  const amount = parseFloat(amountStr.value || '0')
  if (!amount || amount <= 0) {
    uni.showToast({ title: '请输入金额', icon: 'none' })
    return
  }

  const proceed = () => {
    if (editId.value) {
      recordStore.updateRecord(editId.value, {
        amount,
        category: selectedCategory.value,
        nature: selectedNature.value,
        remark: remark.value,
      })
      uni.showToast({ title: '已保存', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 400)
      return
    }
    const item = recordStore.addRecord({
      amount,
      category: selectedCategory.value,
      nature: selectedNature.value,
      remark: remark.value,
    })
    uni.redirectTo({ url: `/pages/record/feedback?id=${item.id}` })
  }

  if (budgetStore.wouldExceedCategory(selectedCategory.value, amount)) {
    const status = budgetStore.getCategoryStatus(selectedCategory.value)
    uni.showModal({
      title: '超支确认',
      content: `本月${selectedCategory.value}已超预算 ¥${Math.abs(status.remaining).toFixed(0)}，仍要记录吗？`,
      success(res) {
        if (res.confirm) proceed()
      },
    })
    return
  }
  proceed()
}

function goBack() {
  uni.navigateBack()
}

function goImport() {
  uni.navigateTo({ url: '/pages/record/import' })
}

function onVoiceParsed(result: VoiceParseResult) {
  amountStr.value = String(result.amount)
  selectedCategory.value = result.category
  selectedNature.value = result.nature
  remark.value = result.remark
  predictedCategory.value = result.category
  useAchievementStore().checkVoice()
  uni.showToast({ title: '已填入，请确认', icon: 'none' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { min-height: 100vh; padding: $spacing-md; background: $color-bg-page; padding-bottom: 40rpx; }
.nav { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-md; }
.title { font-weight: 600; }
.link { color: $color-primary; font-size: $font-size-sm; }
.amount-display { text-align: center; margin: 40rpx 0 $spacing-sm; }
.currency { font-size: $font-size-xl; color: $color-text-secondary; }
.amount { font-size: 72rpx; font-weight: 700; }
.category-hint { display: block; text-align: center; color: $color-text-muted; margin-bottom: $spacing-md; }
.category-row { white-space: nowrap; margin-bottom: $spacing-sm; }
.category-expand { display: flex; flex-wrap: wrap; gap: $spacing-xs; margin-bottom: $spacing-md; }
.category-chip { display: inline-block; padding: $spacing-sm $spacing-md; margin-right: $spacing-xs; background: $color-bg-card; border-radius: $radius-full; font-size: $font-size-sm; }
.category-chip.small { margin-bottom: $spacing-xs; }
.category-chip.active { background: $color-primary; color: #fff; }
.more-panel { margin-bottom: $spacing-sm; }
.panel-label { font-size: $font-size-sm; color: $color-text-secondary; display: block; margin-bottom: $spacing-xs; }
.nature-row { display: flex; gap: $spacing-sm; }
.nature-chip { padding: $spacing-xs $spacing-sm; border-radius: $radius-full; background: $color-bg-page; font-size: $font-size-xs; }
.nature-chip.active { background: rgba(108,92,231,0.15); color: $color-primary; }
.remark-row { margin-bottom: $spacing-sm; }
.remark-input { font-size: $font-size-sm; width: 100%; }
.predict { display: flex; justify-content: space-between; margin-top: $spacing-xs; font-size: $font-size-xs; color: $color-primary; }
.predict-btn { font-weight: 600; }
.more-toggle { display: block; text-align: center; color: $color-text-muted; font-size: $font-size-xs; margin: $spacing-sm 0; }
</style>
