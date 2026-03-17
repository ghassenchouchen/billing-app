import { Injectable } from '@angular/core';
import {
  HttpInterceptor, HttpRequest, HttpHandler, HttpEvent,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);

  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Don't attach token to auth endpoints (login, refresh)
    if (this.isAuthRequest(request.url)) {
      return next.handle(request);
    }

    const token = this.authService.getToken();
    if (token) {
      request = this.addToken(request, token);
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !this.isAuthRequest(request.url)) {
          return this.handle401Error(request, next);
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Handle 401 errors by attempting to refresh the access token.
   * Uses a lock (isRefreshing) to prevent multiple simultaneous refresh calls.
   * Queued requests wait for the refresh to complete, then retry with the new token.
   */
  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      const refreshToken = this.authService.getRefreshToken();
      if (!refreshToken) {
        this.isRefreshing = false;
        this.authService.logout();
        return throwError(() => new HttpErrorResponse({ status: 401 }));
      }

      return this.authService.refreshAccessToken().pipe(
        switchMap(response => {
          this.isRefreshing = false;
          if (response.success && response.token) {
            this.refreshTokenSubject.next(response.token);
            return next.handle(this.addToken(request, response.token));
          }
          this.authService.logout();
          return throwError(() => new HttpErrorResponse({ status: 401 }));
        }),
        catchError(err => {
          this.isRefreshing = false;
          this.authService.logout();
          return throwError(() => err);
        })
      );
    }

    // Another refresh is already in progress — wait for it
    return this.refreshTokenSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap(token => next.handle(this.addToken(request, token!)))
    );
  }

  private addToken(request: HttpRequest<any>, token: string): HttpRequest<any> {
    // Clone request and add/update Authorization header while preserving other headers
    return request.clone({
      setHeaders: { 
        Authorization: `Bearer ${token}` 
      }
    });
  }

  private isAuthRequest(url: string): boolean {
    return url.includes('/api/auth/login') ||
           url.includes('/api/auth/refresh') ||
           url.includes('/api/auth/logout');
  }
}
