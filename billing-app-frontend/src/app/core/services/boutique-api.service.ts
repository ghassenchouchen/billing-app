import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Boutique {
  id: number;
  code: string;
  nom: string;
  adresse: string;
  ville: string;
  codePostal: string;
  telephone: string;
  email: string;
  responsableId: number | null;
  status: string;
  createdAt: string;
}

export interface StockSim {
  id: number;
  iccid: string;
  imsi: string;
  msisdn: string;
  simType: string;
  status: string;
  boutiqueId: number;
  assignedToClientId: number | null;
  assignedAt: string | null;
  createdAt: string;
}

export interface TransactionBoutique {
  id: number;
  reference: string;
  boutiqueId: number;
  agentId: number | null;
  clientId: number | null;
  clientNom: string;
  offreLibelle: string;
  typeTransaction: string;
  montant: number;
  status: string;
  createdAt: string;
}

export interface DashboardData {
  revenueToday: number;
  contractsThisMonth: number;
  contractTarget: number;
  simAvailable: number;
  simLowStock: number;
  simByType: { [key: string]: number };
}

@Injectable({
  providedIn: 'root'
})
export class BoutiqueApiService {

  private baseUrl = '/api/boutiques';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Boutique[]> {
    return this.http.get<Boutique[]>(this.baseUrl);
  }

  getById(id: number): Observable<Boutique> {
    return this.http.get<Boutique>(`${this.baseUrl}/${id}`);
  }

  create(req: Partial<Boutique>): Observable<Boutique> {
    return this.http.post<Boutique>(this.baseUrl, req);
  }

  getDashboard(boutiqueId: number): Observable<DashboardData> {
    return this.http.get<DashboardData>(`${this.baseUrl}/${boutiqueId}/dashboard`);
  }

  getStock(boutiqueId: number): Observable<StockSim[]> {
    return this.http.get<StockSim[]>(`${this.baseUrl}/${boutiqueId}/stock`);
  }

  getAvailableStock(boutiqueId: number): Observable<StockSim[]> {
    return this.http.get<StockSim[]>(`${this.baseUrl}/${boutiqueId}/stock/available`);
  }

  addSimBatch(boutiqueId: number, sims: { iccid: string; imsi: string; msisdn: string; simType: string }[]): Observable<StockSim[]> {
    return this.http.post<StockSim[]>(`${this.baseUrl}/${boutiqueId}/stock/batch`, sims);
  }

  assignSim(iccid: string, clientId: number): Observable<StockSim> {
    return this.http.post<StockSim>(`${this.baseUrl}/stock/${iccid}/assign?clientId=${clientId}`, {});
  }

  assignAndActivateSim(iccid: string, clientId: number): Observable<StockSim> {
    return this.http.post<StockSim>(`${this.baseUrl}/stock/${iccid}/activate?clientId=${clientId}`, {});
  }

  suspendSim(iccid: string): Observable<StockSim> {
    return this.http.post<StockSim>(`${this.baseUrl}/stock/${iccid}/suspend`, {});
  }

  reactivateSim(iccid: string): Observable<StockSim> {
    return this.http.post<StockSim>(`${this.baseUrl}/stock/${iccid}/reactivate`, {});
  }

  getTransactions(boutiqueId: number): Observable<TransactionBoutique[]> {
    return this.http.get<TransactionBoutique[]>(`${this.baseUrl}/${boutiqueId}/transactions`);
  }

  getTodayTransactions(boutiqueId: number): Observable<TransactionBoutique[]> {
    return this.http.get<TransactionBoutique[]>(`${this.baseUrl}/${boutiqueId}/transactions/today`);
  }
}
