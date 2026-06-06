import type { RecordItem } from '@/types/models'
import { sumExpenses } from '@/utils/budget'

export interface SavingsTip {
  id: string
  title: string
  content: string
  category?: string
  dealId?: string
}

function monthRangeOffset(offset: number) {
  const now = new Date()
  const d = new Date(now.getFullYear(), now.getMonth() + offset, 1)
  const start = d.getTime()
  const end = new Date(d.getFullYear(), d.getMonth() + 1, 0, 23, 59, 59, 999).getTime()
  return { start, end, label: `${d.getMonth() + 1}月` }
}

/** 基于账单聚合的省钱洞察（无爬虫） */
export function evaluateSavingsTips(records: RecordItem[]): SavingsTip[] {
  const tips: SavingsTip[] = []
  const current = monthRangeOffset(0)
  const monthExpense = sumExpenses(records, current.start, current.end)

  const last3: number[] = []
  for (let i = 1; i <= 3; i++) {
    const r = monthRangeOffset(-i)
    last3.push(sumExpenses(records, r.start, r.end))
  }
  const avg3 = last3.length ? last3.reduce((a, b) => a + b, 0) / last3.length : 0

  if (avg3 > 0 && monthExpense > avg3 * 1.15) {
    const pct = Math.round(((monthExpense - avg3) / avg3) * 100)
    tips.push({
      id: 'spend_above_avg',
      title: '支出高于近期均值',
      content: `本月支出比近 3 个月均值高 ${pct}%，建议复盘可选消费。`,
    })
  }

  const foodStart = current.start
  const foodEnd = current.end
  const foodAmount = records
    .filter(
      (r) =>
        r.type === 'expense' &&
        r.category === '餐饮' &&
        r.timestamp >= foodStart &&
        r.timestamp <= foodEnd
    )
    .reduce((s, r) => s + r.amount, 0)

  if (foodAmount > 0) {
    const foodAvg =
      last3.reduce((s, _, i) => {
        const r = monthRangeOffset(-(i + 1))
        return (
          s +
          records
            .filter(
              (rec) =>
                rec.type === 'expense' &&
                rec.category === '餐饮' &&
                rec.timestamp >= r.start &&
                rec.timestamp <= r.end
            )
            .reduce((a, b) => a + b.amount, 0)
        )
      }, 0) / 3

    if (foodAvg > 0 && foodAmount > foodAvg * 1.2) {
      tips.push({
        id: 'food_high',
        title: '餐饮支出偏高',
        content: `本月餐饮 ¥${foodAmount.toFixed(0)}，比近 3 月均值高 ${Math.round(((foodAmount - foodAvg) / foodAvg) * 100)}%。`,
        category: '餐饮',
        dealId: 'meituan_food',
      })
    }
  }

  const shopRecords = records.filter(
    (r) => r.type === 'expense' && r.category === '购物' && r.timestamp >= current.start
  )
  if (shopRecords.length >= 5) {
    tips.push({
      id: 'shop_frequent',
      title: '购物频次较高',
      content: `本月已有 ${shopRecords.length} 笔购物记录，下单前可先列清单避免冲动消费。`,
      category: '购物',
      dealId: 'jd_shop',
    })
  }

  const day = new Date().getDay()
  if (day === 3) {
    tips.push({
      id: 'wednesday_tip',
      title: '周三会员日提醒',
      content: '部分商超周三有会员折扣，若常去超市可留意会员日优惠（公开信息，非价格承诺）。',
      dealId: 'market_wed',
    })
  }

  if (!tips.length && monthExpense > 0) {
    tips.push({
      id: 'keep_going',
      title: '消费习惯良好',
      content: '暂无异常支出模式，继续保持记账习惯可发现更多省钱机会。',
    })
  }

  return tips
}
