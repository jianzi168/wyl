<template>
  <view class="page">
    <view class="brand">
      <text class="logo">家计通</text>
      <text class="slogan">记完就知道还能花多少</text>
    </view>

    <view class="features card">
      <text class="feature">✓ 每笔记账即时预算反馈</text>
      <text class="feature">✓ 微信登录，多设备同步</text>
      <text class="feature">✓ 离线记账，联网自动合并</text>
    </view>

    <button
      class="login-btn"
      :loading="userStore.authLoading"
      :disabled="userStore.authLoading"
      @tap="handleLogin"
    >
      微信一键登录
    </button>

    <text class="mode-hint">当前模式：{{ authModeLabel }}</text>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores'
import { useSyncStore } from '@/stores/sync'
import { appConfig } from '@/services/config'

const userStore = useUserStore()
const syncStore = useSyncStore()

const authModeLabel = computed(() =>
  appConfig.authMode === 'cloud' ? '云端同步' : '本地开发'
)

async function handleLogin() {
  try {
    await userStore.loginWithWechat()
    await syncStore.syncNow()

    if (userStore.needsPhoneBind) {
      uni.redirectTo({ url: '/pages/auth/bind-phone' })
      return
    }

    uni.switchTab({ url: '/pages/record/index' })
  } catch (err) {
    const message = err instanceof Error ? err.message : '登录失败'
    uni.showToast({ title: message, icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page {
  min-height: 100vh;
  padding: $spacing-xl $spacing-lg;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand {
  text-align: center;
  margin-bottom: $spacing-xl;
}

.logo {
  font-size: 64rpx;
  font-weight: 800;
  color: $color-primary;
  display: block;
}

.slogan {
  font-size: $font-size-md;
  color: $color-text-secondary;
  margin-top: $spacing-sm;
  display: block;
}

.features {
  margin-bottom: $spacing-xl;
}

.feature {
  display: block;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin-bottom: $spacing-sm;
}

.login-btn {
  background: $color-primary-gradient;
  color: #fff;
  border: none;
  border-radius: $radius-full;
  font-size: $font-size-lg;
  font-weight: 600;
}

.mode-hint {
  text-align: center;
  font-size: $font-size-xs;
  color: $color-text-muted;
  margin-top: $spacing-lg;
}
</style>
