import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, forkJoin, of } from 'rxjs';
import { catchError, takeUntil } from 'rxjs/operators';
import { CustomerService } from '../shared/services/customer.service';
import { AbonnementService } from '../shared/services/abonnement.service';
import { AuthService } from '../core/services/auth.service';
import { Abonnement, Customer, CustomerDetails } from '../core/models';
import { BoutiqueApiService, StockSim } from '../core/services/boutique-api.service';

type CustomerWithAbonnements = Customer & { abonnements?: Abonnement[] };

@Component({
  selector: 'app-customers',
  templateUrl: './customers.component.html',
  styleUrls: ['./customers.component.css']
})
export class CustomersComponent implements OnInit, OnDestroy {
  private static readonly ACTIVE_SIM_STATUSES = new Set(['ACTIVATED', 'ASSIGNED']);

  listOfCustomer: CustomerWithAbonnements[] = [];
  filteredCustomers: CustomerWithAbonnements[] = [];
  customerdetails: CustomerDetails | null = null;
  customerSimsByRef: Record<string, StockSim[]> = {};
  actionInProgressRef = '';

  searchTerm = '';
  typeFilter = '';
  statusFilter = '';
  showCreateForm = false;
  saving = false;
  formData = {
    nom: '', prenom: '', email: '', telephone: '',
    adresse: '', ville: '', codePostal: '', type: 'INDIVIDUAL'
  };

  private destroy$ = new Subject<void>();

  get totalCount(): number { return this.listOfCustomer.length; }
  get activeCount(): number { return this.listOfCustomer.filter(c => c.status === 'ACTIVE').length; }

  get hasActiveFilters(): boolean {
    return !!(this.searchTerm || this.typeFilter || this.statusFilter);
  }

  constructor(
    private customerService: CustomerService,
    private abonnementService: AbonnementService,
    private boutiqueApi: BoutiqueApiService,
    private router: Router,
    private authService: AuthService
  ) {}

  get canCreateCustomer(): boolean {
    return !this.authService.isAdmin();
  }

  ngOnInit(): void {
    this.loadCustomers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCustomers(): void {
    const boutiqueId = this.getCurrentBoutiqueId();
    forkJoin({
      customers: this.customerService.getCustomers().pipe(
        catchError(() => of([] as Customer[]))
      ),
      abonnements: this.abonnementService.getAbonnements().pipe(
        catchError(() => of([] as Abonnement[]))
      ),
      stock: this.boutiqueApi.getStock(boutiqueId).pipe(
        catchError(() => of([] as StockSim[]))
      )
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe(({ customers, abonnements, stock }) => {
        this.listOfCustomer = this.attachAbonnements(customers || [], abonnements);
        this.customerSimsByRef = this.mapCustomerSims(this.listOfCustomer, stock);
        this.filteredCustomers = this.listOfCustomer;
      });
  }

  hasActiveSim(customer: CustomerWithAbonnements): boolean {
    return this.getTargetSim(customer, CustomersComponent.ACTIVE_SIM_STATUSES) !== null;
  }

  hasSuspendedSim(customer: CustomerWithAbonnements): boolean {
    return this.getTargetSim(customer, new Set(['SUSPENDED'])) !== null;
  }

  blockCustomerSim(customer: CustomerWithAbonnements): void {
    const sim = this.getTargetSim(customer, CustomersComponent.ACTIVE_SIM_STATUSES);
    if (!sim || !this.hasActiveSim(customer) || this.actionInProgressRef === customer.customerRef) {
      return;
    }

    this.actionInProgressRef = customer.customerRef;
    this.boutiqueApi.suspendSim(sim.iccid)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: updated => {
          this.updateCustomerSim(customer.customerRef, updated);
          this.actionInProgressRef = '';
        },
        error: () => {
          this.actionInProgressRef = '';
        }
      });
  }

  reactivateCustomerSim(customer: CustomerWithAbonnements): void {
    const sim = this.getTargetSim(customer, new Set(['SUSPENDED']));
    if (!sim || !this.hasSuspendedSim(customer) || this.actionInProgressRef === customer.customerRef) {
      return;
    }

    this.actionInProgressRef = customer.customerRef;
    this.boutiqueApi.reactivateSim(sim.iccid)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: updated => {
          this.updateCustomerSim(customer.customerRef, updated);
          this.actionInProgressRef = '';
        },
        error: () => {
          this.actionInProgressRef = '';
        }
      });
  }

  private attachAbonnements(customers: Customer[], abonnements: Abonnement[]): CustomerWithAbonnements[] {
    const byClientRef = new Map<string, Abonnement[]>();
    const byClientId = new Map<number, Abonnement[]>();

    abonnements.forEach(a => {
      if (a.clientRef) {
        const existing = byClientRef.get(a.clientRef) || [];
        existing.push(a);
        byClientRef.set(a.clientRef, existing);
      }
      if (typeof a.clientId === 'number') {
        const existing = byClientId.get(a.clientId) || [];
        existing.push(a);
        byClientId.set(a.clientId, existing);
      }
    });

    return customers.map(customer => {
      const byRef = byClientRef.get(customer.customerRef) || [];
      // Backward-compatible fallback for legacy subscriptions that may miss clientRef.
      const fallbackById = byRef.length
        ? []
        : (byClientId.get(this.extractClientId(customer.customerRef)) || []);

      const merged = [...byRef, ...fallbackById].reduce<Abonnement[]>((acc, current) => {
        if (!acc.some(item => item.id === current.id)) {
          acc.push(current);
        }
        return acc;
      }, []);

      return { ...customer, abonnements: merged };
    });
  }

  private extractClientId(customerRef: string): number {
    const match = customerRef?.match(/(\d+)$/);
    return match ? parseInt(match[1], 10) : 0;
  }

  private getCurrentBoutiqueId(): number {
    const stored = this.authService.getBoutiqueId();
    const parsed = stored ? parseInt(stored, 10) : NaN;
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
  }

  private mapCustomerSims(customers: CustomerWithAbonnements[], stock: StockSim[]): Record<string, StockSim[]> {
    const refByClientId: Record<number, string> = {};

    customers.forEach(customer => {
      const clientIds = this.getCustomerClientIds(customer);
      clientIds.forEach(clientId => {
        refByClientId[clientId] = customer.customerRef;
      });
    });

    const mapped: Record<string, StockSim[]> = {};
    stock.forEach(sim => {
      const clientId = sim.assignedToClientId;
      if (!clientId) {
        return;
      }

      const customerRef = refByClientId[clientId];
      if (!customerRef) {
        return;
      }

      if (!mapped[customerRef]) {
        mapped[customerRef] = [];
      }
      mapped[customerRef].push(sim);
    });

    Object.keys(mapped).forEach(ref => {
      mapped[ref].sort((a, b) => {
        return new Date(b.assignedAt || 0).getTime() - new Date(a.assignedAt || 0).getTime();
      });
    });

    return mapped;
  }

  private getCustomerClientIds(customer: CustomerWithAbonnements): number[] {
    const ids = new Set<number>();

    (customer.abonnements || []).forEach(abonnement => {
      if (typeof abonnement.clientId === 'number' && abonnement.clientId > 0) {
        ids.add(abonnement.clientId);
      }
    });

    if (ids.size === 0) {
      const fallbackClientId = this.extractClientId(customer.customerRef);
      if (fallbackClientId > 0) {
        ids.add(fallbackClientId);
      }
    }

    return Array.from(ids);
  }

  private getTargetSim(customer: CustomerWithAbonnements, allowedStatuses: Set<string>): StockSim | null {
    const sims = this.customerSimsByRef[customer.customerRef] || [];
    return sims.find(sim => allowedStatuses.has(sim.status)) || null;
  }

  private updateCustomerSim(customerRef: string, updated: StockSim): void {
    const sims = [...(this.customerSimsByRef[customerRef] || [])];
    const index = sims.findIndex(sim => sim.iccid === updated.iccid);

    if (index >= 0) {
      sims[index] = updated;
    } else {
      sims.push(updated);
    }

    sims.sort((a, b) => {
      return new Date(b.assignedAt || 0).getTime() - new Date(a.assignedAt || 0).getTime();
    });

    this.customerSimsByRef[customerRef] = sims;
  }

  applyFilters(): void {
    let filtered = [...this.listOfCustomer];

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(c =>
        (c.nom && c.nom.toLowerCase().includes(term)) ||
        (c.prenom && c.prenom.toLowerCase().includes(term)) ||
        (c.email && c.email.toLowerCase().includes(term)) ||
        (c.customerRef && c.customerRef.toLowerCase().includes(term)) ||
        (c.telephone && c.telephone.includes(term))
      );
    }

    if (this.typeFilter) {
      filtered = filtered.filter(c => c.type === this.typeFilter);
    }

    if (this.statusFilter) {
      if (this.statusFilter === 'INACTIVE') {
        filtered = filtered.filter(c => c.status !== 'ACTIVE');
      } else {
        filtered = filtered.filter(c => c.status === this.statusFilter);
      }
    }

    this.filteredCustomers = filtered;
  }

  setFilter(kind: string, value: string): void {
    if (kind === 'status') {
      this.statusFilter = this.statusFilter === value ? '' : value;
      this.typeFilter = '';
    } else {
      this.typeFilter = this.typeFilter === value ? '' : value;
      this.statusFilter = '';
    }
    this.applyFilters();
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.typeFilter = '';
    this.statusFilter = '';
    this.filteredCustomers = this.listOfCustomer;
  }

  detail(ref: string): void {
    this.router.navigate(['/Customers', ref]);
  }

  closeDetails(): void {
    this.customerdetails = null;
  }

  openCreateForm(): void {
    this.router.navigate(['/Customers/new']);
  }

  closeCreateForm(): void {
    this.showCreateForm = false;
  }

  saveCustomer(): void {
    if (!this.formData.nom || !this.formData.prenom || !this.formData.email) return;
    this.saving = true;
    this.customerService.createCustomer(this.formData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadCustomers();
          this.closeCreateForm();
          this.saving = false;
        },
        error: () => { this.saving = false; }
      });
  }

  getInitials(customer: any): string {
    const f = (customer.prenom || '').charAt(0).toUpperCase();
    const l = (customer.nom || '').charAt(0).toUpperCase();
    return f + l;
  }

  getAvatarColor(customer: any): string {
    const colors = ['#5b4bff', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#10b981'];
    const name = (customer.nom || '') + (customer.prenom || '');
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  getTypeIcon(type: string): string {
    switch ((type || '').toUpperCase()) {
      case 'BUSINESS': case 'B2B': return 'business';
      default: return 'person';
    }
  }

}
