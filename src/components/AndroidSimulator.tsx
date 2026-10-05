import React, { useState, useEffect, useMemo } from 'react';
import {
  Users,
  Wallet,
  Calendar,
  AlertTriangle,
  ArrowRight,
  Plus,
  Search,
  Filter,
  Phone,
  MessageSquare,
  Send,
  Building,
  ArrowDownLeft,
  FileSpreadsheet,
  FileText,
  Settings,
  HardDriveDownload,
  HardDriveUpload,
  Check,
  ChevronRight,
  X,
  Clock,
  Info,
  DollarSign,
  Printer,
  Trash2,
  Edit2,
  Archive,
  RefreshCw,
} from 'lucide-react';
import { AppStorage } from '../services/storage';
import {
  Member,
  Payment,
  PaymentAllocation,
  OtherIncome,
  Remittance,
  ReminderLog,
  AppSettings,
  MonthlyFeeRate,
  IncomeCategory,
  MonthStatus,
} from '../types';
import {
  formatMoney,
  formatDateFr,
  formatMonthFr,
  getCurrentYearMonth,
  getCurrentDateStr,
  computeMemberFinancialStatus,
  computeAutomaticAllocation,
  AllocationPreviewItem,
  normalizePhoneNumber,
  renderReminderMessage,
  checkAntiHarassment,
  generateCsvWithBom,
} from '../services/businessLogic';

export function AndroidSimulator() {
  const storage = useMemo(() => AppStorage.getInstance(), []);

  // State loaded from storage
  const [settings, setSettings] = useState<AppSettings>(() => storage.getSettings());
  const [members, setMembers] = useState<Member[]>(() => storage.getMembers());
  const [feeRates, setFeeRates] = useState<MonthlyFeeRate[]>(() => storage.getFeeRates());
  const [payments, setPayments] = useState<Payment[]>(() => storage.getPayments());
  const [allocations, setAllocations] = useState<PaymentAllocation[]>(() => storage.getAllocations());
  const [otherIncomes, setOtherIncomes] = useState<OtherIncome[]>(() => storage.getOtherIncomes());
  const [categories, setCategories] = useState<IncomeCategory[]>(() => storage.getCategories());
  const [remittances, setRemittances] = useState<Remittance[]>(() => storage.getRemittances());
  const [reminderLogs, setReminderLogs] = useState<ReminderLog[]>(() => storage.getReminderLogs());

  // Navigation tab: 'dashboard' | 'members' | 'income' | 'reminders' | 'more' | 'remittances' | 'journal' | 'settings'
  const [currentTab, setCurrentTab] = useState<'dashboard' | 'members' | 'income' | 'reminders' | 'more'>('dashboard');
  const [subView, setSubView] = useState<'none' | 'remittances' | 'journal' | 'settings' | 'categories'>('none');

  // Dialog & Modal states
  const [memberDetailId, setMemberDetailId] = useState<string | null>(null);
  const [showMemberModal, setShowMemberModal] = useState(false);
  const [editingMember, setEditingMember] = useState<Member | null>(null);

  const [showFeePaymentModal, setShowFeePaymentModal] = useState(false);
  const [selectedMemberForPayment, setSelectedMemberForPayment] = useState<string>('');
  const [paymentAmount, setPaymentAmount] = useState<number>(10000);
  const [paymentDate, setPaymentDate] = useState<string>(getCurrentDateStr());
  const [paymentNote, setPaymentNote] = useState<string>('');
  const [allocationItems, setAllocationItems] = useState<AllocationPreviewItem[]>([]);

  const [showOtherIncomeModal, setShowOtherIncomeModal] = useState(false);
  const [incomeAmount, setIncomeAmount] = useState<number>(5000);
  const [incomeCategoryId, setIncomeCategoryId] = useState<string>('');
  const [incomeMemberId, setIncomeMemberId] = useState<string>('');
  const [incomeDate, setIncomeDate] = useState<string>(getCurrentDateStr());
  const [incomeDesc, setIncomeDesc] = useState<string>('');

  const [showRemittanceModal, setShowRemittanceModal] = useState(false);
  const [remittanceAmount, setRemittanceAmount] = useState<number>(0);
  const [remittanceRecipient, setRemittanceRecipient] = useState<string>('');
  const [remittancePeriod, setRemittancePeriod] = useState<string>('');
  const [remittanceDate, setRemittanceDate] = useState<string>(getCurrentDateStr());
  const [remittanceNote, setRemittanceNote] = useState<string>('');

  const [showPdfPreview, setShowPdfPreview] = useState(false);
  const [showReminderQueue, setShowReminderQueue] = useState(false);
  const [reminderQueueIndex, setReminderQueueIndex] = useState(0);

  // Message preview dialog
  const [activeReminderMember, setActiveReminderMember] = useState<Member | null>(null);
  const [customReminderMessage, setCustomReminderMessage] = useState<string>('');

  // UI snackbar
  const [snackbar, setSnackbar] = useState<string | null>(null);
  const showSnack = (msg: string) => {
    setSnackbar(msg);
    setTimeout(() => setSnackbar(null), 3500);
  };

  const refreshData = () => {
    setSettings(storage.getSettings());
    setMembers(storage.getMembers());
    setFeeRates(storage.getFeeRates());
    setPayments(storage.getPayments());
    setAllocations(storage.getAllocations());
    setOtherIncomes(storage.getOtherIncomes());
    setCategories(storage.getCategories());
    setRemittances(storage.getRemittances());
    setReminderLogs(storage.getReminderLogs());
  };

  // Financial aggregates
  const currentMonth = getCurrentYearMonth();
  const currentYear = currentMonth.slice(0, 4);

  const totalFeeCollected = useMemo(() => {
    return payments.reduce((sum, p) => sum + p.amount, 0);
  }, [payments]);

  const totalOtherIncome = useMemo(() => {
    return otherIncomes.reduce((sum, o) => sum + o.amount, 0);
  }, [otherIncomes]);

  const totalCollectedAllTime = totalFeeCollected + totalOtherIncome;

  const totalRemittedAllTime = useMemo(() => {
    return remittances.reduce((sum, r) => sum + r.amount, 0);
  }, [remittances]);

  const remainingToRemit = totalCollectedAllTime - totalRemittedAllTime;

  // Month and Year aggregates
  const feeCurrentMonth = useMemo(() => {
    return payments
      .filter(p => p.date.startsWith(currentMonth))
      .reduce((sum, p) => sum + p.amount, 0);
  }, [payments, currentMonth]);

  const otherCurrentMonth = useMemo(() => {
    return otherIncomes
      .filter(o => o.date.startsWith(currentMonth))
      .reduce((sum, o) => sum + o.amount, 0);
  }, [otherIncomes, currentMonth]);

  const feeCurrentYear = useMemo(() => {
    return payments
      .filter(p => p.date.startsWith(currentYear))
      .reduce((sum, p) => sum + p.amount, 0);
  }, [payments, currentYear]);

  const otherCurrentYear = useMemo(() => {
    return otherIncomes
      .filter(o => o.date.startsWith(currentYear))
      .reduce((sum, o) => sum + o.amount, 0);
  }, [otherIncomes, currentYear]);

  // Compute status for all active members
  const memberStatuses = useMemo(() => {
    return members.map(m => {
      const lastRem = reminderLogs
        .filter(r => r.memberId === m.id)
        .sort((a, b) => b.createdAt - a.createdAt)[0];
      return computeMemberFinancialStatus(m, allocations, feeRates, currentMonth, lastRem);
    });
  }, [members, allocations, feeRates, currentMonth, reminderLogs]);

  const lateMembers = useMemo(() => {
    return memberStatuses.filter(s => s.member.isActive && s.monthsLateCount >= settings.lateThresholdMonths);
  }, [memberStatuses, settings.lateThresholdMonths]);

  const totalUnpaidAmount = useMemo(() => {
    return lateMembers.reduce((sum, s) => sum + s.balanceDue, 0);
  }, [lateMembers]);

  // Handle auto-allocation calculation when member or amount changes
  useEffect(() => {
    if (selectedMemberForPayment && paymentAmount > 0) {
      const targetMember = members.find(m => m.id === selectedMemberForPayment);
      if (targetMember) {
        const preview = computeAutomaticAllocation(
          paymentAmount,
          targetMember,
          allocations,
          feeRates,
          currentMonth
        );
        setAllocationItems(preview);
      }
    } else {
      setAllocationItems([]);
    }
  }, [selectedMemberForPayment, paymentAmount, members, allocations, feeRates, currentMonth]);

  // Member filtering
  const [memberSearch, setMemberSearch] = useState('');
  const [memberFilter, setMemberFilter] = useState<'all' | 'active' | 'late' | 'archived'>('all');

  const filteredMembers = useMemo(() => {
    return memberStatuses.filter(s => {
      const m = s.member;
      const matchesSearch =
        m.lastName.toLowerCase().includes(memberSearch.toLowerCase()) ||
        m.firstName.toLowerCase().includes(memberSearch.toLowerCase()) ||
        m.phone.includes(memberSearch);

      if (!matchesSearch) return false;
      if (memberFilter === 'active') return m.isActive;
      if (memberFilter === 'archived') return !m.isActive;
      if (memberFilter === 'late') return m.isActive && s.monthsLateCount > 0;
      return true;
    });
  }, [memberStatuses, memberSearch, memberFilter]);

  // Backup check: > 30 days
  const backupWarning = useMemo(() => {
    if (!settings.lastBackupDate) return true;
    const last = new Date(settings.lastBackupDate);
    const diff = Math.floor((Date.now() - last.getTime()) / (1000 * 3600 * 24));
    return diff > 30;
  }, [settings.lastBackupDate]);

  // Helper actions
  const handleSaveMember = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const lastName = (formData.get('lastName') as string).trim();
    const firstName = (formData.get('firstName') as string).trim();
    const phone = (formData.get('phone') as string).trim();
    const joinMonth = formData.get('joinMonth') as string;
    const customAmountStr = formData.get('customMonthlyAmount') as string;
    const customMonthlyAmount = customAmountStr ? parseInt(customAmountStr, 10) : null;
    const note = (formData.get('note') as string).trim();

    if (!lastName || !firstName || !joinMonth) {
      showSnack('Veuillez remplir le nom, prénom et mois d\'adhésion.');
      return;
    }

    if (editingMember) {
      storage.updateMember({
        ...editingMember,
        lastName,
        firstName,
        phone,
        joinMonth,
        customMonthlyAmount,
        note,
      });
      showSnack('Membre mis à jour avec succès.');
    } else {
      storage.addMember({
        lastName,
        firstName,
        phone,
        joinMonth,
        isActive: true,
        customMonthlyAmount,
        note,
      });
      showSnack('Nouveau membre ajouté avec succès.');
    }
    setShowMemberModal(false);
    setEditingMember(null);
    refreshData();
  };

  const handleDeleteMember = (member: Member) => {
    if (!confirm(`Supprimer le membre ${member.firstName} ${member.lastName} ?`)) {
      return;
    }
    const res = storage.deleteMember(member.id);
    if (!res.success) {
      if (confirm(`${res.message}\n\nVoulez-vous quand même FORCER la suppression de ce membre et de toutes ses opérations associées ?`)) {
        storage.deleteMember(member.id, true);
        showSnack(`Membre ${member.firstName} ${member.lastName} et opérations associées supprimés.`);
        setMemberDetailId(null);
        refreshData();
      }
    } else {
      showSnack('Membre supprimé.');
      setMemberDetailId(null);
      refreshData();
    }
  };

  const handleToggleArchiveMember = (member: Member) => {
    storage.archiveMember(member.id, !member.isActive);
    showSnack(member.isActive ? 'Membre archivé (désactivé).' : 'Membre réactivé.');
    refreshData();
  };

  const handleRecordFeePayment = () => {
    if (!selectedMemberForPayment || paymentAmount <= 0) {
      showSnack('Veuillez sélectionner un membre et un montant valide (> 0).');
      return;
    }

    const allocList = allocationItems.map(item => ({
      month: item.month,
      amount: item.allocatedAmount,
    }));

    storage.recordPayment(
      selectedMemberForPayment,
      paymentAmount,
      paymentDate,
      paymentNote,
      allocList
    );

    showSnack(`Paiement de ${formatMoney(paymentAmount, settings.currency)} enregistré en espèces !`);
    setShowFeePaymentModal(false);
    setSelectedMemberForPayment('');
    setPaymentAmount(10000);
    setPaymentNote('');
    refreshData();
  };

  const handleRecordOtherIncome = (e: React.FormEvent) => {
    e.preventDefault();
    if (incomeAmount <= 0 || !incomeCategoryId) {
      showSnack('Veuillez indiquer un montant supérieur à 0 et choisir une catégorie.');
      return;
    }

    storage.addOtherIncome({
      categoryId: incomeCategoryId,
      memberId: incomeMemberId || null,
      amount: incomeAmount,
      date: incomeDate,
      description: incomeDesc,
    });

    showSnack(`Recette de ${formatMoney(incomeAmount, settings.currency)} enregistrée.`);
    setShowOtherIncomeModal(false);
    setIncomeDesc('');
    refreshData();
  };

  const handleRecordRemittance = (e: React.FormEvent) => {
    e.preventDefault();
    if (remittanceAmount <= 0 || !remittanceRecipient.trim()) {
      showSnack('Montant (>0) et destinataire obligatoires.');
      return;
    }

    if (remittanceAmount > remainingToRemit) {
      if (!confirm(`Attention : le versement de ${formatMoney(remittanceAmount, settings.currency)} dépasse le reste à verser en caisse (${formatMoney(remainingToRemit, settings.currency)}). Confirmer quand même ?`)) {
        return;
      }
    }

    storage.addRemittance({
      amount: remittanceAmount,
      recipient: remittanceRecipient.trim(),
      coveredPeriod: remittancePeriod.trim() || undefined,
      date: remittanceDate,
      note: remittanceNote.trim(),
    });

    showSnack(`Versement de ${formatMoney(remittanceAmount, settings.currency)} enregistré.`);
    setShowRemittanceModal(false);
    setRemittanceNote('');
    setRemittancePeriod('');
    refreshData();
  };

  const handleOpenReminderModal = (memberStatus: (typeof memberStatuses)[0]) => {
    setActiveReminderMember(memberStatus.member);
    const msg = renderReminderMessage(
      settings.reminderMessageTemplate,
      memberStatus.member,
      memberStatus.unpaidMonths,
      memberStatus.balanceDue,
      settings
    );
    setCustomReminderMessage(msg);
  };

  const handleTriggerReminderAction = (channel: 'WHATSAPP' | 'SMS' | 'CALL') => {
    if (!activeReminderMember) return;
    const status = memberStatuses.find(s => s.member.id === activeReminderMember.id);
    if (!status) return;

    const normalized = normalizePhoneNumber(activeReminderMember.phone, settings.defaultCountryPrefix);

    if (channel !== 'CALL' && !normalized) {
      alert('Ce membre n\'a pas de numéro de téléphone renseigné. Veuillez l\'ajouter dans sa fiche.');
      return;
    }

    // Anti-harassment check
    const anti = checkAntiHarassment(status.lastReminder?.date, settings.minReminderIntervalDays);
    if (!anti.canRemindWithoutWarning) {
      if (!confirm(`${anti.warningMessage}\n\nSouhaitez-vous continuer ?`)) {
        return;
      }
    }

    // Log reminder in ReminderLog table
    const logDate = `${getCurrentDateStr()} ${new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}`;
    storage.logReminder({
      memberId: activeReminderMember.id,
      date: logDate,
      channel,
      amountDueAtTime: status.balanceDue,
    });

    // Simulated intent opening
    if (channel === 'WHATSAPP') {
      const cleanPhone = normalized.replace('+', '');
      const encodedMsg = encodeURIComponent(customReminderMessage);
      window.open(`https://wa.me/${cleanPhone}?text=${encodedMsg}`, '_blank');
      showSnack(`WhatsApp ouvert pour ${activeReminderMember.firstName} (relance enregistrée)`);
    } else if (channel === 'SMS') {
      const encodedMsg = encodeURIComponent(customReminderMessage);
      window.open(`sms:${normalized}?body=${encodedMsg}`, '_blank');
      showSnack(`Application SMS ouverte pour ${activeReminderMember.firstName}`);
    } else {
      window.open(`tel:${normalized}`, '_blank');
      showSnack(`Composeur téléphonique ouvert pour ${activeReminderMember.firstName}`);
    }

    setActiveReminderMember(null);
    refreshData();
  };

  const handleExportCsvJournal = () => {
    const headers = ['Date', 'Type de Recette', 'Membre Concerné', 'Catégorie / Mois', 'Montant (Espèces)', 'Description / Note'];
    const rows: (string | number)[][] = [];

    // Cotisations
    for (const p of payments) {
      const m = members.find(mem => mem.id === p.memberId);
      const memberName = m ? `${m.lastName} ${m.firstName}` : 'Inconnu';
      const pAllocs = allocations.filter(a => a.paymentId === p.id);
      const monthsStr = pAllocs.map(a => `${formatMonthFr(a.month)} (${a.amount})`).join(', ');

      rows.push([
        formatDateFr(p.date),
        'Cotisation Mensuelle',
        memberName,
        monthsStr,
        p.amount,
        p.note || '',
      ]);
    }

    // Autres recettes
    for (const o of otherIncomes) {
      const cat = categories.find(c => c.id === o.categoryId)?.name || 'Autre';
      const m = o.memberId ? members.find(mem => mem.id === o.memberId) : null;
      const memberName = m ? `${m.lastName} ${m.firstName}` : '-';

      rows.push([
        formatDateFr(o.date),
        'Autre Recette',
        memberName,
        cat,
        o.amount,
        o.description || '',
      ]);
    }

    // Sort by date desc
    rows.sort((a, b) => String(b[0]).localeCompare(String(a[0])));

    const csvContent = generateCsvWithBom(headers, rows);
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `journal_recettes_${getCurrentDateStr()}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
    showSnack('Journal des recettes exporté en CSV (compatible Excel).');
  };

  const handleExportRemittancesCsv = () => {
    const headers = ['Date', 'Montant Versé (Espèces)', 'Destinataire (Caisse Principale)', 'Période Couverte', 'Note'];
    const rows = remittances.map(r => [
      formatDateFr(r.date),
      r.amount,
      r.recipient,
      r.coveredPeriod || '-',
      r.note || '',
    ]);

    const csvContent = generateCsvWithBom(headers, rows);
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `etat_versements_${getCurrentDateStr()}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
    showSnack('État des versements exporté en CSV.');
  };

  const handleBackupJson = () => {
    const json = storage.exportDatabaseJson();
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `sauvegarde_caisse_${getCurrentDateStr()}.json`;
    a.click();
    URL.revokeObjectURL(a.href);
    showSnack('Sauvegarde complète exportée en fichier JSON.');
    refreshData();
  };

  const handleRestoreJson = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!confirm('ATTENTION : La restauration remplacera l\'intégralité des données actuelles par celles du fichier. Continuer ?')) {
      e.target.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = evt => {
      const content = evt.target?.result as string;
      const res = storage.restoreDatabaseJson(content);
      if (res.success) {
        showSnack(res.message);
        refreshData();
      } else {
        alert(res.message);
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  // Helper for Member detail view
  const currentDetailMember = useMemo(() => {
    if (!memberDetailId) return null;
    return memberStatuses.find(s => s.member.id === memberDetailId) || null;
  }, [memberDetailId, memberStatuses]);

  return (
    <div className="flex justify-center items-start py-2">
      {/* Android Device Mockup Frame */}
      <div className="w-full max-w-md bg-white rounded-3xl border-8 border-slate-900 shadow-2xl overflow-hidden flex flex-col h-[750px] relative">
        {/* Android Status Bar */}
        <div className="bg-slate-900 text-slate-300 px-6 py-1.5 flex items-center justify-between text-[11px] select-none shrink-0 font-medium">
          <span>09:41</span>
          <div className="w-20 h-4 bg-slate-800 rounded-full mx-auto" />
          <div className="flex items-center gap-1.5">
            <span>100% Hors-ligne</span>
            <div className="w-2 h-2 rounded-full bg-emerald-400" />
          </div>
        </div>

        {/* Top App Bar */}
        <div className="bg-slate-900 text-white px-4 py-3 flex items-center justify-between shadow-xs shrink-0">
          <div className="flex items-center gap-2">
            {subView !== 'none' ? (
              <button
                onClick={() => setSubView('none')}
                className="p-1 -ml-1 text-slate-300 hover:text-white cursor-pointer"
              >
                <ArrowRight className="w-5 h-5 rotate-180" />
              </button>
            ) : null}
            <div>
              <h1 className="text-sm font-semibold tracking-tight">
                {subView === 'remittances'
                  ? 'Versements à la caisse'
                  : subView === 'journal'
                  ? 'Journal des recettes'
                  : subView === 'settings'
                  ? 'Paramètres & Sauvegarde'
                  : subView === 'categories'
                  ? 'Catégories de recettes'
                  : currentTab === 'dashboard'
                  ? 'Caisse Association'
                  : currentTab === 'members'
                  ? 'Membres & Cotisations'
                  : currentTab === 'income'
                  ? 'Toutes les recettes'
                  : currentTab === 'reminders'
                  ? 'Impayés & Relances'
                  : 'Options & Gestion'}
              </h1>
              <span className="text-[11px] text-slate-400 block -mt-0.5 truncate max-w-[220px]">
                {settings.associationName}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-1">
            {currentTab === 'dashboard' && (
              <button
                onClick={() => setShowFeePaymentModal(true)}
                title="Nouveau paiement"
                className="p-1.5 rounded-full bg-emerald-600 hover:bg-emerald-500 text-white cursor-pointer shadow-xs"
              >
                <Plus className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>

        {/* Main Content Area (Scrollable) */}
        <div className="flex-1 overflow-y-auto bg-slate-50 p-3 space-y-3.5">
          {/* ============================================================== */}
          {/* SUBVIEW: VERSEMENTS À LA CAISSE PRINCIPALE                     */}
          {/* ============================================================== */}
          {subView === 'remittances' && (
            <div className="space-y-3">
              {/* Summary Cards */}
              <div className="grid grid-cols-3 gap-2">
                <div className="bg-white p-2.5 rounded-xl border border-slate-200">
                  <span className="text-[10px] text-slate-500 block font-medium">Total encaissé</span>
                  <span className="text-xs font-bold text-slate-900 font-mono">
                    {formatMoney(totalCollectedAllTime, settings.currency)}
                  </span>
                </div>
                <div className="bg-white p-2.5 rounded-xl border border-slate-200">
                  <span className="text-[10px] text-slate-500 block font-medium">Total versé</span>
                  <span className="text-xs font-bold text-emerald-700 font-mono">
                    {formatMoney(totalRemittedAllTime, settings.currency)}
                  </span>
                </div>
                <div className="bg-amber-50 p-2.5 rounded-xl border border-amber-200">
                  <span className="text-[10px] text-amber-800 block font-semibold">Reste à verser</span>
                  <span className="text-xs font-bold text-amber-900 font-mono">
                    {formatMoney(remainingToRemit, settings.currency)}
                  </span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-2">
                <button
                  onClick={() => {
                    setRemittanceAmount(remainingToRemit > 0 ? remainingToRemit : 0);
                    setRemittanceRecipient(settings.lastRemittanceRecipient);
                    setShowRemittanceModal(true);
                  }}
                  className="flex-1 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-medium flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Nouveau versement
                </button>
                <button
                  onClick={() => setShowPdfPreview(true)}
                  className="py-2 px-3 bg-white border border-slate-200 hover:bg-slate-100 text-slate-700 rounded-xl text-xs font-medium flex items-center gap-1 cursor-pointer"
                >
                  <FileText className="w-3.5 h-3.5 text-rose-600" />
                  État PDF
                </button>
                <button
                  onClick={handleExportRemittancesCsv}
                  className="p-2 bg-white border border-slate-200 hover:bg-slate-100 text-slate-700 rounded-xl text-xs cursor-pointer"
                  title="Export CSV"
                >
                  <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-600" />
                </button>
              </div>

              {/* Remittance History */}
              <div className="bg-white rounded-xl border border-slate-200 p-3 space-y-2">
                <h3 className="text-xs font-semibold text-slate-800">Historique des versements</h3>
                {remittances.length === 0 ? (
                  <p className="text-xs text-slate-400 py-4 text-center">Aucun versement enregistré.</p>
                ) : (
                  <div className="divide-y divide-slate-100">
                    {remittances.map(r => (
                      <div key={r.id} className="py-2 flex items-center justify-between text-xs">
                        <div>
                          <div className="font-semibold text-slate-900">
                            {formatMoney(r.amount, settings.currency)}
                          </div>
                          <div className="text-[11px] text-slate-500">
                            Reçu par : <span className="text-slate-700 font-medium">{r.recipient}</span>
                          </div>
                          {r.coveredPeriod && (
                            <div className="text-[10px] text-slate-400">Période : {r.coveredPeriod}</div>
                          )}
                        </div>
                        <div className="text-right">
                          <span className="text-[11px] text-slate-400">{formatDateFr(r.date)}</span>
                          <button
                            onClick={() => {
                              if (confirm('Supprimer ce versement ?')) {
                                storage.deleteRemittance(r.id);
                                showSnack('Versement supprimé.');
                                refreshData();
                              }
                            }}
                            className="block text-rose-500 hover:text-rose-700 text-[10px] mt-1 ml-auto cursor-pointer"
                          >
                            Supprimer
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* SUBVIEW: JOURNAL DES RECETTES                                   */}
          {/* ============================================================== */}
          {subView === 'journal' && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-500">
                  {payments.length + otherIncomes.length} opération(s)
                </span>
                <button
                  onClick={handleExportCsvJournal}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1.5 bg-emerald-700 hover:bg-emerald-800 text-white rounded-lg text-xs font-medium cursor-pointer shadow-xs"
                >
                  <FileSpreadsheet className="w-3.5 h-3.5" />
                  Exporter CSV Excel (BOM)
                </button>
              </div>

              <div className="bg-white rounded-xl border border-slate-200 divide-y divide-slate-100 overflow-hidden">
                {[
                  ...payments.map(p => ({
                    id: p.id,
                    type: 'COTISATION' as const,
                    date: p.date,
                    amount: p.amount,
                    memberId: p.memberId,
                    note: p.note,
                    sub: allocations
                      .filter(a => a.paymentId === p.id)
                      .map(a => formatMonthFr(a.month, true))
                      .join(', '),
                  })),
                  ...otherIncomes.map(o => ({
                    id: o.id,
                    type: 'AUTRE' as const,
                    date: o.date,
                    amount: o.amount,
                    memberId: o.memberId,
                    note: o.description,
                    sub: categories.find(c => c.id === o.categoryId)?.name || 'Autre',
                  })),
                ]
                  .sort((a, b) => b.date.localeCompare(a.date))
                  .map(item => {
                    const m = item.memberId ? members.find(mem => mem.id === item.memberId) : null;
                    return (
                      <div key={item.id} className="p-3 text-xs flex items-center justify-between">
                        <div className="space-y-0.5">
                          <div className="flex items-center gap-1.5">
                            <span
                              className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${
                                item.type === 'COTISATION'
                                  ? 'bg-blue-50 text-blue-700'
                                  : 'bg-purple-50 text-purple-700'
                              }`}
                            >
                              {item.type === 'COTISATION' ? 'Cotisation' : 'Autre'}
                            </span>
                            <span className="font-semibold text-slate-800">
                              {m ? `${m.firstName} ${m.lastName}` : item.sub}
                            </span>
                          </div>
                          <div className="text-[11px] text-slate-500">
                            {item.type === 'COTISATION' ? `Mois : ${item.sub}` : item.note || item.sub}
                          </div>
                          <div className="text-[10px] text-slate-400">{formatDateFr(item.date)}</div>
                        </div>

                        <div className="text-right">
                          <div className="font-mono font-bold text-slate-900">
                            +{formatMoney(item.amount, settings.currency)}
                          </div>
                          {item.type === 'COTISATION' && (
                            <button
                              onClick={() => {
                                if (confirm('Annuler ce paiement de cotisation ? Les mois redeviendront impayés.')) {
                                  storage.cancelPayment(item.id);
                                  showSnack('Paiement annulé.');
                                  refreshData();
                                }
                              }}
                              className="text-[10px] text-rose-500 hover:text-rose-700 mt-1 cursor-pointer"
                            >
                              Annuler
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* SUBVIEW: PARAMÈTRES & SAUVEGARDE                               */}
          {/* ============================================================== */}
          {subView === 'settings' && (
            <div className="space-y-3 text-xs">
              {/* Configuration Générale */}
              <div className="bg-white p-3.5 rounded-xl border border-slate-200 space-y-3">
                <h3 className="font-semibold text-slate-900 flex items-center gap-1.5">
                  <Settings className="w-4 h-4 text-slate-500" />
                  Configuration de l'association
                </h3>

                <div className="space-y-2">
                  <div>
                    <label className="text-slate-500 block mb-1">Nom de l'association</label>
                    <input
                      type="text"
                      value={settings.associationName}
                      onChange={e => {
                        const updated = { ...settings, associationName: e.target.value };
                        setSettings(updated);
                        storage.saveSettings(updated);
                      }}
                      className="w-full px-2.5 py-1.5 rounded-lg border border-slate-200 text-xs"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="text-slate-500 block mb-1">Devise affichée</label>
                      <input
                        type="text"
                        value={settings.currency}
                        onChange={e => {
                          const updated = { ...settings, currency: e.target.value };
                          setSettings(updated);
                          storage.saveSettings(updated);
                        }}
                        className="w-full px-2.5 py-1.5 rounded-lg border border-slate-200 text-xs font-mono"
                        placeholder="Ar, €, FCFA..."
                      />
                    </div>
                    <div>
                      <label className="text-slate-500 block mb-1">Indicatif pays par défaut</label>
                      <input
                        type="text"
                        value={settings.defaultCountryPrefix}
                        onChange={e => {
                          const updated = { ...settings, defaultCountryPrefix: e.target.value };
                          setSettings(updated);
                          storage.saveSettings(updated);
                        }}
                        className="w-full px-2.5 py-1.5 rounded-lg border border-slate-200 text-xs font-mono"
                        placeholder="+261, +33..."
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* Cotisation par défaut & Historique */}
              <div className="bg-white p-3.5 rounded-xl border border-slate-200 space-y-2">
                <h3 className="font-semibold text-slate-900">Cotisation mensuelle générale</h3>
                <p className="text-slate-500 text-[11px]">
                  Un changement s'applique à partir du mois choisi et ne modifie pas le passé.
                </p>

                <div className="divide-y divide-slate-100">
                  {feeRates.map(r => (
                    <div key={r.id} className="py-1.5 flex items-center justify-between text-xs">
                      <span className="text-slate-600">Depuis {formatMonthFr(r.effectiveFromMonth)}</span>
                      <span className="font-mono font-bold text-slate-900">
                        {formatMoney(r.amount, settings.currency)} / mois
                      </span>
                    </div>
                  ))}
                </div>

                <div className="pt-2 border-t border-slate-100 flex items-center gap-2">
                  <input
                    type="month"
                    id="newRateMonth"
                    defaultValue={currentMonth}
                    className="px-2 py-1 border border-slate-200 rounded text-[11px]"
                  />
                  <input
                    type="number"
                    id="newRateAmount"
                    placeholder="Montant"
                    className="w-24 px-2 py-1 border border-slate-200 rounded text-[11px] font-mono"
                  />
                  <button
                    onClick={() => {
                      const m = (document.getElementById('newRateMonth') as HTMLInputElement)?.value;
                      const a = parseInt((document.getElementById('newRateAmount') as HTMLInputElement)?.value, 10);
                      if (m && a > 0) {
                        storage.addFeeRate(m, a);
                        showSnack('Nouveau taux de cotisation enregistré.');
                        refreshData();
                      }
                    }}
                    className="px-2.5 py-1 bg-slate-900 text-white rounded text-[11px] font-medium cursor-pointer"
                  >
                    Ajouter
                  </button>
                </div>
              </div>

              {/* Modèle de message de relance */}
              <div className="bg-white p-3.5 rounded-xl border border-slate-200 space-y-2">
                <h3 className="font-semibold text-slate-900">Modèle du message de relance</h3>
                <p className="text-[11px] text-slate-500">
                  Variables : {'{prenom}'}, {'{nom}'}, {'{montant_du}'}, {'{mois_impayes}'}, {'{association}'}, {'{devise}'}
                </p>
                <textarea
                  rows={3}
                  value={settings.reminderMessageTemplate}
                  onChange={e => {
                    const updated = { ...settings, reminderMessageTemplate: e.target.value };
                    setSettings(updated);
                    storage.saveSettings(updated);
                  }}
                  className="w-full p-2 border border-slate-200 rounded-lg text-xs leading-relaxed"
                />
              </div>

              {/* Sauvegarde & Restauration */}
              <div className="bg-white p-3.5 rounded-xl border border-slate-200 space-y-3">
                <h3 className="font-semibold text-slate-900 flex items-center gap-1.5">
                  <HardDriveDownload className="w-4 h-4 text-emerald-600" />
                  Sauvegarde & Restauration (SAF)
                </h3>

                <div className="text-[11px] text-slate-500">
                  Dernière sauvegarde :{' '}
                  <span className="font-medium text-slate-800">
                    {settings.lastBackupDate ? formatDateFr(settings.lastBackupDate) : 'Aucune sauvegarde'}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={handleBackupJson}
                    className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-medium flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <HardDriveDownload className="w-3.5 h-3.5" />
                    Exporter sauvegarde (JSON)
                  </button>

                  <label className="flex-1 py-2 bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-lg text-xs font-medium flex items-center justify-center gap-1.5 cursor-pointer">
                    <HardDriveUpload className="w-3.5 h-3.5 text-slate-600" />
                    Restaurer
                    <input type="file" accept=".json" onChange={handleRestoreJson} className="hidden" />
                  </label>
                </div>
              </div>

              {/* Remise à zéro / Base Vierge */}
              <div className="bg-white p-3.5 rounded-xl border border-rose-100 space-y-2">
                <h3 className="font-semibold text-rose-900 text-xs">Données de démonstration & Base vierge</h3>
                <p className="text-[11px] text-slate-500">
                  Vous pouvez vider tous les membres et opérations d'exemple pour commencer immédiatement avec la caisse réelle de votre association.
                </p>
                <div className="flex items-center gap-2 pt-1">
                  <button
                    onClick={() => {
                      if (confirm('Voulez-vous vider tous les membres et opérations d\'exemple pour avoir une base 100% vierge ?')) {
                        storage.clearAllData();
                        showSnack('Base de données vidée : vous démarrez avec 0 membre.');
                        refreshData();
                      }
                    }}
                    className="flex-1 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-lg text-xs font-semibold cursor-pointer"
                  >
                    Vider les exemples (Base vierge)
                  </button>

                  <button
                    onClick={() => {
                      if (confirm('Recharger les données de démonstration ?')) {
                        storage.resetToDefault();
                        showSnack('Données d\'exemple rechargées.');
                        refreshData();
                      }
                    }}
                    className="py-2 px-3 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium cursor-pointer"
                  >
                    Recharger exemples
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* TAB 1 : DASHBOARD (TABLEAU DE BORD)                            */}
          {/* ============================================================== */}
          {subView === 'none' && currentTab === 'dashboard' && (
            <div className="space-y-3">
              {/* Alert Backup if overdue */}
              {backupWarning && (
                <div className="bg-rose-50 border border-rose-200 rounded-xl p-2.5 flex items-start gap-2 text-rose-900 text-xs">
                  <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-semibold">Sauvegarde recommandée !</span>
                    <span className="block text-[11px] text-rose-700">
                      Aucune sauvegarde n'a été effectuée depuis plus de 30 jours.
                    </span>
                  </div>
                </div>
              )}

              {/* Total Encaissé Cards */}
              <div className="bg-white rounded-2xl border border-slate-200 p-3.5 shadow-xs space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Total Encaissé en Espèces
                  </span>
                  <span className="text-[11px] text-slate-400 font-medium">{formatMonthFr(currentMonth)}</span>
                </div>

                <div className="flex items-baseline justify-between">
                  <div>
                    <span className="text-2xl font-bold font-mono tracking-tight text-slate-900">
                      {formatMoney(feeCurrentMonth + otherCurrentMonth, settings.currency)}
                    </span>
                    <span className="text-xs text-slate-500 block">Ce mois-ci</span>
                  </div>
                  <div className="text-right">
                    <span className="text-sm font-semibold font-mono text-slate-700">
                      {formatMoney(feeCurrentYear + otherCurrentYear, settings.currency)}
                    </span>
                    <span className="text-[11px] text-slate-400 block">Année {currentYear}</span>
                  </div>
                </div>

                {/* Breakdown Bar */}
                <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-600">
                  <div className="flex items-center gap-1.5">
                    <div className="w-2.5 h-2.5 rounded-full bg-blue-500" />
                    <span>Cotisations : {formatMoney(feeCurrentMonth, settings.currency)}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <div className="w-2.5 h-2.5 rounded-full bg-purple-500" />
                    <span>Autres : {formatMoney(otherCurrentMonth, settings.currency)}</span>
                  </div>
                </div>
              </div>

              {/* Reste à Verser Card */}
              <div className="bg-amber-50 rounded-2xl border border-amber-200/80 p-3.5 shadow-xs flex items-center justify-between">
                <div>
                  <span className="text-[11px] font-semibold text-amber-800 uppercase tracking-wider block">
                    Caisse Principale
                  </span>
                  <span className="text-lg font-bold font-mono text-amber-950">
                    {formatMoney(remainingToRemit, settings.currency)}
                  </span>
                  <span className="text-[11px] text-amber-700 block">Reste à verser en caisse</span>
                </div>
                <button
                  onClick={() => setSubView('remittances')}
                  className="px-3 py-1.5 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1 cursor-pointer shadow-xs"
                >
                  Verser
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>

              {/* Members in Late Status */}
              <div className="bg-white rounded-2xl border border-slate-200 p-3.5 shadow-xs space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center font-bold">
                      {lateMembers.length}
                    </div>
                    <div>
                      <h3 className="text-xs font-semibold text-slate-900">Membres à relancer</h3>
                      <span className="text-[11px] text-slate-500">
                        Total des impayés : {formatMoney(totalUnpaidAmount, settings.currency)}
                      </span>
                    </div>
                  </div>
                  <button
                    onClick={() => setCurrentTab('reminders')}
                    className="text-xs font-semibold text-slate-700 hover:text-slate-900 flex items-center gap-0.5 cursor-pointer"
                  >
                    Voir
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Fast Action Buttons */}
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setShowFeePaymentModal(true)}
                  className="p-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-2xl text-left shadow-xs transition-colors cursor-pointer"
                >
                  <Plus className="w-5 h-5 mb-1" />
                  <div className="text-xs font-semibold">Paiement Cotisation</div>
                  <div className="text-[10px] text-emerald-100">En espèces (auto-répartition)</div>
                </button>
                <button
                  onClick={() => setShowOtherIncomeModal(true)}
                  className="p-3 bg-slate-900 hover:bg-slate-800 text-white rounded-2xl text-left shadow-xs transition-colors cursor-pointer"
                >
                  <DollarSign className="w-5 h-5 mb-1 text-amber-400" />
                  <div className="text-xs font-semibold">Autre Recette</div>
                  <div className="text-[10px] text-slate-300">Dons, événements, etc.</div>
                </button>
              </div>

              {/* Recent Operations Preview */}
              <div className="bg-white rounded-2xl border border-slate-200 p-3 space-y-2">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-semibold text-slate-800">Dernières opérations</h3>
                  <button
                    onClick={() => setSubView('journal')}
                    className="text-[11px] text-slate-500 hover:text-slate-800 font-medium cursor-pointer"
                  >
                    Tout voir
                  </button>
                </div>

                <div className="divide-y divide-slate-100">
                  {payments.slice(-3).reverse().map(p => {
                    const m = members.find(mem => mem.id === p.memberId);
                    return (
                      <div key={p.id} className="py-2 flex items-center justify-between text-xs">
                        <div>
                          <div className="font-semibold text-slate-900">
                            {m ? `${m.firstName} ${m.lastName}` : 'Membre'}
                          </div>
                          <div className="text-[10px] text-slate-400">{formatDateFr(p.date)}</div>
                        </div>
                        <div className="font-mono font-bold text-emerald-600">
                          +{formatMoney(p.amount, settings.currency)}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* TAB 2 : MEMBRES & COTISATIONS                                  */}
          {/* ============================================================== */}
          {subView === 'none' && currentTab === 'members' && (
            <div className="space-y-3">
              {/* Search & Filter */}
              <div className="space-y-2">
                <div className="relative">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                  <input
                    type="text"
                    value={memberSearch}
                    onChange={e => setMemberSearch(e.target.value)}
                    placeholder="Rechercher par nom, téléphone..."
                    className="w-full pl-9 pr-3 py-2 bg-white border border-slate-200 rounded-xl text-xs placeholder:text-slate-400"
                  />
                </div>

                {/* Filter segments */}
                <div className="flex items-center gap-1 overflow-x-auto pb-1 text-xs">
                  <button
                    onClick={() => setMemberFilter('all')}
                    className={`px-3 py-1 rounded-lg font-medium whitespace-nowrap cursor-pointer ${
                      memberFilter === 'all'
                        ? 'bg-slate-900 text-white'
                        : 'bg-white text-slate-600 border border-slate-200'
                    }`}
                  >
                    Tous ({members.length})
                  </button>
                  <button
                    onClick={() => setMemberFilter('active')}
                    className={`px-3 py-1 rounded-lg font-medium whitespace-nowrap cursor-pointer ${
                      memberFilter === 'active'
                        ? 'bg-slate-900 text-white'
                        : 'bg-white text-slate-600 border border-slate-200'
                    }`}
                  >
                    Actifs ({members.filter(m => m.isActive).length})
                  </button>
                  <button
                    onClick={() => setMemberFilter('late')}
                    className={`px-3 py-1 rounded-lg font-medium whitespace-nowrap cursor-pointer ${
                      memberFilter === 'late'
                        ? 'bg-rose-700 text-white'
                        : 'bg-white text-rose-700 border border-rose-200'
                    }`}
                  >
                    En retard ({lateMembers.length})
                  </button>
                  <button
                    onClick={() => setMemberFilter('archived')}
                    className={`px-3 py-1 rounded-lg font-medium whitespace-nowrap cursor-pointer ${
                      memberFilter === 'archived'
                        ? 'bg-slate-900 text-white'
                        : 'bg-white text-slate-600 border border-slate-200'
                    }`}
                  >
                    Archivés ({members.filter(m => !m.isActive).length})
                  </button>
                </div>
              </div>

              {/* Add Member Button */}
              <button
                onClick={() => {
                  setEditingMember(null);
                  setShowMemberModal(true);
                }}
                className="w-full py-2 bg-white hover:bg-slate-50 border border-dashed border-slate-300 rounded-xl text-xs font-medium text-slate-700 flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <Plus className="w-4 h-4 text-emerald-600" />
                Ajouter un nouveau membre
              </button>

              {/* Members List */}
              <div className="space-y-2">
                {filteredMembers.length === 0 ? (
                  <div className="bg-white rounded-xl p-6 text-center border border-slate-200">
                    <Users className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                    <h3 className="text-xs font-semibold text-slate-800">Aucun membre trouvé</h3>
                    <p className="text-[11px] text-slate-400 mt-1">
                      {members.length === 0
                        ? "La base est actuellement vide. Cliquez sur « Ajouter un nouveau membre » pour inscrire les membres réels de votre association !"
                        : "Aucun membre ne correspond aux critères de recherche ou de filtre."}
                    </p>
                  </div>
                ) : (
                  filteredMembers.map(item => {
                    const m = item.member;
                    return (
                      <div
                        key={m.id}
                        onClick={() => setMemberDetailId(m.id)}
                        className="bg-white p-3 rounded-xl border border-slate-200 hover:border-slate-300 transition-colors cursor-pointer flex items-center justify-between"
                      >
                        <div className="space-y-1">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-xs text-slate-900">
                              {m.lastName.toUpperCase()} {m.firstName}
                            </span>
                            {!m.isActive && (
                              <span className="text-[10px] bg-slate-100 text-slate-500 px-1.5 py-0.5 rounded">
                                Archivé
                              </span>
                            )}
                            {m.customMonthlyAmount && (
                              <span className="text-[10px] bg-amber-50 text-amber-700 px-1.5 py-0.5 rounded font-mono">
                                {formatMoney(m.customMonthlyAmount, settings.currency)}/m
                              </span>
                            )}
                          </div>

                          <div className="text-[11px] text-slate-500 flex items-center gap-2">
                            <span>Adhésion : {formatMonthFr(m.joinMonth, true)}</span>
                            {m.phone ? (
                              <span>· {m.phone}</span>
                            ) : (
                              <span className="text-amber-600 font-medium">· Tél manquant</span>
                            )}
                          </div>
                        </div>

                        <div className="text-right">
                          {item.monthsLateCount > 0 ? (
                            <div className="space-y-0.5">
                              <span className="inline-block text-[10px] font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-full">
                                {item.monthsLateCount} mois dû{item.monthsLateCount > 1 ? 's' : ''}
                              </span>
                              <div className="font-mono text-xs font-bold text-rose-700">
                                {formatMoney(item.balanceDue, settings.currency)}
                              </div>
                            </div>
                          ) : (
                            <span className="inline-block text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                              À jour
                            </span>
                          )}
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* TAB 3 : RECETTES & COTISATIONS                                 */}
          {/* ============================================================== */}
          {subView === 'none' && currentTab === 'income' && (
            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setShowFeePaymentModal(true)}
                  className="p-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-left cursor-pointer"
                >
                  <Plus className="w-4 h-4 mb-1" />
                  <div className="text-xs font-semibold">Paiement Cotisation</div>
                  <div className="text-[10px] text-emerald-100">En espèces</div>
                </button>
                <button
                  onClick={() => setShowOtherIncomeModal(true)}
                  className="p-3 bg-purple-700 hover:bg-purple-800 text-white rounded-xl text-left cursor-pointer"
                >
                  <Plus className="w-4 h-4 mb-1" />
                  <div className="text-xs font-semibold">Autre Recette</div>
                  <div className="text-[10px] text-purple-200">Dons, événements...</div>
                </button>
              </div>

              {/* List of Other Incomes */}
              <div className="bg-white rounded-xl border border-slate-200 p-3 space-y-2">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-semibold text-slate-800">Autres Recettes</h3>
                  <button
                    onClick={() => setSubView('categories')}
                    className="text-[11px] text-slate-500 hover:text-slate-800 cursor-pointer"
                  >
                    Gérer catégories
                  </button>
                </div>

                <div className="divide-y divide-slate-100">
                  {otherIncomes.length === 0 ? (
                    <p className="text-xs text-slate-400 py-4 text-center">Aucune autre recette.</p>
                  ) : (
                    otherIncomes.map(o => {
                      const cat = categories.find(c => c.id === o.categoryId)?.name || 'Autre';
                      const m = o.memberId ? members.find(mem => mem.id === o.memberId) : null;
                      return (
                        <div key={o.id} className="py-2 flex items-center justify-between text-xs">
                          <div>
                            <div className="font-semibold text-slate-800">{cat}</div>
                            <div className="text-[11px] text-slate-500">
                              {o.description || (m ? `Par ${m.firstName} ${m.lastName}` : '-')}
                            </div>
                            <div className="text-[10px] text-slate-400">{formatDateFr(o.date)}</div>
                          </div>
                          <div className="text-right">
                            <span className="font-mono font-bold text-purple-700">
                              +{formatMoney(o.amount, settings.currency)}
                            </span>
                            <button
                              onClick={() => {
                                if (confirm('Supprimer cette recette ?')) {
                                  storage.deleteOtherIncome(o.id);
                                  showSnack('Recette supprimée.');
                                  refreshData();
                                }
                              }}
                              className="block text-[10px] text-rose-500 hover:text-rose-700 mt-1 ml-auto cursor-pointer"
                            >
                              Supprimer
                            </button>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* TAB 4 : IMPAYÉS & RELANCES                                     */}
          {/* ============================================================== */}
          {subView === 'none' && currentTab === 'reminders' && (
            <div className="space-y-3">
              {/* Reminder Header Bar */}
              <div className="bg-white p-3 rounded-xl border border-slate-200 flex items-center justify-between text-xs">
                <div>
                  <div className="font-semibold text-slate-900">
                    {lateMembers.length} membre{lateMembers.length > 1 ? 's' : ''} en retard
                  </div>
                  <div className="text-[11px] text-slate-500">
                    Total dû : {formatMoney(totalUnpaidAmount, settings.currency)}
                  </div>
                </div>

                {lateMembers.length > 0 && (
                  <button
                    onClick={() => {
                      setReminderQueueIndex(0);
                      setShowReminderQueue(true);
                    }}
                    className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-medium flex items-center gap-1.5 cursor-pointer shadow-xs"
                  >
                    <Send className="w-3.5 h-3.5" />
                    Relancer en série
                  </button>
                )}
              </div>

              {/* Late Members List */}
              <div className="space-y-2">
                {lateMembers.length === 0 ? (
                  <div className="bg-white rounded-xl p-8 text-center border border-slate-200">
                    <Check className="w-8 h-8 text-emerald-500 mx-auto mb-2" />
                    <h3 className="text-xs font-semibold text-slate-800">Tous les membres sont à jour !</h3>
                    <p className="text-[11px] text-slate-400 mt-1">Aucune relance nécessaire pour le moment.</p>
                  </div>
                ) : (
                  lateMembers.map(s => {
                    const m = s.member;
                    const unpaidMonthsNames = s.unpaidMonths
                      .map(u => formatMonthFr(u.month, true))
                      .join(', ');

                    return (
                      <div
                        key={m.id}
                        className="bg-white p-3 rounded-xl border border-rose-100 shadow-xs space-y-2"
                      >
                        <div className="flex items-start justify-between">
                          <div>
                            <span className="font-bold text-xs text-slate-900">
                              {m.lastName.toUpperCase()} {m.firstName}
                            </span>
                            <div className="text-[11px] text-rose-700 font-medium mt-0.5">
                              {s.monthsLateCount} mois impayé{s.monthsLateCount > 1 ? 's' : ''} : {unpaidMonthsNames}
                            </div>
                            <div className="text-[10px] text-slate-400 mt-0.5">
                              {s.lastReminder
                                ? `Dernière relance : ${s.lastReminder.date} (${s.lastReminder.channel})`
                                : 'Jamais relancé'}
                            </div>
                          </div>

                          <div className="text-right">
                            <span className="text-xs font-mono font-bold text-rose-700">
                              {formatMoney(s.balanceDue, settings.currency)}
                            </span>
                            {!m.phone && (
                              <span className="block text-[10px] text-amber-600 font-semibold mt-1">
                                Tél manquant
                              </span>
                            )}
                          </div>
                        </div>

                        {/* Quick action buttons */}
                        <div className="flex items-center gap-1.5 pt-2 border-t border-slate-100">
                          <button
                            onClick={() => handleOpenReminderModal(s)}
                            className="flex-1 py-1.5 px-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-[11px] font-medium flex items-center justify-center gap-1 cursor-pointer"
                          >
                            <MessageSquare className="w-3.5 h-3.5" />
                            WhatsApp / SMS
                          </button>
                          <button
                            onClick={() => {
                              setSelectedMemberForPayment(m.id);
                              setShowFeePaymentModal(true);
                            }}
                            className="py-1.5 px-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-[11px] font-medium cursor-pointer"
                          >
                            Régler
                          </button>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* TAB 5 : PLUS / MENU                                            */}
          {/* ============================================================== */}
          {subView === 'none' && currentTab === 'more' && (
            <div className="space-y-2 text-xs">
              <button
                onClick={() => setSubView('remittances')}
                className="w-full bg-white p-3.5 rounded-xl border border-slate-200 flex items-center justify-between hover:bg-slate-50 cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center">
                    <Building className="w-4 h-4" />
                  </div>
                  <div className="text-left">
                    <div className="font-semibold text-slate-900">Versements à la caisse</div>
                    <div className="text-[11px] text-slate-500">
                      Reste à verser : {formatMoney(remainingToRemit, settings.currency)}
                    </div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400" />
              </button>

              <button
                onClick={() => setSubView('journal')}
                className="w-full bg-white p-3.5 rounded-xl border border-slate-200 flex items-center justify-between hover:bg-slate-50 cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center">
                    <FileSpreadsheet className="w-4 h-4" />
                  </div>
                  <div className="text-left">
                    <div className="font-semibold text-slate-900">Journal & Export CSV</div>
                    <div className="text-[11px] text-slate-500">Toutes les recettes chronologiques</div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400" />
              </button>

              <button
                onClick={() => setShowPdfPreview(true)}
                className="w-full bg-white p-3.5 rounded-xl border border-slate-200 flex items-center justify-between hover:bg-slate-50 cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-rose-50 text-rose-700 flex items-center justify-center">
                    <FileText className="w-4 h-4" />
                  </div>
                  <div className="text-left">
                    <div className="font-semibold text-slate-900">État de versement PDF</div>
                    <div className="text-[11px] text-slate-500">Document imprimable avec signatures</div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400" />
              </button>

              <button
                onClick={() => setSubView('settings')}
                className="w-full bg-white p-3.5 rounded-xl border border-slate-200 flex items-center justify-between hover:bg-slate-50 cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-slate-100 text-slate-700 flex items-center justify-center">
                    <Settings className="w-4 h-4" />
                  </div>
                  <div className="text-left">
                    <div className="font-semibold text-slate-900">Paramètres & Sauvegarde</div>
                    <div className="text-[11px] text-slate-500">Devise, modèles, export/import JSON</div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400" />
              </button>
            </div>
          )}
        </div>

        {/* Bottom Navigation Bar (Material 3 Tabs) */}
        <div className="bg-white border-t border-slate-200 px-2 py-1.5 flex items-center justify-around shrink-0 select-none">
          <button
            onClick={() => {
              setSubView('none');
              setCurrentTab('dashboard');
            }}
            className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-colors cursor-pointer ${
              currentTab === 'dashboard' && subView === 'none'
                ? 'text-slate-900 font-semibold'
                : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <Wallet className="w-4 h-4" />
            <span className="text-[10px]">Accueil</span>
          </button>

          <button
            onClick={() => {
              setSubView('none');
              setCurrentTab('members');
            }}
            className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-colors cursor-pointer ${
              currentTab === 'members' && subView === 'none'
                ? 'text-slate-900 font-semibold'
                : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <Users className="w-4 h-4" />
            <span className="text-[10px]">Membres</span>
          </button>

          <button
            onClick={() => {
              setSubView('none');
              setCurrentTab('income');
            }}
            className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-colors cursor-pointer ${
              currentTab === 'income' && subView === 'none'
                ? 'text-slate-900 font-semibold'
                : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <DollarSign className="w-4 h-4" />
            <span className="text-[10px]">Recettes</span>
          </button>

          <button
            onClick={() => {
              setSubView('none');
              setCurrentTab('reminders');
            }}
            className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-colors cursor-pointer relative ${
              currentTab === 'reminders' && subView === 'none'
                ? 'text-slate-900 font-semibold'
                : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <div className="relative">
              <MessageSquare className="w-4 h-4" />
              {lateMembers.length > 0 && (
                <span className="absolute -top-1 -right-2 bg-rose-600 text-white text-[9px] font-bold rounded-full w-3.5 h-3.5 flex items-center justify-center">
                  {lateMembers.length}
                </span>
              )}
            </div>
            <span className="text-[10px]">Relances</span>
          </button>

          <button
            onClick={() => {
              setSubView('none');
              setCurrentTab('more');
            }}
            className={`flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl transition-colors cursor-pointer ${
              currentTab === 'more' || subView !== 'none'
                ? 'text-slate-900 font-semibold'
                : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <Settings className="w-4 h-4" />
            <span className="text-[10px]">Plus</span>
          </button>
        </div>

        {/* Floating Snackbar Alert */}
        {snackbar && (
          <div className="absolute bottom-16 left-4 right-4 bg-slate-900 text-white px-3 py-2 rounded-xl text-xs shadow-lg animate-in fade-in duration-200 z-50 flex items-center justify-between">
            <span>{snackbar}</span>
            <button onClick={() => setSnackbar(null)} className="text-slate-400 hover:text-white">
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* ============================================================== */}
        {/* MODAL : ENREGISTREMENT PAIEMENT COTISATION                     */}
        {/* ============================================================== */}
        {showFeePaymentModal && (
          <div className="absolute inset-0 bg-black/60 z-50 flex items-end">
            <div className="w-full bg-white rounded-t-3xl max-h-[90%] overflow-y-auto p-4 space-y-3">
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <h3 className="font-semibold text-xs text-slate-900">Paiement de Cotisation (Espèces)</h3>
                <button
                  onClick={() => setShowFeePaymentModal(false)}
                  className="p-1 text-slate-400 hover:text-slate-600"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-2 text-xs">
                <div>
                  <label className="text-slate-500 block mb-1">Membre</label>
                  <select
                    value={selectedMemberForPayment}
                    onChange={e => setSelectedMemberForPayment(e.target.value)}
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                  >
                    <option value="">Sélectionner un membre...</option>
                    {members
                      .filter(m => m.isActive)
                      .map(m => (
                        <option key={m.id} value={m.id}>
                          {m.lastName.toUpperCase()} {m.firstName} ({formatMonthFr(m.joinMonth, true)})
                        </option>
                      ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-slate-500 block mb-1">Montant reçu en espèces</label>
                    <input
                      type="number"
                      step="500"
                      value={paymentAmount}
                      onChange={e => setPaymentAmount(Math.max(0, parseInt(e.target.value, 10) || 0))}
                      className="w-full p-2 border border-slate-200 rounded-lg text-xs font-mono font-bold"
                    />
                  </div>
                  <div>
                    <label className="text-slate-500 block mb-1">Date</label>
                    <input
                      type="date"
                      value={paymentDate}
                      onChange={e => setPaymentDate(e.target.value)}
                      className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-slate-500 block mb-1">Note optionnelle</label>
                  <input
                    type="text"
                    value={paymentNote}
                    onChange={e => setPaymentNote(e.target.value)}
                    placeholder="Remise en main propre lors de la réunion..."
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                  />
                </div>

                {/* Ventilation Preview */}
                {allocationItems.length > 0 && (
                  <div className="bg-slate-50 p-2.5 rounded-xl border border-slate-200 space-y-1.5">
                    <span className="font-semibold text-[11px] text-slate-700 block">
                      Aperçu de la répartition automatique :
                    </span>
                    <div className="divide-y divide-slate-200">
                      {allocationItems.map((item, idx) => (
                        <div key={item.month} className="py-1 flex items-center justify-between text-[11px]">
                          <div>
                            <span className="font-medium text-slate-800">{item.monthLabel}</span>
                            <span className="text-[10px] text-slate-500 ml-1">
                              ({item.statusAfter === 'PAID'
                                ? 'Soldé'
                                : item.statusAfter === 'ADVANCE'
                                ? 'Avance'
                                : 'Partiel'})
                            </span>
                          </div>
                          <div className="flex items-center gap-1 font-mono font-bold text-emerald-700">
                            <span>{formatMoney(item.allocatedAmount, settings.currency)}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <button
                  onClick={handleRecordFeePayment}
                  className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-semibold cursor-pointer shadow-xs"
                >
                  Valider et enregistrer le paiement
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* MODAL : NOUVELLE AUTRE RECETTE                                 */}
        {/* ============================================================== */}
        {showOtherIncomeModal && (
          <div className="absolute inset-0 bg-black/60 z-50 flex items-end">
            <form
              onSubmit={handleRecordOtherIncome}
              className="w-full bg-white rounded-t-3xl max-h-[90%] overflow-y-auto p-4 space-y-3"
            >
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <h3 className="font-semibold text-xs text-slate-900">Enregistrer une Autre Recette</h3>
                <button
                  type="button"
                  onClick={() => setShowOtherIncomeModal(false)}
                  className="p-1 text-slate-400 hover:text-slate-600"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-2 text-xs">
                <div>
                  <label className="text-slate-500 block mb-1">Catégorie</label>
                  <select
                    value={incomeCategoryId}
                    onChange={e => setIncomeCategoryId(e.target.value)}
                    required
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                  >
                    <option value="">Sélectionner une catégorie...</option>
                    {categories
                      .filter(c => c.isActive)
                      .map(c => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-slate-500 block mb-1">Montant en espèces</label>
                    <input
                      type="number"
                      step="500"
                      value={incomeAmount}
                      onChange={e => setIncomeAmount(Math.max(0, parseInt(e.target.value, 10) || 0))}
                      required
                      className="w-full p-2 border border-slate-200 rounded-lg text-xs font-mono font-bold"
                    />
                  </div>
                  <div>
                    <label className="text-slate-500 block mb-1">Date</label>
                    <input
                      type="date"
                      value={incomeDate}
                      onChange={e => setIncomeDate(e.target.value)}
                      required
                      className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-slate-500 block mb-1">Membre concerné (optionnel)</label>
                  <select
                    value={incomeMemberId}
                    onChange={e => setIncomeMemberId(e.target.value)}
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                  >
                    <option value="">Aucun / Association</option>
                    {members.map(m => (
                      <option key={m.id} value={m.id}>
                        {m.lastName.toUpperCase()} {m.firstName}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="text-slate-500 block mb-1">Description / Motif</label>
                  <input
                    type="text"
                    value={incomeDesc}
                    onChange={e => setIncomeDesc(e.target.value)}
                    placeholder="ex: Don anonyme, amende retard..."
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-2.5 bg-purple-700 hover:bg-purple-800 text-white rounded-xl text-xs font-semibold cursor-pointer shadow-xs"
                >
                  Enregistrer la recette
                </button>
              </div>
            </form>
          </div>
        )}

        {/* ============================================================== */}
        {/* MODAL : NOUVEAU VERSEMENT À LA CAISSE                          */}
        {/* ============================================================== */}
        {showRemittanceModal && (
          <div className="absolute inset-0 bg-black/60 z-50 flex items-end">
            <form
              onSubmit={handleRecordRemittance}
              className="w-full bg-white rounded-t-3xl max-h-[90%] overflow-y-auto p-4 space-y-3"
            >
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <h3 className="font-semibold text-xs text-slate-900">Nouveau Versement à la Caisse</h3>
                <button
                  type="button"
                  onClick={() => setShowRemittanceModal(false)}
                  className="p-1 text-slate-400 hover:text-slate-600"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-2 text-xs">
                <div className="bg-amber-50 p-2.5 rounded-xl border border-amber-200 text-amber-900">
                  <span className="text-[10px] block">Reste à verser calculé :</span>
                  <span className="font-mono font-bold text-sm">
                    {formatMoney(remainingToRemit, settings.currency)}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-slate-500 block mb-1">Montant versé</label>
                    <input
                      type="number"
                      step="500"
                      value={remittanceAmount}
                      onChange={e => setRemittanceAmount(Math.max(0, parseInt(e.target.value, 10) || 0))}
                      required
                      className="w-full p-2 border border-slate-200 rounded-lg text-xs font-mono font-bold"
                    />
                  </div>
                  <div>
                    <label className="text-slate-500 block mb-1">Date</label>
                    <input
                      type="date"
                      value={remittanceDate}
                      onChange={e => setRemittanceDate(e.target.value)}
                      required
                      className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-slate-500 block mb-1">Destinataire (Caisse Principale)</label>
                  <input
                    type="text"
                    value={remittanceRecipient}
                    onChange={e => setRemittanceRecipient(e.target.value)}
                    placeholder="Nom du trésorier central ou entité..."
                    required
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                  />
                </div>

                <div>
                  <label className="text-slate-500 block mb-1">Période d'encaissement couverte</label>
                  <input
                    type="text"
                    value={remittancePeriod}
                    onChange={e => setRemittancePeriod(e.target.value)}
                    placeholder="ex: Février - Mars 2026..."
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                  />
                </div>

                <div>
                  <label className="text-slate-500 block mb-1">Note optionnelle</label>
                  <input
                    type="text"
                    value={remittanceNote}
                    onChange={e => setRemittanceNote(e.target.value)}
                    placeholder="Reçu signé, etc."
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-semibold cursor-pointer shadow-xs"
                >
                  Confirmer le versement
                </button>
              </div>
            </form>
          </div>
        )}

        {/* ============================================================== */}
        {/* MODAL : FICHE MEMBRE COMPLÈTE & GRILLE 12 MOIS                 */}
        {/* ============================================================== */}
        {currentDetailMember && (
          <div className="absolute inset-0 bg-black/60 z-50 flex items-end">
            <div className="w-full bg-white rounded-t-3xl max-h-[92%] overflow-y-auto p-4 space-y-3.5">
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <div>
                  <h3 className="font-bold text-xs text-slate-900">
                    {currentDetailMember.member.lastName.toUpperCase()} {currentDetailMember.member.firstName}
                  </h3>
                  <span className="text-[11px] text-slate-500">
                    {currentDetailMember.member.phone || 'Pas de numéro'} · Adhésion : {formatMonthFr(currentDetailMember.member.joinMonth)}
                  </span>
                </div>
                <button
                  onClick={() => setMemberDetailId(null)}
                  className="p-1 text-slate-400 hover:text-slate-600"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {/* Status Banner */}
              <div className={`p-3 rounded-xl border text-xs flex items-center justify-between ${
                currentDetailMember.balanceDue > 0
                  ? 'bg-rose-50 border-rose-200 text-rose-950'
                  : 'bg-emerald-50 border-emerald-200 text-emerald-950'
              }`}>
                <div>
                  <span className="text-[10px] block opacity-80">Situation des cotisations :</span>
                  <span className="font-bold text-sm font-mono">
                    {currentDetailMember.balanceDue > 0
                      ? `Reste dû : ${formatMoney(currentDetailMember.balanceDue, settings.currency)}`
                      : 'Cotisations à jour'}
                  </span>
                </div>
                <button
                  onClick={() => {
                    setSelectedMemberForPayment(currentDetailMember.member.id);
                    setShowFeePaymentModal(true);
                  }}
                  className="px-3 py-1.5 bg-slate-900 text-white rounded-lg text-xs font-semibold cursor-pointer"
                >
                  Payer
                </button>
              </div>

              {/* 12-Month Grid for Current Year */}
              <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-800">
                    Grille des mois ({currentYear})
                  </span>
                  <div className="flex items-center gap-2 text-[10px]">
                    <span className="flex items-center gap-1"><span className="w-2 h-2 rounded bg-emerald-500" /> Payé</span>
                    <span className="flex items-center gap-1"><span className="w-2 h-2 rounded bg-amber-500" /> Partiel</span>
                    <span className="flex items-center gap-1"><span className="w-2 h-2 rounded bg-rose-500" /> Impayé</span>
                  </div>
                </div>

                <div className="grid grid-cols-4 gap-1.5 text-center text-xs">
                  {['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'].map(mNum => {
                    const ym = `${currentYear}-${mNum}`;
                    const item = currentDetailMember.monthsDue.find(d => d.month === ym);
                    const isAdvance = currentDetailMember.advanceMonths.find(d => d.month === ym);

                    let bg = 'bg-slate-100 text-slate-400';
                    let label = 'Non échu';

                    if (item) {
                      if (item.status === 'PAID') {
                        bg = 'bg-emerald-600 text-white font-semibold';
                        label = 'Payé';
                      } else if (item.status === 'PARTIAL') {
                        bg = 'bg-amber-500 text-white font-semibold';
                        label = 'Partiel';
                      } else {
                        bg = 'bg-rose-600 text-white font-semibold';
                        label = 'Impayé';
                      }
                    } else if (isAdvance) {
                      bg = 'bg-blue-600 text-white font-semibold';
                      label = 'Avance';
                    }

                    return (
                      <div key={mNum} className={`p-1.5 rounded-lg border border-transparent ${bg}`}>
                        <div className="font-bold text-[11px]">{formatMonthFr(ym, true).split(' ')[0]}</div>
                        <div className="text-[9px] truncate">{label}</div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Member Operations & Actions */}
              <div className="flex items-center gap-2 pt-2 border-t border-slate-100">
                <button
                  onClick={() => {
                    setEditingMember(currentDetailMember.member);
                    setShowMemberModal(true);
                  }}
                  className="flex-1 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium cursor-pointer"
                >
                  Modifier la fiche
                </button>
                <button
                  onClick={() => handleToggleArchiveMember(currentDetailMember.member)}
                  className="py-1.5 px-3 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium cursor-pointer"
                >
                  {currentDetailMember.member.isActive ? 'Archiver' : 'Activer'}
                </button>
                <button
                  onClick={() => handleDeleteMember(currentDetailMember.member)}
                  className="py-1.5 px-3 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-lg text-xs font-medium cursor-pointer"
                >
                  Supprimer
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* MODAL : AJOUT / MODIFICATION MEMBRE                            */}
        {/* ============================================================== */}
        {showMemberModal && (
          <div className="absolute inset-0 bg-black/60 z-50 flex items-end">
            <form
              onSubmit={handleSaveMember}
              className="w-full bg-white rounded-t-3xl max-h-[90%] overflow-y-auto p-4 space-y-3"
            >
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <h3 className="font-semibold text-xs text-slate-900">
                  {editingMember ? 'Modifier le membre' : 'Ajouter un membre'}
                </h3>
                <button
                  type="button"
                  onClick={() => setShowMemberModal(false)}
                  className="p-1 text-slate-400 hover:text-slate-600"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-2 text-xs">
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-slate-500 block mb-1">Nom *</label>
                    <input
                      type="text"
                      name="lastName"
                      defaultValue={editingMember?.lastName || ''}
                      required
                      placeholder="ex: Ranaivo"
                      className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                    />
                  </div>
                  <div>
                    <label className="text-slate-500 block mb-1">Prénom *</label>
                    <input
                      type="text"
                      name="firstName"
                      defaultValue={editingMember?.firstName || ''}
                      required
                      placeholder="ex: Jean"
                      className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-slate-500 block mb-1">Téléphone</label>
                    <input
                      type="tel"
                      name="phone"
                      defaultValue={editingMember?.phone || ''}
                      placeholder="034 11 222 33"
                      className="w-full p-2 border border-slate-200 rounded-lg text-xs font-mono"
                    />
                  </div>
                  <div>
                    <label className="text-slate-500 block mb-1">Mois d'adhésion *</label>
                    <input
                      type="month"
                      name="joinMonth"
                      defaultValue={editingMember?.joinMonth || currentMonth}
                      required
                      className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-slate-500 block mb-1">
                    Montant mensuel spécifique (optionnel)
                  </label>
                  <input
                    type="number"
                    step="500"
                    name="customMonthlyAmount"
                    defaultValue={editingMember?.customMonthlyAmount || ''}
                    placeholder="Laisser vide pour utiliser le montant par défaut"
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs font-mono"
                  />
                </div>

                <div>
                  <label className="text-slate-500 block mb-1">Note libre</label>
                  <input
                    type="text"
                    name="note"
                    defaultValue={editingMember?.note || ''}
                    placeholder="Fonction, informations..."
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-semibold cursor-pointer shadow-xs"
                >
                  {editingMember ? 'Enregistrer les modifications' : 'Créer le membre'}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* ============================================================== */}
        {/* MODAL : APERÇU ET ENVOI DE MESSAGE DE RELANCE                  */}
        {/* ============================================================== */}
        {activeReminderMember && (
          <div className="absolute inset-0 bg-black/60 z-50 flex items-end">
            <div className="w-full bg-white rounded-t-3xl max-h-[90%] overflow-y-auto p-4 space-y-3">
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <div>
                  <h3 className="font-semibold text-xs text-slate-900">Relance de Cotisation</h3>
                  <span className="text-[11px] text-slate-500">
                    Membre : {activeReminderMember.firstName} {activeReminderMember.lastName}
                  </span>
                </div>
                <button
                  onClick={() => setActiveReminderMember(null)}
                  className="p-1 text-slate-400 hover:text-slate-600"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-2 text-xs">
                <div>
                  <label className="text-slate-500 block mb-1">Message à envoyer (modifiable pour ce membre) :</label>
                  <textarea
                    rows={4}
                    value={customReminderMessage}
                    onChange={e => setCustomReminderMessage(e.target.value)}
                    className="w-full p-2 border border-slate-200 rounded-lg text-xs leading-relaxed"
                  />
                </div>

                <div className="grid grid-cols-3 gap-2 pt-2">
                  <button
                    onClick={() => handleTriggerReminderAction('WHATSAPP')}
                    className="py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-semibold flex items-center justify-center gap-1 cursor-pointer shadow-xs"
                  >
                    <MessageSquare className="w-4 h-4" />
                    WhatsApp
                  </button>
                  <button
                    onClick={() => handleTriggerReminderAction('SMS')}
                    className="py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold flex items-center justify-center gap-1 cursor-pointer shadow-xs"
                  >
                    <Send className="w-4 h-4" />
                    SMS
                  </button>
                  <button
                    onClick={() => handleTriggerReminderAction('CALL')}
                    className="py-2.5 bg-slate-800 hover:bg-slate-900 text-white rounded-xl text-xs font-semibold flex items-center justify-center gap-1 cursor-pointer shadow-xs"
                  >
                    <Phone className="w-4 h-4" />
                    Appel
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* MODAL : ÉTAT DE VERSEMENT PDF IMPRIMABLE                       */}
        {/* ============================================================== */}
        {showPdfPreview && (
          <div className="absolute inset-0 bg-black/70 z-50 p-2 flex flex-col justify-center">
            <div className="bg-white rounded-2xl p-4 max-h-[95%] overflow-y-auto space-y-3 text-slate-900">
              <div className="flex items-center justify-between border-b pb-2">
                <div>
                  <h3 className="font-bold text-xs uppercase tracking-wide">État de Versement de Caisse</h3>
                  <span className="text-[11px] text-slate-500">{settings.associationName}</span>
                </div>
                <button
                  onClick={() => setShowPdfPreview(false)}
                  className="p-1 text-slate-400 hover:text-slate-600"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {/* Printable Document Sheet Simulation */}
              <div className="border border-slate-300 rounded-lg p-3 text-[11px] space-y-3 bg-white">
                <div className="text-center pb-2 border-b border-slate-200">
                  <h4 className="font-bold text-sm">{settings.associationName}</h4>
                  <div className="text-[10px] text-slate-500">Document comptable de trésorerie - Espèces</div>
                  <div className="text-[10px] text-slate-500">Édité le {formatDateFr(getCurrentDateStr())}</div>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <span className="text-slate-500 block text-[10px]">Total Encaissé :</span>
                    <span className="font-mono font-bold">{formatMoney(totalCollectedAllTime, settings.currency)}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Total Déjà Versé :</span>
                    <span className="font-mono font-bold text-emerald-700">{formatMoney(totalRemittedAllTime, settings.currency)}</span>
                  </div>
                </div>

                <div className="bg-slate-50 p-2 rounded border border-slate-200">
                  <span className="text-[10px] text-slate-500 block font-semibold">SOLDE RESTANT EN CAISSE :</span>
                  <span className="font-mono font-bold text-base text-slate-900">
                    {formatMoney(remainingToRemit, settings.currency)}
                  </span>
                </div>

                {/* Signatures Boxes */}
                <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-200">
                  <div className="border border-dashed border-slate-300 rounded p-2 h-16 flex flex-col justify-between">
                    <span className="text-[9px] text-slate-500">Visa / Signature du Trésorier :</span>
                    <span className="text-[9px] text-slate-400 text-center">[Espèces remises]</span>
                  </div>
                  <div className="border border-dashed border-slate-300 rounded p-2 h-16 flex flex-col justify-between">
                    <span className="text-[9px] text-slate-500">Visa / Signature du Destinataire :</span>
                    <span className="text-[9px] text-slate-400 text-center">[Espèces reçues]</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => {
                    window.print();
                  }}
                  className="flex-1 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <Printer className="w-3.5 h-3.5" />
                  Imprimer / Enregistrer PDF
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
