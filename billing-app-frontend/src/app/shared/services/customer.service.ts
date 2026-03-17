import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Customer, CustomerDetails } from '../../core/models';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private baseUrl = '/api/customers';

  constructor(private http: HttpClient) {}

  getCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.baseUrl);
  }

  getCustomersByBoutique(boutiqueRef: string): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.baseUrl, { params: { boutiqueRef } });
  }

  getActiveCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(`${this.baseUrl}/active`);
  }

  getCustomerDetails(customerRef: string): Observable<CustomerDetails> {
    return this.http.get<CustomerDetails>(`${this.baseUrl}/ref/${customerRef}`);
  }

  getCustomerById(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.baseUrl}/${id}`);
  }

  getCustomerByEmail(email: string): Observable<Customer> {
    return this.http.get<Customer>(`${this.baseUrl}/email/${email}`);
  }

  createCustomer(customer: any): Observable<Customer> {
    return this.http.post<Customer>(this.baseUrl, customer);
  }

  updateCustomer(customerRef: string, customer: any): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/ref/${customerRef}`, customer);
  }

  suspendCustomer(customerRef: string, reason?: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/ref/${customerRef}/suspend`, null, {
      params: reason ? { reason } : {}
    });
  }

  reactivateCustomer(customerRef: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/ref/${customerRef}/reactivate`, null);
  }
}
