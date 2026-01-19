import { Routes } from '@angular/router';
import { adminGuard } from '../../core/guards/admin.guard';

export const adminRoutes: Routes = [
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
      }
    ]
  }
];
