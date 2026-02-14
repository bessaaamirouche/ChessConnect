import { Routes } from '@angular/router';
import { adminGuard } from '../../core/guards/admin.guard';

export const adminRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('../auth/admin-login/admin-login.component')
      .then(m => m.AdminLoginComponent)
  },
  {
    path: '',
    canActivate: [adminGuard],
    loadComponent: () => import('./admin-layout/admin-layout.component')
      .then(m => m.AdminLayoutComponent),
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/admin-dashboard.component')
          .then(m => m.AdminDashboardComponent)
      },
      {
        path: 'users',
        loadComponent: () => import('./users/user-list.component')
          .then(m => m.UserListComponent)
      },
      {
        path: 'lessons',
        loadComponent: () => import('./lessons/admin-lessons.component')
          .then(m => m.AdminLessonsComponent)
      },
      {
        path: 'accounting',
        loadComponent: () => import('./accounting/accounting.component')
          .then(m => m.AccountingComponent)
      },
      {
        path: 'invoices',
        loadComponent: () => import('./invoices/admin-invoices.component')
          .then(m => m.AdminInvoicesComponent)
      },
      {
        path: 'blog',
        loadComponent: () => import('./articles/admin-articles.component')
          .then(m => m.AdminArticlesComponent)
      },
      {
        path: 'stripe-connect',
        loadComponent: () => import('./stripe-connect/stripe-connect.component')
          .then(m => m.StripeConnectComponent)
      },
      {
        path: 'messages',
        loadComponent: () => import('./messages/admin-messages.component')
          .then(m => m.AdminMessagesComponent)
      },
      {
        path: 'library',
        loadComponent: () => import('./library/admin-library.component')
          .then(m => m.AdminLibraryComponent)
      },
      {
        path: 'promo-codes',
        loadComponent: () => import('./promo-codes/admin-promo-codes.component')
          .then(m => m.AdminPromoCodesComponent)
      }
    ]
  }
];
