
export interface Customer {
  customerRef: string;
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  pieceIdentite?: string;   // CIN / Passport number
  adresse: string;
  ville?: string;
  codePostal?: string;
  gouvernorat?: string;
  pays?: string;
  type: string;
  status?: string;
  accountBalance?: number;
  creditLimit?: number;
  hasSim?: boolean;         // Whether customer has an assigned SIM
  createdAt?: string;
}

export interface CustomerDetails extends Customer {
  abonnements?: Abonnement[];
  bills?: Bill[];
}

// Matches AbonnementDto (subscription-service)
export interface Abonnement {
  id: number;
  clientId: number;
  clientRef?: string;
  offreId: number;
  dateDebut?: string;
  dateFin?: string;
  status?: string;
  createdAt?: string;
}

// Matches ServiceDto (catalog-service)
// Backend returns: id, code, libelle, unite, prixUnitaire, category, active
export interface Service {
  id: number;
  code: string;
  libelle: string;
  unite: string;
  prixUnitaire: number;
  category?: string;
  active: boolean;
  // Aliases used by templates
  nom?: string;
  description?: string;
  typeService?: string;
  uniteFacturation?: string;
  included_quantity?: number;
}

// Matches OffreDto (catalog-service)
// Backend returns: id, code, libelle, description, prixMensuel, dateDebut, dateFin, status, serviceIds
export interface Offer {
  id: number;
  code: string;
  libelle: string;
  description?: string;
  prixMensuel: number;
  dateDebut?: string;
  dateFin?: string;
  status?: string;
  paymentType?: string;
  serviceIds?: number[];
  nom?: string;
  prixBase?: number;
  active?: boolean;
}

// Matches FactureDto (billing-service)
export interface Bill {
  id: number;
  numeroFacture: string;
  clientId: number;
  abonnementId?: number;
  dateFacture: string;
  dateEcheance: string;
  periodeDebut?: string;
  periodeFin?: string;
  montantHT: number;
  montantTVA: number;
  montantTTC: number;
  statut: string;
  nombreLignes?: number;
  createdAt?: string;
  paidAt?: string;
}

export interface BillDetails extends Bill {
  customer?: Customer;
  lines?: InvoiceLine[];
}

// Matches InvoiceLine entity (billing-service)
// Backend returns: id, type, description, serviceId, usageId, quantite, prixUnitaire, montant
export interface InvoiceLine {
  id: number;
  factureId?: number;
  type?: string;
  description: string;
  serviceId?: number;
  usageId?: number;
  quantite: number;
  prixUnitaire: number;
  montant: number;
  montantHT?: number;
}

// Matches PaymentDto (payment-service)
export interface Payment {
  id: number;
  reference: string;
  clientId: number;
  factureId: number;
  montant: number;
  methodePaiement: string;
  statut: string;
  transactionId?: string;
  errorMessage?: string;
  createdAt?: string;
  processedAt?: string;
}

// Matches UsageRecordDto (usage-service)
export interface UsageRecord {
  id: number;
  abonnementId: number;
  serviceId: number;
  quantite: number;
  prixUnitaire: number;
  montantTotal: number;
  dateUsage: string;
  rated: boolean;
}
