/** 生成本地唯一 ID（MVP 阶段，后续可换 UUID */
export function createId(prefix = 'id'): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`
}
