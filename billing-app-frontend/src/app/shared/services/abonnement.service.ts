import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Abonnement } from '../../core/models';

@Injectable({
  providedIn: 'root'
})
export class AbonnementService {
  private baseUrl = '/api/subscriptions';

  constructor(private http: HttpClient) {}

  getAbonnements(): Observable<Abonnement[]> {
    return this.http.get<Abonnement[]>(this.baseUrl);
  }

  getActiveAbonnements(): Observable<Abonnement[]> {
    return this.http.get<Abonnement[]>(`${this.baseUrl}/active`);
  }

  getAbonnementsByCustomer(clientId: string): Observable<Abonnement[]> {
    return this.http.get<Abonnement[]>(`${this.baseUrl}/client/${clientId}`);
  }

  getAbonnementDetails(abonnementId: string): Observable<Abonnement> {
    return this.http.get<Abonnement>(`${this.baseUrl}/${abonnementId}`);
  }

  createAbonnement(abonnement: any): Observable<Abonnement> {
    return this.http.post<Abonnement>(this.baseUrl, abonnement);
  }

  activateAbonnement(abonnementId: string): Observable<Abonnement> {
    return this.http.put<Abonnement>(`${this.baseUrl}/${abonnementId}/activate`, null);
  }

  suspendAbonnement(abonnementId: string): Observable<Abonnement> {
    return this.http.put<Abonnement>(`${this.baseUrl}/${abonnementId}/suspend`, null);
  }

  terminateAbonnement(abonnementId: string): Observable<Abonnement> {
    return this.http.put<Abonnement>(`${this.baseUrl}/${abonnementId}/terminate`, null);
  }

  requestCancellation(abonnementId: string): Observable<Abonnement> {
    return this.http.put<Abonnement>(`${this.baseUrl}/${abonnementId}/terminate`, null);
  }
  getAbonnementsByCustomerRef(customerRef: string): Observable<Abonnement[]> {
    return this.http.get<Abonnement[]>(`${this.baseUrl}/client/ref/${customerRef}`);
  }
}
