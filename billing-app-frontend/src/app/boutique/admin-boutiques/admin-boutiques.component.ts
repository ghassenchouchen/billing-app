import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BoutiqueApiService, Boutique, StockSim, TransactionBoutique, DashboardData } from '../../core/services/boutique-api.service';

interface BoutiqueWithStats extends Boutique {
  stockCount: number;
  availableSim: number;
  transactionCount: number;
  revenue: number;
  dashboard?: DashboardData;
  stock?: StockSim[];
  recentTransactions?: TransactionBoutique[];
}

@Component({
  selector: 'app-admin-boutiques',
  templateUrl: './admin-boutiques.component.html',
  styleUrls: ['./admin-boutiques.component.css']
})
export class AdminBoutiquesComponent implements OnInit, OnDestroy {
  boutiques: BoutiqueWithStats[] = [];
  filteredBoutiques: BoutiqueWithStats[] = [];
  selectedBoutique: BoutiqueWithStats | null = null;
  loading = true;
  detailLoading = false;

  searchQuery = '';
  filterStatus = '';

  totalBoutiques = 0;
  activeBoutiques = 0;
  totalSim = 0;
  totalRevenue = 0;

  private destroy$ = new Subject<void>();

  constructor(private boutiqueApi: BoutiqueApiService, private router: Router) {}

  ngOnInit(): void {
    this.loadBoutiques();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadBoutiques(): void {
    this.loading = true;
    this.boutiqueApi.getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (boutiques) => {
          this.boutiques = boutiques.map(b => ({
            ...b,
            stockCount: 0,
            availableSim: 0,
            transactionCount: 0,
            revenue: 0
          }));
          this.computeGlobalKpis();
          this.applyFilters();
          this.loading = false;

          // Load stats for each boutique
          this.boutiques.forEach(b => this.loadBoutiqueStats(b));
        },
        error: () => {
          // Fallback data
          this.boutiques = [
            { id: 1, code: 'BTQ-TUNIS-01', nom: 'Boutique Tunis Centre', adresse: 'Av. Habib Bourguiba, 28', ville: 'Tunis', codePostal: '1000', telephone: '+216 71 300 100', email: 'tunis.centre@telecom.tn', responsableId: 3, status: 'ACTIVE', createdAt: '2024-06-01T09:00:00', stockCount: 10, availableSim: 5, transactionCount: 5, revenue: 342.80 },
            { id: 2, code: 'BTQ-SFAX-01', nom: 'Boutique Sfax Médina', adresse: 'Rue de la République, 5', ville: 'Sfax', codePostal: '3000', telephone: '+216 74 200 200', email: 'sfax.medina@telecom.tn', responsableId: null, status: 'ACTIVE', createdAt: '2024-07-15T09:00:00', stockCount: 4, availableSim: 4, transactionCount: 0, revenue: 0 },
            { id: 3, code: 'BTQ-SOUSSE-01', nom: 'Boutique Sousse Marina', adresse: 'Port El Kantaoui, Bloc 2', ville: 'Sousse', codePostal: '4000', telephone: '+216 73 100 300', email: 'sousse.marina@telecom.tn', responsableId: null, status: 'ACTIVE', createdAt: '2024-08-01T09:00:00', stockCount: 0, availableSim: 0, transactionCount: 0, revenue: 0 },
          ];
          this.computeGlobalKpis();
          this.applyFilters();
          this.loading = false;
        }
      });
  }

  loadBoutiqueStats(boutique: BoutiqueWithStats): void {
    this.boutiqueApi.getStock(boutique.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stock) => {
          boutique.stockCount = stock.length;
          boutique.availableSim = stock.filter(s => s.status === 'AVAILABLE').length;
          this.computeGlobalKpis();
        },
        error: () => {}
      });

    this.boutiqueApi.getTransactions(boutique.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (txns) => {
          boutique.transactionCount = txns.length;
          boutique.revenue = txns
            .filter(t => t.status === 'COMPLETED')
            .reduce((sum, t) => sum + t.montant, 0);
          this.computeGlobalKpis();
        },
        error: () => {}
      });
  }

  selectBoutique(boutique: BoutiqueWithStats): void {
    this.router.navigate(['/Boutiques', boutique.id]);
  }

  closeBoutiqueDetail(): void {
    this.selectedBoutique = null;
  }

  computeGlobalKpis(): void {
    this.totalBoutiques = this.boutiques.length;
    this.activeBoutiques = this.boutiques.filter(b => b.status === 'ACTIVE').length;
    this.totalSim = this.boutiques.reduce((sum, b) => sum + b.stockCount, 0);
    this.totalRevenue = this.boutiques.reduce((sum, b) => sum + b.revenue, 0);
  }

  applyFilters(): void {
    this.filteredBoutiques = this.boutiques.filter(b => {
      const matchSearch = !this.searchQuery ||
        b.nom.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        b.code.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        b.ville.toLowerCase().includes(this.searchQuery.toLowerCase());
      const matchStatus = !this.filterStatus || b.status === this.filterStatus;
      return matchSearch && matchStatus;
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'INACTIVE': return 'status-inactive';
      case 'CLOSED': return 'status-closed';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'Active';
      case 'INACTIVE': return 'Inactive';
      case 'CLOSED': return 'Fermée';
      default: return status;
    }
  }

  getSimStatusLabel(status: string): string {
    switch (status) {
      case 'AVAILABLE': return 'Disponible';
      case 'ASSIGNED': return 'Attribuée';
      case 'ACTIVATED': return 'Activée';
      case 'DAMAGED': return 'Endommagée';
      case 'LOST': return 'Perdue';
      default: return status;
    }
  }

  getSimStatusClass(status: string): string {
    switch (status) {
      case 'AVAILABLE': return 'sim-available';
      case 'ASSIGNED': return 'sim-assigned';
      case 'ACTIVATED': return 'sim-activated';
      case 'DAMAGED': return 'sim-damaged';
      case 'LOST': return 'sim-lost';
      default: return '';
    }
  }

  getTypeLabel(type: string): string {
    switch (type) {
      case 'STANDARD': return 'Standard';
      case 'ESIM': return 'eSIM';
      default: return type;
    }
  }

  getTxnTypeLabel(type: string): string {
    switch (type) {
      case 'NEW_SUBSCRIPTION': return 'Nouvel abonnement';
      case 'RENEWAL': return 'Renouvellement';
      case 'ACCESSORY_SALE': return 'Accessoire';
      case 'CANCELLATION': return 'Résiliation';
      case 'INVOICE_PAYMENT': return 'Paiement facture';
      case 'RECHARGE': return 'Recharge';
      default: return type;
    }
  }

  getTxnTypeIcon(type: string): string {
    switch (type) {
      case 'NEW_SUBSCRIPTION': return 'add_circle';
      case 'RENEWAL': return 'autorenew';
      case 'ACCESSORY_SALE': return 'devices';
      case 'CANCELLATION': return 'cancel';
      case 'INVOICE_PAYMENT': return 'payments';
      case 'RECHARGE': return 'recharge';
      default: return 'receipt';
    }
  }

  getTxnStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'txn-completed';
      case 'PENDING': return 'txn-pending';
      case 'CANCELLED': return 'txn-cancelled';
      default: return '';
    }
  }

  getTxnStatusLabel(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'Terminé';
      case 'PENDING': return 'En attente';
      case 'CANCELLED': return 'Annulé';
      default: return status;
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

  getStockByStatus(stock: StockSim[] | undefined): { status: string; count: number; label: string }[] {
    if (!stock) return [];
    const map: Record<string, number> = {};
    stock.forEach(s => map[s.status] = (map[s.status] || 0) + 1);
    return Object.entries(map).map(([status, count]) => ({
      status,
      count,
      label: this.getSimStatusLabel(status)
    }));
  }

  getStockByType(stock: StockSim[] | undefined): { type: string; count: number; label: string }[] {
    if (!stock) return [];
    const map: Record<string, number> = {};
    stock.forEach(s => map[s.simType] = (map[s.simType] || 0) + 1);
    return Object.entries(map).map(([type, count]) => ({
      type,
      count,
      label: this.getTypeLabel(type)
    }));
  }
}
