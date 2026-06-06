import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Insight } from '@/types/models'
import type { InsightChannel, InsightPriority } from '@/types/auth'
import { getStorage, setStorage } from '@/utils/storage'
import { createId } from '@/utils/id'
import { INSIGHT_RULES } from '@/constants/insight-rules'
import { useInsightContext } from '@/composables/useInsights'

const KEY = 'insights_inbox'

export const useInsightStore = defineStore('insight', () => {
  const items = ref<Insight[]>(getStorage<Insight[]>(KEY, []))

  const unreadCount = computed(() => items.value.filter((i) => !i.readAt).length)

  function persist() {
    setStorage(KEY, items.value)
  }

  function pushInsight(payload: {
    ruleId: string
    title: string
    content: string
    priority: InsightPriority
    channel: InsightChannel
  }) {
    const dup = items.value.find(
      (i) => i.ruleId === payload.ruleId && !i.readAt && Date.now() - i.createdAt < 86400000
    )
    if (dup) return dup

    const item: Insight = {
      id: createId('ins'),
      userId: 'local',
      ruleId: payload.ruleId,
      title: payload.title,
      content: payload.content,
      priority: payload.priority,
      channel: payload.channel,
      createdAt: Date.now(),
    }
    items.value.unshift(item)
    if (items.value.length > 100) items.value.pop()
    persist()
    return item
  }

  function generateFromRules(channels?: InsightChannel[]) {
    const ctx = useInsightContext()
    const created: Insight[] = []
    for (const rule of INSIGHT_RULES) {
      if (channels && !channels.includes(rule.channel)) continue
      const result = rule.evaluate(ctx)
      if (!result) continue
      const item = pushInsight({
        ruleId: rule.id,
        title: result.title,
        content: result.content,
        priority: rule.priority,
        channel: rule.channel,
      })
      if (item) created.push(item)
    }
    return created
  }

  function markRead(id: string) {
    const item = items.value.find((i) => i.id === id)
    if (item && !item.readAt) {
      item.readAt = Date.now()
      persist()
    }
  }

  function markAllRead() {
    const now = Date.now()
    items.value.forEach((i) => {
      if (!i.readAt) i.readAt = now
    })
    persist()
  }

  function dismiss(id: string) {
    const item = items.value.find((i) => i.id === id)
    if (item) {
      item.dismissedAt = Date.now()
      item.readAt = item.readAt || Date.now()
      persist()
    }
  }

  function clearDismissed() {
    items.value = items.value.filter((i) => !i.dismissedAt)
    persist()
  }

  return {
    items,
    unreadCount,
    pushInsight,
    generateFromRules,
    markRead,
    markAllRead,
    dismiss,
    clearDismissed,
  }
})
