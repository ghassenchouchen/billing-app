import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AbonnementService } from '../shared/services/abonnement.service';
import { Abonnement } from '../core/models';

@Component({
  selector: 'app-abonnements',
  templateUrl: './abonnements.component.html',
  styleUrls: ['./abonnements.component.css']
})
export class AbonnementsComponent implements OnInit, OnDestroy {
  listofAbonnements: Abonnement[] = [];
  filteredAbonnements: Abonnement[] = [];
  searchTerm = '';
  statusFilter = '';
  totalCount = 0;
  usingSampleData = false;

  deactivatingId: string | null = null;
  showConfirmModal = false;
  confirmModalConfig = {
    title: '',
    message: '',
    confirmText: 'Confirmer',
    confirmClass: 'danger',
    action: null as (() => void) | null
  };

  private destroy$ = new Subject<void>();

  private offerNames: { [key: number]: string } = {
    1: 'Fibre Essentiel 20M',
    2: 'Fibre Pro 100M',
    3: 'Mobile 5G 10Go',
    4: 'Forfait Mobile 4G 25 Go',
    5: 'Prépayé Mobile',
    6: 'Entreprise Convergent',
    7: 'Pack Roaming Maghreb',
    8: 'Forfait Jeune 4G'
  };

  private customerNames: { [key: number]: { nom: string; prenom: string; ref: string; cin: string; type: string } } = {
    1: { nom: 'Ben Ali', prenom: 'Mohamed', ref: 'CLT-2024-001', cin: '09876543', type: 'INDIVIDUAL' },
    2: { nom: 'Trabelsi', prenom: 'Amira', ref: 'CLT-2024-002', cin: '12345678', type: 'INDIVIDUAL' },
    3: { nom: 'Digital Solutions SARL', prenom: '', ref: 'CLT-2024-003', cin: 'RNE-B0147523', type: 'BUSINESS' },
    4: { nom: 'Hamdi', prenom: 'Yassine', ref: 'CLT-2024-004', cin: '07654321', type: 'INDIVIDUAL' },
    5: { nom: 'MédiaPlus SARL', prenom: '', ref: 'CLT-2024-005', cin: 'RNE-A0298341', type: 'BUSINESS' },
    6: { nom: 'Gharbi', prenom: 'Fatma', ref: 'CLT-2024-006', cin: '11223344', type: 'INDIVIDUAL' }
  };

  constructor(
    private abonnementService: AbonnementService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAbonnements();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAbonnements(): void {
    this.abonnementService.getAbonnements()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          if (data && data.length) {
            this.listofAbonnements = data;
            this.usingSampleData = false;
          } else {
            this.listofAbonnements = this.getSampleAbonnements();
            this.usingSampleData = true;
          }
          this.totalCount = this.listofAbonnements.length;
          this.applyFilters();
        },
        error: () => {
          this.listofAbonnements = this.getSampleAbonnements();
          this.usingSampleData = true;
          this.totalCount = this.listofAbonnements.length;
          this.applyFilters();
        }
      });
  }

  applyFilters(): void {
    let result = [...this.listofAbonnements];
    const term = this.searchTerm.toLowerCase().trim();

    if (term) {
      result = result.filter(a => {
        const offerName = this.getOfferName(a.offreId).toLowerCase();
        const customerName = this.getCustomerName(a.clientId).toLowerCase();
        const ref = ('ABN-' + a.id).toLowerCase();
        return offerName.includes(term) || customerName.includes(term) || ref.includes(term);
      });
    }

    if (this.statusFilter) {
      result = result.filter(a => a.status === this.statusFilter);
    }

    this.filteredAbonnements = result;
  }

  setFilter(status: string): void {
    this.statusFilter = this.statusFilter === status ? '' : status;
    this.applyFilters();
  }

  clearFilters(): void {
    this.statusFilter = '';
    this.searchTerm = '';
    this.applyFilters();
  }

  getActiveCount(): number {
    return this.listofAbonnements.filter(a => a.status === 'ACTIVE').length;
  }

  getSuspendedCount(): number {
    return this.listofAbonnements.filter(a => a.status === 'SUSPENDED').length;
  }

  getTerminatedCount(): number {
    return this.listofAbonnements.filter(a => a.status === 'TERMINATED').length;
  }

  detail(id: number): void {
    this.router.navigate(['/Abonnements', id]);
  }

  createNew(): void {
    this.router.navigate(['/Abonnements/new']);
  }

  getOfferName(offreId: number): string {
    return this.offerNames[offreId] || ('Offre #' + offreId);
  }

  getOfferIcon(offreId: number): string {
    const name = this.getOfferName(offreId).toLowerCase();
    if (name.match(/fibre|internet/)) return 'wifi';
    if (name.match(/mobile|4g|5g|forfait|prépayé|jeune/)) return 'smartphone';
    if (name.match(/entreprise|convergent/)) return 'business';
    if (name.match(/roaming/)) return 'public';
    return 'redeem';
  }

  getCustomerName(clientId: number): string {
    const c = this.customerNames[clientId];
    if (!c) return 'Client #' + clientId;
    if (c.type === 'BUSINESS') return c.nom;
    return (c.prenom + ' ' + c.nom).trim();
  }

  getCustomerRef(clientId: number): string {
    return this.customerNames[clientId]?.ref || ('CLT-' + clientId);
  }

  getCustomerCin(clientId: number): string {
    return this.customerNames[clientId]?.cin || '—';
  }

  getInitials(clientId: number): string {
    const c = this.customerNames[clientId];
    if (!c) return '?';
    const f = (c.prenom || '').charAt(0).toUpperCase();
    const l = (c.nom || '').charAt(0).toUpperCase();
    return f + l || l;
  }

  getAvatarColor(clientId: number): string {
    const colors = ['#5b4bff', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#10b981'];
    const c = this.customerNames[clientId];
    const name = c ? (c.nom + c.prenom) : String(clientId);
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  getFormattedDate(dateStr?: string): string {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleDateString('fr-TN', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch { return dateStr; }
  }

  getStatusLabel(status?: string): string {
    switch (status) {
      case 'ACTIVE': return 'Actif';
      case 'SUSPENDED': return 'Suspendu';
      case 'TERMINATED': return 'Résilié';
      case 'PENDING': return 'En attente';
      default: return status || 'Inconnu';
    }
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'SUSPENDED': return 'warning';
      case 'TERMINATED': return 'danger';
      case 'PENDING': return 'info';
      default: return 'neutral';
    }
  }

  suspend(a: Abonnement): void {
    this.confirmModalConfig = {
      title: "Suspendre l'abonnement",
      message: "Êtes-vous sûr de vouloir suspendre l'abonnement ABN-" + a.id + " (" + this.getOfferName(a.offreId) + ") ?",
      confirmText: 'Suspendre',
      confirmClass: 'warning',
      action: () => this.confirmSuspend(a)
    };
    this.showConfirmModal = true;
  }

  private confirmSuspend(a: Abonnement): void {
    this.showConfirmModal = false;
    this.deactivatingId = String(a.id);

    if (this.usingSampleData) {
      a.status = 'SUSPENDED';
      this.deactivatingId = null;
      this.applyFilters();
      return;
    }

    this.abonnementService.suspendAbonnement(String(a.id))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.patchAbonnement(data);
          this.deactivatingId = null;
          this.applyFilters();
        },
        error: () => { this.deactivatingId = null; }
      });
  }

  cancelConfirm(): void {
    this.showConfirmModal = false;
  }

  private patchAbonnement(updated: Abonnement): void {
    const idx = this.listofAbonnements.findIndex(a => a.id === updated.id);
    if (idx >= 0) {
      this.listofAbonnements[idx] = { ...this.listofAbonnements[idx], ...updated };
    }
  }

  private getSampleAbonnements(): Abonnement[] {
    return [
      { id: 1, clientId: 1, offreId: 4, dateDebut: '2024-10-24', status: 'ACTIVE', createdAt: '2024-10-24' },
      { id: 2, clientId: 1, offreId: 7, dateDebut: '2024-11-01', status: 'ACTIVE', createdAt: '2024-11-01' },
      { id: 3, clientId: 3, offreId: 6, dateDebut: '2024-10-23', status: 'ACTIVE', createdAt: '2024-10-23' },
      { id: 4, clientId: 2, offreId: 1, dateDebut: '2024-09-15', status: 'ACTIVE', createdAt: '2024-09-15' },
      { id: 5, clientId: 4, offreId: 3, dateDebut: '2024-08-01', status: 'SUSPENDED', createdAt: '2024-08-01' },
      { id: 6, clientId: 5, offreId: 2, dateDebut: '2024-07-10', status: 'ACTIVE', createdAt: '2024-07-10' },
      { id: 7, clientId: 6, offreId: 8, dateDebut: '2024-11-15', status: 'PENDING', createdAt: '2024-11-15' },
      { id: 8, clientId: 2, offreId: 5, dateDebut: '2024-06-01', dateFin: '2024-12-31', status: 'TERMINATED', createdAt: '2024-06-01' }
    ];
  }
}
