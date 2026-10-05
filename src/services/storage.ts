import {
  Member,
  MonthlyFeeRate,
  Payment,
  PaymentAllocation,
  IncomeCategory,
  OtherIncome,
  Remittance,
  ReminderLog,
  AppSettings,
} from '../types';
import { getCurrentDateStr } from './businessLogic';

const STORAGE_KEY_PREFIX = 'caisse_asso_v1_';

export interface DatabaseBackup {
  schemaVersion: number;
  appName: string;
  exportDate: string;
  settings: AppSettings;
  members: Member[];
  feeRates: MonthlyFeeRate[];
  payments: Payment[];
  allocations: PaymentAllocation[];
  categories: IncomeCategory[];
  otherIncomes: OtherIncome[];
  remittances: Remittance[];
  reminderLogs: ReminderLog[];
}

const DEFAULT_SETTINGS: AppSettings = {
  associationName: 'Association Solidarité & Entraide',
  currency: 'Ar',
  defaultCountryPrefix: '+261',
  reminderMessageTemplate:
    'Bonjour {prenom}, sauf erreur de notre part, il vous reste {montant_du} de cotisation à régler pour : {mois_impayes}. Merci ! – {association}',
  minReminderIntervalDays: 7,
  lateThresholdMonths: 1,
  reminderNotificationDay: 5,
  reminderNotificationHour: 9,
  reminderNotificationsEnabled: true,
  remittanceReminderThreshold: 100000,
  remittanceReminderDaysInterval: 14,
  lastRemittanceRecipient: 'Trésorerie Centrale (M. Rakoto)',
  backupFrequency: 'WEEKLY',
  backupRetentionCount: 5,
  lastBackupDate: null,
};

const DEFAULT_CATEGORIES: IncomeCategory[] = [
  { id: 'cat-1', name: 'Dons', isActive: true },
  { id: 'cat-2', name: 'Événements', isActive: true },
  { id: 'cat-3', name: 'Amendes', isActive: true },
  { id: 'cat-4', name: 'Autres', isActive: true },
];

const INITIAL_FEE_RATES: MonthlyFeeRate[] = [
  { id: 'rate-1', effectiveFromMonth: '2025-01', amount: 5000 },
  { id: 'rate-2', effectiveFromMonth: '2026-01', amount: 10000 },
];

const INITIAL_MEMBERS: Member[] = [
  {
    id: 'm-1',
    lastName: 'Ranaivo',
    firstName: 'Jean-Luc',
    phone: '034 11 222 33',
    joinMonth: '2026-01',
    isActive: true,
    customMonthlyAmount: null,
    note: 'Membre fondateur',
    createdAt: 1735689600000,
  },
  {
    id: 'm-2',
    lastName: 'Andrianina',
    firstName: 'Hanta',
    phone: '+261 33 44 555 66',
    joinMonth: '2026-01',
    isActive: true,
    customMonthlyAmount: null,
    note: 'Secrétaire adjointe',
    createdAt: 1735689600000,
  },
  {
    id: 'm-3',
    lastName: 'Razafindrakoto',
    firstName: 'Mamy',
    phone: '032 77 888 99',
    joinMonth: '2026-03',
    isActive: true,
    customMonthlyAmount: 15000, // Montant solidaire surchargé
    note: 'Cotisation volontaire majorée',
    createdAt: 1740787200000,
  },
  {
    id: 'm-4',
    lastName: 'Rakotomalala',
    firstName: 'Faly',
    phone: '',
    joinMonth: '2026-02',
    isActive: true,
    customMonthlyAmount: null,
    note: 'Numéro à récupérer lors de la prochaine AG',
    createdAt: 1738368000000,
  },
  {
    id: 'm-5',
    lastName: 'Ravelo',
    firstName: 'Soa',
    phone: '034 99 000 11',
    joinMonth: '2025-10',
    isActive: false, // Membre archivé
    note: 'Déménagement en province',
    createdAt: 1727740800000,
  },
];

const INITIAL_PAYMENTS: Payment[] = [
  {
    id: 'p-1',
    memberId: 'm-1',
    date: '2026-01-15',
    amount: 20000,
    note: 'Règlement Janvier et Février 2026',
    createdAt: 1736937600000,
  },
  {
    id: 'p-2',
    memberId: 'm-2',
    date: '2026-02-10',
    amount: 10000,
    note: 'Cotisation Janvier 2026',
    createdAt: 1739184000000,
  },
];

const INITIAL_ALLOCATIONS: PaymentAllocation[] = [
  { id: 'pa-1', paymentId: 'p-1', memberId: 'm-1', month: '2026-01', amount: 10000 },
  { id: 'pa-2', paymentId: 'p-1', memberId: 'm-1', month: '2026-02', amount: 10000 },
  { id: 'pa-3', paymentId: 'p-2', memberId: 'm-2', month: '2026-01', amount: 10000 },
];

const INITIAL_OTHER_INCOME: OtherIncome[] = [
  {
    id: 'oi-1',
    categoryId: 'cat-1',
    memberId: 'm-3',
    date: '2026-02-14',
    amount: 50000,
    description: 'Don pour la fête annuelle de la communauté',
    createdAt: 1739529600000,
  },
  {
    id: 'oi-2',
    categoryId: 'cat-3',
    memberId: null,
    date: '2026-03-01',
    amount: 5000,
    description: 'Amende pour retard à la réunion mensuelle',
    createdAt: 1740787200000,
  },
];

const INITIAL_REMITTANCES: Remittance[] = [
  {
    id: 'rem-1',
    date: '2026-02-28',
    amount: 30000,
    recipient: 'Trésorerie Centrale (M. Rakoto)',
    coveredPeriod: 'Janvier - Février 2026',
    note: 'Premier versement des cotisations perçues',
    createdAt: 1740700800000,
  },
];

const INITIAL_REMINDERS: ReminderLog[] = [
  {
    id: 'rl-1',
    memberId: 'm-2',
    date: '2026-03-10 14:30',
    channel: 'WHATSAPP',
    amountDueAtTime: 20000,
    createdAt: 1741617000000,
  },
];

function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_PREFIX + key);
    if (!raw) return fallback;
    return JSON.parse(raw);
  } catch (e) {
    console.error(`Error loading key ${key}:`, e);
    return fallback;
  }
}

function saveJson<T>(key: string, data: T): void {
  try {
    localStorage.setItem(STORAGE_KEY_PREFIX + key, JSON.stringify(data));
  } catch (e) {
    console.error(`Error saving key ${key}:`, e);
  }
}

export class AppStorage {
  private static instance: AppStorage;

  static getInstance(): AppStorage {
    if (!AppStorage.instance) {
      AppStorage.instance = new AppStorage();
      AppStorage.instance.initIfEmpty();
    }
    return AppStorage.instance;
  }

  private initIfEmpty() {
    if (!localStorage.getItem(STORAGE_KEY_PREFIX + 'initialized')) {
      this.saveSettings(DEFAULT_SETTINGS);
      this.saveMembers(INITIAL_MEMBERS);
      this.saveFeeRates(INITIAL_FEE_RATES);
      this.savePayments(INITIAL_PAYMENTS);
      this.saveAllocations(INITIAL_ALLOCATIONS);
      this.saveCategories(DEFAULT_CATEGORIES);
      this.saveOtherIncomes(INITIAL_OTHER_INCOME);
      this.saveRemittances(INITIAL_REMITTANCES);
      this.saveReminderLogs(INITIAL_REMINDERS);
      localStorage.setItem(STORAGE_KEY_PREFIX + 'initialized', 'true');
    }
  }

  // Settings
  getSettings(): AppSettings {
    return loadJson<AppSettings>('settings', DEFAULT_SETTINGS);
  }
  saveSettings(settings: AppSettings): void {
    saveJson('settings', settings);
  }

  // Members
  getMembers(): Member[] {
    return loadJson<Member[]>('members', []);
  }
  saveMembers(members: Member[]): void {
    saveJson('members', members);
  }
  addMember(member: Omit<Member, 'id' | 'createdAt'>): Member {
    const members = this.getMembers();
    const newMember: Member = {
      ...member,
      id: 'm-' + Date.now() + '-' + Math.floor(Math.random() * 1000),
      createdAt: Date.now(),
    };
    members.push(newMember);
    this.saveMembers(members);
    return newMember;
  }
  updateMember(member: Member): void {
    const members = this.getMembers().map(m => (m.id === member.id ? member : m));
    this.saveMembers(members);
  }
  archiveMember(id: string, isActive: boolean): void {
    const members = this.getMembers().map(m => (m.id === id ? { ...m, isActive } : m));
    this.saveMembers(members);
  }
  deleteMember(id: string, force: boolean = false): { success: boolean; message?: string } {
    // Vérifier si le membre a des paiements ou des recettes associées
    const payments = this.getPayments().filter(p => p.memberId === id);
    const otherIncomes = this.getOtherIncomes().filter(o => o.memberId === id);
    if ((payments.length > 0 || otherIncomes.length > 0) && !force) {
      return {
        success: false,
        message: 'Impossible de supprimer ce membre car il possède un historique de paiements ou de recettes. Vous pouvez l\'archiver, ou confirmer la suppression complète avec ses opérations.',
      };
    }

    if (force) {
      const paymentIds = new Set(payments.map(p => p.id));
      this.savePayments(this.getPayments().filter(p => !paymentIds.has(p.id)));
      this.saveAllocations(this.getAllocations().filter(a => a.memberId !== id));
      this.saveOtherIncomes(this.getOtherIncomes().filter(o => o.memberId !== id));
      this.saveReminderLogs(this.getReminderLogs().filter(r => r.memberId !== id));
    }

    const members = this.getMembers().filter(m => m.id !== id);
    this.saveMembers(members);
    return { success: true };
  }

  clearAllData(): void {
    this.saveMembers([]);
    this.savePayments([]);
    this.saveAllocations([]);
    this.saveOtherIncomes([]);
    this.saveRemittances([]);
    this.saveReminderLogs([]);
  }

  // Monthly Fee Rates
  getFeeRates(): MonthlyFeeRate[] {
    return loadJson<MonthlyFeeRate[]>('fee_rates', INITIAL_FEE_RATES);
  }
  saveFeeRates(rates: MonthlyFeeRate[]): void {
    saveJson('fee_rates', rates);
  }
  addFeeRate(effectiveFromMonth: string, amount: number): void {
    const rates = this.getFeeRates();
    // Si un taux existe déjà pour ce mois exact, le mettre à jour
    const existing = rates.find(r => r.effectiveFromMonth === effectiveFromMonth);
    if (existing) {
      existing.amount = amount;
    } else {
      rates.push({
        id: 'rate-' + Date.now(),
        effectiveFromMonth,
        amount,
      });
    }
    rates.sort((a, b) => b.effectiveFromMonth.localeCompare(a.effectiveFromMonth));
    this.saveFeeRates(rates);
  }

  // Payments & Allocations
  getPayments(): Payment[] {
    return loadJson<Payment[]>('payments', []);
  }
  savePayments(payments: Payment[]): void {
    saveJson('payments', payments);
  }
  getAllocations(): PaymentAllocation[] {
    return loadJson<PaymentAllocation[]>('allocations', []);
  }
  saveAllocations(allocations: PaymentAllocation[]): void {
    saveJson('allocations', allocations);
  }

  recordPayment(
    memberId: string,
    amount: number,
    date: string,
    note: string,
    allocationsList: { month: string; amount: number }[]
  ): Payment {
    const payments = this.getPayments();
    const allocations = this.getAllocations();

    const paymentId = 'p-' + Date.now() + '-' + Math.floor(Math.random() * 1000);
    const newPayment: Payment = {
      id: paymentId,
      memberId,
      amount,
      date,
      note,
      createdAt: Date.now(),
    };

    const newAllocations: PaymentAllocation[] = allocationsList.map(a => ({
      id: 'pa-' + Date.now() + '-' + Math.random().toString(36).substring(2, 7),
      paymentId,
      memberId,
      month: a.month,
      amount: a.amount,
    }));

    payments.push(newPayment);
    allocations.push(...newAllocations);

    this.savePayments(payments);
    this.saveAllocations(allocations);
    return newPayment;
  }

  cancelPayment(paymentId: string): void {
    const payments = this.getPayments().filter(p => p.id !== paymentId);
    const allocations = this.getAllocations().filter(a => a.paymentId !== paymentId);
    this.savePayments(payments);
    this.saveAllocations(allocations);
  }

  // Other Incomes & Categories
  getCategories(): IncomeCategory[] {
    return loadJson<IncomeCategory[]>('categories', DEFAULT_CATEGORIES);
  }
  saveCategories(categories: IncomeCategory[]): void {
    saveJson('categories', categories);
  }
  addCategory(name: string): IncomeCategory {
    const categories = this.getCategories();
    const newCat: IncomeCategory = {
      id: 'cat-' + Date.now(),
      name,
      isActive: true,
    };
    categories.push(newCat);
    this.saveCategories(categories);
    return newCat;
  }
  updateCategory(id: string, name: string, isActive: boolean): void {
    const categories = this.getCategories().map(c => (c.id === id ? { ...c, name, isActive } : c));
    this.saveCategories(categories);
  }

  getOtherIncomes(): OtherIncome[] {
    return loadJson<OtherIncome[]>('other_incomes', []);
  }
  saveOtherIncomes(incomes: OtherIncome[]): void {
    saveJson('other_incomes', incomes);
  }
  addOtherIncome(income: Omit<OtherIncome, 'id' | 'createdAt'>): OtherIncome {
    const list = this.getOtherIncomes();
    const item: OtherIncome = {
      ...income,
      id: 'oi-' + Date.now() + '-' + Math.floor(Math.random() * 1000),
      createdAt: Date.now(),
    };
    list.push(item);
    this.saveOtherIncomes(list);
    return item;
  }
  deleteOtherIncome(id: string): void {
    const list = this.getOtherIncomes().filter(o => o.id !== id);
    this.saveOtherIncomes(list);
  }

  // Remittances
  getRemittances(): Remittance[] {
    return loadJson<Remittance[]>('remittances', []);
  }
  saveRemittances(remittances: Remittance[]): void {
    saveJson('remittances', remittances);
  }
  addRemittance(rem: Omit<Remittance, 'id' | 'createdAt'>): Remittance {
    const list = this.getRemittances();
    const item: Remittance = {
      ...rem,
      id: 'rem-' + Date.now() + '-' + Math.floor(Math.random() * 1000),
      createdAt: Date.now(),
    };
    list.push(item);
    this.saveRemittances(list);

    // Mettre à jour le dernier destinataire dans les paramètres
    const settings = this.getSettings();
    settings.lastRemittanceRecipient = rem.recipient;
    this.saveSettings(settings);

    return item;
  }
  deleteRemittance(id: string): void {
    const list = this.getRemittances().filter(r => r.id !== id);
    this.saveRemittances(list);
  }

  // Reminders
  getReminderLogs(): ReminderLog[] {
    return loadJson<ReminderLog[]>('reminder_logs', []);
  }
  saveReminderLogs(logs: ReminderLog[]): void {
    saveJson('reminder_logs', logs);
  }
  logReminder(log: Omit<ReminderLog, 'id' | 'createdAt'>): ReminderLog {
    const list = this.getReminderLogs();
    const item: ReminderLog = {
      ...log,
      id: 'rl-' + Date.now() + '-' + Math.floor(Math.random() * 1000),
      createdAt: Date.now(),
    };
    list.push(item);
    this.saveReminderLogs(list);
    return item;
  }
  deleteReminderLog(id: string): void {
    const list = this.getReminderLogs().filter(r => r.id !== id);
    this.saveReminderLogs(list);
  }

  // Backup & Restore
  exportDatabaseJson(): string {
    const backup: DatabaseBackup = {
      schemaVersion: 1,
      appName: 'Caisse Association',
      exportDate: new Date().toISOString(),
      settings: this.getSettings(),
      members: this.getMembers(),
      feeRates: this.getFeeRates(),
      payments: this.getPayments(),
      allocations: this.getAllocations(),
      categories: this.getCategories(),
      otherIncomes: this.getOtherIncomes(),
      remittances: this.getRemittances(),
      reminderLogs: this.getReminderLogs(),
    };

    // Mettre à jour la date de dernière sauvegarde
    const settings = this.getSettings();
    settings.lastBackupDate = getCurrentDateStr();
    this.saveSettings(settings);

    return JSON.stringify(backup, null, 2);
  }

  restoreDatabaseJson(jsonString: string): { success: boolean; message: string } {
    try {
      const data: DatabaseBackup = JSON.parse(jsonString);
      if (!data.appName || data.schemaVersion === undefined) {
        return {
          success: false,
          message: 'Format de fichier de sauvegarde invalide. Impossible de trouver les métadonnées de schéma.',
        };
      }

      this.saveSettings(data.settings || DEFAULT_SETTINGS);
      this.saveMembers(data.members || []);
      this.saveFeeRates(data.feeRates || INITIAL_FEE_RATES);
      this.savePayments(data.payments || []);
      this.saveAllocations(data.allocations || []);
      this.saveCategories(data.categories || DEFAULT_CATEGORIES);
      this.saveOtherIncomes(data.otherIncomes || []);
      this.saveRemittances(data.remittances || []);
      this.saveReminderLogs(data.reminderLogs || []);

      return {
        success: true,
        message: `Restauration réussie avec succès ! ${data.members?.length || 0} membres, ${data.payments?.length || 0} paiements rechargés.`,
      };
    } catch (e: any) {
      return {
        success: false,
        message: `Erreur lors de la lecture du fichier : ${e?.message || 'fichier corrompu'}`,
      };
    }
  }

  resetToDefault(): void {
    localStorage.clear();
    this.initIfEmpty();
  }
}
