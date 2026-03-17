import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { BoutiqueApiService, StockSim } from '../../core/services/boutique-api.service';

interface StockItem {
  iccid: string;
  imsi: string;
  msisdn: string;
  simType: string;
}

interface ProductType {
  key: string;
  label: string;
  icon: string;
  description: string;
  enabled: boolean;
}

@Component({
  selector: 'app-add-stock',
  templateUrl: './add-stock.component.html',
  styleUrls: ['./add-stock.component.css']
})
export class AddStockComponent implements OnInit, OnDestroy {
  private static readonly ICCID_MIN = 18;
  private static readonly ICCID_MAX = 20;
  private static readonly IMSI_MAX = 20;
  private static readonly MSISDN_LOCAL_DIGITS = 8;
  private static readonly MSISDN_REGEX = /^\+216\d{8}$/;

  boutiqueId = 1;

  productTypes: ProductType[] = [
    { key: 'SIM', label: 'Cartes SIM', icon: 'sim_card', description: 'SIM physiques et eSIM', enabled: true },
    { key: 'DEVICE', label: 'Terminaux', icon: 'smartphone', description: 'Téléphones et tablettes', enabled: false },
    { key: 'ACCESSORY', label: 'Accessoires', icon: 'headset', description: 'Coques, chargeurs, etc.', enabled: false },
  ];
  selectedProductType = 'SIM';

  newSimData: StockItem = { iccid: '', imsi: '', msisdn: '', simType: 'STANDARD' };
  batch: StockItem[] = [];
  batchQuantity: number = 25;
  saving = false;
  saveSuccess = false;
  saveError = '';

  totalStock = 0;
  availableStock = 0;
  stockByType: { [key: string]: number } = {};

  loadingStats = true;
  private destroy$ = new Subject<void>();

  constructor(
    private boutiqueApi: BoutiqueApiService,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    const storedId = this.authService.getBoutiqueId();
    this.boutiqueId = storedId ? parseInt(storedId, 10) : 1;
    this.loadStockStats();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadStockStats(): void {
    this.loadingStats = true;
    this.boutiqueApi.getStock(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stock) => {
          this.totalStock = stock.length;
          this.availableStock = stock.filter(s => s.status === 'AVAILABLE').length;
          this.stockByType = {};
          stock.forEach(s => {
            const key = s.simType || 'OTHER';
            this.stockByType[key] = (this.stockByType[key] || 0) + 1;
          });
          this.loadingStats = false;
        },
        error: () => {
          this.loadingStats = false;
        }
      });
  }

  selectProductType(key: string): void {
    const pt = this.productTypes.find(p => p.key === key);
    if (pt && pt.enabled) {
      this.selectedProductType = key;
      this.batch = [];
    }
  }

  generateSimData(): void {
    const generated = this.generateStockItem(this.newSimData.simType);
    this.newSimData.iccid = generated.iccid;
    this.newSimData.imsi = generated.imsi;
    this.newSimData.msisdn = generated.msisdn;
  }

  get canAddToList(): boolean {
    return this.validateItem(this.newSimData).length === 0;
  }

  get isDuplicate(): boolean {
    return this.batch.some(s => s.iccid === this.newSimData.iccid);
  }

  addToList(): void {
    const normalizedMsisdn = this.normalizeMsisdn(this.newSimData.msisdn);
    this.newSimData.msisdn = normalizedMsisdn;

    const errors = this.validateItem(this.newSimData);
    if (errors.length > 0) {
      this.saveError = errors.join(' ');
      return;
    }

    if (this.isDuplicate) {
      this.saveError = 'ICCID deja present dans le lot.';
      return;
    }

    this.batch.push({ ...this.newSimData, msisdn: normalizedMsisdn });
    this.newSimData = { iccid: '', imsi: '', msisdn: '', simType: this.newSimData.simType };
    this.saveSuccess = false;
    this.saveError = '';
  }

  removeFromList(index: number): void {
    this.batch.splice(index, 1);
  }

  clearBatch(): void {
    this.batch = [];
  }

  generateBatch(count: number): void {
    const simType = this.newSimData.simType;
    for (let i = 0; i < count; i++) {
      const generated = this.generateStockItem(simType);

      while (this.batch.some(s => s.iccid === generated.iccid)) {
        Object.assign(generated, this.generateStockItem(simType));
      }

      this.batch.push(generated);
    }
    this.saveError = '';
  }

  save(): void {
    if (this.batch.length === 0 || this.saving) return;

    const invalidItems = this.batch
      .map((item, index) => {
        item.msisdn = this.normalizeMsisdn(item.msisdn);
        return { index, errors: this.validateItem(item) };
      })
      .filter(x => x.errors.length > 0);

    if (invalidItems.length > 0) {
      const first = invalidItems[0];
      this.saveError = `Element ${first.index + 1} invalide: ${first.errors.join(' ')}`;
      return;
    }

    this.saving = true;
    this.saveSuccess = false;
    this.saveError = '';

    this.boutiqueApi.addSimBatch(this.boutiqueId, this.batch)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saving = false;
          this.saveSuccess = true;
          this.batch = [];
          this.loadStockStats();
        },
        error: (err) => {
          this.saving = false;
          this.saveError = err?.error?.error || err?.error?.message || 'Erreur lors de l\'enregistrement du stock.';
        }
      });
  }

  private generateStockItem(simType: string): StockItem {
    const ts = Date.now().toString();
    const rand = Math.floor(Math.random() * 900000000) + 100000000;

    // Backend constraint is 18..20 chars for ICCID.
    const iccid = ('892160' + ts.slice(-8) + rand.toString().slice(0, 6)).slice(0, AddStockComponent.ICCID_MAX);

    return {
      iccid,
      imsi: ('60501' + rand.toString().slice(0, 10)).slice(0, AddStockComponent.IMSI_MAX),
      msisdn: '+216' + rand.toString().slice(0, AddStockComponent.MSISDN_LOCAL_DIGITS),
      simType
    };
  }

  private validateItem(item: StockItem): string[] {
    const errors: string[] = [];
    const iccid = (item.iccid || '').trim();
    const simType = (item.simType || '').trim();
    const msisdn = this.normalizeMsisdn(item.msisdn);
    const imsi = (item.imsi || '').trim();

    if (!iccid) {
      errors.push("ICCID obligatoire.");
    } else if (iccid.length < AddStockComponent.ICCID_MIN || iccid.length > AddStockComponent.ICCID_MAX) {
      errors.push(`ICCID doit contenir entre ${AddStockComponent.ICCID_MIN} et ${AddStockComponent.ICCID_MAX} caracteres.`);
    }

    if (!simType || (simType !== 'STANDARD' && simType !== 'ESIM')) {
      errors.push('Type SIM invalide (STANDARD ou ESIM).');
    }

    if (!msisdn) {
      errors.push('MSISDN obligatoire.');
    } else if (!AddStockComponent.MSISDN_REGEX.test(msisdn)) {
      errors.push('MSISDN invalide. Format attendu: +216 suivi de 8 chiffres.');
    }

    if (imsi && imsi.length > AddStockComponent.IMSI_MAX) {
      errors.push('IMSI trop long (max 20).');
    }

    return errors;
  }

  private normalizeMsisdn(raw: string): string {
    const trimmed = (raw || '').trim();
    if (!trimmed) {
      return '';
    }

    if (trimmed.startsWith('+216')) {
      return '+216' + trimmed.slice(4).replace(/\D/g, '').slice(0, AddStockComponent.MSISDN_LOCAL_DIGITS);
    }

    const digitsOnly = trimmed.replace(/\D/g, '').slice(0, AddStockComponent.MSISDN_LOCAL_DIGITS);
    return '+216' + digitsOnly;
  }

  goBack(): void {
    this.router.navigate(['/Boutique/stock']);
  }

  formatIccid(iccid: string): string {
    if (iccid.length > 12) {
      return iccid.slice(0, 6) + '…' + iccid.slice(-6);
    }
    return iccid;
  }
}
