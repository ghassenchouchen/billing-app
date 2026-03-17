import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService, UserRole } from './auth.service';

export interface UserDto {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  status: 'ACTIVE' | 'DISABLED';
  boutiqueId: number | null;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  boutiqueId?: number | null;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  password?: string;
  role?: UserRole;
  boutiqueId?: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private baseUrl = '/api/users';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getHeaders(): HttpHeaders {
    const role = this.authService.getUserRole();
    if (!role) {
      throw new Error('User is not authenticated or not authorized to access this resource');
    }
    return new HttpHeaders({
      'X-Auth-Role': role
    });
  }

  getAllUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(this.baseUrl, { headers: this.getHeaders() });
  }

  getUserById(id: number): Observable<UserDto> {
    return this.http.get<UserDto>(`${this.baseUrl}/${id}`, { headers: this.getHeaders() });
  }

  getUsersByRole(role: string): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(`${this.baseUrl}/role/${role}`, { headers: this.getHeaders() });
  }

  getUsersByBoutique(boutiqueId: number): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(`${this.baseUrl}/boutique/${boutiqueId}`, { headers: this.getHeaders() });
  }

  getTeamByBoutique(boutiqueId: number): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(`${this.baseUrl}/boutique/${boutiqueId}/team`, { headers: this.getHeaders() });
  }

  createUser(request: CreateUserRequest): Observable<UserDto> {
    return this.http.post<UserDto>(this.baseUrl, request, { headers: this.getHeaders() });
  }

  updateUser(id: number, request: UpdateUserRequest): Observable<UserDto> {
    return this.http.put<UserDto>(`${this.baseUrl}/${id}`, request, { headers: this.getHeaders() });
  }

  disableUser(id: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/disable`, {}, { headers: this.getHeaders() });
  }

  enableUser(id: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/enable`, {}, { headers: this.getHeaders() });
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { headers: this.getHeaders() });
  }
}
