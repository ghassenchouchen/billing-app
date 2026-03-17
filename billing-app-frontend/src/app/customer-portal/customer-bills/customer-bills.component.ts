import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BillService } from '../../shared/services/bill.service';
import { Bill } from '../../core/models';

@Component({
  selector: 'app-customer-bills',
  templateUrl: './customer-bills.component.html',
  styleUrls: ['./customer-bills.component.css']
})
export class CustomerBillsComponent implements OnInit, OnDestroy {
  @Input() customerId: string | null = null;
  
  listofBills: Bill[] = [];
  billdetails: any = null;
  billLines: any[] = [];
  private destroy$ = new Subject<void>();

  constructor(private billService: BillService) {}

  ngOnInit(): void {
    if (this.customerId) {
      this.loadBills();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadBills(): void {
    if (!this.customerId) return;
    
    this.billService.getBillsByCustomer(this.customerId)
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        // All bills returned are already filtered for this customer by the backend
        this.listofBills = data;
      });
  }

  detail(id: any): void {
    this.billService.getBillDetails(String(id))
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        if (String(data.clientId) === this.customerId) {
          this.billdetails = data;
          this.billLines = data.lines || [];
        }
      });
  }

  loadBillLines(billId: string): void {
    this.billService.getBillLines(billId)
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.billLines = data;
      });
  }

  isPaid(status: string): boolean {
    return status === 'PAYEE';
  }

  downloadInvoice(billId: string): void {
    // For now, just log - in production, this would generate/download a PDF
    // You could use a service like jsPDF or call a backend endpoint to generate PDF
    console.log('Download invoice for bill:', billId);
    // Example: window.open(`/api/invoice/pdf/${billId}`, '_blank');
  }
}
