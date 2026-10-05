export type MonthStatus = 'PAID' | 'PARTIAL' | 'UNPAID' | 'ADVANCE';

export interface Member {
  id: string;
  lastName: string;
  firstName: string;
  phone: string;
  joinMonth: string; // "YYYY-MM"
  isActive: boolean;
  customMonthlyAmount?: number | null;
  note: string;
  createdAt: number;
}

export interface MonthlyFeeRate {
  id: string;
  effectiveFromMonth: string; // "YYYY-MM"
  amount: number;
}

export interface PaymentAllocation {
  id: string;
  paymentId: string;
  memberId: string;
  month: string; // "YYYY-MM"
  amount: number;
}

export interface Payment {
  id: string;
  memberId: string;
  date: string; // "YYYY-MM-DD"
  amount: number;
  note: string;
  createdAt: number;
  allocations?: PaymentAllocation[];
}

export interface IncomeCategory {
  id: string;
  name: string;
  isActive: boolean;
}

export interface OtherIncome {
  id: string;
  categoryId: string;
  memberId?: string | null;
  date: string; // "YYYY-MM-DD"
  amount: number;
  description: string;
  createdAt: number;
}

export interface Remittance {
  id: string;
  date: string; // "YYYY-MM-DD"
  amount: number;
  recipient: string;
  coveredPeriod?: string;
  note: string;
  createdAt: number;
}

export type ReminderChannel = 'WHATSAPP' | 'SMS' | 'CALL';

export interface ReminderLog {
  id: string;
  memberId: string;
  date: string; // "YYYY-MM-DD HH:mm"
  channel: ReminderChannel;
  amountDueAtTime: number;
  createdAt: number;
}

export interface AppSettings {
  associationName: string;
  currency: string;
  defaultCountryPrefix: string;
  reminderMessageTemplate: string;
  minReminderIntervalDays: number;
  lateThresholdMonths: number;
  reminderNotificationDay: number;
  reminderNotificationHour: number;
  reminderNotificationsEnabled: boolean;
  remittanceReminderThreshold: number;
  remittanceReminderDaysInterval: number;
  lastRemittanceRecipient: string;
  backupFrequency: 'DAILY' | 'WEEKLY' | 'DISABLED';
  backupRetentionCount: number;
  lastBackupDate: string | null;
}

export interface MonthBalance {
  month: string;
  dueAmount: number;
  paidAmount: number;
  status: MonthStatus;
  remainingAmount: number;
}

export interface MemberFinancialStatus {
  member: Member;
  monthsDue: MonthBalance[];
  unpaidMonths: MonthBalance[];
  advanceMonths: MonthBalance[];
  totalDue: number;
  totalPaid: number;
  balanceDue: number; // positive = owes money, negative = advance
  monthsLateCount: number;
  lastReminder?: ReminderLog;
}

export interface AndroidCodeFile {
  path: string;
  step: number; // 1 to 7
  title: string;
  category: 'config' | 'entity' | 'dao' | 'database' | 'repository' | 'viewmodel' | 'ui' | 'worker' | 'test' | 'res' | 'gradle';
  language: 'kotlin' | 'toml' | 'xml' | 'gradle';
  content: string;
}
