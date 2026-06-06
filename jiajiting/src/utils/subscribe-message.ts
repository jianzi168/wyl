import { getStorage, setStorage } from '@/utils/storage'

const PREF_KEY = 'remind_after_pay'
const TMPL_IDS_KEY = 'subscribe_tmpl_ids'

/** 支付后提醒补记 — 订阅消息模板 ID（需在微信公众平台配置后填入） */
export const SUBSCRIBE_TMPL_IDS = {
  recordRemind: import.meta.env.VITE_SUBSCRIBE_RECORD_TMPL || '',
}

export function isRemindAfterPayEnabled(): boolean {
  return getStorage(PREF_KEY, true)
}

export function setRemindAfterPayEnabled(enabled: boolean) {
  setStorage(PREF_KEY, enabled)
}

/** 请求订阅消息授权（微信小程序） */
export function requestRecordRemindSubscribe(): Promise<boolean> {
  if (!isRemindAfterPayEnabled()) return Promise.resolve(false)

  const tmplId = SUBSCRIBE_TMPL_IDS.recordRemind
  if (!tmplId) {
    console.log('[subscribe] 未配置模板 ID，跳过订阅')
    return Promise.resolve(false)
  }

  return new Promise((resolve) => {
    // #ifdef MP-WEIXIN
    uni.requestSubscribeMessage({
      tmplIds: [tmplId],
      success(res) {
        const accepted = (res as unknown as Record<string, string>)[tmplId] === 'accept'
        if (accepted) setStorage(TMPL_IDS_KEY, [tmplId])
        resolve(accepted)
      },
      fail: () => resolve(false),
    })
    // #endif
    // #ifndef MP-WEIXIN
    resolve(false)
    // #endif
  })
}

/** 记账成功后尝试订阅（每日最多提示一次） */
export async function trySubscribeAfterRecord() {
  const lastAsk = getStorage<number>('subscribe_ask_date', 0)
  const today = new Date().setHours(0, 0, 0, 0)
  if (lastAsk >= today) return
  const ok = await requestRecordRemindSubscribe()
  if (ok || SUBSCRIBE_TMPL_IDS.recordRemind) {
    setStorage('subscribe_ask_date', Date.now())
  }
}
