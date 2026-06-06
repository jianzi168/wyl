import { parseVoiceText } from '@/utils/voice-parser'
import type { VoiceParseResult } from '@/utils/voice-parser'

declare function requirePlugin(name: string): {
  getRecordRecognitionManager: () => RecordRecognitionManager
}

interface RecordRecognitionManager {
  onStart: (cb: () => void) => void
  onStop: (cb: (res: { result?: string }) => void) => void
  onError: (cb: (res: { msg: string }) => void) => void
  start: (opts: { duration: number; lang: string }) => void
  stop: () => void
}

let manager: RecordRecognitionManager | null = null
let pendingResolve: ((r: VoiceParseResult) => void) | null = null
let pendingReject: ((e: Error) => void) | null = null

function initManager(): RecordRecognitionManager | null {
  if (manager) return manager
  // #ifdef MP-WEIXIN
  try {
    const plugin = requirePlugin('WechatSI')
    const m = plugin.getRecordRecognitionManager()
    m.onStop((res) => {
      const parsed = parseVoiceText(res.result || '')
      if (parsed) pendingResolve?.(parsed)
      else pendingReject?.(new Error(`无法识别：「${res.result || '空'}」`))
      pendingResolve = null
      pendingReject = null
    })
    m.onError((res) => {
      pendingReject?.(new Error(res.msg || '语音识别失败'))
      pendingResolve = null
      pendingReject = null
    })
    manager = m
    return manager
  } catch {
    return null
  }
  // #endif
  return null
}

export function isVoicePluginAvailable(): boolean {
  return !!initManager()
}

/** 按住开始录音识别 */
export function beginVoiceCapture(): Promise<VoiceParseResult> {
  return new Promise((resolve, reject) => {
    pendingResolve = resolve
    pendingReject = reject
    const m = initManager()
    if (m) {
      m.start({ duration: 30000, lang: 'zh_CN' })
      return
    }
    pendingResolve = null
    pendingReject = null
    reject(new Error('NO_PLUGIN'))
  })
}

export function endVoiceCapture() {
  initManager()?.stop()
}

/** 无插件时的文本输入降级 */
export function promptVoiceFallback(): Promise<VoiceParseResult> {
  return new Promise((resolve, reject) => {
    uni.showModal({
      title: '语音记账',
      editable: true,
      placeholderText: '午饭38 / 麦当劳68餐饮',
      success(res) {
        if (!res.confirm || !res.content) {
          reject(new Error('已取消'))
          return
        }
        const parsed = parseVoiceText(res.content)
        if (parsed) resolve(parsed)
        else reject(new Error('无法解析，请说「项目+金额」'))
      },
      fail: () => reject(new Error('已取消')),
    })
  })
}
