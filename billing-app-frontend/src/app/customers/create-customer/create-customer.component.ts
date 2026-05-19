import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CustomerService } from '../../shared/services/customer.service';
import { OffersService } from '../../shared/services/offers.service';
import { Offer } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { BoutiqueApiService, StockSim } from '../../core/services/boutique-api.service';
import { TUNISIA_CITIES, getCodePostalByVille, getGouvernoratByVille, getAllVilles } from '../../shared/constants/tunisia-cities';

@Component({
  selector: 'app-create-customer',
  templateUrl: './create-customer.component.html',
  styleUrls: ['./create-customer.component.css']
})
export class CreateCustomerComponent implements OnInit, OnDestroy {
  currentStep = 1;
  saving = false;

  
  flowType: 'standard' | 'sim' = 'standard';

  clientForm = {
    type: 'INDIVIDUAL',
    nom: '',
    prenom: '',
    pieceIdentite: '',
    telephone: '',
    email: '',
    adresse: '',
    ville: '',
    codePostal: '',
    gouvernorat: ''
  };

  tunisiaVilles: string[] = [];

  availableSims: StockSim[] = [];
  filteredSims: StockSim[] = [];
  selectedSim: StockSim | null = null;
  simSearchQuery = '';
  simFilterType = '';
  loadingSims = false;
  boutiqueId = 1;

  availableOffers: Offer[] = [];
  filteredOffers: Offer[] = [];
  selectedOfferId: number | null = null;
  paymentType = 'POSTPAID';
  loadingOffers = false;

  formErrors: { [key: string]: string } = {};

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private customerService: CustomerService,
    private offersService: OffersService,
    private authService: AuthService,
    private boutiqueApi: BoutiqueApiService
  ) {}

  ngOnInit(): void {
    this.tunisiaVilles = getAllVilles();
    if (this.authService.isAdmin()) {
      this.router.navigate(['/Customers']);
      return;
    }
    const storedId = this.authService.getBoutiqueId();
    this.boutiqueId = storedId ? parseInt(storedId, 10) : 1;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get totalSteps(): number {
    return this.flowType === 'sim' ? 5 : 4;
  }

  getStepLabel(step: number): string {
    if (this.flowType === 'sim') {
      switch (step) {
        case 1: return 'Client';
        case 2: return 'Parcours';
        case 3: return 'SIM';
        case 4: return 'Offre';
        case 5: return 'Validation';
      }
    } else {
      switch (step) {
        case 1: return 'Client';
        case 2: return 'Parcours';
        case 3: return 'Offre';
        case 4: return 'Validation';
      }
    }
    return '';
  }

  //Navigation
  cancel(): void {
    this.router.navigate(['/Customers']);
  }

  nextStep(): void {
    if (this.currentStep === 1 && this.validateStep1()) {
      this.currentStep = 2;
    } else if (this.currentStep === 2) {
      // Flow selection done
      if (this.flowType === 'sim') {
        this.currentStep = 3;
        this.loadAvailableSims();
      } else {
        this.currentStep = 3;
        this.loadOffers(false);
      }
    } else if (this.currentStep === 3) {
      if (this.flowType === 'sim') {
        if (!this.selectedSim) {
          this.formErrors['sim'] = 'Veuillez sélectionner une carte SIM';
          return;
        }
        this.formErrors = {};
        this.currentStep = 4;
        this.loadOffers(true); // Mobile only
      } else {
        if (!this.validateStepOffer()) return;
        this.currentStep = 4;
      }
    } else if (this.currentStep === 4) {
      if (this.flowType === 'sim') {
        if (!this.validateStepOffer()) return;
        this.currentStep = 5;
      }
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  goToStep(step: number): void {
    if (step < this.currentStep) {
      this.currentStep = step;
    }
  }

  setFlowType(type: 'standard' | 'sim'): void {
    this.flowType = type;
    // Reset SIM and offer selection when changing flow
    this.selectedSim = null;
    this.selectedOfferId = null;
  }

  onPhoneChange(): void {
    // only 8 digits
    this.clientForm.telephone = this.clientForm.telephone.replace(/\D/g, '').slice(0, 8);
  }

  onVilleChange(): void {
    if (this.clientForm.ville) {
      this.clientForm.codePostal = getCodePostalByVille(this.clientForm.ville);
      this.clientForm.gouvernorat = getGouvernoratByVille(this.clientForm.ville);
    }
  }

  validateStep1(): boolean {
    this.formErrors = {};
    if (!this.clientForm.nom.trim()) {
      this.formErrors['nom'] = 'Le nom est requis';
    }
    if (this.clientForm.type === 'INDIVIDUAL' && !this.clientForm.prenom.trim()) {
      this.formErrors['prenom'] = 'Le prénom est requis';
    }
    if (!this.clientForm.email.trim()) {
      this.formErrors['email'] = 'L\'email est requis';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.clientForm.email)) {
      this.formErrors['email'] = 'Email invalide';
    }
    if (!this.clientForm.telephone.trim()) {
      this.formErrors['telephone'] = 'Le téléphone est requis';
    } else if (!/^\d{8}$/.test(this.clientForm.telephone.trim())) {
      this.formErrors['telephone'] = 'Le téléphone doit contenir exactement 8 chiffres';
    }
    if (!this.clientForm.pieceIdentite.trim()) {
      this.formErrors['pieceIdentite'] = this.clientForm.type === 'BUSINESS'
        ? 'L\'identifiant fiscal est requis'
        : 'La pièce d\'identité est requise';
    } else if (this.clientForm.type === 'INDIVIDUAL' && !/^\d{8}$/.test(this.clientForm.pieceIdentite.trim())) {
      this.formErrors['pieceIdentite'] = 'Le CIN doit contenir exactement 8 chiffres';
    } else if (this.clientForm.type === 'BUSINESS' && !/^\d{7}[A-Z]\/[A-Z]\/[A-Z]\/\d{3}$/.test(this.clientForm.pieceIdentite.trim())
               && this.clientForm.pieceIdentite.trim().length < 7) {
      this.formErrors['pieceIdentite'] = 'L\'identifiant fiscal semble invalide';
    }
    return Object.keys(this.formErrors).length === 0;
  }

  validateStepOffer(): boolean {
    this.formErrors = {};
    if (!this.selectedOfferId) {
      this.formErrors['offer'] = 'Veuillez sélectionner une offre';
    }
    return Object.keys(this.formErrors).length === 0;
  }

  loadAvailableSims(): void {
    this.loadingSims = true;
    this.boutiqueApi.getAvailableStock(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (sims) => {
          this.availableSims = sims.filter(s => s.status === 'AVAILABLE');
          this.filteredSims = this.availableSims;
          this.loadingSims = false;
        },
        error: () => {
          this.availableSims = [];
          this.filteredSims = this.availableSims;
          this.loadingSims = false;
        }
      });
  }

  filterSims(): void {
    const q = this.simSearchQuery.toLowerCase().trim();
    this.filteredSims = this.availableSims.filter(s => {
      const matchSearch = !q || s.iccid.includes(q) || s.msisdn.includes(q) || s.imsi.includes(q);
      const matchType = !this.simFilterType || s.simType === this.simFilterType;
      return matchSearch && matchType;
    });
  }

  selectSim(sim: StockSim): void {
    this.selectedSim = sim;
    this.formErrors = {};
  }

  loadOffers(mobileOnly: boolean): void {
    this.loadingOffers = true;
    this.offersService.getOffers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          let offers = (data && data.length) ? data.filter(o => o.status === 'ACTIVE') : [];
          if (mobileOnly) {
            offers = this.filterMobileOffers(offers);
          }
          this.availableOffers = offers;
          this.filteredOffers = offers;
          this.loadingOffers = false;
        },
        error: () => {
          let offers: Offer[] = [];
          if (mobileOnly) {
            offers = this.filterMobileOffers(offers);
          }
          this.availableOffers = offers;
          this.filteredOffers = offers;
          this.loadingOffers = false;
        }
      });
  }

  filterMobileOffers(offers: Offer[]): Offer[] {
    return offers.filter(o => {
      const name = (o.libelle || o.nom || '').toLowerCase();
      const desc = (o.description || '').toLowerCase();
      return name.match(/mobile|4g|5g|forfait|prépayé|prepaid/) ||
             desc.match(/appel|sms|data|go\s|go$/) ||
             (o.code && o.code.toLowerCase().match(/mob|gsm/));
    });
  }

  selectOffer(id: number): void {
    this.selectedOfferId = this.selectedOfferId === id ? null : id;
    this.formErrors = {};
  }

  getSelectedOffer(): Offer | null {
    if (!this.selectedOfferId) return null;
    return this.availableOffers.find(o => o.id === this.selectedOfferId) || null;
  }

  submit(): void {
    this.saving = true;
    const localPhone = (this.clientForm.telephone || '').replace(/\D/g, '').slice(0, 8);
    const customerPayload = {
      nom: this.clientForm.nom,
      prenom: this.clientForm.prenom || '',
      email: this.clientForm.email,
      telephone: `+216${localPhone}`,
      pieceIdentite: this.clientForm.pieceIdentite || '',
      adresse: this.clientForm.adresse,
      type: this.clientForm.type,
      ville: this.clientForm.ville || '',
      codePostal: this.clientForm.codePostal || ''
    };

    this.customerService.createCustomer(customerPayload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (createdCustomer) => {
          if (this.flowType === 'sim' && this.selectedSim) {
            // Extract client ID and activate SIM
            const clientId = this.extractClientId(createdCustomer);
            this.boutiqueApi.assignAndActivateSim(this.selectedSim.iccid, clientId)
              .pipe(takeUntil(this.destroy$))
              .subscribe({
                next: () => {
                  this.saving = false;
                  this.router.navigate(['/Customers']);
                },
                error: () => {
                  this.saving = false;
                  this.router.navigate(['/Customers']);
                }
              });
          } else {
            this.saving = false;
            this.router.navigate(['/Customers']);
          }
        },
        error: () => {
          this.saving = false;
          this.router.navigate(['/Customers']);
        }
      });
  }

  extractClientId(customer: any): number {
    if (customer?.id) return customer.id;
    if (customer?.customerRef) {
      const match = customer.customerRef.match(/(\d+)$/);
      return match ? parseInt(match[1], 10) : 1;
    }
    return 1;
  }

  resetForm(): void {
    this.clientForm = {
      type: 'INDIVIDUAL', nom: '', prenom: '',
      pieceIdentite: '', telephone: '', email: '', adresse: '',
      ville: '', codePostal: '', gouvernorat: ''
    };
    this.formErrors = {};
    this.selectedSim = null;
    this.selectedOfferId = null;
    this.flowType = 'standard';
  }

  getOfferIcon(offer: Offer): string {
    const name = (offer.libelle || '').toLowerCase();
    if (name.includes('fibre')) return 'wifi';
    if (name.includes('5g') || name.includes('4g') || name.includes('mobile')) return 'smartphone';
    if (name.includes('entreprise') || name.includes('business')) return 'business';
    if (name.includes('data') || name.includes('recharge')) return 'language';
    if (name.includes('roaming')) return 'public';
    return 'redeem';
  }

  getTypeLabel(): string {
    return this.clientForm.type === 'BUSINESS' ? 'Business / B2B' : 'Individu';
  }

  formatIccid(iccid: string): string {
    return '…' + iccid.slice(-8);
  }

  getSimTypeLabel(type: string): string {
    return type === 'ESIM' ? 'eSIM' : 'Standard';
  }

}
