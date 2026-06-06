import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getStorage, setStorage } from '@/utils/storage'
import { MVP_ACHIEVEMENTS } from '@/constants/achievements'
import { useUserStore } from './user'
import { useRecordStore } from './record'
import { useBudgetStore } from './budget'
import { getShareCount } from '@/utils/share'
import { getStat } from '@/utils/usage-stats'
import { isPremium } from '@/utils/subscription'
import { sumExpenses } from '@/utils/budget'
import { track } from '@/utils/analytics'

const KEY = 'achievements_unlocked'
const STREAK_KEY = 'record_streak'

function dateKey(d = new Date()) {
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}

function monthRange(year: number, month: number) {
  const start = new Date(year, month - 1, 1).getTime()
  const end = new Date(year, month, 0, 23, 59, 59, 999).getTime()
  return { start, end }
}

export const useAchievementStore = defineStore('achievement', () => {
  const unlocked = ref<string[]>(getStorage<string[]>(KEY, []))
  const streak = ref(getStorage<{ days: number; lastDate: string }>(STREAK_KEY, { days: 0, lastDate: '' }))

  const list = computed(() =>
    MVP_ACHIEVEMENTS.map((a) => ({
      ...a,
      unlocked: unlocked.value.includes(a.code),
    }))
  )

  const unlockedCount = computed(() => unlocked.value.length)

  function persist() {
    setStorage(KEY, unlocked.value)
    setStorage(STREAK_KEY, streak.value)
  }

  function unlock(code: string) {
    if (unlocked.value.includes(code)) return
    unlocked.value.push(code)
    persist()
    track('achievement_unlock', { code })
    const name = MVP_ACHIEVEMENTS.find((a) => a.code === code)?.name
    uni.showToast({ title: `解锁成就：${name}`, icon: 'none' })
  }

  function updateStreak() {
    const today = dateKey()
    const yesterday = dateKey(new Date(Date.now() - 86400000))
    if (streak.value.lastDate === today) return
    if (streak.value.lastDate === yesterday) {
      streak.value.days += 1
    } else {
      streak.value.days = 1
    }
    streak.value.lastDate = today
    persist()
    if (streak.value.days >= 3) unlock('streak_3')
    if (streak.value.days >= 7) unlock('streak_7')
    if (streak.value.days >= 14) unlock('streak_14')
    if (streak.value.days >= 30) unlock('streak_30')
    if (streak.value.days >= 90) unlock('streak_90')
  }

  function checkRecordCount() {
    const count = useRecordStore().records.length
    if (count >= 100) unlock('records_100')
  }

  function checkSpendDown() {
    const records = useRecordStore().records
    const now = new Date()
    const m0 = monthRange(now.getFullYear(), now.getMonth() + 1)
    const m1d = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    const m1 = monthRange(m1d.getFullYear(), m1d.getMonth() + 1)
    const m2d = new Date(now.getFullYear(), now.getMonth() - 2, 1)
    const m2 = monthRange(m2d.getFullYear(), m2d.getMonth() + 1)
    const e0 = sumExpenses(records, m0.start, m0.end)
    const e1 = sumExpenses(records, m1.start, m1.end)
    const e2 = sumExpenses(records, m2.start, m2.end)
    if (e1 < e2 && e0 < e1) unlock('spend_down')
  }

  function checkSavingsGoal() {
    const user = useUserStore().user
    const recordStore = useRecordStore()
    if (!user?.savingsGoal || !user.monthlyIncome) return
    const saved = user.monthlyIncome - recordStore.monthExpenseTotal
    if (saved >= user.savingsGoal) unlock('savings_goal')
  }

  function checkAfterRecord() {
    const recordStore = useRecordStore()
    updateStreak()
    if (recordStore.records.length === 1) unlock('first_record')
    checkRecordCount()
    checkSpendDown()
    checkSavingsGoal()
  }

  function checkOnboarding() {
    unlock('rookie')
  }

  function checkBudgetMonthEnd() {
    const budgetStore = useBudgetStore()
    if (budgetStore.optionalUsagePercent <= 100) unlock('budget_optional')
  }

  function checkImport() {
    unlock('import_first')
  }

  function checkVoice() {
    unlock('voice_first')
  }

  function checkShare() {
    if (getShareCount() >= 3) unlock('share_report')
  }

  function checkFamilyJoin() {
    unlock('family_member')
  }

  function checkPremium() {
    if (isPremium()) unlock('premium_member')
  }

  function checkCoachUsage() {
    if (getStat('coach_usage_count') >= 10) unlock('coach_fan')
  }

  function checkShareMaster() {
    if (getShareCount() >= 10) unlock('share_master')
  }

  function checkFamilyTeam(memberCount: number) {
    if (memberCount >= 3) unlock('family_team')
  }

  function checkAnniversary() {
    const user = useUserStore().user
    if (!user?.createdAt) return
    const days = (Date.now() - user.createdAt) / 86400000
    if (days >= 365) unlock('anniversary')
  }

  function runPeriodicChecks() {
    checkAnniversary()
    checkPremium()
    checkCoachUsage()
    checkShareMaster()
    checkBudgetMonthEnd()
  }

  return {
    list,
    unlockedCount,
    streak,
    unlock,
    checkAfterRecord,
    checkOnboarding,
    checkBudgetMonthEnd,
    checkImport,
    checkVoice,
    checkShare,
    checkFamilyJoin,
    checkPremium,
    checkCoachUsage,
    checkShareMaster,
    checkFamilyTeam,
    checkAnniversary,
    runPeriodicChecks,
  }
})
