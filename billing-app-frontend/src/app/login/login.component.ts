import { Component, OnInit, OnDestroy } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit, OnDestroy {
  errorMessage: string = '';
  returnUrl: string = '';
  isLoading: boolean = false;
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/Boutiques';

    // Redirect if already logged in
    if (this.authService.isLoggedIn()) {
      this.router.navigate([this.returnUrl]);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit(form: NgForm): void {
    if (!form || form.invalid) {
      if (form) form.form.markAllAsTouched();
      this.errorMessage = 'Veuillez saisir votre nom d\'utilisateur et mot de passe.';
      return;
    }

    this.errorMessage = '';
    this.isLoading = true;

    const username = (form.value.username || '').trim();
    const password = form.value.password || '';

    this.authService.login(username, password)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          if (response.success) {
            if (this.returnUrl && this.returnUrl !== '/Boutiques') {
              this.router.navigate([this.returnUrl]);
            } else {
              // Role-based default landing page
              const role = response.role;
              if (role === 'ADMIN') {
                this.router.navigate(['/Boutiques']);
              } else
              if (role === 'RESPONSABLE_BOUTIQUE') {
                this.router.navigate(['/Boutique/dashboard']);
              } else {
                this.router.navigate(['/Customers']);
              }
            }
          } else {
            this.errorMessage = response.message || 'Identifiants incorrects.';
          }
        },
        error: (error) => {
          this.isLoading = false;
          if (error.status === 401) {
            this.errorMessage = error.error?.message || 'Identifiants incorrects.';
          } else if (error.status === 423) {
            this.errorMessage = 'Compte verrouillé. Réessayez dans quelques minutes.';
          } else {
            this.errorMessage = 'Erreur de connexion. Veuillez réessayer.';
          }
        }
      });
  }
}
