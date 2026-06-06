/** AI 财务教练 — 仅传聚合数据，不传明细 */

export type CoachScene = 'weekly' | 'monthly' | 'qa'

export interface CategoryAgg {
  name: string
  amount: number
  percent: number
}

export interface WeeklyAggregate {
  weekLabel: string
  weekTotal: number
  lastWeekTotal: number
  diffPercent: number
  topCategories: CategoryAgg[]
}

export interface MonthlyAggregate {
  monthLabel: string
  income: number
  expense: number
  balance: number
  budgetAchievementRate: number
  optionalUsagePercent: number
  topCategories: CategoryAgg[]
}

export interface QaAggregate {
  monthExpense: number
  lastMonthExpense: number
  optionalUsagePercent: number
  topCategory: string
  topCategoryAmount: number
  recent3MonthTrend: { label: string; total: number }[]
}

export interface CoachRequest {
  scene: CoachScene
  weekly?: WeeklyAggregate
  monthly?: MonthlyAggregate
  qa?: QaAggregate
  question?: string
}

export interface CoachResponse {
  content: string
  scene: CoachScene
  generatedAt: number
}
