import { createId } from '@/utils/id'
import type { RecordItem, RecordNature, ImportSource } from '@/types/models'
import { DEFAULT_NATURE } from '@/constants/categories'
import { predictCategory } from '@/constants/keywords'

export type ParsedImportRow = {
  tempId: string
  amount: number
  remark: string
  timestamp: number
  suggestedCategory: string
  selectedCategory: string
  nature: RecordNature
  duplicate: boolean
}

function parseAmount(raw: string): number {
  const n = parseFloat(raw.replace(/[¥,\s]/g, ''))
  return Math.abs(n)
}

function parseWechatCsvLine(cols: string[]): ParsedImportRow | null {
  const timeStr = cols[0]
  const type = cols[1]
  const merchant = cols[2] || cols[3] || ''
  const direction = cols[4]
  const amountStr = cols[5]
  if (!amountStr || direction !== '支出') return null
  const amount = parseAmount(amountStr)
  if (!amount) return null
  const ts = new Date(timeStr.replace(/\//g, '-')).getTime() || Date.now()
  const remark = merchant
  const cat = predictCategory(remark) || '其他'
  return {
    tempId: createId('imp'),
    amount,
    remark,
    timestamp: ts,
    suggestedCategory: cat,
    selectedCategory: cat,
    nature: DEFAULT_NATURE,
    duplicate: false,
  }
}

function parseAlipayCsvLine(cols: string[]): ParsedImportRow | null {
  const timeStr = cols[0]
  const direction = cols[5] || cols[4]
  const amountStr = cols[6] || cols[5]
  const merchant = cols[7] || cols[2] || ''
  if (!String(direction).includes('支出')) return null
  const amount = parseAmount(String(amountStr))
  if (!amount) return null
  const ts = new Date(timeStr.replace(/\//g, '-')).getTime() || Date.now()
  const remark = merchant
  const cat = predictCategory(remark) || '其他'
  return {
    tempId: createId('imp'),
    amount,
    remark,
    timestamp: ts,
    suggestedCategory: cat,
    selectedCategory: cat,
    nature: DEFAULT_NATURE,
    duplicate: false,
  }
}

export function parseCsvContent(content: string, source: ImportSource): ParsedImportRow[] {
  const lines = content.split(/\r?\n/).filter((l) => l.trim())
  if (lines.length < 2) return []

  const results: ParsedImportRow[] = []
  let dataStart = 0
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].includes('交易时间') || lines[i].includes('交易号')) {
      dataStart = i + 1
      break
    }
  }

  for (let i = dataStart; i < lines.length; i++) {
    const cols = lines[i].split(',').map((c) => c.trim().replace(/^"|"$/g, ''))
    const row = source === 'alipay' ? parseAlipayCsvLine(cols) : parseWechatCsvLine(cols)
    if (row) results.push(row)
  }
  return results
}

export function parsePasteText(text: string): ParsedImportRow[] {
  const lines = text.split(/\r?\n/).filter((l) => l.trim())
  const results: ParsedImportRow[] = []

  for (const line of lines) {
    const amountMatch = line.match(/[-+]?\s*¥?\s*(\d+\.?\d*)\s*$/) || line.match(/(\d+\.?\d*)\s*元/)
    if (!amountMatch) continue
    const amount = parseAmount(amountMatch[1])
    const remark = line.replace(amountMatch[0], '').replace(/\d{2}[-/]\d{2}/, '').trim() || '导入消费'
    const dateMatch = line.match(/(\d{4}[-/]\d{2}[-/]\d{2})|(\d{2}[-/]\d{2})/)
    let ts = Date.now()
    if (dateMatch) {
      const d = dateMatch[0]
      ts = d.length <= 5
        ? new Date(`${new Date().getFullYear()}-${d.replace('/', '-')}`).getTime()
        : new Date(d.replace(/\//g, '-')).getTime()
    }
    const cat = predictCategory(remark) || '其他'
    results.push({
      tempId: createId('imp'),
      amount,
      remark,
      timestamp: ts || Date.now(),
      suggestedCategory: cat,
      selectedCategory: cat,
      nature: DEFAULT_NATURE,
      duplicate: false,
    })
  }
  return results
}

export function markDuplicates(
  rows: ParsedImportRow[],
  existing: RecordItem[]
): ParsedImportRow[] {
  return rows.map((row) => {
    const dup = existing.some((r) => {
      const timeClose = Math.abs(r.timestamp - row.timestamp) < 5 * 60 * 1000
      const amountMatch = Math.abs(r.amount - row.amount) < 0.01
      const remarkMatch = (r.remark || '').includes(row.remark) || row.remark.includes(r.remark || '')
      return timeClose && amountMatch && remarkMatch
    })
    return { ...row, duplicate: dup }
  })
}

export function rowsToRecords(
  rows: ParsedImportRow[],
  userId: string,
  batchId: string
): RecordItem[] {
  const now = Date.now()
  return rows
    .filter((r) => !r.duplicate)
    .map((row) => ({
      id: createId('rec'),
      userId,
      amount: row.amount,
      type: 'expense' as const,
      category: row.selectedCategory,
      nature: row.nature,
      remark: row.remark,
      source: 'import' as const,
      importBatchId: batchId,
      timestamp: row.timestamp,
      isRecurring: false,
      createdAt: now,
      updatedAt: now,
    }))
}
