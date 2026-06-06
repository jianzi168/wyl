/** 官方联盟 Deep Link（无爬虫、无价格承诺） */

export interface AllianceDeal {
  id: string
  title: string
  subtitle: string
  category: string
  emoji: string
  /** 小程序跳转或 H5 外链 */
  link: string
  linkType: 'mini_program' | 'webview'
  appId?: string
}

export const ALLIANCE_DEALS: AllianceDeal[] = [
  {
    id: 'meituan_food',
    title: '美团外卖',
    subtitle: '餐饮消费后可领平台优惠',
    category: '餐饮',
    emoji: '🍜',
    link: 'https://i.meituan.com',
    linkType: 'webview',
  },
  {
    id: 'jd_shop',
    title: '京东购物',
    subtitle: '官方活动入口',
    category: '购物',
    emoji: '🛒',
    link: 'https://m.jd.com',
    linkType: 'webview',
  },
  {
    id: 'market_wed',
    title: '超市会员日',
    subtitle: '周三会员日资讯（公开信息）',
    category: '购物',
    emoji: '🏪',
    link: 'https://www.walmart.cn',
    linkType: 'webview',
  },
  {
    id: 'didi_travel',
    title: '出行优惠',
    subtitle: '打车平台官方活动页',
    category: '出行',
    emoji: '🚕',
    link: 'https://www.didiglobal.com',
    linkType: 'webview',
  },
]
