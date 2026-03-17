import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Offer } from '../../core/models';

@Injectable({
  providedIn: 'root'
})
export class OffersService {
  private baseUrl = '/api/offres';

  constructor(private http: HttpClient) {}

  getOffers(): Observable<Offer[]> {
    return this.http.get<Offer[]>(this.baseUrl);
  }

  getActiveOffers(): Observable<Offer[]> {
    return this.http.get<Offer[]>(`${this.baseUrl}/active`);
  }

  getOfferDetails(offerId: string): Observable<Offer> {
    return this.http.get<Offer>(`${this.baseUrl}/${offerId}`);
  }

  createOffer(request: any): Observable<Offer> {
    return this.http.post<Offer>(this.baseUrl, request);
  }

  updateOffer(offerId: string, request: any): Observable<Offer> {
    return this.http.put<Offer>(`${this.baseUrl}/${offerId}`, request);
  }

  deleteOffer(offerId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${offerId}`);
  }
}
