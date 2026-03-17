import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Service } from '../../core/models';

@Injectable({
  providedIn: 'root'
})
export class ServicesService {
  private baseUrl = '/api/services';

  constructor(private http: HttpClient) {}

  getServices(): Observable<Service[]> {
    return this.http.get<Service[]>(this.baseUrl);
  }

  getActiveServices(): Observable<Service[]> {
    return this.http.get<Service[]>(`${this.baseUrl}/active`);
  }

  getServiceDetails(serviceId: string): Observable<Service> {
    return this.http.get<Service>(`${this.baseUrl}/${serviceId}`);
  }

  getServiceByCode(code: string): Observable<Service> {
    return this.http.get<Service>(`${this.baseUrl}/code/${code}`);
  }

  createService(request: any): Observable<Service> {
    return this.http.post<Service>(this.baseUrl, request);
  }

  updateService(serviceId: string, request: any): Observable<Service> {
    return this.http.put<Service>(`${this.baseUrl}/${serviceId}`, request);
  }

  deleteService(serviceId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${serviceId}`);
  }
}
