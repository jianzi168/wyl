/** 家计通核心数据模型 — 对齐设计文档 v2.0 */

export type RecordType = 'expense' | 'income'
export type RecordNature = '刚需' | '可选' | '奢侈'
export type RecordSource = 'manual' | 'import' | 'recurring'
export type RepeatRule = 'monthly' | 'weekly' | 'yearly'
export type ImportSource = 'wechat' | 'alipay' | 'paste'
export type ImportStatus = 'pending' | 'confirmed' | 'failed'
export type InsightPriority = 'low' | 'medium' | 'high' | 'critical'
export type InsightChannel = 'inline' | 'card' | 'inbox' | 'push'

export interface User {
  id: string
  wxOpenId: string
  phone?: string
  nickname: string
  avatar: string
  monthlyIncome?: number
  savingsGoal?: number
  onboardingCompleted: boolean
  createdAt: number
}

export interface RecordItem {
  id: string
  userId: string
  amount: number
  type: RecordType
  category: string
  subCategory?: string
  nature: RecordNature
  remark?: string
  aiTag?: string
  source: RecordSource
  importBatchId?: string
  timestamp: number
  isRecurring: boolean
  recurringId?: string
  memberId?: string
  createdAt: number
  updatedAt: number
}

export interface Category {
  id: string
  userId: string
  name: string
  emoji: string
  iconColor: string
  budget: number
  budgetWarnPercent: number
  isVisible: boolean
  order: number
  subCategories: string[]
}

export interface OptionalBudget {
  userId: string
  year: number
  annualLimit: number
  warnPercent: number
  currentUsage: number
}

export interface RecurringItem {
  id: string
  userId: string
  name: string
  amount: number
  type: RecordType
  category: string
  repeatRule: RepeatRule
  dayOfMonth: number
  isActive: boolean
  lastTriggered?: number
  nextTrigger: number
}

export interface Achievement {
  id: string
  userId: string
  code: string
  unlockedAt: number
  notified: boolean
}

export interface ImportBatch {
  id: string
  userId: string
  source: ImportSource
  fileName?: string
  totalCount: number
  importedCount: number
  status: ImportStatus
  createdAt: number
}

export interface Insight {
  id: string
  userId: string
  ruleId: string
  title: string
  content: string
  priority: InsightPriority
  channel: InsightChannel
  readAt?: number
  dismissedAt?: number
  createdAt: number
}
