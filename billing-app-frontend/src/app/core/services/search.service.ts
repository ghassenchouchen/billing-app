import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable, of } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';

export interface SearchResult {
  label: string;
  sublabel?: string;
  route: string;
}

export interface SearchSection {
  title: string;
  results: SearchResult[];
}

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private clients: any[] | null = null;
  private bills: any[] | null = null;
  private abonnements: any[] | null = null;
  private services: any[] | null = null;
  private offers: any[] | null = null;

  constructor(private http: HttpClient) {}

  search(term: string): Observable<SearchSection[]> {
    const query = (term || '').trim().toLowerCase();
    if (!query) {
      return of([]);
    }

    return this.ensureData().pipe(
      map(() => {
        const sections: SearchSection[] = [];

        const clientResults = (this.clients || [])
          .filter((c) => this.match(query, `${c.nom} ${c.prenom} ${c.email} ${c.adresse} ${c.type}`))
          .map((c) => ({
            label: `${c.prenom} ${c.nom}`.trim(),
            sublabel: c.email,
            route: '/Customers'
          }));
        if (clientResults.length) {
          sections.push({ title: 'Clients', results: clientResults });
        }

        const billResults = (this.bills || [])
          .filter((b) => this.match(query, `facture ${b.numeroFacture} ${b.id} client ${b.clientId} ${b.statut}`))
          .map((b) => ({
            label: `Facture #${b.numeroFacture || b.id}`,
            sublabel: `Client #${b.clientId} · ${b.statut || 'En attente'}`,
            route: '/Bills'
          }));
        if (billResults.length) {
          sections.push({ title: 'Factures', results: billResults });
        }

        const abonnementResults = (this.abonnements || [])
          .filter((c) => this.match(query, `${c.id} abonnement client ${c.clientId} offre ${c.offreId} ${c.status}`))
          .map((c) => ({
            label: `Abonnement #${c.id}`,
            sublabel: `Client #${c.clientId} · ${c.status || 'ACTIVE'}`,
            route: '/Abonnements'
          }));
        if (abonnementResults.length) {
          sections.push({ title: 'Abonnements', results: abonnementResults });
        }

        const serviceResults = (this.services || [])
          .filter((s) => this.match(query, `${s.libelle} ${s.code} ${s.unite} ${s.category}`))
          .map((s) => ({
            label: `${s.libelle || s.code}`,
            sublabel: `${s.unite || ''} · ${s.active ? 'Actif' : 'Inactif'}`,
            route: '/Services'
          }));
        if (serviceResults.length) {
          sections.push({ title: 'Services', results: serviceResults });
        }

        const offerResults = (this.offers || [])
          .filter((o) => this.match(query, `${o.libelle} ${o.code} ${o.description} ${o.status}`))
          .map((o) => ({
            label: `${o.libelle || o.code}`,
            sublabel: `${o.description || ''} · ${o.status || ''}`.trim(),
            route: '/Offers'
          }));
        if (offerResults.length) {
          sections.push({ title: 'Offres', results: offerResults });
        }

        return sections;
      })
    );
  }

  private ensureData(): Observable<void> {
    const requests: Observable<any>[] = [];
    if (!this.clients) {
      requests.push(this.http.get<any[]>('/api/customers').pipe(
        tap((data) => (this.clients = data)),
        catchError(() => { this.clients = []; return of([]); })
      ));
    }
    if (!this.bills) {
      requests.push(this.http.get<any[]>('/api/invoices').pipe(
        tap((data) => (this.bills = data)),
        catchError(() => { this.bills = []; return of([]); })
      ));
    }
    if (!this.abonnements) {
      requests.push(this.http.get<any[]>('/api/subscriptions').pipe(
        tap((data) => (this.abonnements = data)),
        catchError(() => { this.abonnements = []; return of([]); })
      ));
    }
    if (!this.services) {
      requests.push(this.http.get<any[]>('/api/services').pipe(
        tap((data) => (this.services = data)),
        catchError(() => { this.services = []; return of([]); })
      ));
    }
    if (!this.offers) {
      requests.push(this.http.get<any[]>('/api/offres').pipe(
        tap((data) => (this.offers = data)),
        catchError(() => { this.offers = []; return of([]); })
      ));
    }

    if (!requests.length) {
      return of(void 0);
    }

    return forkJoin(requests).pipe(map(() => void 0));
  }

  private match(term: string, haystack: string): boolean {
    return (haystack || '').toLowerCase().includes(term);
  }
}
