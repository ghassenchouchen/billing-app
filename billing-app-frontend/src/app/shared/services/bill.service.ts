import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { Bill, BillDetails, InvoiceLine } from '../../core/models';

@Injectable({
  providedIn: 'root'
})
export class BillService {
  private baseUrl = '/api/invoices';

  constructor(private http: HttpClient) {}

  getBills(): Observable<Bill[]> {
    return this.http.get<Bill[]>(this.baseUrl);
  }

  getBillsByCustomer(clientId: string): Observable<Bill[]> {
    return this.http.get<Bill[]>(`${this.baseUrl}/client/${clientId}`);
  }

  getBillDetails(billId: string): Observable<BillDetails> {
    return this.http.get<BillDetails>(`${this.baseUrl}/${billId}`);
  }

  getBillByNumber(numeroFacture: string): Observable<BillDetails> {
    return this.http.get<BillDetails>(`${this.baseUrl}/number/${numeroFacture}`);
  }

  getBillLines(billId: string): Observable<InvoiceLine[]> {
    // Get lines from bill details
    return this.getBillDetails(billId).pipe(
      map(details => details.lines || [])
    );
  }

  getUnpaidBills(clientId: string): Observable<Bill[]> {
    return this.http.get<Bill[]>(`${this.baseUrl}/client/${clientId}/unpaid`);
  }

  getOutstandingBalance(clientId: string): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/client/${clientId}/balance`);
  }

  getBillsByStatus(status: string): Observable<Bill[]> {
    return this.http.get<Bill[]>(`${this.baseUrl}/status/${status}`);
  }

  generateInvoice(abonnementId: number, periodeDebut: string, periodeFin: string): Observable<Bill> {
    return this.http.post<Bill>(`${this.baseUrl}/generate`, {
      abonnementId,
      periodeDebut,
      periodeFin
    });
  }

  finalizeInvoice(billId: string): Observable<Bill> {
    return this.http.post<Bill>(`${this.baseUrl}/${billId}/finalize`, null);
  }

  sendInvoice(billId: string): Observable<Bill> {
    return this.http.post<Bill>(`${this.baseUrl}/${billId}/send`, null);
  }

  markAsPaid(billId: string, paymentRef: string): Observable<Bill> {
    return this.http.post<Bill>(`${this.baseUrl}/${billId}/pay`, null, {
      params: { paymentRef }
    });
  }
}
