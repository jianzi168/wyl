/** 成就徽章定义 */
export interface AchievementDef {
  code: string
  name: string
  emoji: string
  description: string
}

export const MVP_ACHIEVEMENTS: AchievementDef[] = [
  { code: 'rookie', name: '记账新秀', emoji: '🎊', description: '完成新手引导' },
  { code: 'first_record', name: '第一笔', emoji: '✏️', description: '完成首笔记账' },
  { code: 'streak_3', name: '初来乍到', emoji: '🔥', description: '连续记账 3 天' },
  { code: 'streak_7', name: '一周坚持', emoji: '🔥', description: '连续记账 7 天' },
  { code: 'streak_14', name: '半月坚持', emoji: '🔥', description: '连续记账 14 天' },
  { code: 'streak_30', name: '月度满勤', emoji: '🔥', description: '连续记账 30 天' },
  { code: 'streak_90', name: '季度坚守', emoji: '🔥', description: '连续记账 90 天' },
  { code: 'records_100', name: '数据觉醒', emoji: '📊', description: '累计记账 100 笔' },
  { code: 'budget_optional', name: '精打细算', emoji: '💎', description: '单月不超可选预算' },
  { code: 'spend_down', name: '越来越省', emoji: '📉', description: '连续 2 月支出环比下降' },
  { code: 'savings_goal', name: '说到做到', emoji: '🎯', description: '达成月度存款目标' },
  { code: 'import_first', name: '导入达人', emoji: '📥', description: '完成首次账单导入' },
  { code: 'voice_first', name: '语音先锋', emoji: '🎤', description: '首次语音记账' },
  { code: 'share_report', name: '分享达人', emoji: '📤', description: '分享报告 3 次' },
  { code: 'family_member', name: '家庭共享', emoji: '👨‍👩‍👧', description: '邀请家人加入家庭' },
  { code: 'premium_member', name: '高级会员', emoji: '⭐', description: '开通高级版订阅' },
  { code: 'coach_fan', name: '教练粉丝', emoji: '🤖', description: '使用 AI 教练 10 次' },
  { code: 'share_master', name: '分享达人+', emoji: '📣', description: '分享报告 10 次' },
  { code: 'family_team', name: '三口之家', emoji: '🏠', description: '家庭成员达 3 人' },
  { code: 'anniversary', name: '一周年', emoji: '🎂', description: '使用满 365 天' },
]

export const ALL_ACHIEVEMENTS = MVP_ACHIEVEMENTS
