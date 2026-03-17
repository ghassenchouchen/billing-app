import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AbonnementService } from '../../shared/services/abonnement.service';
import { Abonnement } from '../../core/models';

@Component({
  selector: 'app-customer-abonnements',
  templateUrl: './customer-abonnements.component.html',
  styleUrls: ['./customer-abonnements.component.css']
})
export class CustomerAbonnementsComponent implements OnInit, OnDestroy {
  @Input() customerId: string | null = null;
  
  listofAbonnements: Abonnement[] = [];
  abonnementDetails: Abonnement | null = null;
  cancellationRequestingId: string | null = null;
  cancellationReason: string = '';
  showCancellationForm: boolean = false;
  selectedAbonnementForCancellation: Abonnement | null = null;
  private destroy$ = new Subject<void>();

  constructor(private abonnementService: AbonnementService) {}

  ngOnInit(): void {
    if (this.customerId) {
      this.loadAbonnements();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAbonnements(): void {
    if (!this.customerId) return;
    
    this.abonnementService.getAbonnementsByCustomer(this.customerId)
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.listofAbonnements = data;
      });
  }

  detail(id: any): void {
    this.abonnementService.getAbonnementDetails(String(id))
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        if (String(data.clientId) === String(this.customerId)) {
          this.abonnementDetails = data;
        }
      });
  }

  requestCancellation(abonnement: Abonnement): void {
    this.selectedAbonnementForCancellation = abonnement;
    this.showCancellationForm = true;
  }

  submitCancellationRequest(): void {
    if (!this.selectedAbonnementForCancellation || !this.cancellationReason.trim()) {
      return;
    }
    this.cancellationRequestingId = String(this.selectedAbonnementForCancellation.id);
    this.abonnementService.requestCancellation(String(this.selectedAbonnementForCancellation.id))
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        const index = this.listofAbonnements.findIndex(a => a.id === data.id);
        if (index >= 0) {
          this.listofAbonnements = this.listofAbonnements.map((abonnement, idx) => 
            idx === index ? { ...abonnement, ...data } : abonnement
          );
        }
        if (this.abonnementDetails && this.abonnementDetails.id === data.id) {
          this.abonnementDetails = data;
        }
        this.closeCancellationForm();
        this.cancellationRequestingId = null;
      });
  }

  closeCancellationForm(): void {
    this.showCancellationForm = false;
    this.cancellationReason = '';
    this.selectedAbonnementForCancellation = null;
  }
}
