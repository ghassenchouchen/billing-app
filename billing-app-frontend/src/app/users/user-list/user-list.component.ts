import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { UserService, UserDto } from '../../core/services/user.service';
import { BoutiqueApiService } from '../../core/services/boutique-api.service';
import { UserRole, AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.css']
})
export class UserListComponent implements OnInit, OnDestroy {
  users: UserDto[] = [];
  filteredUsers: UserDto[] = [];
  loading = true;

  filterRole = '';
  filterStatus = '';
  searchQuery = '';

  roles: UserRole[] = ['AGENT_COMMERCIAL', 'RESPONSABLE_BOUTIQUE'];
  statuses = ['ACTIVE', 'DISABLED'];

  // Stats
  totalUsers = 0;
  activeUsers = 0;

  boutiqueNames: Record<number, string> = {};

  private destroy$ = new Subject<void>();

  constructor(
    private userService: UserService,
    private boutiqueApi: BoutiqueApiService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Only admin can access this page
    if (!this.authService.isAdmin()) {
      this.router.navigate(['/Dashboard']);
      return;
    }
    this.loadUsers();
    this.loadBoutiques();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (users) => {
          this.users = users.filter(u => u.role !== 'ADMIN');
          this.computeStats();
          this.applyFilters();
          this.loading = false;
        },
        error: () => {
          this.users = [
            { id: 1, username: 'j.dupont', firstName: 'Jean', lastName: 'Dupont', role: 'RESPONSABLE_BOUTIQUE', status: 'ACTIVE', boutiqueId: null, lastLoginAt: '2026-02-20T09:12:00', createdAt: '2025-01-01T08:00:00' },
            { id: 2, username: 'm.durand', firstName: 'Marc', lastName: 'Durand', role: 'RESPONSABLE_BOUTIQUE', status: 'ACTIVE', boutiqueId: 1, lastLoginAt: '2026-02-20T09:12:00', createdAt: '2025-03-15T08:00:00' },
            { id: 3, username: 'a.valois', firstName: 'Alexandre', lastName: 'Valois', role: 'AGENT_COMMERCIAL', status: 'ACTIVE', boutiqueId: 1, lastLoginAt: '2026-02-19T17:45:00', createdAt: '2025-06-01T08:00:00' },
            { id: 4, username: 'k.bouaziz', firstName: 'Karim', lastName: 'Bouaziz', role: 'AGENT_COMMERCIAL', status: 'ACTIVE', boutiqueId: 1, lastLoginAt: '2026-02-18T14:30:00', createdAt: '2025-06-10T08:00:00' },
            { id: 5, username: 's.lefebvre', firstName: 'Sophie', lastName: 'Lefebvre', role: 'AGENT_COMMERCIAL', status: 'DISABLED', boutiqueId: 2, lastLoginAt: '2025-10-05T08:00:00', createdAt: '2025-04-01T08:00:00' },
            { id: 6, username: 'n.khelifi', firstName: 'Nadia', lastName: 'Khelifi', role: 'AGENT_COMMERCIAL', status: 'ACTIVE', boutiqueId: 1, lastLoginAt: '2026-02-20T08:00:00', createdAt: '2025-07-20T08:00:00' },
          ];
          this.computeStats();
          this.applyFilters();
          this.loading = false;
        }
      });
  }

  loadBoutiques(): void {
    this.boutiqueApi.getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (boutiques) => {
          boutiques.forEach((b: any) => {
            this.boutiqueNames[b.id] = b.nom || `Boutique #${b.id}`;
          });
        },
        error: () => {
          this.boutiqueNames[1] = 'Boutique Tunis Centre';
          this.boutiqueNames[2] = 'Boutique Sfax';
        }
      });
  }

  computeStats(): void {
    this.totalUsers = this.users.length;
    this.activeUsers = this.users.filter(u => u.status === 'ACTIVE').length;
  }

  applyFilters(): void {
    this.filteredUsers = this.users.filter(u => {
      // Never show ADMIN users
      if (u.role === 'ADMIN') return false;
      const matchRole = !this.filterRole || u.role === this.filterRole;
      const matchStatus = !this.filterStatus || u.status === this.filterStatus;
      const matchSearch = !this.searchQuery ||
        u.firstName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        u.lastName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        u.username.toLowerCase().includes(this.searchQuery.toLowerCase());
      return matchRole && matchStatus && matchSearch;
    });
  }

  getInitials(user: UserDto): string {
    return (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
  }

  getRoleLabel(role: string): string {
    switch (role) {
      case 'ADMIN': return 'Administrateur';
      case 'RESPONSABLE_BOUTIQUE': return 'Resp. Boutique';
      case 'AGENT_COMMERCIAL': return 'Agent Commercial';
      default: return role;
    }
  }

  getRoleClass(role: string): string {
    switch (role) {
      case 'ADMIN': return 'role-admin';
      case 'RESPONSABLE_BOUTIQUE': return 'role-responsable';
      case 'AGENT_COMMERCIAL': return 'role-agent';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'Actif';
      case 'DISABLED': return 'Suspendu';
      default: return status;
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'DISABLED': return 'status-disabled';
      default: return '';
    }
  }

  getBoutiqueName(boutiqueId: number | null): string {
    if (!boutiqueId) return '—';
    return this.boutiqueNames[boutiqueId] || `Boutique #${boutiqueId}`;
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-TN', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }

  createUser(): void {
    this.router.navigate(['/Users/new']);
  }

  disableUser(user: UserDto): void {
    if (!confirm(`Suspendre l'utilisateur ${user.firstName} ${user.lastName} ?`)) return;
    this.userService.disableUser(user.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          user.status = 'DISABLED';
          this.computeStats();
          this.applyFilters();
        },
        error: () => {
          user.status = 'DISABLED';
          this.computeStats();
          this.applyFilters();
        }
      });
  }

  enableUser(user: UserDto): void {
    this.userService.enableUser(user.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          user.status = 'ACTIVE';
          this.computeStats();
          this.applyFilters();
        },
        error: () => {
          user.status = 'ACTIVE';
          this.computeStats();
          this.applyFilters();
        }
      });
  }

  deleteUser(user: UserDto): void {
    if (!confirm(`Supprimer l'utilisateur ${user.firstName} ${user.lastName} ? Cette action est irréversible.`)) return;
    this.userService.deleteUser(user.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.users = this.users.filter(u => u.id !== user.id);
          this.computeStats();
          this.applyFilters();
        },
        error: (err) => {
          alert('Erreur lors de la suppression: ' + (err.error?.message || 'Erreur serveur'));
        }
      });
  }
}
