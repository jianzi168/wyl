/** 商家关键词 → 分类预判规则 */
export const MERCHANT_KEYWORDS: Array<{ keywords: string[]; category: string }> = [
  { keywords: ['麦当劳', '肯德基', '沙县', '瑞幸', '喜茶', '奈雪', '星巴克', '外卖', '美团', '饿了么'], category: '餐饮' },
  { keywords: ['永辉', '沃尔玛', '盒马', '超市', '京东', '淘宝', '拼多多'], category: '购物' },
  { keywords: ['滴滴', '高德', '打车', '地铁', '公交', '加油', '停车'], category: '出行' },
  { keywords: ['房租', '物业', '水电', '燃气', '宽带'], category: '账单' },
  { keywords: ['移动', '联通', '电信', '话费'], category: '通讯' },
  { keywords: ['电影', '游戏', 'KTV', '网吧'], category: '娱乐' },
  { keywords: ['医院', '药店', '大药房'], category: '医疗' },
  { keywords: ['红包', '转账', '礼金'], category: '人情' },
]

export function predictCategory(remark: string): string | null {
  const text = remark.trim()
  if (!text) return null
  for (const rule of MERCHANT_KEYWORDS) {
    if (rule.keywords.some((k) => text.includes(k))) return rule.category
  }
  return null
}
