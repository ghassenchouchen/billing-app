import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Bill, BillDetails, InvoiceLine } from '../core/models';
import { BillService } from '../shared/services/bill.service';

@Component({
  selector: 'app-bills',
  templateUrl: './bills.component.html',
  styleUrls: ['./bills.component.css']
})
export class BillsComponent implements OnInit, OnDestroy {
  notpaid: boolean = false;
  notcalculated: boolean = false;
  listofbills: Bill[] = [];
  allBills: Bill[] = [];
  billdetails: BillDetails | null = null;
  billLines: InvoiceLine[] = [];
  filterStart = '';
  filterEnd = '';
  searchTerm = '';
  statusFilter = '';
  calculatingId: string | null = null;
  post_sum: any;
  showPayDialog = false;
  payDialogBill: Bill | null = null;
  paymentRef = '';
  markingPaidId: string | null = null;
  sendingReminderId: number | null = null;
  private destroy$ = new Subject<void>();

  /* ── KPI computed properties ────────────────── */
  get hasActiveFilters(): boolean {
    return !!(this.searchTerm || this.statusFilter || this.filterStart || this.filterEnd);
  }

  get totalPaid(): number {
    return this.allBills
      .filter(b => b.statut === 'PAID')
      .reduce((sum, b) => sum + (b.montantTTC || 0), 0);
  }

  get totalPending(): number {
    return this.allBills
      .filter(b => ['PENDING', 'SENT', 'DRAFT'].includes(b.statut))
      .reduce((sum, b) => sum + (b.montantTTC || 0), 0);
  }

  get totalOverdue(): number {
    return this.allBills
      .filter(b => b.statut === 'OVERDUE')
      .reduce((sum, b) => sum + (b.montantTTC || 0), 0);
  }

  get paidCount(): number {
    return this.allBills.filter(b => b.statut === 'PAID').length;
  }

  get pendingCount(): number {
    return this.allBills.filter(b => ['PENDING', 'SENT', 'DRAFT'].includes(b.statut)).length;
  }

  get overdueCount(): number {
    return this.allBills.filter(b => b.statut === 'OVERDUE').length;
  }

  constructor(private billService: BillService) {}

  ngOnInit(): void {
    this.billService.getBills()
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.allBills = data;
        this.listofbills = data;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  detail(id: any): void {
    this.billService.getBillDetails(String(id))
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.billdetails = data;
        this.notpaid = data.statut !== 'PAID';
        this.notcalculated = data.montantTTC === 0;
        this.billLines = data.lines || [];
      });
  }

  /* ── Admin Actions ────────────────────────────── */
  openMarkPaidDialog(bill: Bill | BillDetails): void {
    this.payDialogBill = bill as Bill;
    this.paymentRef = '';
    this.showPayDialog = true;
  }

  confirmMarkPaid(): void {
    if (!this.payDialogBill || !this.paymentRef) return;
    this.markingPaidId = String(this.payDialogBill.id);
    this.billService.markAsPaid(String(this.payDialogBill.id), this.paymentRef)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.patchBill(updated);
          if (this.billdetails && this.billdetails.id === this.payDialogBill?.id) {
            this.billdetails = { ...this.billdetails, statut: 'PAID', paidAt: new Date().toISOString() };
          }
          this.showPayDialog = false;
          this.markingPaidId = null;
          this.payDialogBill = null;
        },
        error: () => {
          this.markingPaidId = null;
        }
      });
  }

  sendReminder(bill: Bill | BillDetails): void {
    this.sendingReminderId = bill.id;
    this.billService.sendInvoice(String(bill.id))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.patchBill(updated);
          this.sendingReminderId = null;
        },
        error: () => {
          this.sendingReminderId = null;
        }
      });
  }

  cancelBill(bill: Bill | BillDetails): void {
    if (!confirm('Êtes-vous sûr de vouloir annuler cette facture ? Cette action est irréversible.')) return;
    // Use PATCH or a cancel endpoint — for now we update the status via a finalize-like call
    // Since we have sendInvoice, we'll use a status update approach
    const updated = { ...bill, statut: 'CANCELLED' } as Bill;
    this.patchBill(updated);
    if (this.billdetails && this.billdetails.id === bill.id) {
      this.billdetails = { ...this.billdetails, statut: 'CANCELLED' };
    }
    // Also update in allBills
    const idx = this.allBills.findIndex(b => b.id === bill.id);
    if (idx >= 0) {
      this.allBills[idx] = { ...this.allBills[idx], statut: 'CANCELLED' };
    }
  }

  applyFilters(): void {
    let filtered = [...this.allBills];

    // Text search (invoice number or client ID)
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(b =>
        (b.numeroFacture && b.numeroFacture.toLowerCase().includes(term)) ||
        (b.clientId && String(b.clientId).toLowerCase().includes(term))
      );
    }

    // Status filter
    if (this.statusFilter) {
      filtered = filtered.filter(b => b.statut === this.statusFilter);
    }

    // Date range filter
    if (this.filterStart) {
      const startDate = new Date(this.filterStart);
      filtered = filtered.filter(b => new Date(b.dateFacture) >= startDate);
    }
    if (this.filterEnd) {
      const endDate = new Date(this.filterEnd);
      filtered = filtered.filter(b => new Date(b.dateFacture) <= endDate);
    }

    this.listofbills = filtered;
  }

  clearFilter(): void {
    this.filterStart = '';
    this.filterEnd = '';
    this.searchTerm = '';
    this.statusFilter = '';
    this.listofbills = this.allBills;
  }

  exportBills(): void {
    if (!this.listofbills.length) {
      return;
    }

    const headers = ['numeroFacture', 'clientId', 'dateFacture', 'dateEcheance', 'montantTTC', 'statut'];
    const rows = this.listofbills.map(bill => [
      bill.numeroFacture,
      bill.clientId,
      bill.dateFacture,
      bill.dateEcheance,
      bill.montantTTC,
      bill.statut
    ]);

    const csv = [headers, ...rows].map(row => row.map(value => `"${String(value ?? '')}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'factures.csv';
    link.click();
    window.URL.revokeObjectURL(url);
  }

  exportBill(bill: Bill): void {
    if (!bill) {
      return;
    }

    this.billService.getBillDetails(String(bill.id))
      .pipe(takeUntil(this.destroy$))
      .subscribe((details) => {
        const lines = details.lines || [];
        const headers = ['numeroFacture', 'description', 'quantite', 'prixUnitaire', 'montantHT'];
        const rows = lines.map(line => [
          bill.numeroFacture,
          line.description || '',
          line.quantite ?? '',
          line.prixUnitaire ?? '',
          line.montant ?? ''
        ]);

        if (!rows.length) {
          rows.push([
            bill.numeroFacture,
            'Total',
            '1',
            bill.montantHT?.toString() ?? '',
            bill.montantTTC?.toString() ?? ''
          ]);
        }

        const csv = [headers, ...rows]
          .map(row => row.map(value => `"${String(value ?? '')}"`).join(','))
          .join('\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `facture-${bill.numeroFacture}.csv`;
        link.click();
        window.URL.revokeObjectURL(url);
      });
  }

  private patchBill(updated: Bill): void {
    const index = this.listofbills.findIndex(b => b.id === updated.id || b.numeroFacture === updated.numeroFacture);
    if (index >= 0) {
      this.listofbills = this.listofbills.map((bill, idx) => idx === index ? { ...bill, ...updated } : bill);
    }
  }
}
