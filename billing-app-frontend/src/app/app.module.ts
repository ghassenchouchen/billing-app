import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule }   from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

import { CustomersComponent } from './customers/customers.component';
import { CustomerDetailComponent } from './customers/customer-detail/customer-detail.component';
import { CreateCustomerComponent } from './customers/create-customer/create-customer.component';
import { ServicesComponent } from './services/services.component';
import { AbonnementsComponent } from './abonnements/abonnements.component';
import { CreateSubscriptionComponent } from './abonnements/create-subscription/create-subscription.component';
import { SubscriptionDetailComponent } from './abonnements/subscription-detail/subscription-detail.component';
import { OffersComponent } from './offers/offers.component';
import { BillsComponent } from './bills/bills.component';
import { LoginComponent } from './login/login.component';
import { ConfirmModalComponent } from './shared/components/confirm-modal/confirm-modal.component';
import { BoutiqueDashboardComponent } from './boutique/boutique-dashboard/boutique-dashboard.component';
import { BoutiqueStockComponent } from './boutique/boutique-stock/boutique-stock.component';
import { BoutiqueTransactionsComponent } from './boutique/boutique-transactions/boutique-transactions.component';
import { BoutiqueTeamComponent } from './boutique/boutique-team/boutique-team.component';
import { AdminBoutiquesComponent } from './boutique/admin-boutiques/admin-boutiques.component';
import { SimActivateComponent } from './boutique/boutique-stock/sim-activate/sim-activate.component';
import { UserListComponent } from './users/user-list/user-list.component';
import { CreateUserComponent as CreateUserComponentAdmin } from './users/create-user/create-user.component';
import { CreateBoutiqueComponent } from './boutique/create-boutique/create-boutique.component';
import { AddStockComponent } from './boutique/add-stock/add-stock.component';
import { AdminBoutiqueDetailComponent } from './boutique/admin-boutique-detail/admin-boutique-detail.component';

import { ApiService } from './core/services/api.service';
import { AuthService } from './core/services/auth.service';
import { BoutiqueApiService } from './core/services/boutique-api.service';
import { UserService } from './core/services/user.service';

import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';

import { SHARED_PIPES } from './shared/pipes';

@NgModule({
  declarations: [
    AppComponent,
    CustomersComponent,
    CustomerDetailComponent,
    CreateCustomerComponent,
    ServicesComponent,
    AbonnementsComponent,
    CreateSubscriptionComponent,
    SubscriptionDetailComponent,
    OffersComponent,
    BillsComponent,
    LoginComponent,
    ConfirmModalComponent,
    BoutiqueDashboardComponent,
    BoutiqueStockComponent,
    BoutiqueTransactionsComponent,
    BoutiqueTeamComponent,
    AdminBoutiquesComponent,
    SimActivateComponent,
    UserListComponent,
    CreateUserComponentAdmin,
    CreateBoutiqueComponent,
    AddStockComponent,
    AdminBoutiqueDetailComponent,
    ...SHARED_PIPES,
  ],
  imports: [
    BrowserModule,
    CommonModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule,
    BrowserAnimationsModule
  ],
  providers: [
    ApiService,
    AuthService,
    BoutiqueApiService,
    UserService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    }
  ],                   
  bootstrap: [AppComponent]
})
export class AppModule { }
