import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { BoutiqueApiService, TransactionBoutique } from '../../core/services/boutique-api.service';

@Component({
  selector: 'app-boutique-transactions',
  templateUrl: './boutique-transactions.component.html',
  styleUrls: ['./boutique-transactions.component.css']
})
export class BoutiqueTransactionsComponent implements OnInit, OnDestroy {
  boutiqueId = 1;
  allTransactions: TransactionBoutique[] = [];
  filteredTransactions: TransactionBoutique[] = [];
  loading = true;

  filterType = '';
  filterStatus = '';
  searchQuery = '';

  txnTypes = ['NEW_SUBSCRIPTION', 'RENEWAL', 'SIM_SWAP', 'ACCESSORY_SALE', 'TOP_UP', 'CANCELLATION'];
  txnStatuses = ['COMPLETED', 'PENDING', 'CANCELLED'];

  totalRevenue = 0;
  completedCount = 0;
  pendingCount = 0;

  private destroy$ = new Subject<void>();

  constructor(
    private boutiqueApi: BoutiqueApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const storedId = this.authService.getBoutiqueId();
    this.boutiqueId = storedId ? parseInt(storedId, 10) : 1;
    this.loadTransactions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadTransactions(): void {
    this.loading = true;
    this.boutiqueApi.getTransactions(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (txns) => {
          this.allTransactions = txns;
          this.computeStats();
          this.applyFilters();
          this.loading = false;
        },
        error: () => {
          this.allTransactions = [
            { id: 1, reference: 'TXN-BTQ-2025-001', boutiqueId: 1, agentId: 2, clientId: 1, clientNom: 'Mohamed Ben Ali', offreLibelle: 'Forfait Mobile 4G 25 Go', typeTransaction: 'NEW_SUBSCRIPTION', montant: 19.90, status: 'COMPLETED', createdAt: '2025-02-16T09:15:00' },
            { id: 2, reference: 'TXN-BTQ-2025-002', boutiqueId: 1, agentId: 2, clientId: 6, clientNom: 'Fatma Mansouri', offreLibelle: 'Forfait Mobile 4G 25 Go', typeTransaction: 'NEW_SUBSCRIPTION', montant: 19.90, status: 'COMPLETED', createdAt: '2025-02-16T10:30:00' },
            { id: 3, reference: 'TXN-BTQ-2025-003', boutiqueId: 1, agentId: 2, clientId: 2, clientNom: 'Amira Trabelsi', offreLibelle: 'Recharge Data 10 Go', typeTransaction: 'TOP_UP', montant: 8.00, status: 'COMPLETED', createdAt: '2025-02-16T11:00:00' },
            { id: 4, reference: 'TXN-BTQ-2025-004', boutiqueId: 1, agentId: 2, clientId: 5, clientNom: 'TechnoServ SA', offreLibelle: 'Entreprise Convergent', typeTransaction: 'RENEWAL', montant: 280.00, status: 'COMPLETED', createdAt: '2025-02-16T14:00:00' },
            { id: 5, reference: 'TXN-BTQ-2025-005', boutiqueId: 1, agentId: 2, clientId: 4, clientNom: 'Youssef Gharbi', offreLibelle: 'Mobile 5G Illimité', typeTransaction: 'SIM_SWAP', montant: 15.00, status: 'PENDING', createdAt: '2025-02-16T15:30:00' },
          ];
          this.computeStats();
          this.applyFilters();
          this.loading = false;
        }
      });
  }

  computeStats(): void {
    this.totalRevenue = this.allTransactions
      .filter(t => t.status === 'COMPLETED')
      .reduce((sum, t) => sum + t.montant, 0);
    this.completedCount = this.allTransactions.filter(t => t.status === 'COMPLETED').length;
    this.pendingCount = this.allTransactions.filter(t => t.status === 'PENDING').length;
  }

  applyFilters(): void {
    this.filteredTransactions = this.allTransactions.filter(t => {
      const matchType = !this.filterType || t.typeTransaction === this.filterType;
      const matchStatus = !this.filterStatus || t.status === this.filterStatus;
      const matchSearch = !this.searchQuery ||
        t.clientNom.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        t.reference.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        t.offreLibelle.toLowerCase().includes(this.searchQuery.toLowerCase());
      return matchType && matchStatus && matchSearch;
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'status-completed';
      case 'PENDING': return 'status-pending';
      case 'CANCELLED': return 'status-cancelled';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'Terminé';
      case 'PENDING': return 'En attente';
      case 'CANCELLED': return 'Annulé';
      default: return status;
    }
  }

  getTypeLabel(type: string): string {
    switch (type) {
      case 'NEW_SUBSCRIPTION': return 'Nouvel abonnement';
      case 'RENEWAL': return 'Renouvellement';
      case 'SIM_SWAP': return 'Échange SIM';
      case 'ACCESSORY_SALE': return 'Accessoire';
      case 'TOP_UP': return 'Recharge';
      case 'CANCELLATION': return 'Résiliation';
      default: return type;
    }
  }

  getTypeIcon(type: string): string {
    switch (type) {
      case 'NEW_SUBSCRIPTION': return 'add_circle';
      case 'RENEWAL': return 'autorenew';
      case 'SIM_SWAP': return 'sim_card';
      case 'ACCESSORY_SALE': return 'devices';
      case 'TOP_UP': return 'bolt';
      case 'CANCELLATION': return 'cancel';
      default: return 'receipt';
    }
  }

  formatCurrency(amount: number): string {
    return amount.toFixed(2).replace('.', ',') + ' DT';
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('fr-TN', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatTime(dateStr: string): string {
    try {
      return new Date(dateStr).toLocaleTimeString('fr-TN', { hour: '2-digit', minute: '2-digit' });
    } catch { return ''; }
  }
}
