import { predictCategory } from '@/constants/keywords'
import type { RecordNature } from '@/types/models'

export interface VoiceParseResult {
  amount: number
  category: string
  remark: string
  nature: RecordNature
}

const NATURE_KEYWORDS: Record<string, RecordNature> = {
  刚需: '刚需',
  可选: '可选',
  奢侈: '奢侈',
}

/** 解析语音/文本：「午饭38」「麦当劳68餐饮」「打车22可选」 */
export function parseVoiceText(text: string): VoiceParseResult | null {
  const raw = text.trim().replace(/[，,。.！!？?]/g, '')
  if (!raw) return null

  const amountMatch =
    raw.match(/(\d+(?:\.\d{1,2})?)\s*(?:元|块)?$/) ||
    raw.match(/(?:花了|消费|付了|支出)?\s*(\d+(?:\.\d{1,2})?)/)
  if (!amountMatch) return null

  const amount = parseFloat(amountMatch[1])
  if (!amount || amount <= 0) return null

  let nature: RecordNature = '可选'
  for (const [kw, val] of Object.entries(NATURE_KEYWORDS)) {
    if (raw.includes(kw)) {
      nature = val
      break
    }
  }

  const categoryNames = ['餐饮', '出行', '购物', '账单', '通讯', '娱乐', '服饰', '医疗', '人情', '其他']
  let category = categoryNames.find((c) => raw.includes(c)) || predictCategory(raw) || '其他'

  let remark = raw
    .replace(amountMatch[0], '')
    .replace(/元|块/g, '')
    .replace(/刚需|可选|奢侈/g, '')
    .replace(new RegExp(category), '')
    .trim()

  if (!remark) remark = raw.slice(0, 20)

  return { amount, category, remark, nature }
}
