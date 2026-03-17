import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

export type UserRole = 'ADMIN' | 'RESPONSABLE_BOUTIQUE' | 'AGENT_COMMERCIAL';

export interface LoginResponse {
  success: boolean;
  token: string | null;
  refreshToken: string | null;
  role: UserRole | null;
  username: string | null;
  firstName: string | null;
  lastName: string | null;
  boutiqueId: number | null;
  message: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly USERNAME_KEY = 'username';
  private readonly ROLE_KEY = 'user_role';
  private readonly FIRST_NAME_KEY = 'first_name';
  private readonly LAST_NAME_KEY = 'last_name';
  private readonly BOUTIQUE_ID_KEY = 'boutique_id';

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  private userRoleSubject = new BehaviorSubject<UserRole | null>(this.getStoredRole());

  isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  userRole$ = this.userRoleSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', {
      username,
      password
    }).pipe(
      tap(response => {
        if (response.success) {
          localStorage.setItem(this.TOKEN_KEY, response.token || '');
          localStorage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken || '');
          localStorage.setItem(this.USERNAME_KEY, response.username || '');
          localStorage.setItem(this.ROLE_KEY, response.role || '');
          localStorage.setItem(this.FIRST_NAME_KEY, response.firstName || '');
          localStorage.setItem(this.LAST_NAME_KEY, response.lastName || '');
          if (response.boutiqueId) {
            localStorage.setItem(this.BOUTIQUE_ID_KEY, response.boutiqueId.toString());
          }
          this.isAuthenticatedSubject.next(true);
          this.userRoleSubject.next(response.role);
        }
      })
    );
  }

  /**
   * Refresh the access token using the stored refresh token.
   * Called automatically by the AuthInterceptor on 401 responses.
   */
  refreshAccessToken(): Observable<LoginResponse> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<LoginResponse>('/api/auth/refresh', {}, {
      headers: { Authorization: `Bearer ${refreshToken}` }
    }).pipe(
      tap(response => {
        if (response.success) {
          localStorage.setItem(this.TOKEN_KEY, response.token || '');
          localStorage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken || '');
        }
      })
    );
  }

  logout(): void {
    const token = this.getToken();
    if (token) {
      this.http.post('/api/auth/logout', {}, {
        headers: { Authorization: `Bearer ${token}` }
      }).subscribe({ error: () => {} });
    }

    this.clearStorage();
    this.isAuthenticatedSubject.next(false);
    this.userRoleSubject.next(null);
    this.router.navigate(['/Login']);
  }

  isLoggedIn(): boolean {
    return this.hasToken();
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  getUsername(): string | null {
    return localStorage.getItem(this.USERNAME_KEY);
  }

  getFullName(): string {
    const first = localStorage.getItem(this.FIRST_NAME_KEY) || '';
    const last = localStorage.getItem(this.LAST_NAME_KEY) || '';
    return `${first} ${last}`.trim() || this.getUsername() || '';
  }

  getUserRole(): UserRole | null {
    return localStorage.getItem(this.ROLE_KEY) as UserRole | null;
  }

  getBoutiqueId(): string | null {
    return localStorage.getItem(this.BOUTIQUE_ID_KEY);
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  isResponsableBoutique(): boolean {
    return this.getUserRole() === 'RESPONSABLE_BOUTIQUE';
  }

  isAgentCommercial(): boolean {
    return this.getUserRole() === 'AGENT_COMMERCIAL';
  }

  /** Check if user has at least the given role level */
  hasMinRole(minRole: UserRole): boolean {
    const hierarchy: Record<UserRole, number> = {
      'AGENT_COMMERCIAL': 1,
      'RESPONSABLE_BOUTIQUE': 2,
      'ADMIN': 3
    };
    const current = this.getUserRole();
    if (!current) return false;
    return hierarchy[current] >= hierarchy[minRole];
  }

  getRoleLabel(): string {
    const labels: Record<UserRole, string> = {
      'ADMIN': 'Administrateur',
      'RESPONSABLE_BOUTIQUE': 'Responsable Boutique',
      'AGENT_COMMERCIAL': 'Agent Commercial'
    };
    const role = this.getUserRole();
    return role ? labels[role] : '';
  }

  private getStoredRole(): UserRole | null {
    return localStorage.getItem(this.ROLE_KEY) as UserRole | null;
  }

  private clearStorage(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USERNAME_KEY);
    localStorage.removeItem(this.ROLE_KEY);
    localStorage.removeItem(this.FIRST_NAME_KEY);
    localStorage.removeItem(this.LAST_NAME_KEY);
    localStorage.removeItem(this.BOUTIQUE_ID_KEY);
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }
}
