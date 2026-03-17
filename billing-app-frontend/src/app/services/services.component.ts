import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Service } from '../core/models';
import { ServicesService } from '../shared/services/services.service';

@Component({
  selector: 'app-services',
  templateUrl: './services.component.html',
  styleUrls: ['./services.component.css']
})
export class ServicesComponent implements OnInit, OnDestroy {
  listofServices: Service[] = [];
  servicedetails: Service | null = null;
  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  formData = { code: '', libelle: '', prixUnitaire: 0, unite: 'OCTET', category: 'DATA' };
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
    return this.listofServices.filter(s => s.active).length;
  }

  get inactiveCount(): number {
    return this.listofServices.filter(s => !s.active).length;
  }

  constructor(private serviceService: ServicesService) {}

  ngOnInit(): void {
    this.loadServices();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadServices(): void {
    this.serviceService.getServices()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          if (data && data.length) {
            this.listofServices = data;
            this.usingSampleData = false;
          } else {
            this.listofServices = this.getSampleServices();
            this.usingSampleData = true;
          }
        },
        error: () => {
          this.listofServices = this.getSampleServices();
          this.usingSampleData = true;
        }
      });
  }

  detail(id: string): void {
    this.serviceService.getServiceDetails(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.servicedetails = data;
      });
  }

  openCreateForm(): void {
    this.formMode = 'create';
    this.formData = { code: '', libelle: '', prixUnitaire: 0, unite: 'OCTET', category: 'DATA' };
    this.showForm = true;
  }

  openEditForm(service: any): void {
    this.formMode = 'edit';
    this.formData = {
      code: service.code || '',
      libelle: service.libelle || '',
      prixUnitaire: service.prixUnitaire || 0,
      unite: (service.unite || 'OCTET').toUpperCase(),
      category: service.category || 'DATA'
    };
    this.servicedetails = service;
    this.showForm = true;
  }

  saveService(): void {
    if (!this.formData.libelle) {
      return;
    }
    this.saving = true;
    const request = {
      code: this.formData.code,
      libelle: this.formData.libelle,
      unite: this.formData.unite,
      prixUnitaire: this.formData.prixUnitaire,
      category: this.formData.category
    };
    if (this.formMode === 'create') {
      this.serviceService.createService(request)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.loadServices();
            this.closeForm();
            this.saving = false;
          },
          error: () => {
            this.saving = false;
          }
        });
    } else if (this.servicedetails) {
      this.serviceService.updateService(String(this.servicedetails.id), request)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.loadServices();
            this.closeForm();
            this.saving = false;
          },
          error: () => {
            this.saving = false;
          }
        });
    }
  }

  deleteService(id: any): void {
    this.confirmModalConfig = {
      title: 'Supprimer le service',
      message: 'Êtes-vous sûr de vouloir supprimer ce service ? Cette action est irréversible.',
      action: () => this.confirmDelete(String(id))
    };
    this.showConfirmModal = true;
  }

  confirmDelete(id: string): void {
    this.showConfirmModal = false;
    if (this.usingSampleData) {
      this.listofServices = this.listofServices.filter(s => String(s.id) !== id);
    } else {
      this.serviceService.deleteService(id)
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => {
          this.loadServices();
        });
    }
  }

  cancelConfirm(): void {
    this.showConfirmModal = false;
  }

  closeForm(): void {
    this.showForm = false;
    this.formData = { code: '', libelle: '', prixUnitaire: 0, unite: 'OCTET', category: 'DATA' };
    this.servicedetails = null;
  }

  getServiceIcon(service: Service): string {
    const cat = (service.category || '').toUpperCase();
    switch (cat) {
      case 'VOICE': return 'call';
      case 'DATA': return 'cell_tower';
      case 'SMS': return 'sms';
      case 'ROAMING': return 'public';
      case 'VALUE_ADDED': return 'star';
      default: return 'miscellaneous_services';
    }
  }

  getCategoryClass(category?: string): string {
    switch ((category || '').toUpperCase()) {
      case 'VOICE': return 'cat-voice';
      case 'DATA': return 'cat-data';
      case 'SMS': return 'cat-sms';
      case 'ROAMING': return 'cat-roaming';
      case 'VALUE_ADDED': return 'cat-value';
      default: return 'cat-default';
    }
  }

  getCategoryLabel(category?: string): string {
    switch ((category || '').toUpperCase()) {
      case 'VOICE': return 'Voix';
      case 'DATA': return 'Données';
      case 'SMS': return 'SMS';
      case 'ROAMING': return 'Roaming';
      case 'VALUE_ADDED': return 'Valeur ajoutée';
      default: return category || 'Autre';
    }
  }

  getUniteLabel(unite?: string): string {
    switch ((unite || '').toUpperCase()) {
      case 'SECONDE': return 'sec';
      case 'OCTET': return 'Mo';
      case 'SMS': return 'sms';
      case 'UNITE': return 'unité';
      default: return unite || 'unité';
    }
  }

  private getSampleServices(): Service[] {
    return [
      { id: 1, code: 'SVC_APPELS', libelle: 'Appels', unite: 'SECONDE', prixUnitaire: 0.005, category: 'VOICE', active: true },
      { id: 2, code: 'SVC_DATA', libelle: 'Données Mobiles', unite: 'OCTET', prixUnitaire: 0.010, category: 'DATA', active: true },
      { id: 3, code: 'SVC_SMS', libelle: 'SMS', unite: 'SMS', prixUnitaire: 0.040, category: 'SMS', active: true },
      { id: 4, code: 'SVC_ROAMING', libelle: 'Roaming', unite: 'SECONDE', prixUnitaire: 0.090, category: 'ROAMING', active: true },
    ];
  }
}
