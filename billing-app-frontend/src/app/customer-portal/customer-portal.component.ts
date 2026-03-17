import { Component, OnInit } from '@angular/core';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-customer-portal',
  templateUrl: './customer-portal.component.html',
  styleUrls: ['./customer-portal.component.css']
})
export class CustomerPortalComponent implements OnInit {
  activeTab: string = 'abonnements';
  currentCustomerId: string | null = null;
  userName: string = '';
  accountStatus: string = 'Actif';

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    // Get customer ID from auth service
    this.currentCustomerId = this.authService.getCustomerId();
    // Get customer name - you may need to implement this in AuthService
    this.userName = localStorage.getItem('userName') || 'Client';
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }

  logout(): void {
    this.authService.logout();
  }
}
