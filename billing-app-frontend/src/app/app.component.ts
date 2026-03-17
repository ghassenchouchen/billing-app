import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { filter, takeUntil } from 'rxjs/operators';
import { AuthService, UserRole } from './core/services/auth.service';
import { SearchSection, SearchService } from './core/services/search.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  isLoginRoute = false;
  searchTerm = '';
  searchOpen = false;
  searchSections: SearchSection[] = [];
  userRole: UserRole | null = null;
  userFullName = '';
  userRoleLabel = '';
  private search$ = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(private router: Router, private authService: AuthService, private searchService: SearchService) {}

  ngOnInit(): void {
    this.authService.userRole$.pipe(takeUntil(this.destroy$)).subscribe(role => {
      this.userRole = role;
      this.userFullName = this.authService.getFullName();
      this.userRoleLabel = this.authService.getRoleLabel();
    });
    
    this.evaluateRoute(this.router.url);
    this.router.events
      .pipe(
        takeUntil(this.destroy$),
        filter((event): event is NavigationEnd => event instanceof NavigationEnd)
      )
      .subscribe((event) => {
        this.evaluateRoute(event.urlAfterRedirects);
        this.closeSearch();
      });

    this.search$
      .pipe(takeUntil(this.destroy$), debounceTime(200), distinctUntilChanged())
      .subscribe((term: string) => {
        this.searchService.search(term).subscribe((sections) => {
          this.searchSections = sections;
          this.searchOpen = !!term;
        });
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  logout(): void {
    this.authService.logout();
  }

  onSearchChange(value: string): void {
    this.searchTerm = value;
    this.search$.next(value);
  }

  selectResult(route: string): void {
    this.router.navigate([route]);
    this.closeSearch();
  }

  closeSearch(): void {
    this.searchOpen = false;
  }

  private evaluateRoute(currentUrl: string): void {
    this.isLoginRoute = currentUrl.toLowerCase().includes('/login');
  }
}

