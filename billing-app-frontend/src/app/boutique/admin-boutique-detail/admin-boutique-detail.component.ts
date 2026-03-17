import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AuthService } from '../../core/services/auth.service';
import {
  BoutiqueApiService,
  Boutique,
  DashboardData,
  StockSim,
  TransactionBoutique
} from '../../core/services/boutique-api.service';
import { UserService, UserDto } from '../../core/services/user.service';
import { CustomerService } from '../../shared/services/customer.service';
import { AbonnementService } from '../../shared/services/abonnement.service';
import { Customer, Abonnement } from '../../core/models';
import { filterList } from '../../shared/utils/list-filter.util';

type TabName = 'dashboard' | 'stock' | 'transactions' | 'team' | 'customers' | 'subscriptions';

@Component({
  selector: 'app-admin-boutique-detail',
  templateUrl: './admin-boutique-detail.component.html',
  styleUrls: ['./admin-boutique-detail.component.css']
})
export class AdminBoutiqueDetailComponent implements OnInit, OnDestroy {

  // state
  boutiqueId!: number;
  boutique: Boutique | null = null;
  activeTab: TabName = 'dashboard';
  loading = true;
  forbidden = false;

  dashboard: DashboardData | null = null;
  stock: StockSim[] = [];
  transactions: TransactionBoutique[] = [];
  team: UserDto[] = [];
  customers: Customer[] = [];
  subscriptions: Abonnement[] = [];

  filteredStock: StockSim[] = [];
  filteredTransactions: TransactionBoutique[] = [];
  filteredCustomers: Customer[] = [];
  filteredSubscriptions: Abonnement[] = [];

  stockSearch = '';
  stockStatusFilter = '';
  txnSearch = '';
  txnStatusFilter = '';
  customerSearch = '';
  customerStatusFilter = '';
  subSearch = '';
  subStatusFilter = '';

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private boutiqueApi: BoutiqueApiService,
    private userService: UserService,
    private customerService: CustomerService,
    private abonnementService: AbonnementService
  ) {}

// lifecycle
  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.boutiqueId = idParam ? parseInt(idParam, 10) : 0;

    if (!this.authService.isAdmin()) {
      const userBoutiqueId = this.authService.getBoutiqueId();
      if (!userBoutiqueId || parseInt(userBoutiqueId, 10) !== this.boutiqueId) {
        this.forbidden = true;
        this.loading = false;
        return;
      }
    }

    this.loadBoutique();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // tab management

  switchTab(tab: TabName): void {
    this.activeTab = tab;
    this.loadTabData();
  }

// data loading
  loadBoutique(): void {
    this.loading = true;
    this.boutiqueApi.getById(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (b) => {
          this.boutique = b;
          this.loading = false;
          this.loadTabData();
        },
        error: () => { this.loading = false; }
      });
  }

  private loadTabData(): void {
    const loaders: Record<TabName, () => void> = {
      dashboard: () => this.loadDashboard(),
      stock: () => this.loadStock(),
      transactions: () => this.loadTransactions(),
      team: () => this.loadTeam(),
      customers: () => this.loadCustomers(),
      subscriptions: () => this.loadSubscriptions(),
    };
    loaders[this.activeTab]();
  }

  private loadDashboard(): void {
    if (this.dashboard) return;
    this.boutiqueApi.getDashboard(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (d) => this.dashboard = d,
        error: () => {
          this.dashboard = {
            revenueToday: 0, contractsThisMonth: 0, contractTarget: 10,
            simAvailable: 0, simLowStock: 0, simByType: {}
          };
        }
      });
  }

  private loadStock(): void {
    if (this.stock.length) { this.applyStockFilters(); return; }
    this.boutiqueApi.getStock(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (s) => { this.stock = s; this.applyStockFilters(); },
        error: () => { this.stock = []; this.filteredStock = []; }
      });
  }

  private loadTransactions(): void {
    if (this.transactions.length) { this.applyTxnFilters(); return; }
    this.boutiqueApi.getTransactions(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (t) => { this.transactions = t; this.applyTxnFilters(); },
        error: () => { this.transactions = []; this.filteredTransactions = []; }
      });
  }

  private loadTeam(): void {
    if (this.team.length) return;
    this.userService.getTeamByBoutique(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (users) => { this.team = users; },
        error: () => { this.team = []; }
      });
  }

  private loadCustomers(): void {
    if (this.customers.length) { this.applyCustomerFilters(); return; }
    if (!this.boutique) return;
    this.customerService.getCustomersByBoutique(this.boutique.code)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (c) => { this.customers = c; this.applyCustomerFilters(); },
        error: () => { this.customers = []; this.filteredCustomers = []; }
      });
  }

  private loadSubscriptions(): void {
    if (this.subscriptions.length) { this.applySubFilters(); return; }
    
    // Fetch all subscriptions in a single call, then filter by customer refs
    this.abonnementService.getAbonnements()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (allSubs) => {
          // Get customer refs from already loaded customers, or load them first
          if (this.customers.length) {
            this.filterSubscriptionsByCustomers(allSubs, this.customers);
          } else if (this.boutique) {
            // Need to load customers first to know which subscriptions belong to this boutique
            this.customerService.getCustomersByBoutique(this.boutique.code)
              .pipe(takeUntil(this.destroy$))
              .subscribe({
                next: (customers) => {
                  this.customers = customers;
                  this.filterSubscriptionsByCustomers(allSubs, customers);
                },
                error: () => { this.subscriptions = []; this.filteredSubscriptions = []; }
              });
          } else {
            this.subscriptions = [];
            this.filteredSubscriptions = [];
          }
        },
        error: () => { this.subscriptions = []; this.filteredSubscriptions = []; }
      });
  }

  private filterSubscriptionsByCustomers(allSubs: Abonnement[], customers: Customer[]): void {
    const customerRefs = new Set(customers.map(c => c.customerRef));
    this.subscriptions = allSubs.filter(sub => 
      sub.clientRef && customerRefs.has(sub.clientRef)
    );
    this.applySubFilters();
  }


  applyStockFilters(): void {
    this.filteredStock = filterList(this.stock, {
      search: this.stockSearch,
      searchFields: ['iccid', 'msisdn'],
      statusField: 'status',
      statusValue: this.stockStatusFilter,
    });
  }

  applyTxnFilters(): void {
    this.filteredTransactions = filterList(this.transactions, {
      search: this.txnSearch,
      searchFields: ['clientNom', 'reference', 'offreLibelle'],
      statusField: 'status',
      statusValue: this.txnStatusFilter,
    });
  }

  applyCustomerFilters(): void {
    this.filteredCustomers = filterList(this.customers, {
      search: this.customerSearch,
      searchFields: ['nom', 'prenom', 'email', 'customerRef', 'telephone'],
      statusField: 'status',
      statusValue: this.customerStatusFilter,
    });
  }

  applySubFilters(): void {
    this.filteredSubscriptions = filterList(this.subscriptions, {
      search: this.subSearch,
      searchFields: ['clientRef'],
      statusField: 'status',
      statusValue: this.subStatusFilter,
    });
  }

  // navigation

  goBack(): void { this.router.navigate(['/Boutiques']); }
  viewCustomers(): void { this.switchTab('customers'); }
  viewCustomerDetail(ref: string): void { this.router.navigate(['/Customers', ref]); }
  viewSubscriptionDetail(id: number): void { this.router.navigate(['/Abonnements', id]); }

  // helpers

  get availableSimCount(): number {
    return this.stock.filter(s => s.status === 'AVAILABLE').length;
  }

  getCustomerTypeIcon(type: string): string {
    const upper = (type || '').toUpperCase();
    return upper === 'BUSINESS' || upper === 'B2B' ? 'business' : 'person';
  }

  getSimTypeLabel(type: string): string {
    return type === 'ESIM' ? 'eSIM' : type === 'STANDARD' ? 'Standard' : type;
  }
}
