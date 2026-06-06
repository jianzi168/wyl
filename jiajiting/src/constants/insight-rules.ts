import type { InsightPriority, InsightChannel } from '@/types/auth'

export interface InsightRule {
  id: string
  priority: InsightPriority
  channel: InsightChannel
  evaluate: (ctx: InsightContext) => InsightResult | null
}

export interface InsightContext {
  monthExpenseTotal: number
  lastMonthSamePeriodTotal: number
  categorySpent: Record<string, number>
  categoryBudget: Record<string, number>
  optionalUsagePercent: number
  optionalRemaining: number
  lastRecordCategory?: string
  lastRecordAmount?: number
}

export interface InsightResult {
  title: string
  content: string
}

export const INSIGHT_RULES: InsightRule[] = [
  {
    id: 'optional_critical',
    priority: 'critical',
    channel: 'inline',
    evaluate(ctx) {
      if (ctx.optionalUsagePercent >= 100) {
        return { title: '可选预算已用完', content: '本月可选支出已达上限，建议控制非必要消费' }
      }
      return null
    },
  },
  {
    id: 'optional_warn',
    priority: 'high',
    channel: 'card',
    evaluate(ctx) {
      if (ctx.optionalUsagePercent >= 80 && ctx.optionalUsagePercent < 100) {
        return { title: '可选预算预警', content: `可选预算已用 ${Math.round(ctx.optionalUsagePercent)}%，剩余 ¥${ctx.optionalRemaining.toFixed(0)}` }
      }
      return null
    },
  },
  {
    id: 'category_over_80',
    priority: 'high',
    channel: 'inline',
    evaluate(ctx) {
      if (!ctx.lastRecordCategory) return null
      const spent = ctx.categorySpent[ctx.lastRecordCategory] || 0
      const budget = ctx.categoryBudget[ctx.lastRecordCategory] || 0
      if (!budget) return null
      const pct = (spent / budget) * 100
      if (pct >= 80) {
        return { title: `${ctx.lastRecordCategory}预算预警`, content: `本月已用 ${Math.round(pct)}%，剩余 ¥${Math.max(0, budget - spent).toFixed(0)}` }
      }
      return null
    },
  },
  {
    id: 'spend_less_than_last_month',
    priority: 'low',
    channel: 'card',
    evaluate(ctx) {
      if (ctx.lastMonthSamePeriodTotal > 0 && ctx.monthExpenseTotal < ctx.lastMonthSamePeriodTotal) {
        const saved = ctx.lastMonthSamePeriodTotal - ctx.monthExpenseTotal
        return { title: '比上月同期少花', content: `目前已少花 ¥${saved.toFixed(0)}，继续保持 👍` }
      }
      return null
    },
  },
  {
    id: 'first_record',
    priority: 'low',
    channel: 'card',
    evaluate(ctx) {
      if (ctx.monthExpenseTotal === 0) {
        return { title: '开始记账', content: '记完第一笔，立刻看到预算反馈' }
      }
      return null
    },
  },
]

const PRIORITY_ORDER = { critical: 0, high: 1, medium: 2, low: 3 }

export function pickInsight(ctx: InsightContext, channel?: InsightChannel): InsightResult & { ruleId: string } | null {
  const matches = INSIGHT_RULES.map((rule) => {
    if (channel && rule.channel !== channel) return null
    const result = rule.evaluate(ctx)
    if (!result) return null
    return { ...result, ruleId: rule.id, priority: rule.priority }
  }).filter(Boolean) as Array<InsightResult & { ruleId: string; priority: InsightPriority }>

  if (!matches.length) return null
  matches.sort((a, b) => PRIORITY_ORDER[a.priority as keyof typeof PRIORITY_ORDER] - PRIORITY_ORDER[b.priority as keyof typeof PRIORITY_ORDER])
  const top = matches[0]
  return { title: top.title, content: top.content, ruleId: top.ruleId }
}
