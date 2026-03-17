import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import {
  BoutiqueApiService,
  DashboardData,
  TransactionBoutique,
  Boutique
} from '../../core/services/boutique-api.service';

@Component({
  selector: 'app-boutique-dashboard',
  templateUrl: './boutique-dashboard.component.html',
  styleUrls: ['./boutique-dashboard.component.css']
})
export class BoutiqueDashboardComponent implements OnInit, OnDestroy {
  boutiqueId: number = 1;
  boutique: Boutique | null = null;
  dashboard: DashboardData | null = null;
  recentTransactions: TransactionBoutique[] = [];
  loading = true;
  error = '';

  private destroy$ = new Subject<void>();

  constructor(
    private boutiqueApi: BoutiqueApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Get boutique ID from user's stored boutique (default to 1 for now)
    const storedId = this.authService.getBoutiqueId();
    this.boutiqueId = storedId ? parseInt(storedId, 10) : 1;
    this.loadDashboard();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadDashboard(): void {
    this.loading = true;

    this.boutiqueApi.getById(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (b) => this.boutique = b,
        error: () => {
          // Fallback to sample data if backend isn't running
          this.boutique = {
            id: 1, code: 'BTQ-TUNIS-01', nom: 'Boutique Tunis Centre',
            adresse: 'Av. Habib Bourguiba, 28', ville: 'Tunis', codePostal: '1000',
            telephone: '+216 71 300 100', email: 'tunis.centre@telecom.tn',
            responsableId: 3, status: 'ACTIVE', createdAt: '2024-06-01T09:00:00'
          };
        }
      });

    this.boutiqueApi.getDashboard(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (d) => {
          this.dashboard = d;
          // Calculate low stock flag: if available SIMs < 5, mark as low stock
          if (this.dashboard && this.dashboard.simAvailable < 5) {
            this.dashboard.simLowStock = 1;
          } else if (this.dashboard) {
            this.dashboard.simLowStock = 0;
          }
          this.loading = false;
        },
        error: () => {
          this.dashboard = {
            revenueToday: 342.80,
            contractsThisMonth: 47,
            contractTarget: 200,
            simAvailable: 7,
            simLowStock: 0,
            simByType: { STANDARD: 5, ESIM: 2 }
          };
          this.loading = false;
        }
      });

    this.boutiqueApi.getTodayTransactions(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (txns) => this.recentTransactions = txns,
        error: () => {
          // Fallback sample
          this.recentTransactions = [
            { id: 1, reference: 'TXN-BTQ-2025-001', boutiqueId: 1, agentId: 2, clientId: 1, clientNom: 'Mohamed Ben Ali', offreLibelle: 'Forfait Mobile 4G 25 Go', typeTransaction: 'NEW_SUBSCRIPTION', montant: 19.90, status: 'COMPLETED', createdAt: '2025-02-16T09:15:00' },
            { id: 2, reference: 'TXN-BTQ-2025-002', boutiqueId: 1, agentId: 2, clientId: 6, clientNom: 'Fatma Mansouri', offreLibelle: 'Forfait Mobile 4G 25 Go', typeTransaction: 'NEW_SUBSCRIPTION', montant: 19.90, status: 'COMPLETED', createdAt: '2025-02-16T10:30:00' },
            { id: 3, reference: 'TXN-BTQ-2025-003', boutiqueId: 1, agentId: 2, clientId: 2, clientNom: 'Amira Trabelsi', offreLibelle: 'Recharge Data 10 Go', typeTransaction: 'TOP_UP', montant: 8.00, status: 'COMPLETED', createdAt: '2025-02-16T11:00:00' },
            { id: 4, reference: 'TXN-BTQ-2025-004', boutiqueId: 1, agentId: 2, clientId: 5, clientNom: 'TechnoServ SA', offreLibelle: 'Entreprise Convergent', typeTransaction: 'RENEWAL', montant: 280.00, status: 'COMPLETED', createdAt: '2025-02-16T14:00:00' },
            { id: 5, reference: 'TXN-BTQ-2025-005', boutiqueId: 1, agentId: 2, clientId: 4, clientNom: 'Youssef Gharbi', offreLibelle: 'Mobile 5G Illimité', typeTransaction: 'SIM_SWAP', montant: 15.00, status: 'PENDING', createdAt: '2025-02-16T15:30:00' }
          ];
        }
      });
  }

  getContractPercentage(): number {
    if (!this.dashboard) return 0;
    return Math.round((this.dashboard.contractsThisMonth / this.dashboard.contractTarget) * 100);
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

  formatTime(dateStr: string): string {
    try {
      return new Date(dateStr).toLocaleTimeString('fr-TN', { hour: '2-digit', minute: '2-digit' });
    } catch { return ''; }
  }

  getSimTypeKeys(): string[] {
    if (!this.dashboard) return [];
    return Object.keys(this.dashboard.simByType);
  }
}
