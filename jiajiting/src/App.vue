<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useSyncStore } from '@/stores/sync'
import { useRecurringStore } from '@/stores/recurring'
import { useInsightStore } from '@/stores/insight'
import { useAchievementStore } from '@/stores/achievement'
import { getStorage, setStorage } from '@/utils/storage'

function ensurePrivacyAccepted() {
  if (getStorage('privacy_accepted', false)) return
  uni.showModal({
    title: '隐私政策',
    content: '使用家计通即表示您同意我们的隐私政策。记账数据用于预算分析，不会出售给第三方。',
    confirmText: '同意',
    showCancel: false,
    success() {
      setStorage('privacy_accepted', true)
    },
  })
}

onLaunch(async () => {
  ensurePrivacyAccepted()
  const userStore = useUserStore()
  const syncStore = useSyncStore()
  const recurringStore = useRecurringStore()

  const restored = await userStore.restoreSession()
  if (restored) {
    recurringStore.runDueItems()
    await syncStore.syncNow()
    useInsightStore().generateFromRules()
    useAchievementStore().runPeriodicChecks()
  }
})

onShow(() => {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) return

  const pages = getCurrentPages()
  const route = pages[pages.length - 1]?.route || ''
  if (route.includes('auth/')) return

  useRecurringStore().runDueItems()
  useSyncStore().syncNow()
  useInsightStore().generateFromRules()
  useAchievementStore().runPeriodicChecks()
})

uni.onNetworkStatusChange((res) => {
  if (res.isConnected && useUserStore().isLoggedIn) {
    useSyncStore().syncNow()
  }
})
</script>

<style lang="scss">
@import '@/styles/common.scss';
</style>
