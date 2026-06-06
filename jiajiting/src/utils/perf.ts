/** 大数据量性能优化 */

export const LARGE_DATA_THRESHOLD = 500
export const PAGE_SIZE = 50

export function isLargeDataset(count: number) {
  return count > LARGE_DATA_THRESHOLD
}

/** 分页切片 */
export function paginate<T>(items: T[], page: number, pageSize = PAGE_SIZE): T[] {
  return items.slice(0, page * pageSize)
}

/** 防抖（用于 canvas 重绘） */
export function debounce<T extends (...args: unknown[]) => void>(fn: T, ms = 120): T {
  let timer: ReturnType<typeof setTimeout> | null = null
  return ((...args: unknown[]) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), ms)
  }) as T
}
