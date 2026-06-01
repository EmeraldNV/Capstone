import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guard';
import { AdminShellComponent } from './layout/admin-shell/admin-shell.component';
import { SiteShellComponent } from './layout/site-shell/site-shell.component';

export const routes: Routes = [
  {
    path: 'admin',
    canActivate: [roleGuard(['ADMIN'])],
    component: AdminShellComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/admin/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./pages/admin/admin-users-page.component').then((m) => m.AdminUsersPageComponent),
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./pages/admin/admin-products-page.component').then((m) => m.AdminProductsPageComponent),
      },
    ],
  },
  {
    path: '',
    component: SiteShellComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/home/home.component').then((m) => m.HomeComponent),
      },
      {
        path: 'auth/login',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('./pages/auth/login-page.component').then((m) => m.LoginPageComponent),
      },
      {
        path: 'auth/register',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('./pages/auth/register-page.component').then((m) => m.RegisterPageComponent),
      },
      {
        path: 'auth/verify-email',
        loadComponent: () =>
          import('./pages/auth/verify-email-page.component').then((m) => m.VerifyEmailPageComponent),
      },
      {
        path: 'account/profile',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./pages/account/profile-page.component').then((m) => m.ProfilePageComponent),
      },
      {
        path: 'shop',
        pathMatch: 'full',
        loadComponent: () =>
          import('./pages/shop/shop-page.component').then((m) => m.ShopPageComponent),
      },
      {
        path: 'shop/:slug',
        loadComponent: () =>
          import('./pages/shop/product-detail-page.component').then((m) => m.ProductDetailPageComponent),
      },
      {
        path: 'cart',
        loadComponent: () =>
          import('./pages/cart/cart-page.component').then((m) => m.CartPageComponent),
      },
      {
        path: 'checkout/stripe/:result',
        loadComponent: () =>
          import('./pages/checkout/stripe-checkout-result-page.component').then((m) => m.StripeCheckoutResultPageComponent),
      },
      {
        path: '**',
        redirectTo: '',
      },
    ],
  },
];
