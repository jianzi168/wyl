<template>
  <view class="page">
    <view class="tabs">
      <text :class="{ active: mode === 'paste' }" @tap="mode = 'paste'">粘贴文本</text>
      <text :class="{ active: mode === 'wechat' }" @tap="mode = 'wechat'">微信 CSV</text>
      <text :class="{ active: mode === 'alipay' }" @tap="mode = 'alipay'">支付宝 CSV</text>
    </view>

    <textarea
      v-if="mode === 'paste'"
      v-model="pasteText"
      class="textarea"
      placeholder="粘贴账单文本，每行一条&#10;例：05-23 麦当劳 -38.00"
    />

    <view v-else class="upload card" @tap="chooseFile">
      <text>{{ fileName || '点击选择 CSV 文件' }}</text>
    </view>

    <button class="parse" @tap="parse">解析预览</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { parseCsvContent, parsePasteText } from '@/utils/import-parser'
import type { ImportSource } from '@/types/models'
import { setStorage } from '@/utils/storage'

const mode = ref<'paste' | 'wechat' | 'alipay'>('paste')
const pasteText = ref('')
const fileName = ref('')
const fileContent = ref('')

function chooseFile() {
  // #ifdef H5
  uni.chooseFile({
    count: 1,
    extension: ['.csv'],
    success(res) {
      const files = res.tempFiles as Array<{ name: string }>
      const f = files[0]
      if (!f) return
      fileName.value = f.name
      const reader = new FileReader()
      reader.onload = () => {
        fileContent.value = String(reader.result || '')
      }
      reader.readAsText(f as unknown as Blob)
    },
  })
  // #endif
  // #ifndef H5
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    extension: ['csv'],
    success(res) {
      const f = res.tempFiles[0]
      fileName.value = f.name
      uni.getFileSystemManager().readFile({
        filePath: f.path,
        encoding: 'utf-8',
        success(r) {
          fileContent.value = String(r.data || '')
        },
      })
    },
  })
  // #endif
}

function parse() {
  const source: ImportSource = mode.value === 'alipay' ? 'alipay' : mode.value === 'wechat' ? 'wechat' : 'paste'
  let rows =
    mode.value === 'paste'
      ? parsePasteText(pasteText.value)
      : parseCsvContent(fileContent.value, source)

  if (!rows.length) {
    uni.showToast({ title: '未解析到记录', icon: 'none' })
    return
  }
  setStorage('import_preview', { rows, source, fileName: fileName.value || 'paste' })
  uni.navigateTo({ url: '/pages/record/import-confirm' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.tabs { display: flex; gap: $spacing-md; margin-bottom: $spacing-md; font-size: $font-size-sm; }
.tabs .active { color: $color-primary; font-weight: 600; }
.textarea { width: 100%; height: 400rpx; background: $color-bg-card; border-radius: $radius-md; padding: $spacing-md; font-size: $font-size-sm; }
.upload { padding: $spacing-xl; text-align: center; color: $color-text-muted; margin-bottom: $spacing-md; }
.parse { background: $color-primary; color: #fff; border-radius: $radius-full; border: none; margin-top: $spacing-md; }
</style>
