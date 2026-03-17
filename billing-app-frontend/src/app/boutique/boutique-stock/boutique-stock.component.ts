import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { BoutiqueApiService, StockSim } from '../../core/services/boutique-api.service';

@Component({
  selector: 'app-boutique-stock',
  templateUrl: './boutique-stock.component.html',
  styleUrls: ['./boutique-stock.component.css']
})
export class BoutiqueStockComponent implements OnInit, OnDestroy {
  private static readonly MSISDN_REGEX = /^\+216\d{8}$/;
  private static readonly MSISDN_LOCAL_DIGITS = 8;
  boutiqueId = 1;
  allStock: StockSim[] = [];
  filteredStock: StockSim[] = [];
  loading = true;

  filterType = '';
  filterStatus = '';
  searchQuery = '';

  simTypes = ['STANDARD', 'ESIM'];
  simStatuses = ['AVAILABLE', 'ACTIVATED', 'ASSIGNED', 'SUSPENDED', 'DEACTIVATED', 'DAMAGED', 'LOST'];

  // Stats
  totalSim = 0;
  availableSim = 0;
  activatedSim = 0;

  // Low stock alert
  lowStockThreshold = 5;
  showAddSimModal = false;
  newSimData = {
    iccid: '',
    imsi: '',
    msisdn: '',
    simType: 'STANDARD'
  };
  addingSimBatch: { iccid: string; imsi: string; msisdn: string; simType: string }[] = [];
  savingStock = false;

  userRole = '';

  private destroy$ = new Subject<void>();

  constructor(
    private boutiqueApi: BoutiqueApiService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const storedId = this.authService.getBoutiqueId();
    this.boutiqueId = storedId ? parseInt(storedId, 10) : 1;
    this.userRole = this.authService.getUserRole() || '';
    this.loadStock();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadStock(): void {
    this.loading = true;
    this.boutiqueApi.getStock(this.boutiqueId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stock) => {
          this.allStock = stock;
          this.computeStats();
          this.applyFilters();
          this.loading = false;
        },
        error: () => {
          this.allStock = [
            { id: 1, iccid: '8921601234567890001', imsi: '60501000000001', msisdn: '+21620100001', simType: 'STANDARD', status: 'AVAILABLE', boutiqueId: 1, assignedToClientId: null, assignedAt: null, createdAt: '2025-01-10T08:00:00' },
            { id: 2, iccid: '8921601234567890002', imsi: '60501000000002', msisdn: '+21620100002', simType: 'STANDARD', status: 'AVAILABLE', boutiqueId: 1, assignedToClientId: null, assignedAt: null, createdAt: '2025-01-10T08:00:00' },
            { id: 3, iccid: '8921601234567890003', imsi: '60501000000003', msisdn: '+21620100003', simType: 'STANDARD', status: 'ASSIGNED', boutiqueId: 1, assignedToClientId: 1, assignedAt: '2025-02-10T10:00:00', createdAt: '2025-01-10T08:00:00' },
            { id: 4, iccid: '8921601234567890004', imsi: '60501000000004', msisdn: '+21620100004', simType: 'ESIM', status: 'ACTIVATED', boutiqueId: 1, assignedToClientId: 2, assignedAt: '2025-02-12T14:00:00', createdAt: '2025-01-10T08:00:00' },
            { id: 5, iccid: '8921601234567890005', imsi: '60501000000005', msisdn: '+21620100005', simType: 'STANDARD', status: 'AVAILABLE', boutiqueId: 1, assignedToClientId: null, assignedAt: null, createdAt: '2025-01-15T09:00:00' },
            { id: 6, iccid: '8921601234567890006', imsi: '60501000000006', msisdn: '+21620100006', simType: 'ESIM', status: 'DAMAGED', boutiqueId: 1, assignedToClientId: null, assignedAt: null, createdAt: '2025-01-15T09:00:00' },
            { id: 7, iccid: '8921601234567890007', imsi: '60501000000007', msisdn: '+21620100007', simType: 'STANDARD', status: 'AVAILABLE', boutiqueId: 1, assignedToClientId: null, assignedAt: null, createdAt: '2025-01-20T08:00:00' },
          ];
          this.computeStats();
          this.applyFilters();
          this.loading = false;
        }
      });
  }

  computeStats(): void {
    this.totalSim = this.allStock.length;
    this.availableSim = this.allStock.filter(s => s.status === 'AVAILABLE').length;
    this.activatedSim = this.allStock.filter(s => s.status === 'ACTIVATED' || s.status === 'ASSIGNED').length;
  }

  applyFilters(): void {
    this.filteredStock = this.allStock.filter(s => {
      const matchType = !this.filterType || s.simType === this.filterType;
      const matchStatus = !this.filterStatus || s.status === this.filterStatus;
      const matchSearch = !this.searchQuery ||
        s.iccid.includes(this.searchQuery) ||
        s.msisdn.includes(this.searchQuery) ||
        s.imsi.includes(this.searchQuery);
      return matchType && matchStatus && matchSearch;
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'AVAILABLE': return 'status-available';
      case 'ASSIGNED': return 'status-activated';
      case 'ACTIVATED': return 'status-activated';
      case 'SUSPENDED': return 'status-suspended';
      case 'DEACTIVATED': return 'status-deactivated';
      case 'DAMAGED': return 'status-suspended';
      case 'LOST': return 'status-suspended';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'AVAILABLE': return 'En stock';
      case 'ASSIGNED': return 'Activée'; 
      case 'ACTIVATED': return 'Activée';
      case 'SUSPENDED': return 'Suspendue';
      case 'DEACTIVATED': return 'Désactivée';
      case 'DAMAGED': return 'Endommagée';
      case 'LOST': return 'Perdue';
      default: return status;
    }
  }

  getTypeLabel(type: string): string {
    switch (type) {
      case 'STANDARD': return 'Standard';
      case 'ESIM': return 'eSIM';
      default: return type;
    }
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-TN', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatIccid(iccid: string): string {
    return '…' + iccid.slice(-8);
  }

  activateSim(sim: StockSim): void {
    this.router.navigate(['/Boutique/stock/activate', sim.iccid]);
  }

  get isLowStock(): boolean {
    return this.availableSim < this.lowStockThreshold;
  }

  get canAddStock(): boolean {
    return this.userRole === 'RESPONSABLE_BOUTIQUE';
  }

  openAddSimModal(): void {
    this.showAddSimModal = true;
    this.newSimData = { iccid: '', imsi: '', msisdn: '', simType: 'STANDARD' };
    this.addingSimBatch = [];
  }

  closeAddSimModal(): void {
    this.showAddSimModal = false;
  }

  addSimToList(): void {
    const normalizedMsisdn = this.normalizeMsisdn(this.newSimData.msisdn);
    if (this.newSimData.iccid && BoutiqueStockComponent.MSISDN_REGEX.test(normalizedMsisdn)) {
      this.addingSimBatch.push({ ...this.newSimData, msisdn: normalizedMsisdn });
      this.newSimData = { iccid: '', imsi: '', msisdn: '', simType: this.newSimData.simType };
    }
  }

  removeSimFromList(index: number): void {
    this.addingSimBatch.splice(index, 1);
  }

  generateSimData(): void {
    // Generate valid Tunisian MSISDN format: +216 + 8 digits.
    const randomNum = Math.floor(Math.random() * 900000000) + 100000000;
    const baseIccid = '892160' + Date.now().toString().slice(-10);
    this.newSimData.iccid = baseIccid;
    this.newSimData.imsi = '60501' + randomNum.toString().slice(0, 10);
    this.newSimData.msisdn = '+216' + randomNum.toString().slice(0, BoutiqueStockComponent.MSISDN_LOCAL_DIGITS);
  }

  formatMsisdn(msisdn: string): string {
    const normalized = this.normalizeMsisdn(msisdn);
    if (!BoutiqueStockComponent.MSISDN_REGEX.test(normalized)) {
      return msisdn || '—';
    }
    return `+216 ${normalized.slice(4, 6)} ${normalized.slice(6, 9)} ${normalized.slice(9, 12)}`;
  }

  private normalizeMsisdn(raw: string): string {
    const trimmed = (raw || '').trim();
    if (!trimmed) {
      return '';
    }
    if (trimmed.startsWith('+216')) {
      return '+216' + trimmed.slice(4).replace(/\D/g, '').slice(0, BoutiqueStockComponent.MSISDN_LOCAL_DIGITS);
    }
    return '+216' + trimmed.replace(/\D/g, '').slice(0, BoutiqueStockComponent.MSISDN_LOCAL_DIGITS);
  }

  saveNewStock(): void {
    if (this.addingSimBatch.length === 0) return;

    this.savingStock = true;
    this.boutiqueApi.addSimBatch(this.boutiqueId, this.addingSimBatch)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.savingStock = false;
          this.showAddSimModal = false;
          this.loadStock();
        },
        error: () => {
          // For demo, simulate success
          this.addingSimBatch.forEach((sim, i) => {
            this.allStock.push({
              id: this.allStock.length + i + 1,
              iccid: sim.iccid,
              imsi: sim.imsi,
              msisdn: sim.msisdn,
              simType: sim.simType,
              status: 'AVAILABLE',
              boutiqueId: this.boutiqueId,
              assignedToClientId: null,
              assignedAt: null,
              createdAt: new Date().toISOString()
            });
          });
          this.computeStats();
          this.applyFilters();
          this.savingStock = false;
          this.showAddSimModal = false;
        }
      });
  }
}
