import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { BoutiqueApiService, StockSim } from '../../../core/services/boutique-api.service';
import { CustomerService } from '../../../shared/services/customer.service';
import { Customer } from '../../../core/models';
import { OffersService } from '../../../shared/services/offers.service';
import { Offer } from '../../../core/models';

@Component({
  selector: 'app-sim-activate',
  templateUrl: './sim-activate.component.html',
  styleUrls: ['./sim-activate.component.css']
})
export class SimActivateComponent implements OnInit, OnDestroy {
  // Wizard steps: 1=Client, 2=SIM Info, 3=Offrer, 4=Confirmation
  currentStep = 1;
  
  sim: StockSim | null = null;
  customers: Customer[] = [];
  filteredCustomers: Customer[] = [];
  selectedCustomer: Customer | null = null;
  searchQuery = '';
  loading = true;
  assigning = false;
  success = false;
  error = '';
  
  // Offer selection (step 3)
  offers: Offer[] = [];
  filteredOffers: Offer[] = [];
  selectedOffer: Offer | null = null;
  offerFilter = 'Tous';
  loadingOffers = false;
  billingFrequency: 'MONTHLY' | 'QUARTERLY' | 'ANNUAL' = 'MONTHLY';

  private iccid = '';
  private boutiqueId = 1;
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private boutiqueApi: BoutiqueApiService,
    private customerService: CustomerService,
    private offersService: OffersService
  ) {}

  ngOnInit(): void {
    this.iccid = this.route.snapshot.paramMap.get('iccid') || '';
    const storedId = this.authService.getBoutiqueId();
    this.boutiqueId = storedId ? parseInt(storedId, 10) : 1;

    this.loadSimAndCustomers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadSimAndCustomers(): void {
    this.loading = true;

    // Load stock to find the SIM
    this.boutiqueApi.getStock(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stock) => {
          this.sim = stock.find(s => s.iccid === this.iccid) || null;
          if (!this.sim) {
            this.error = 'Carte SIM introuvable';
          }
        },
        error: () => {
          // Fallback sample SIM
          this.sim = {
            id: 1, iccid: this.iccid, imsi: '60501000000001', msisdn: '+21620100001',
            simType: 'STANDARD', status: 'AVAILABLE', boutiqueId: this.boutiqueId,
            assignedToClientId: null, assignedAt: null, createdAt: '2025-01-10T08:00:00'
          };
        }
      });

    // Load customers
    this.customerService.getCustomers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (customers) => {
          this.customers = customers;
          this.filteredCustomers = customers;
          this.loading = false;
        },
        error: () => {
          // Fallback sample customers
          this.customers = [
            { customerRef: 'CL-000001', nom: 'Ben Ali', prenom: 'Mohamed', email: 'mohamed.benali@email.tn', telephone: '+216 20 100 001', adresse: 'Rue de la Liberté, Tunis', type: 'INDIVIDUAL', status: 'ACTIVE' },
            { customerRef: 'CL-000002', nom: 'Trabelsi', prenom: 'Amira', email: 'amira.trabelsi@email.tn', telephone: '+216 20 200 002', adresse: 'Av. Bourguiba, Sfax', type: 'INDIVIDUAL', status: 'ACTIVE' },
            { customerRef: 'CL-000003', nom: 'Mansouri', prenom: 'Fatma', email: 'fatma.mansouri@email.tn', telephone: '+216 20 300 003', adresse: 'Rue Ibn Khaldoun, Sousse', type: 'INDIVIDUAL', status: 'ACTIVE' },
            { customerRef: 'CL-000004', nom: 'Gharbi', prenom: 'Youssef', email: 'youssef.gharbi@email.tn', telephone: '+216 20 400 004', adresse: 'Av. de France, Tunis', type: 'INDIVIDUAL', status: 'ACTIVE' },
            { customerRef: 'CL-000005', nom: 'TechnoServ', prenom: 'SA', email: 'contact@technoserv.tn', telephone: '+216 71 500 005', adresse: 'Zone Industrielle, Ben Arous', type: 'BUSINESS', status: 'ACTIVE' },
          ];
          this.filteredCustomers = this.customers;
          this.loading = false;
        }
      });
  }

  filterCustomers(): void {
    const q = this.searchQuery.toLowerCase().trim();
    if (!q) {
      this.filteredCustomers = this.customers;
      return;
    }
    this.filteredCustomers = this.customers.filter(c =>
      c.nom.toLowerCase().includes(q) ||
      c.prenom.toLowerCase().includes(q) ||
      c.email.toLowerCase().includes(q) ||
      c.customerRef.toLowerCase().includes(q) ||
      (c.telephone && c.telephone.includes(q))
    );
  }

  selectCustomer(customer: Customer): void {
    this.selectedCustomer = customer;
  }

  // offer loading
  loadOffers(): void {
    this.loadingOffers = true;
    this.offersService.getOffers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.offers = data && data.length ? data : this.getSampleOffers();
          this.filteredOffers = this.offers.filter(o => o.status === 'ACTIVE' || o.active);
          this.loadingOffers = false;
        },
        error: () => {
          this.offers = this.getSampleOffers();
          this.filteredOffers = this.offers.filter(o => o.status === 'ACTIVE' || o.active);
          this.loadingOffers = false;
        }
      });
  }

  setOfferFilter(filter: string): void {
    this.offerFilter = filter;
    const active = this.offers.filter(o => o.status === 'ACTIVE' || o.active);
    if (filter === 'Tous') {
      this.filteredOffers = active;
    } else if (filter === 'Mobile') {
      this.filteredOffers = active.filter(o =>
        (o.libelle || o.nom || '').toLowerCase().match(/mobile|4g|5g|forfait/) ||
        (o.description || '').toLowerCase().match(/appel|sms|data/)
      );
    } else if (filter === 'Internet') {
      this.filteredOffers = active.filter(o =>
        (o.libelle || o.nom || '').toLowerCase().match(/fibre|internet|adsl/)
      );
    } else if (filter === 'B2B') {
      this.filteredOffers = active.filter(o =>
        (o.libelle || o.nom || '').toLowerCase().match(/entreprise|pro|business|convergent/)
      );
    }
  }

  selectOffer(offer: Offer): void {
    this.selectedOffer = offer;
  }

  // wizard navigation
  nextStep(): void {
    if (this.currentStep === 1 && this.selectedCustomer) {
      this.currentStep = 2;
    } else if (this.currentStep === 2) {
      this.loadOffers();
      this.currentStep = 3;
    } else if (this.currentStep === 3 && this.selectedOffer) {
      this.currentStep = 4;
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  goToStep(step: number): void {
    if (step < this.currentStep && step >= 1) {
      this.currentStep = step;
    }
  }

  confirmAssignment(): void {
    if (!this.selectedCustomer || !this.sim || !this.selectedOffer) return;

    this.assigning = true;
    this.error = '';

    // Extract client ID from customerRef (e.g., 'CL-000001' -> 1)
    const match = this.selectedCustomer.customerRef.match(/(\d+)$/);
    const clientId = match ? parseInt(match[1], 10) : 0;

    if (!clientId) {
      this.error = 'ID client invalide';
      this.assigning = false;
      return;
    }

    this.boutiqueApi.assignAndActivateSim(this.sim.iccid, clientId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.assigning = false;
          this.router.navigate(['/Boutique/stock']);
        },
        error: () => {
          this.assigning = false;
          this.error = 'Erreur lors de l\'activation de la SIM';
        }
      });
  }

  private getSampleOffers(): Offer[] {
    return [
      { id: 1, code: 'FIBRE-20M', libelle: 'Fibre Essentiel 20M', description: 'Internet fibre optique 20 Mbps', prixMensuel: 35.00, status: 'ACTIVE', paymentType: 'POSTPAID', serviceIds: [] },
      { id: 2, code: 'MOB-5G-10G', libelle: 'Mobile 5G 10Go', description: 'Forfait mobile 5G avec 10 Go', prixMensuel: 25.00, status: 'ACTIVE', paymentType: 'POSTPAID', serviceIds: [] },
      { id: 3, code: 'MOB-4G-25G', libelle: 'Forfait Mobile 4G 25 Go', description: 'Forfait mobile 4G, 25 Go data', prixMensuel: 45.00, status: 'ACTIVE', paymentType: 'POSTPAID', serviceIds: [] },
      { id: 4, code: 'MOB-PREPAID', libelle: 'Prépayé Mobile', description: 'Forfait prépayé flexible', prixMensuel: 15.00, status: 'ACTIVE', paymentType: 'PREPAID', serviceIds: [] },
    ];
  }

  goBack(): void {
    this.router.navigate(['/Boutique/stock']);
  }

  formatIccid(iccid: string): string {
    return '…' + iccid.slice(-8);
  }

  getInitials(customer: Customer): string {
    return (customer.prenom.charAt(0) + customer.nom.charAt(0)).toUpperCase();
  }

  getOfferIcon(offer: Offer): string {
    const name = (offer.libelle || offer.nom || '').toLowerCase();
    if (name.match(/fibre|internet/)) return 'wifi';
    if (name.match(/mobile|4g|5g|forfait/)) return 'smartphone';
    if (name.match(/entreprise|pro|convergent|business/)) return 'business';
    return 'redeem';
  }

  getOfferPrice(offer: Offer): number {
    return offer.prixMensuel || offer.prixBase || 0;
  }

  getPaymentBadgeClass(offer: Offer): string {
    return (offer.paymentType || '').toUpperCase() === 'PREPAID' ? 'prepaid' : 'postpaid';
  }

  getFrequencyLabel(): string {
    switch (this.billingFrequency) {
      case 'QUARTERLY': return 'Trimestrielle';
      case 'ANNUAL': return 'Annuelle';
      default: return 'Mensuelle';
    }
  }

  getCustomerFullName(c: Customer): string {
    if (c.type === 'BUSINESS') return c.nom;
    return `${c.prenom} ${c.nom}`.trim();
  }

  getAvatarColor(c: Customer): string {
    const colors = ['#5b4bff', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#10b981'];
    const name = (c.nom || '') + (c.prenom || '');
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }
}
