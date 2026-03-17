import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
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
import { AuthGuard } from './core/guards/auth.guard';
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

const routes: Routes = [
  { path: '', redirectTo: 'Login', pathMatch: 'full' },
  { path: 'Customers', component: CustomersComponent, canActivate: [AuthGuard] },
  { path: 'Customers/new', component: CreateCustomerComponent, canActivate: [AuthGuard] },
  { path: 'Customers/:ref', component: CustomerDetailComponent, canActivate: [AuthGuard] },
  { path: 'Abonnements', component: AbonnementsComponent, canActivate: [AuthGuard] },
  { path: 'Abonnements/new', component: CreateSubscriptionComponent, canActivate: [AuthGuard] },
  { path: 'Abonnements/new/:customerRef', component: CreateSubscriptionComponent, canActivate: [AuthGuard] },
  { path: 'Abonnements/:id', component: SubscriptionDetailComponent, canActivate: [AuthGuard] },
  { path: 'Services', component: ServicesComponent, canActivate: [AuthGuard] },
  { path: 'Offers', component: OffersComponent, canActivate: [AuthGuard] },
  { path: 'Bills', component: BillsComponent, canActivate: [AuthGuard] },
  { path: 'Boutique/dashboard', component: BoutiqueDashboardComponent, canActivate: [AuthGuard] },
  { path: 'Boutique/stock/activate/:iccid', component: SimActivateComponent, canActivate: [AuthGuard] },
  { path: 'Boutique/stock/add', component: AddStockComponent, canActivate: [AuthGuard] },
  { path: 'Boutique/stock', component: BoutiqueStockComponent, canActivate: [AuthGuard] },
  { path: 'Boutique/transactions', component: BoutiqueTransactionsComponent, canActivate: [AuthGuard] },
  { path: 'Boutique/team', component: BoutiqueTeamComponent, canActivate: [AuthGuard] },
  { path: 'Boutiques', component: AdminBoutiquesComponent, canActivate: [AuthGuard] },
  { path: 'Boutiques/new', component: CreateBoutiqueComponent, canActivate: [AuthGuard] },
  { path: 'Boutiques/:id', component: AdminBoutiqueDetailComponent, canActivate: [AuthGuard] },
  { path: 'Users', component: UserListComponent, canActivate: [AuthGuard] },
  { path: 'Users/new', component: CreateUserComponentAdmin, canActivate: [AuthGuard] },
  { path: 'Login', component: LoginComponent },
  { path: '**', redirectTo: 'Login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
