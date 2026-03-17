import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BoutiqueApiService, Boutique } from '../../core/services/boutique-api.service';
import { UserService, UserDto } from '../../core/services/user.service';
import { TUNISIA_CITIES, getCodePostalByVille, getGouvernoratByVille, getAllVilles } from '../../shared/constants/tunisia-cities';

@Component({
  selector: 'app-create-boutique',
  templateUrl: './create-boutique.component.html',
  styleUrls: ['./create-boutique.component.css']
})
export class CreateBoutiqueComponent implements OnInit, OnDestroy {
  // Id
  code = '';
  nom = '';

  // Localisation
  adresse = '';
  ville = '';
  codePostal = '';
  gouvernorat = '';
  tunisiaVilles: string[] = [];

  // Contact
  telephone = '';
  email = '';

  // Responsable
  responsableId: number | null = null;
  responsables: UserDto[] = [];

  // State
  saving = false;
  errorMessage = '';
  successMessage = '';
  formErrors: { [key: string]: string } = {};

  private destroy$ = new Subject<void>();

  constructor(
    private boutiqueApi: BoutiqueApiService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.tunisiaVilles = getAllVilles();
    this.loadResponsables();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadResponsables(): void {
    this.userService.getUsersByRole('RESPONSABLE_BOUTIQUE')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (users) => {
          // Filter to only show active responsables who don't already have a boutique
          this.responsables = users.filter(u => u.status === 'ACTIVE' && !u.boutiqueId);
        },
        error: () => {
          // Fallback mock data - filter out those with boutiqueId
          this.responsables = [
            { id: 7, username: 'a.mejri', firstName: 'Ahmed', lastName: 'Mejri', role: 'RESPONSABLE_BOUTIQUE', status: 'ACTIVE', boutiqueId: null, lastLoginAt: null, createdAt: '2025-05-01' },
          ];
        }
      });
  }

  onVilleChange(): void {
    if (this.ville) {
      this.codePostal = getCodePostalByVille(this.ville);
      this.gouvernorat = getGouvernoratByVille(this.ville);
      this.generateCode();
    }
  }

  generateCode(): void {
    if (!this.ville) return;
    const prefix = this.ville.substring(0, 3).toUpperCase().replace(/\s/g, '');
    const num = Math.floor(100 + Math.random() * 900);
    this.code = `BQ-${prefix}-${num}`;
  }

  get formValid(): boolean {
    this.formErrors = {};
    
    if (!this.code || !this.nom || !this.adresse || !this.ville || !this.telephone) {
      return false;
    }

    // Validate phone (8 digits)
    if (!/^\d{8}$/.test(this.telephone.replace(/\s/g, ''))) {
      this.formErrors['telephone'] = 'Le numéro de téléphone doit contenir exactement 8 chiffres';
      return false;
    }

    // Validate email if provided
    if (this.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)) {
      this.formErrors['email'] = 'Format d\'email invalide';
      return false;
    }

    return true;
  }

  submitForm(): void {
    if (!this.formValid || this.saving) return;

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const boutique: Partial<Boutique> = {
      code: this.code,
      nom: this.nom,
      adresse: this.adresse,
      ville: this.ville,
      codePostal: this.codePostal,
      telephone: this.telephone,
      email: this.email,
      responsableId: this.responsableId
    };

    this.boutiqueApi.create(boutique)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.successMessage = 'Boutique créée avec succès !';
          this.saving = false;
          setTimeout(() => this.router.navigate(['/Boutiques']), 1500);
        },
        error: (err) => {
          this.saving = false;
          this.errorMessage = err.error?.message || 'Erreur lors de la création de la boutique.';
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/Boutiques']);
  }
}
