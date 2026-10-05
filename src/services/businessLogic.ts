import {
  Member,
  MonthlyFeeRate,
  PaymentAllocation,
  MonthBalance,
  MonthStatus,
  MemberFinancialStatus,
  AppSettings,
  ReminderLog,
} from '../types';

/**
 * Formate un nombre avec séparateur de milliers et devise.
 * Pas de décimales par défaut.
 */
export function formatMoney(amount: number, currency: string = 'Ar'): string {
  const rounded = Math.round(amount);
  const formatted = new Intl.NumberFormat('fr-FR', {
    useGrouping: true,
    maximumFractionDigits: 0,
  })
    .format(rounded)
    .replace(/[\u202F\u00A0]/g, ' ');
  return `${formatted} ${currency}`;
}

/**
 * Formate une date ISO "YYYY-MM-DD" en "jj/mm/aaaa".
 */
export function formatDateFr(dateStr: string): string {
  if (!dateStr) return '';
  const parts = dateStr.split('-');
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  return dateStr;
}

/**
 * Retourne le libellé français d'un mois "YYYY-MM" (ex: "Janv. 2026")
 */
export function formatMonthFr(yearMonth: string, short: boolean = true): string {
  if (!yearMonth) return '';
  const [year, month] = yearMonth.split('-');
  const monthNamesShort = [
    'Janv.', 'Févr.', 'Mars', 'Avr.', 'Mai', 'Juin',
    'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.', 'Déc.'
  ];
  const monthNamesLong = [
    'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
  ];
  const idx = parseInt(month, 10) - 1;
  const name = short ? monthNamesShort[idx] : monthNamesLong[idx];
  return `${name || month} ${year}`;
}

/**
 * Obtenir le mois courant au format "YYYY-MM"
 */
export function getCurrentYearMonth(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
}

/**
 * Obtenir la date courante au format "YYYY-MM-DD"
 */
export function getCurrentDateStr(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Calcule le mois suivant ou précédent
 */
export function shiftYearMonth(yearMonth: string, deltaMonths: number): string {
  const [yearStr, monthStr] = yearMonth.split('-');
  let year = parseInt(yearStr, 10);
  let month = parseInt(monthStr, 10) + deltaMonths;
  while (month > 12) {
    month -= 12;
    year += 1;
  }
  while (month < 1) {
    month += 12;
    year -= 1;
  }
  return `${year}-${String(month).padStart(2, '0')}`;
}

/**
 * Génère la liste des mois consécutifs entre startMonth et endMonth inclus
 */
export function getMonthRange(startMonth: string, endMonth: string): string[] {
  if (startMonth > endMonth) return [];
  const list: string[] = [];
  let cur = startMonth;
  while (cur <= endMonth) {
    list.push(cur);
    cur = shiftYearMonth(cur, 1);
  }
  return list;
}

/**
 * Détermine le montant mensuel applicable pour un mois donné et un membre donné.
 * Priorité :
 * 1. customMonthlyAmount du membre si défini et > 0
 * 2. Taux par défaut le plus récent applicable (effectiveFromMonth <= targetMonth)
 * 3. Si aucun, taux le plus ancien ou fallback 10000
 */
export function getExpectedFeeForMonth(
  targetMonth: string,
  member: Member,
  rates: MonthlyFeeRate[]
): number {
  if (member.customMonthlyAmount && member.customMonthlyAmount > 0) {
    return member.customMonthlyAmount;
  }
  if (!rates || rates.length === 0) {
    return 10000;
  }
  // Trier les taux du plus récent au plus ancien
  const sorted = [...rates].sort((a, b) => b.effectiveFromMonth.localeCompare(a.effectiveFromMonth));
  const applicable = sorted.find(r => r.effectiveFromMonth <= targetMonth);
  if (applicable) {
    return applicable.amount;
  }
  // Si le mois demandé est antérieur au tout premier taux, prendre le plus ancien
  return sorted[sorted.length - 1].amount;
}

/**
 * Calcule l'état financier complet d'un membre (mois dus, paiements affectés, solde, impayés, avances)
 */
export function computeMemberFinancialStatus(
  member: Member,
  allocations: PaymentAllocation[],
  rates: MonthlyFeeRate[],
  currentMonth: string = getCurrentYearMonth(),
  lastReminder?: ReminderLog
): MemberFinancialStatus {
  const memberAllocations = allocations.filter(a => a.memberId === member.id);
  const paidByMonth: Record<string, number> = {};
  for (const a of memberAllocations) {
    paidByMonth[a.month] = (paidByMonth[a.month] || 0) + a.amount;
  }

  // Les mois dus vont du joinMonth jusqu'à currentMonth inclus (si le membre est actif)
  const dueMonthsList = getMonthRange(member.joinMonth, currentMonth);

  // Mois futurs ayant des allocations (avances)
  const allPaidMonths = Object.keys(paidByMonth).sort();
  const futureMonthsWithPayments = allPaidMonths.filter(m => m > currentMonth);

  const monthsDue: MonthBalance[] = [];
  const unpaidMonths: MonthBalance[] = [];
  const advanceMonths: MonthBalance[] = [];

  let totalDue = 0;
  let totalPaid = 0;

  for (const m of dueMonthsList) {
    const dueAmount = getExpectedFeeForMonth(m, member, rates);
    const paidAmount = paidByMonth[m] || 0;
    totalDue += dueAmount;
    totalPaid += paidAmount;

    let status: MonthStatus = 'UNPAID';
    if (paidAmount >= dueAmount) {
      status = 'PAID';
    } else if (paidAmount > 0) {
      status = 'PARTIAL';
    }

    const item: MonthBalance = {
      month: m,
      dueAmount,
      paidAmount,
      status,
      remainingAmount: Math.max(0, dueAmount - paidAmount),
    };

    monthsDue.push(item);
    if (status === 'UNPAID' || status === 'PARTIAL') {
      unpaidMonths.push(item);
    }
  }

  // Traiter les mois d'avance (futurs)
  for (const m of futureMonthsWithPayments) {
    const dueAmount = getExpectedFeeForMonth(m, member, rates);
    const paidAmount = paidByMonth[m] || 0;
    totalPaid += paidAmount;

    let status: MonthStatus = 'ADVANCE';
    if (paidAmount < dueAmount) {
      status = 'PARTIAL';
    }

    advanceMonths.push({
      month: m,
      dueAmount,
      paidAmount,
      status,
      remainingAmount: Math.max(0, dueAmount - paidAmount),
    });
  }

  const balanceDue = totalDue - (Object.values(paidByMonth).reduce((s, v) => s + v, 0));

  return {
    member,
    monthsDue,
    unpaidMonths,
    advanceMonths,
    totalDue,
    totalPaid,
    balanceDue,
    monthsLateCount: unpaidMonths.length,
    lastReminder,
  };
}

/**
 * Algorithme automatique de répartition d'un paiement en espèces :
 * Affecte le montant en priorité aux mois impayés / partiels les plus anciens,
 * puis s'il reste un solde, aux mois futurs consécutifs.
 */
export interface AllocationPreviewItem {
  month: string;
  monthLabel: string;
  expectedFee: number;
  previouslyPaid: number;
  allocatedAmount: number;
  remainingDueAfter: number;
  statusAfter: MonthStatus;
}

export function computeAutomaticAllocation(
  amountReceived: number,
  member: Member,
  existingAllocations: PaymentAllocation[],
  rates: MonthlyFeeRate[],
  currentMonth: string = getCurrentYearMonth()
): AllocationPreviewItem[] {
  let remainingCash = amountReceived;
  const result: AllocationPreviewItem[] = [];

  // 1. Calculer ce qui a déjà été payé par mois
  const memberAllocations = existingAllocations.filter(a => a.memberId === member.id);
  const paidByMonth: Record<string, number> = {};
  for (const a of memberAllocations) {
    paidByMonth[a.month] = (paidByMonth[a.month] || 0) + a.amount;
  }

  // 2. Parcourir depuis joinMonth
  let cursorMonth = member.joinMonth;
  // Déterminer la borne minimale et continuer tant qu'il reste de l'argent ou jusqu'à currentMonth
  while (remainingCash > 0 || cursorMonth <= currentMonth) {
    const expected = getExpectedFeeForMonth(cursorMonth, member, rates);
    const previouslyPaid = paidByMonth[cursorMonth] || 0;
    const neededToSettle = Math.max(0, expected - previouslyPaid);

    if (neededToSettle > 0) {
      if (remainingCash > 0) {
        const allocate = Math.min(remainingCash, neededToSettle);
        const newPaid = previouslyPaid + allocate;
        remainingCash -= allocate;

        let statusAfter: MonthStatus = cursorMonth > currentMonth ? 'ADVANCE' : 'PAID';
        if (newPaid < expected) {
          statusAfter = 'PARTIAL';
        }

        result.push({
          month: cursorMonth,
          monthLabel: formatMonthFr(cursorMonth),
          expectedFee: expected,
          previouslyPaid,
          allocatedAmount: allocate,
          remainingDueAfter: Math.max(0, expected - newPaid),
          statusAfter,
        });
      } else {
        // Mois impayé non couvert
        result.push({
          month: cursorMonth,
          monthLabel: formatMonthFr(cursorMonth),
          expectedFee: expected,
          previouslyPaid,
          allocatedAmount: 0,
          remainingDueAfter: neededToSettle,
          statusAfter: previouslyPaid > 0 ? 'PARTIAL' : 'UNPAID',
        });
      }
    } else if (previouslyPaid >= expected && cursorMonth <= currentMonth) {
      // Déjà soldé, on n'ajoute pas à l'aperçu sauf si nécessaire ou pour clarté
    }

    cursorMonth = shiftYearMonth(cursorMonth, 1);

    // Sécurité garde-fou pour éviter boucle infinie si montant très grand (max 60 mois)
    if (result.length >= 60) break;
  }

  // Ne garder dans la proposition de répartition que les mois qui reçoivent une allocation (> 0)
  // ou afficher les mois concernés
  return result.filter(item => item.allocatedAmount > 0);
}

/**
 * Normalisation de numéro de téléphone.
 * Règle : applique l'indicatif par défaut (ex: +261, +33) aux numéros saisis sans indicatif,
 * nettoie les espaces, tirets, parenthèses et supprime le 0 initial si indicatif ajouté.
 */
export function normalizePhoneNumber(rawPhone: string, defaultPrefix: string = '+261'): string {
  if (!rawPhone) return '';
  let cleaned = rawPhone.replace(/[\s\-\(\)\.]/g, '');

  if (cleaned.startsWith('+')) {
    return cleaned;
  }
  if (cleaned.startsWith('00')) {
    return '+' + cleaned.substring(2);
  }

  const prefix = defaultPrefix.startsWith('+') ? defaultPrefix : `+${defaultPrefix}`;
  // Si commence par un zéro initial (format national ex: 0341234567 ou 0612345678)
  if (cleaned.startsWith('0')) {
    cleaned = cleaned.substring(1);
  }
  return `${prefix}${cleaned}`;
}

/**
 * Remplacement des variables dans le modèle de relance
 * Variables supportées : {prenom}, {nom}, {montant_du}, {mois_impayes}, {association}, {devise}
 */
export function renderReminderMessage(
  template: string,
  member: Member,
  unpaidMonths: MonthBalance[],
  totalDue: number,
  settings: AppSettings
): string {
  const moisNoms = unpaidMonths.map(m => formatMonthFr(m.month, true)).join(', ');
  const formattedMontant = formatMoney(totalDue, settings.currency);

  let message = template;
  message = message.replace(/{prenom}/g, member.firstName || '');
  message = message.replace(/{nom}/g, member.lastName || '');
  message = message.replace(/{montant_du}/g, formattedMontant);
  message = message.replace(/{mois_impayes}/g, moisNoms || 'aucun mois');
  message = message.replace(/{association}/g, settings.associationName || 'Notre Association');
  message = message.replace(/{devise}/g, settings.currency || 'Ar');

  return message;
}

/**
 * Vérifie la règle anti-harcèlement (délai minimal entre deux relances)
 */
export interface AntiHarassmentCheck {
  canRemindWithoutWarning: boolean;
  daysSinceLastReminder: number | null;
  warningMessage?: string;
}

export function checkAntiHarassment(
  lastReminderDateStr: string | null | undefined,
  minIntervalDays: number = 7
): AntiHarassmentCheck {
  if (!lastReminderDateStr) {
    return {
      canRemindWithoutWarning: true,
      daysSinceLastReminder: null,
    };
  }

  const lastDate = new Date(lastReminderDateStr.replace(' ', 'T'));
  const now = new Date();
  const diffMs = now.getTime() - lastDate.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays < minIntervalDays) {
    return {
      canRemindWithoutWarning: false,
      daysSinceLastReminder: diffDays,
      warningMessage: `Attention : ce membre a déjà été relancé il y a ${diffDays} jour${diffDays > 1 ? 's' : ''} (délai minimal recommandé : ${minIntervalDays} jours).`,
    };
  }

  return {
    canRemindWithoutWarning: true,
    daysSinceLastReminder: diffDays,
  };
}

/**
 * Génère le contenu CSV avec UTF-8 BOM pour Excel
 */
export function generateCsvWithBom(headers: string[], rows: (string | number)[][]): string {
  const BOM = '\uFEFF';
  const csvLines: string[] = [];

  const escapeCsv = (val: string | number) => {
    const s = String(val ?? '').replace(/"/g, '""');
    return `"${s}"`;
  };

  csvLines.push(headers.map(escapeCsv).join(';'));
  for (const row of rows) {
    csvLines.push(row.map(escapeCsv).join(';'));
  }

  return BOM + csvLines.join('\r\n');
}
