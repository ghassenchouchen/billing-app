import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Offer, Service } from '../core/models';
import { OffersService } from '../shared/services/offers.service';
import { ServicesService } from '../shared/services/services.service';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-offers',
  templateUrl: './offers.component.html',
  styleUrls: ['./offers.component.css']
})
export class OffersComponent implements OnInit, OnDestroy {
  listofoffers: Offer[] = [];
  offersdetails: Offer | null = null;
  availableServices: Service[] = [];
  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  formData = { libelle: '', serviceId: '', description: '', prixMensuel: 0, paymentType: 'POSTPAID', status: 'ACTIVE' };
  saving = false;
  viewMode: 'cards' | 'table' = 'cards';
  showConfirmModal = false;
  confirmModalConfig = {
    title: '',
    message: '',
    action: null as (() => void) | null
  };
    usingSampleData = false;
  private destroy$ = new Subject<void>();

  get activeCount(): number {
    return this.listofoffers.filter(o => o.status === 'ACTIVE').length;
  }

  get inactiveCount(): number {
    return this.listofoffers.filter(o => o.status !== 'ACTIVE').length;
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  constructor(private offersService: OffersService, private servicesService: ServicesService, private authService: AuthService) {}

  ngOnInit(): void {
    this.loadOffers();
    this.loadServices();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadOffers(): void {
    this.offersService.getOffers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          if (data && data.length) {
            this.listofoffers = data;
            this.usingSampleData = false;
          } else {
            this.listofoffers = this.getSampleOffers();
            this.usingSampleData = true;
          }
        },
        error: () => {
          this.listofoffers = this.getSampleOffers();
          this.usingSampleData = true;
        }
      });
  }

  private getSampleOffers(): Offer[] {
    return [
      { id: 1, code: 'FIBRE_20', libelle: 'Fibre Essentiel 20M', description: 'Connexion fibre optique 20 Mbps \u2014 id\u00e9ale pour les particuliers. Inclut Wi-Fi et assistance 7j/7.', prixMensuel: 35.00, status: 'ACTIVE', paymentType: 'POSTPAID', serviceIds: [5] },
      { id: 2, code: 'FIBRE_100', libelle: 'Fibre Pro 100M', description: 'Fibre 100 Mbps d\u00e9di\u00e9e aux professionnels avec IP fixe et SLA garanti 99.9%.', prixMensuel: 85.00, status: 'ACTIVE', paymentType: 'POSTPAID', serviceIds: [5] },
      { id: 3, code: 'MOB_5G_ILL', libelle: 'Mobile 5G Illimit\u00e9', description: 'Appels & SMS illimit\u00e9s + 100 Go data 5G. Roaming Maghreb inclus.', prixMensuel: 75.00, status: 'ACTIVE', paymentType: 'POSTPAID', serviceIds: [1, 4, 6, 8] },
      { id: 4, code: 'MOB_4G_25', libelle: 'Forfait Mobile 4G 25 Go', description: 'Forfait pr\u00e9pay\u00e9 25 Go data 4G + 2h d\'appels nationaux. Rechargeable en ligne.', prixMensuel: 19.90, status: 'ACTIVE', paymentType: 'PREPAID', serviceIds: [1, 3, 6] },
      { id: 5, code: 'MOB_4G_10', libelle: 'Forfait Mobile 4G 10 Go', description: 'Forfait \u00e9conomique 10 Go data 4G + 1h d\'appels. Id\u00e9al pour usage l\u00e9ger.', prixMensuel: 12.00, status: 'ACTIVE', paymentType: 'PREPAID', serviceIds: [1, 3, 6] },
      { id: 6, code: 'PRO_CONV', libelle: 'Entreprise Convergent', description: 'Solution convergente Fibre 200 Mbps + 5 lignes mobiles + standard t\u00e9l\u00e9phonique IP.', prixMensuel: 280.00, status: 'ACTIVE', paymentType: 'POSTPAID', serviceIds: [1, 2, 4, 5, 6] },
      { id: 7, code: 'DATA_BOOST', libelle: 'Recharge Data 10 Go', description: 'Extension data 10 Go \u00e0 activer en compl\u00e9ment de votre forfait existant.', prixMensuel: 8.00, status: 'ACTIVE', paymentType: 'PREPAID', serviceIds: [3] },
      { id: 8, code: 'ROAM_MAG', libelle: 'Pack Roaming Maghreb', description: 'Forfait roaming voix + data pour Alg\u00e9rie, Maroc et Libye. 5 Go + 3h d\'appels.', prixMensuel: 45.00, status: 'INACTIVE', paymentType: 'PREPAID', serviceIds: [8, 9] },
    ];
  }

  loadServices(): void {
    this.servicesService.getServices()
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.availableServices = data;
      });
  }

  detail(id: string): void {
    this.offersService.getOfferDetails(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.offersdetails = data;
      });
  }

  openCreateForm(): void {
    this.formMode = 'create';
    this.formData = { libelle: '', serviceId: '', description: '', prixMensuel: 0, paymentType: 'POSTPAID', status: 'ACTIVE' };
    this.showForm = true;
  }

  openEditForm(offer: any): void {
    this.formMode = 'edit';
    this.formData = {
      libelle: offer.libelle || offer.nom || offer.code || '',
      serviceId: offer.serviceIds?.length ? String(offer.serviceIds[0]) : '',
      description: offer.description || '',
      prixMensuel: offer.prixMensuel || 0,
      paymentType: offer.paymentType || 'POSTPAID',
      status: offer.status || 'ACTIVE'
    };
    this.offersdetails = offer;
    this.showForm = true;
  }

  saveOffer(): void {
    if (!this.formData.libelle || !this.formData.serviceId) {
      return;
    }
    this.saving = true;
    const request = {
      libelle: this.formData.libelle,
      description: this.formData.description,
      prixMensuel: this.formData.prixMensuel,
      paymentType: this.formData.paymentType,
      status: this.formData.status,
      serviceIds: [parseInt(this.formData.serviceId, 10)]
    };
    if (this.formMode === 'create') {
      this.offersService.createOffer(request)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.loadOffers();
            this.closeForm();
            this.saving = false;
          },
          error: () => {
            this.saving = false;
          }
        });
    } else if (this.offersdetails) {
      this.offersService.updateOffer(String(this.offersdetails.id), request)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.loadOffers();
            this.closeForm();
            this.saving = false;
          },
          error: () => {
            this.saving = false;
          }
        });
    }
  }

  deleteOffer(id: any): void {
    this.confirmModalConfig = {
      title: 'Supprimer l\'offre',
      message: 'Êtes-vous sûr de vouloir supprimer cette offre ? Cette action est irréversible.',
      action: () => this.confirmDelete(String(id))
    };
    this.showConfirmModal = true;
  }

  confirmDelete(id: string): void {
    this.showConfirmModal = false;
    if (this.usingSampleData) {
      this.listofoffers = this.listofoffers.filter(o => String(o.id) !== id);
    } else {
      this.offersService.deleteOffer(id)
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => {
          this.loadOffers();
        });
    }
  }

  cancelConfirm(): void {
    this.showConfirmModal = false;
  }

  closeForm(): void {
    this.showForm = false;
    this.formData = { libelle: '', serviceId: '', description: '', prixMensuel: 0, paymentType: 'POSTPAID', status: 'ACTIVE' };
    this.offersdetails = null;
  }

  getOfferIcon(offer: Offer): string {
    const name = (offer.libelle || offer.nom || offer.code || '').toLowerCase();
    if (name.includes('fibre') || name.includes('fiber')) return 'wifi';
    if (name.includes('5g') || name.includes('4g') || name.includes('mobile')) return 'smartphone';
    if (name.includes('pro') || name.includes('entreprise') || name.includes('business')) return 'business';
    if (name.includes('illimité') || name.includes('ultra') || name.includes('premium')) return 'bolt';
    if (name.includes('data') || name.includes('internet')) return 'language';
    return 'redeem';
  }
}
