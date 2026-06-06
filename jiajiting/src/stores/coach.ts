import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CoachScene, CoachResponse } from '@/services/ai/types'
import {
  interpretCoach,
  buildWeeklyAggregate,
  buildMonthlyAggregate,
  buildQaAggregate,
} from '@/services/ai'
import { getStorage, setStorage } from '@/utils/storage'
import { useRecordStore } from '@/stores/record'
import { useBudgetStore } from '@/stores/budget'
import { useReportStore } from '@/stores/report'
import { track } from '@/utils/analytics'
import { isPremium } from '@/utils/subscription'
import { incrementStat } from '@/utils/usage-stats'

const HISTORY_KEY = 'coach_history'
const USAGE_KEY = 'coach_usage'

interface CoachMessage {
  id: string
  role: 'user' | 'coach'
  content: string
  scene: CoachScene
  createdAt: number
}

interface UsageState {
  weeklyKey: string
  monthlyKey: string
  qaWeekKey: string
  qaCount: number
}

function weekKey(d = new Date()) {
  const start = new Date(d)
  const day = start.getDay() || 7
  start.setDate(start.getDate() - day + 1)
  return start.toISOString().slice(0, 10)
}

function monthKey(d = new Date()) {
  return `${d.getFullYear()}-${d.getMonth() + 1}`
}

export const useCoachStore = defineStore('coach', () => {
  const messages = ref<CoachMessage[]>(getStorage<CoachMessage[]>(HISTORY_KEY, []))
  const usage = ref<UsageState>(
    getStorage<UsageState>(USAGE_KEY, {
      weeklyKey: '',
      monthlyKey: '',
      qaWeekKey: '',
      qaCount: 0,
    })
  )
  const loading = ref(false)

  function persist() {
    setStorage(HISTORY_KEY, messages.value)
    setStorage(USAGE_KEY, usage.value)
  }

  function pushMessage(msg: CoachMessage) {
    messages.value.unshift(msg)
    if (messages.value.length > 50) messages.value.pop()
    persist()
  }

  function canAskWeekly() {
    return usage.value.weeklyKey !== weekKey()
  }

  function canAskMonthly() {
    return usage.value.monthlyKey !== monthKey()
  }

  function canAskQa() {
    if (isPremium()) return true
    const wk = weekKey()
    if (usage.value.qaWeekKey !== wk) {
      usage.value.qaWeekKey = wk
      usage.value.qaCount = 0
      persist()
    }
    return usage.value.qaCount < 3
  }

  function afterCoachUse() {
    incrementStat('coach_usage_count')
    import('./achievement').then(({ useAchievementStore }) => {
      useAchievementStore().checkCoachUsage()
    })
  }

  function markWeeklyUsed() {
    usage.value.weeklyKey = weekKey()
    persist()
  }

  function markMonthlyUsed() {
    usage.value.monthlyKey = monthKey()
    persist()
  }

  function markQaUsed() {
    usage.value.qaCount += 1
    persist()
  }

  async function interpretWeekly(): Promise<CoachResponse> {
    if (!canAskWeekly()) throw new Error('本周已生成过周报解读')
    loading.value = true
    try {
      const records = useRecordStore().records
      const weekly = buildWeeklyAggregate(records)
      const res = await interpretCoach({ scene: 'weekly', weekly })
      markWeeklyUsed()
      pushMessage({ id: `c_${Date.now()}`, role: 'coach', content: res.content, scene: 'weekly', createdAt: res.generatedAt })
      track('coach_weekly', {})
      afterCoachUse()
      return res
    } finally {
      loading.value = false
    }
  }

  async function interpretMonthly(): Promise<CoachResponse> {
    if (!canAskMonthly()) throw new Error('本月已生成过月报解读')
    loading.value = true
    try {
      const recordStore = useRecordStore()
      const budgetStore = useBudgetStore()
      const reportStore = useReportStore()
      const monthly = buildMonthlyAggregate(
        recordStore.records,
        reportStore.selectedYear,
        reportStore.selectedMonth,
        budgetStore.totalMonthlyBudget,
        budgetStore.optionalUsagePercent
      )
      const res = await interpretCoach({ scene: 'monthly', monthly })
      markMonthlyUsed()
      pushMessage({ id: `c_${Date.now()}`, role: 'coach', content: res.content, scene: 'monthly', createdAt: res.generatedAt })
      track('coach_monthly', {})
      afterCoachUse()
      return res
    } finally {
      loading.value = false
    }
  }

  async function askQuestion(question: string): Promise<CoachResponse> {
    if (!question.trim()) throw new Error('请输入问题')
    if (!canAskQa()) throw new Error('本周问答已达 3 次上限')
    loading.value = true
    try {
      const recordStore = useRecordStore()
      const budgetStore = useBudgetStore()
      const qa = buildQaAggregate(recordStore.records)
      qa.optionalUsagePercent = budgetStore.optionalUsagePercent
      pushMessage({ id: `u_${Date.now()}`, role: 'user', content: question, scene: 'qa', createdAt: Date.now() })
      const res = await interpretCoach({ scene: 'qa', qa, question })
      markQaUsed()
      pushMessage({ id: `c_${Date.now()}`, role: 'coach', content: res.content, scene: 'qa', createdAt: res.generatedAt })
      track('coach_qa', {})
      afterCoachUse()
      return res
    } finally {
      loading.value = false
    }
  }

  return {
    messages,
    loading,
    canAskWeekly,
    canAskMonthly,
    canAskQa,
    qaRemaining: () => (isPremium() ? 99 : Math.max(0, 3 - usage.value.qaCount)),
    interpretWeekly,
    interpretMonthly,
    askQuestion,
  }
})
