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
          this.subscription = data || this.getSampleSubscription(id);
          this.loadRelatedData();
        },
        error: () => {
          this.subscription = this.getSampleSubscription(id);
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
        this.customer = customer || this.getSampleCustomer(customerId);
        this.offer = offer || this.getSampleOffer(offerId);
        this.loading = false;
      },
      error: () => {
        this.customer = this.getSampleCustomer(this.subscription!.clientId);
        this.offer = this.getSampleOffer(this.subscription!.offreId);
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

  // ─── Sample Data ───
  private getSampleSubscription(id: string): Abonnement {
    const samples: { [key: string]: Abonnement } = {
      '1': { id: 1, clientId: 1, offreId: 4, dateDebut: '2024-10-24', status: 'ACTIVE', createdAt: '2024-10-24' },
      '2': { id: 2, clientId: 1, offreId: 7, dateDebut: '2024-11-01', status: 'ACTIVE', createdAt: '2024-11-01' },
      '3': { id: 3, clientId: 3, offreId: 6, dateDebut: '2024-10-23', status: 'ACTIVE', createdAt: '2024-10-23' },
      '4': { id: 4, clientId: 2, offreId: 1, dateDebut: '2024-09-15', status: 'SUSPENDED', createdAt: '2024-09-15' }
    };
    return samples[id] || { id: parseInt(id, 10), clientId: 1, offreId: 1, dateDebut: '2024-01-01', status: 'ACTIVE', createdAt: '2024-01-01' };
  }

  private getSampleCustomer(clientId: number): Customer {
    const samples: { [key: number]: Customer } = {
      1: { customerRef: 'CLT-2024-001', nom: 'Ben Ali', prenom: 'Mohamed', email: 'mohamed.benali@email.tn', telephone: '+216 71 234 567', adresse: 'Av. Habib Bourguiba, 15', ville: 'Tunis', type: 'INDIVIDUAL', status: 'ACTIVE' },
      2: { customerRef: 'CLT-2024-002', nom: 'Trabelsi', prenom: 'Amira', email: 'amira.trabelsi@email.tn', telephone: '+216 22 345 678', adresse: 'Rue de Marseille, 42', ville: 'Tunis', type: 'INDIVIDUAL', status: 'ACTIVE' },
      3: { customerRef: 'CLT-2024-003', nom: 'Digital Solutions SARL', prenom: '', email: 'contact@digitalsol.tn', telephone: '+216 71 456 789', adresse: 'Zone Industrielle, Lot 8', ville: 'Sousse', type: 'BUSINESS', status: 'ACTIVE' }
    };
    return samples[clientId] || { customerRef: `CLT-${clientId}`, nom: 'Client', prenom: 'Inconnu', email: 'client@email.tn', adresse: '—', type: 'INDIVIDUAL', status: 'ACTIVE' };
  }

  private getSampleOffer(offreId: number): Offer {
    const samples: { [key: number]: Offer } = {
      1: { id: 1, code: 'FIBRE-20M', libelle: 'Fibre Essentiel 20M', description: 'Internet fibre optique 20 Mbps', prixMensuel: 35.00, status: 'ACTIVE', paymentType: 'POSTPAID' },
      2: { id: 2, code: 'FIBRE-100M', libelle: 'Fibre Pro 100M', description: 'Internet fibre 100 Mbps', prixMensuel: 89.00, status: 'ACTIVE', paymentType: 'POSTPAID' },
      3: { id: 3, code: 'MOB-5G-10G', libelle: 'Mobile 5G 10Go', description: 'Forfait mobile 5G avec 10 Go de data', prixMensuel: 25.00, status: 'ACTIVE', paymentType: 'POSTPAID' },
      4: { id: 4, code: 'MOB-4G-25G', libelle: 'Forfait Mobile 4G 25 Go', description: 'Forfait mobile 4G, 25 Go data, appels & SMS illimités', prixMensuel: 45.00, status: 'ACTIVE', paymentType: 'POSTPAID' },
      5: { id: 5, code: 'MOB-PREPAID', libelle: 'Prépayé Mobile', description: 'Forfait prépayé flexible', prixMensuel: 15.00, status: 'ACTIVE', paymentType: 'PREPAID' },
      6: { id: 6, code: 'ENT-CONV', libelle: 'Entreprise Convergent', description: 'Pack convergent entreprise', prixMensuel: 199.00, status: 'ACTIVE', paymentType: 'POSTPAID' },
      7: { id: 7, code: 'ROAMING-MAG', libelle: 'Pack Roaming Maghreb', description: 'Roaming voix & data Maghreb', prixMensuel: 30.00, status: 'ACTIVE', paymentType: 'POSTPAID' }
    };
    return samples[offreId] || { id: offreId, code: `OFF-${offreId}`, libelle: `Offre #${offreId}`, prixMensuel: 0, status: 'ACTIVE' };
  }
}
