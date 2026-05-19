import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, forkJoin, of } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { Abonnement, Customer, Offer, Bill } from '../../core/models';
import { AbonnementService } from '../../shared/services/abonnement.service';
import { CustomerService } from '../../shared/services/customer.service';
import { OffersService } from '../../shared/services/offers.service';

@Component({
  selector: 'app-subscription-detail',
  templateUrl: './subscription-detail.component.html',
  styleUrls: ['./subscription-detail.component.css']
})
export class SubscriptionDetailComponent implements OnInit, OnDestroy {
  subscription: Abonnement | null = null;
  customer: Customer | null = null;
  offer: Offer | null = null;
  bills: Bill[] = [];
  loading = true;
  error = false;
  actionLoading = '';

  showConfirmModal = false;
  confirmModalConfig = { title: '', message: '', action: null as (() => void) | null, confirmText: '', confirmClass: '' };

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private abonnementService: AbonnementService,
    private customerService: CustomerService,
    private offersService: OffersService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadSubscription(id);
    } else {
      this.error = true;
      this.loading = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadSubscription(id: string): void {
    this.loading = true;
    this.abonnementService.getAbonnementDetails(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.subscription = data || null;
          this.loadRelatedData();
        },
        error: () => {
          this.subscription = null;
          this.loadRelatedData();
        }
      });
  }

  loadRelatedData(): void {
    if (!this.subscription) { this.loading = false; return; }

    const customerId = this.subscription.clientId;
    const offerId = this.subscription.offreId;

    forkJoin({
      customer: this.customerService.getCustomerById(customerId).pipe(catchError(() => of(null))),
      offer: this.offersService.getOfferDetails(String(offerId)).pipe(catchError(() => of(null)))
    })
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: ({ customer, offer }) => {
        this.customer = customer || null;
        this.offer = offer || null;
        this.loading = false;
      },
      error: () => {
        this.customer = null;
        this.offer = null;
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/Abonnements']);
  }

  goToCustomer(): void {
    if (this.customer) {
      this.router.navigate(['/Customers', this.customer.customerRef]);
    }
  }

  // ─── Actions ───
  suspendSubscription(): void {
    this.confirmModalConfig = {
      title: 'Suspendre l\'abonnement',
      message: 'Êtes-vous sûr de vouloir suspendre cet abonnement ? Le client ne pourra plus utiliser les services associés.',
      action: () => this.confirmSuspend(),
      confirmText: 'Suspendre',
      confirmClass: 'warning'
    };
    this.showConfirmModal = true;
  }

  terminateSubscription(): void {
    this.confirmModalConfig = {
      title: 'Résilier l\'abonnement',
      message: 'Attention : cette action est irréversible. L\'abonnement sera définitivement résilié.',
      action: () => this.confirmTerminate(),
      confirmText: 'Résilier',
      confirmClass: 'danger'
    };
    this.showConfirmModal = true;
  }

  reactivateSubscription(): void {
    if (!this.subscription) return;
    this.actionLoading = 'activate';
    this.abonnementService.activateAbonnement(String(this.subscription.id))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.subscription = { ...this.subscription!, ...data, status: data.status || 'ACTIVE' };
          this.actionLoading = '';
        },
        error: () => { this.actionLoading = ''; }
      });
  }

  private confirmSuspend(): void {
    if (!this.subscription) return;
    this.showConfirmModal = false;
    this.actionLoading = 'suspend';
    this.abonnementService.suspendAbonnement(String(this.subscription.id))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.subscription = { ...this.subscription!, ...data, status: data.status || 'SUSPENDED' };
          this.actionLoading = '';
        },
        error: () => { this.actionLoading = ''; }
      });
  }

  private confirmTerminate(): void {
    if (!this.subscription) return;
    this.showConfirmModal = false;
    this.actionLoading = 'terminate';
    this.abonnementService.terminateAbonnement(String(this.subscription.id))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.subscription = { ...this.subscription!, ...data, status: data.status || 'TERMINATED' };
          this.actionLoading = '';
        },
        error: () => { this.actionLoading = ''; }
      });
  }

  cancelConfirm(): void {
    this.showConfirmModal = false;
  }

  // ─── Helpers ───
  getStatusLabel(): string {
    switch (this.subscription?.status) {
      case 'ACTIVE': return 'Actif';
      case 'SUSPENDED': return 'Suspendu';
      case 'TERMINATED': return 'Résilié';
      case 'PENDING': return 'En attente';
      default: return this.subscription?.status || 'Inconnu';
    }
  }

  getStatusClass(): string {
    switch (this.subscription?.status) {
      case 'ACTIVE': return 'success';
      case 'SUSPENDED': return 'warning';
      case 'TERMINATED': return 'danger';
      case 'PENDING': return 'info';
      default: return 'neutral';
    }
  }

  getFormattedDate(dateStr?: string): string {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleDateString('fr-TN', { day: '2-digit', month: 'long', year: 'numeric' });
    } catch { return dateStr; }
  }

  getOfferIcon(): string {
    const name = (this.offer?.libelle || this.offer?.nom || '').toLowerCase();
    if (name.match(/fibre|internet/)) return 'wifi';
    if (name.match(/mobile|4g|5g|forfait/)) return 'smartphone';
    if (name.match(/entreprise|pro|convergent|business/)) return 'business';
    if (name.match(/roaming/)) return 'public';
    return 'redeem';
  }

  getOfferPrice(): number {
    return this.offer?.prixMensuel || this.offer?.prixBase || 0;
  }

  getCustomerFullName(): string {
    if (!this.customer) return 'Client #' + (this.subscription?.clientId || '');
    if (this.customer.type === 'BUSINESS') return this.customer.nom;
    return `${this.customer.prenom} ${this.customer.nom}`.trim();
  }

  getInitials(): string {
    if (!this.customer) return '?';
    const f = (this.customer.prenom || '').charAt(0).toUpperCase();
    const l = (this.customer.nom || '').charAt(0).toUpperCase();
    return f + l || l;
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

}
