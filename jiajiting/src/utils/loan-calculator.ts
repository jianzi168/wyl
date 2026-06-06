export type LoanType = 'mortgage' | 'car'
export type RepayMethod = 'equal_payment' | 'equal_principal'

export interface LoanInput {
  principal: number
  annualRate: number
  months: number
  method: RepayMethod
}

export interface LoanScheduleRow {
  month: number
  payment: number
  principal: number
  interest: number
  balance: number
}

export interface LoanResult {
  monthlyPayment: number
  totalPayment: number
  totalInterest: number
  schedule: LoanScheduleRow[]
}

/** 等额本息 */
function calcEqualPayment(input: LoanInput): LoanResult {
  const { principal, annualRate, months } = input
  const monthlyRate = annualRate / 100 / 12
  const payment =
    monthlyRate === 0
      ? principal / months
      : (principal * monthlyRate * Math.pow(1 + monthlyRate, months)) /
        (Math.pow(1 + monthlyRate, months) - 1)

  const schedule: LoanScheduleRow[] = []
  let balance = principal
  let totalInterest = 0

  for (let m = 1; m <= months; m++) {
    const interest = balance * monthlyRate
    const principalPart = payment - interest
    balance = Math.max(0, balance - principalPart)
    totalInterest += interest
    schedule.push({
      month: m,
      payment: round(payment),
      principal: round(principalPart),
      interest: round(interest),
      balance: round(balance),
    })
  }

  return {
    monthlyPayment: round(payment),
    totalPayment: round(payment * months),
    totalInterest: round(totalInterest),
    schedule,
  }
}

/** 等额本金 */
function calcEqualPrincipal(input: LoanInput): LoanResult {
  const { principal, annualRate, months } = input
  const monthlyRate = annualRate / 100 / 12
  const principalPerMonth = principal / months

  const schedule: LoanScheduleRow[] = []
  let balance = principal
  let totalPayment = 0
  let totalInterest = 0
  let firstPayment = 0

  for (let m = 1; m <= months; m++) {
    const interest = balance * monthlyRate
    const payment = principalPerMonth + interest
    if (m === 1) firstPayment = payment
    balance = Math.max(0, balance - principalPerMonth)
    totalPayment += payment
    totalInterest += interest
    schedule.push({
      month: m,
      payment: round(payment),
      principal: round(principalPerMonth),
      interest: round(interest),
      balance: round(balance),
    })
  }

  return {
    monthlyPayment: round(firstPayment),
    totalPayment: round(totalPayment),
    totalInterest: round(totalInterest),
    schedule,
  }
}

function round(n: number) {
  return Math.round(n * 100) / 100
}

export function calculateLoan(input: LoanInput): LoanResult {
  if (!input.principal || input.principal <= 0) throw new Error('请输入有效贷款金额')
  if (!input.months || input.months <= 0) throw new Error('请输入有效期数')
  if (input.annualRate < 0) throw new Error('利率不能为负')

  return input.method === 'equal_principal'
    ? calcEqualPrincipal(input)
    : calcEqualPayment(input)
}
