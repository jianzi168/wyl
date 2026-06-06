import type { Category } from '@/types/models'

/** 默认 10 个主分类 */
export const DEFAULT_CATEGORIES: Omit<Category, 'id' | 'userId'>[] = [
  { name: '餐饮', emoji: '🍜', iconColor: '#FF6B6B', budget: 1600, budgetWarnPercent: 80, isVisible: true, order: 0, subCategories: ['早餐', '午餐', '晚餐', '零食', '饮料'] },
  { name: '出行', emoji: '🚕', iconColor: '#45B7D1', budget: 500, budgetWarnPercent: 80, isVisible: true, order: 1, subCategories: ['打车', '公交地铁', '油费', '停车', '其他'] },
  { name: '购物', emoji: '🛒', iconColor: '#4ECDC4', budget: 800, budgetWarnPercent: 80, isVisible: true, order: 2, subCategories: ['生鲜', '日用品', '服装网买', '数码', '其他'] },
  { name: '账单', emoji: '💡', iconColor: '#96CEB4', budget: 2500, budgetWarnPercent: 80, isVisible: true, order: 3, subCategories: ['房租', '水电', '燃气', '物业', '其他'] },
  { name: '通讯', emoji: '📱', iconColor: '#BB8FCE', budget: 200, budgetWarnPercent: 80, isVisible: true, order: 4, subCategories: ['手机费', '宽带', '其他'] },
  { name: '娱乐', emoji: '🎮', iconColor: '#F7DC6F', budget: 300, budgetWarnPercent: 80, isVisible: true, order: 5, subCategories: ['电影', '游戏', '聚餐', '旅游', '其他'] },
  { name: '服饰', emoji: '👕', iconColor: '#DDA0DD', budget: 400, budgetWarnPercent: 80, isVisible: false, order: 6, subCategories: ['衣服', '鞋子', '护肤彩妆', '箱包', '其他'] },
  { name: '医疗', emoji: '🏥', iconColor: '#85C1E9', budget: 300, budgetWarnPercent: 80, isVisible: false, order: 7, subCategories: ['医院', '药店', '保健品', '其他'] },
  { name: '人情', emoji: '🎁', iconColor: '#F8B500', budget: 500, budgetWarnPercent: 80, isVisible: false, order: 8, subCategories: ['红包', '礼物', '请客', '医药费', '其他'] },
  { name: '其他', emoji: '···', iconColor: '#BDC3C7', budget: 500, budgetWarnPercent: 80, isVisible: true, order: 9, subCategories: [] },
]

export const DEFAULT_NATURE = '可选' as const
