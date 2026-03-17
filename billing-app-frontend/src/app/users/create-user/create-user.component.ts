import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { UserService, CreateUserRequest } from '../../core/services/user.service';
import { BoutiqueApiService } from '../../core/services/boutique-api.service';
import { UserRole } from '../../core/services/auth.service';

interface BoutiqueOption {
  id: number;
  name: string;
  selected: boolean;
}

@Component({
  selector: 'app-create-user',
  templateUrl: './create-user.component.html',
  styleUrls: ['./create-user.component.css']
})
export class CreateUserComponent implements OnInit, OnDestroy {
  // Form fields - combined Information + Rôles & Accès
  firstName = '';
  lastName = '';
  email = '';
  phone = '';
  role: UserRole = 'AGENT_COMMERCIAL';

  // Boutique assignment
  boutiques: BoutiqueOption[] = [];
  selectedBoutiqueId: number | null = null;

  // State
  saving = false;
  errorMessage = '';
  successMessage = '';

  private destroy$ = new Subject<void>();

  constructor(
    private userService: UserService,
    private boutiqueApi: BoutiqueApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadBoutiques();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadBoutiques(): void {
    this.boutiqueApi.getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (boutiques: any[]) => {
          this.boutiques = boutiques.map(b => ({
            id: b.id,
            name: b.nom || `Boutique #${b.id}`,
            selected: false
          }));
        },
        error: () => {
          // Fallback mock
          this.boutiques = [
            { id: 1, name: 'Boutique Tunis Centre', selected: false },
            { id: 2, name: 'Boutique Sfax Nord', selected: false },
          ];
        }
      });
  }

  get username(): string {
    if (!this.firstName || !this.lastName) return '';
    return `${this.lastName.toLowerCase().replace(/\s+/g, '')}.${this.firstName.toLowerCase().replace(/\s+/g, '')}`;
  }

  get needsBoutique(): boolean {
    return this.role === 'AGENT_COMMERCIAL' || this.role === 'RESPONSABLE_BOUTIQUE';
  }

  get formValid(): boolean {
    if (!this.firstName.trim() || !this.lastName.trim() || !this.role) return false;
    if (!this.email.trim()) return false;
    if (!this.isValidEmail(this.email)) return false;
    if (!this.phone.trim()) return false;
    if (!this.isValidPhone(this.phone)) return false;
    if (this.needsBoutique && !this.selectedBoutiqueId) return false;
    return true;
  }

  isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  isValidPhone(phone: string): boolean {
    const phoneRegex = /^\d{8}$/;
    return phoneRegex.test(phone.replace(/\s+/g, ''));
  }

  private generatePassword(): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*';
    let pwd = '';
    for (let i = 0; i < 14; i++) {
      pwd += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return pwd;
  }

  selectBoutique(boutique: BoutiqueOption): void {
    this.boutiques.forEach(b => b.selected = false);
    boutique.selected = true;
    this.selectedBoutiqueId = boutique.id;
  }

  onPhoneChange(): void {
    // Remove non-digits and limit to 8 characters
    this.phone = this.phone.replace(/\D/g, '').slice(0, 8);
  }

  submitForm(): void {
    if (!this.formValid || this.saving) return;

    if (!this.isValidEmail(this.email)) {
      this.errorMessage = 'Adresse email invalide';
      return;
    }

    if (!this.isValidPhone(this.phone)) {
      this.errorMessage = 'Numéro de téléphone invalide (8 chiffres requis)';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request: CreateUserRequest = {
      username: this.username,
      password: this.generatePassword(),
      firstName: this.firstName,
      lastName: this.lastName,
      role: this.role,
      boutiqueId: this.needsBoutique ? this.selectedBoutiqueId : null
    };

    this.userService.createUser(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.successMessage = 'Utilisateur créé avec succès !';
          this.saving = false;
          setTimeout(() => this.router.navigate(['/Users']), 1500);
        },
        error: (err) => {
          this.saving = false;
          this.errorMessage = err.error?.message || 'Erreur lors de la création de l\'utilisateur.';
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/Users']);
  }
}
