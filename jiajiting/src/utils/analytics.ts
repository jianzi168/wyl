/** 核心埋点 — MVP 阶段 console + 本地计数 */

import { getStorage, setStorage } from '@/utils/storage'

type EventName =
  | 'record_create'
  | 'record_feedback_view'
  | 'budget_onboarding_complete'
  | 'import_confirm'
  | 'achievement_unlock'
  | 'export'
  | 'account_delete'
  | 'coach_weekly'
  | 'coach_monthly'
  | 'coach_qa'
  | 'family_create'
  | 'family_join'
  | 'alliance_open'
  | 'coupon_use'
  | 'loan_calc'
  | 'loan_sync_recurring'
  | 'premium_subscribe'

export function track(event: EventName, params?: Record<string, unknown>) {
  const key = 'analytics_log'
  const log = getStorage<Array<{ event: string; params?: Record<string, unknown>; at: number }>>(
    key,
    []
  )
  log.push({ event, params, at: Date.now() })
  if (log.length > 200) log.shift()
  setStorage(key, log)
  console.log('[analytics]', event, params || '')
}
