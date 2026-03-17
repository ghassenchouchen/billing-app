import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, forkJoin, of } from 'rxjs';
import { takeUntil, map, catchError } from 'rxjs/operators';
import { CustomerDetails, Abonnement, Bill } from '../../core/models';
import { CustomerService } from '../../shared/services/customer.service';
import { AbonnementService } from '../../shared/services/abonnement.service';
import { AuthService } from '../../core/services/auth.service';
import { BoutiqueApiService, StockSim } from '../../core/services/boutique-api.service';

@Component({
  selector: 'app-customer-detail',
  templateUrl: './customer-detail.component.html',
  styleUrls: ['./customer-detail.component.css']
})
export class CustomerDetailComponent implements OnInit, OnDestroy {
  private static readonly ACTIVE_SIM_STATUSES = new Set(['ACTIVATED', 'ASSIGNED']);

  customer: CustomerDetails | null = null;
  loading = true;
  error = false;
  showEditMode = false;
  editData = { nom: '', prenom: '', email: '', telephone: '', adresse: '', ville: '', codePostal: '' };
  saving = false;
  showConfirmSuspend = false;
  customerSims: StockSim[] = [];
  simActionInProgress = false;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private customerService: CustomerService,
    private abonnementService: AbonnementService,
    private boutiqueApi: BoutiqueApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const ref = this.route.snapshot.paramMap.get('ref');
    if (ref) {
      this.loadCustomer(ref);
    } else {
      this.error = true;
      this.loading = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCustomer(ref: string): void {
    this.loading = true;
    const boutiqueId = this.getCurrentBoutiqueId();
    // Load customer and abonnements separately, then merge
    forkJoin({
      customer: this.customerService.getCustomerDetails(ref),
      abonnements: this.abonnementService.getAbonnementsByCustomerRef(ref).pipe(
        catchError(() => of([]))
      ),
      stock: this.boutiqueApi.getStock(boutiqueId).pipe(
        catchError(() => of([] as StockSim[]))
      )
    }).pipe(
      map(({ customer, abonnements, stock }) => {
        const mergedCustomer: CustomerDetails = {
          ...customer,
          abonnements: abonnements || []
        };

        const clientIds = this.getCustomerClientIds(mergedCustomer);
        const sims = (stock || [])
          .filter(sim => !!sim.assignedToClientId && clientIds.has(sim.assignedToClientId))
          .sort((a, b) => new Date(b.assignedAt || 0).getTime() - new Date(a.assignedAt || 0).getTime());

        return { mergedCustomer, sims };
      }),
      takeUntil(this.destroy$)
    )
      .subscribe({
        next: ({ mergedCustomer, sims }) => {
          this.customer = mergedCustomer;
          this.customerSims = sims;
          this.error = false;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading customer details:', err);
          this.customer = null;
          this.error = true;
          this.loading = false;
        }
      });
  }

  hasActiveSim(): boolean {
    return this.getTargetSim(CustomerDetailComponent.ACTIVE_SIM_STATUSES) !== null;
  }

  hasSuspendedSim(): boolean {
    return this.getTargetSim(new Set(['SUSPENDED'])) !== null;
  }

  getPrimarySim(): StockSim | null {
    return this.customerSims.length ? this.customerSims[0] : null;
  }

  goBack(): void {
    this.router.navigate(['/Customers']);
  }

  getInitials(): string {
    if (!this.customer) return '';
    const f = (this.customer.prenom || '').charAt(0).toUpperCase();
    const l = (this.customer.nom || '').charAt(0).toUpperCase();
    return f + l;
  }

  getAvatarColor(): string {
    if (!this.customer) return '#5b4bff';
    const colors = ['#5b4bff', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#10b981'];
    const name = (this.customer.nom || '') + (this.customer.prenom || '');
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  getFullName(): string {
    if (!this.customer) return '';
    if (this.customer.type === 'BUSINESS') return this.customer.nom;
    return `${this.customer.prenom} ${this.customer.nom}`.trim();
  }

  getTypeLabel(): string {
    if (!this.customer) return '';
    return this.customer.type === 'BUSINESS' ? 'Business / B2B' : 'Individu';
  }

  getStatusLabel(): string {
    if (!this.customer?.status) return 'Inconnu';
    switch (this.customer.status) {
      case 'ACTIVE': return 'Actif';
      case 'SUSPENDED': return 'Suspendu';
      default: return this.customer.status;
    }
  }

  getStatusClass(): string {
    if (!this.customer?.status) return 'neutral';
    switch (this.customer.status) {
      case 'ACTIVE': return 'success';
      case 'SUSPENDED': return 'warning';
      default: return 'neutral';
    }
  }

  getFormattedDate(dateStr?: string): string {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('fr-TN', { day: '2-digit', month: 'long', year: 'numeric' });
    } catch {
      return dateStr;
    }
  }

  formatTelephone(telephone?: string): string {
    const raw = (telephone || '').trim();
    if (!raw) {
      return '—';
    }

    const digits = raw.replace(/\D/g, '');
    if (digits.length === 8) {
      return `+216 ${digits.slice(0, 2)} ${digits.slice(2, 5)} ${digits.slice(5, 8)}`;
    }
    if (digits.length === 11 && digits.startsWith('216')) {
      return `+216 ${digits.slice(3, 5)} ${digits.slice(5, 8)} ${digits.slice(8, 11)}`;
    }
    return raw;
  }

  getActiveAbonnements(): Abonnement[] {
    if (!this.customer?.abonnements) return [];
    return this.customer.abonnements.filter(a => a.status === 'ACTIVE' || !a.status);
  }

  getOfferIcon(offreId: number): string {
    // Map by offer id pattern
    if (offreId <= 2) return 'wifi';
    if (offreId <= 5) return 'smartphone';
    if (offreId === 6) return 'business';
    return 'redeem';
  }

  getOfferName(offreId: number): string {
    const names: { [key: number]: string } = {
      1: 'Fibre Essentiel 20M',
      2: 'Fibre Pro 100M',
      3: 'Mobile 5G 10Go',
      4: 'Forfait Mobile 4G 25 Go',
      5: 'Entreprise Convergent',
      6: 'Pack Roaming Maghreb'
    };
    return names[offreId] || `Offre #${offreId}`;
  }

  getUnpaidBills(): Bill[] {
    if (!this.customer?.bills) return [];
    return this.customer.bills.filter(b => b.statut === 'IMPAYEE' || b.statut === 'EN_RETARD');
  }

  getLatestBill(): Bill | null {
    if (!this.customer?.bills || !this.customer.bills.length) return null;
    return this.customer.bills.sort((a, b) =>
      new Date(b.dateFacture).getTime() - new Date(a.dateFacture).getTime()
    )[0];
  }

  getBalanceStatus(): string {
    const unpaid = this.getUnpaidBills().length;
    if (unpaid === 0) return 'Pas d\'impayés';
    return `${unpaid} facture${unpaid > 1 ? 's' : ''} impayée${unpaid > 1 ? 's' : ''}`;
  }

  openEditMode(): void {
    if (!this.customer) return;
    this.editData = {
      nom: this.customer.nom,
      prenom: this.customer.prenom,
      email: this.customer.email,
      telephone: this.customer.telephone || '',
      adresse: this.customer.adresse,
      ville: this.customer.ville || '',
      codePostal: this.customer.codePostal || ''
    };
    this.showEditMode = true;
  }

  cancelEdit(): void {
    this.showEditMode = false;
  }

  saveEdit(): void {
    if (!this.customer) return;
    this.saving = true;
    this.customerService.updateCustomer(this.customer.customerRef, this.editData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          Object.assign(this.customer!, this.editData);
          this.showEditMode = false;
          this.saving = false;
        },
        error: () => { this.saving = false; }
      });
  }

  suspendSim(): void {
    const sim = this.getTargetSim(CustomerDetailComponent.ACTIVE_SIM_STATUSES);
    if (!sim || this.simActionInProgress) {
      this.showConfirmSuspend = false;
      return;
    }

    this.simActionInProgress = true;
    this.boutiqueApi.suspendSim(sim.iccid)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.updateSim(updated);
          this.showConfirmSuspend = false;
          this.simActionInProgress = false;
        },
        error: () => {
          this.showConfirmSuspend = false;
          this.simActionInProgress = false;
        }
      });
  }

  reactivateSim(): void {
    const sim = this.getTargetSim(new Set(['SUSPENDED']));
    if (!sim || this.simActionInProgress) {
      return;
    }

    this.simActionInProgress = true;
    this.boutiqueApi.reactivateSim(sim.iccid)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.updateSim(updated);
          this.simActionInProgress = false;
        },
        error: () => {
          this.simActionInProgress = false;
        }
      });
  }

  private getCurrentBoutiqueId(): number {
    const stored = this.authService.getBoutiqueId();
    const parsed = stored ? parseInt(stored, 10) : NaN;
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
  }

  private getCustomerClientIds(customer: CustomerDetails): Set<number> {
    const ids = new Set<number>();

    (customer.abonnements || []).forEach(abonnement => {
      if (typeof abonnement.clientId === 'number' && abonnement.clientId > 0) {
        ids.add(abonnement.clientId);
      }
    });

    if (ids.size === 0) {
      const fallback = this.extractClientId(customer.customerRef || '');
      if (fallback > 0) {
        ids.add(fallback);
      }
    }

    return ids;
  }

  private extractClientId(customerRef: string): number {
    const match = customerRef?.match(/(\d+)$/);
    return match ? parseInt(match[1], 10) : 0;
  }

  private getTargetSim(allowedStatuses: Set<string>): StockSim | null {
    return this.customerSims.find(sim => allowedStatuses.has(sim.status)) || null;
  }

  private updateSim(updated: StockSim): void {
    const sims = [...this.customerSims];
    const index = sims.findIndex(sim => sim.iccid === updated.iccid);

    if (index >= 0) {
      sims[index] = updated;
    } else {
      sims.push(updated);
    }

    sims.sort((a, b) => new Date(b.assignedAt || 0).getTime() - new Date(a.assignedAt || 0).getTime());
    this.customerSims = sims;
  }

  navigateToBills(): void {
    this.router.navigate(['/Bills']);
  }

  createSubscription(): void {
    if (this.customer) {
      this.router.navigate(['/Abonnements/new', this.customer.customerRef]);
    }
  }

}
