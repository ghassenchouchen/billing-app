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
  loadError = false;
  private idToDelete: string | null = null;
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
    this.loadError = false;
    this.offersService.getOffers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.listofoffers = data || [];
        },
        error: () => {
          this.listofoffers = [];
          this.loadError = true;
        }
      });
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
    const request: any = {
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
      // Include the existing code when updating
      request.code = this.offersdetails.code;
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
    console.log('Delete offer button clicked for ID:', id);
    this.idToDelete = String(id);
    this.confirmModalConfig = {
      title: 'Supprimer l\'offre',
      message: 'Êtes-vous sûr de vouloir supprimer cette offre ? Cette action est irréversible.',
      action: null
    };
    this.showConfirmModal = true;
  }

  confirmDelete(id: string): void {
    this.showConfirmModal = false;
    this.offersService.deleteOffer(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadOffers();
      });
  }

  cancelConfirm(): void {
    this.showConfirmModal = false;
  }

  onConfirmDelete(): void {
    console.log('Confirmation modal: Supprimer clicked. ID to delete:', this.idToDelete);
    if (this.idToDelete) {
      this.confirmDelete(this.idToDelete);
    }
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
