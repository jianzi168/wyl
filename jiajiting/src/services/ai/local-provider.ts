import type { CoachRequest, CoachResponse } from './types'
import { formatMoney } from '@/utils/date'

function weeklyInterpret(req: CoachRequest): string {
  const w = req.weekly!
  const trend =
    w.diffPercent > 10
      ? `本周支出比上周多 ${w.diffPercent}%，需要关注消费节奏。`
      : w.diffPercent < -10
        ? `本周支出比上周少 ${Math.abs(w.diffPercent)}%，控制得不错。`
        : '本周支出与上周基本持平。'
  const top = w.topCategories[0]
  const topLine = top
    ? `支出最高分类是${top.name}（¥${formatMoney(top.amount)}，占 ${top.percent}%）。`
    : '本周暂无支出记录。'
  return `【周报解读 · ${w.weekLabel}】\n本周共支出 ¥${formatMoney(w.weekTotal)}，上周 ¥${formatMoney(w.lastWeekTotal)}。${trend}\n${topLine}\n建议：优先控制可选消费，把大额支出安排在预算充裕的分类里。`
}

function monthlyInterpret(req: CoachRequest): string {
  const m = req.monthly!
  const budgetLine =
    m.budgetAchievementRate >= 80
      ? `预算达成率 ${m.budgetAchievementRate}%，整体在可控范围内。`
      : `预算达成率 ${m.budgetAchievementRate}%，本月支出偏高，建议复盘可选消费。`
  const optionalLine =
    m.optionalUsagePercent > 80
      ? `可选预算已用 ${m.optionalUsagePercent}%，接近上限。`
      : `可选预算使用 ${m.optionalUsagePercent}%，仍有调整空间。`
  const top = m.topCategories[0]
  const topLine = top ? `最大支出分类：${top.name}（¥${formatMoney(top.amount)}）。` : ''
  return `【月报解读 · ${m.monthLabel}】\n收入 ¥${formatMoney(m.income)}，支出 ¥${formatMoney(m.expense)}，结余 ¥${formatMoney(m.balance)}。\n${budgetLine}${optionalLine}\n${topLine}\n下月建议：设定 1-2 个分类的硬性上限，每周复盘一次。`
}

function qaInterpret(req: CoachRequest): string {
  const q = (req.question || '').trim()
  const agg = req.qa!
  const diff = agg.lastMonthExpense
    ? Math.round(((agg.monthExpense - agg.lastMonthExpense) / agg.lastMonthExpense) * 100)
    : 0

  if (/为什么|怎么|为何/.test(q) && /花|支出|多/.test(q)) {
    return `本月支出 ¥${formatMoney(agg.monthExpense)}，比上月${diff >= 0 ? '多' : '少'} ${Math.abs(diff)}%。主要增长来自「${agg.topCategory}」分类（¥${formatMoney(agg.topCategoryAmount)}）。建议查看该分类近 4 周趋势，确认是否有可合并的重复消费。`
  }
  if (/省|节约|存钱/.test(q)) {
    return `近 3 月支出趋势：${agg.recent3MonthTrend.map((t) => `${t.label} ¥${formatMoney(t.total)}`).join(' → ')}。若要保持结余，建议把可选消费控制在月支出的 30% 以内，并设置每周消费上限提醒。`
  }
  if (/预算|超支/.test(q)) {
    return `当前可选预算使用率 ${agg.optionalUsagePercent}%。若已接近超支，优先暂停非刚需消费，把大额支出推迟到下月。`
  }
  return `根据近 3 月聚合数据：本月支出 ¥${formatMoney(agg.monthExpense)}，最高分类为「${agg.topCategory}」。你可以问「为什么本月花多了」「怎么省钱」「预算超支怎么办」等，我会基于聚合统计回答（不含商家明细）。`
}

export async function interpretLocal(req: CoachRequest): Promise<CoachResponse> {
  await new Promise((r) => setTimeout(r, 400))
  let content: string
  if (req.scene === 'weekly') content = weeklyInterpret(req)
  else if (req.scene === 'monthly') content = monthlyInterpret(req)
  else content = qaInterpret(req)
  return { content, scene: req.scene, generatedAt: Date.now() }
}
