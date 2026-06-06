import type { RecordItem } from '@/types/models'
import { formatDateTime } from '@/utils/date'

export function recordsToCsv(records: RecordItem[]): string {
  const header = '时间,类型,分类,性质,金额,备注'
  const lines = records.map((r) =>
    [
      formatDateTime(r.timestamp),
      r.type === 'expense' ? '支出' : '收入',
      r.category,
      r.nature,
      r.amount.toFixed(2),
      (r.remark || '').replace(/,/g, ' '),
    ].join(',')
  )
  return [header, ...lines].join('\n')
}

export function downloadCsvInH5(filename: string, content: string) {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export function exportRecords(records: RecordItem[]) {
  const csv = recordsToCsv(records)
  const filename = `家计通账单_${Date.now()}.csv`

  // #ifdef H5
  downloadCsvInH5(filename, csv)
  uni.showToast({ title: '已导出', icon: 'success' })
  // #endif

  // #ifndef H5
  uni.setClipboardData({
    data: csv,
    success: () => uni.showToast({ title: '已复制到剪贴板', icon: 'success' }),
  })
  // #endif
}
