<template>
  <view class="page">
    <text class="title">绑定手机号</text>
    <text class="desc">用于多设备同步与账号安全，绑定后可跨设备找回数据</text>

    <!-- 云端模式：微信授权手机号 -->
  <button
      v-if="isCloud"
      class="bind-btn primary"
      open-type="getPhoneNumber"
      @getphonenumber="onGetPhoneNumber"
    >
      微信授权手机号
    </button>

    <!-- 本地模式：手动输入 -->
    <view v-else class="form card">
      <input
        v-model="phone"
        class="input"
        type="number"
        maxlength="11"
        placeholder="请输入手机号"
      />
      <button class="bind-btn primary" @tap="bindManual">确认绑定</button>
    </view>

    <button class="bind-btn ghost" @tap="skip">暂时跳过</button>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores'
import { isCloudMode } from '@/services/config'

const userStore = useUserStore()
const phone = ref('')
const isCloud = computed(() => isCloudMode())

async function onGetPhoneNumber(e: { detail: Record<string, string | undefined> }) {
  const detail = e.detail
  if (detail.errMsg !== 'getPhoneNumber:ok') {
    uni.showToast({ title: '需要授权手机号', icon: 'none' })
    return
  }

  try {
    await userStore.bindPhone({
      code: detail.code,
      encryptedData: detail.encryptedData,
      iv: detail.iv,
    })
    uni.showToast({ title: '绑定成功', icon: 'success' })
    goHome()
  } catch (err) {
    const message = err instanceof Error ? err.message : '绑定失败'
    uni.showToast({ title: message, icon: 'none' })
  }
}

async function bindManual() {
  try {
    await userStore.bindPhone({ phone: phone.value })
    uni.showToast({ title: '绑定成功', icon: 'success' })
    goHome()
  } catch (err) {
    const message = err instanceof Error ? err.message : '绑定失败'
    uni.showToast({ title: message, icon: 'none' })
  }
}

function skip() {
  goHome()
}

function goHome() {
  uni.switchTab({ url: '/pages/record/index' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page {
  min-height: 100vh;
  padding: $spacing-xl $spacing-lg;
}

.title {
  font-size: $font-size-xl;
  font-weight: 700;
  display: block;
}

.desc {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin: $spacing-sm 0 $spacing-xl;
  display: block;
  line-height: 1.6;
}

.form {
  margin-bottom: $spacing-md;
}

.input {
  background: $color-bg-page;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-md;
  font-size: $font-size-md;
}

.bind-btn {
  border-radius: $radius-full;
  font-size: $font-size-md;
  margin-bottom: $spacing-sm;
}

.bind-btn.primary {
  background: $color-primary;
  color: #fff;
  border: none;
}

.bind-btn.ghost {
  background: transparent;
  color: $color-text-muted;
  border: none;
}
</style>
