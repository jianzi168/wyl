/** 预算模板：分类名 → 占月收入比例 */
export const BUDGET_TEMPLATES = {
  worker: {
    label: '工薪族',
    ratios: { 餐饮: 0.2, 出行: 0.08, 购物: 0.1, 账单: 0.25, 通讯: 0.03, 娱乐: 0.05, 服饰: 0.05, 医疗: 0.02, 人情: 0.05, 其他: 0.05 },
    optionalRatio: 0.15,
  },
  renter: {
    label: '租房党',
    ratios: { 餐饮: 0.18, 出行: 0.06, 购物: 0.08, 账单: 0.35, 通讯: 0.03, 娱乐: 0.04, 服饰: 0.04, 医疗: 0.02, 人情: 0.05, 其他: 0.05 },
    optionalRatio: 0.12,
  },
} as const

export type BudgetTemplateKey = keyof typeof BUDGET_TEMPLATES
