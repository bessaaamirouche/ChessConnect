import { Routes } from '@angular/router';
import { authGuard, guestGuard, studentGuard, teacherGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'teachers',
    loadComponent: () => import('./features/teachers/teacher-list/teacher-list.component').then(m => m.TeacherListComponent)
  },
  {
    path: 'teachers/:id',
    loadComponent: () => import('./features/teachers/teacher-profile/teacher-profile.component').then(m => m.TeacherProfileComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'lessons',
    canActivate: [authGuard],
    loadComponent: () => import('./features/lessons/lesson-list/lesson-list.component').then(m => m.LessonListComponent)
  },
  {
    path: 'book/:teacherId',
    canActivate: [authGuard, studentGuard],
    loadComponent: () => import('./features/lessons/book-lesson/book-lesson.component').then(m => m.BookLessonComponent)
  },
  {
    path: 'lessons/payment/success',
    // No auth guard - Stripe redirects here
    loadComponent: () => import('./features/lessons/payment-success/payment-success.component').then(m => m.PaymentSuccessComponent)
  },
  {
    path: 'progress',
    canActivate: [authGuard, studentGuard],
    loadComponent: () => import('./features/progress/progress.component').then(m => m.ProgressComponent)
  },
  {
    path: 'quiz',
    canActivate: [authGuard, studentGuard],
    loadComponent: () => import('./features/quiz/quiz.component').then(m => m.QuizComponent)
  },
  {
    path: 'subscription',
    canActivate: [authGuard, studentGuard],
    loadComponent: () => import('./features/subscription/subscription.component').then(m => m.SubscriptionComponent)
  },
  {
    path: 'subscription/success',
    // No auth guard - Stripe redirects here and we need to handle the session
    loadComponent: () => import('./features/subscription/subscription-success.component').then(m => m.SubscriptionSuccessComponent)
  },
  {
    path: 'subscription/cancel',
    loadComponent: () => import('./features/subscription/subscription-cancel.component').then(m => m.SubscriptionCancelComponent)
  },
  {
    path: 'availability',
    canActivate: [authGuard, teacherGuard],
    loadComponent: () => import('./features/availability/availability-management.component').then(m => m.AvailabilityManagementComponent)
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () => import('./features/settings/settings.component').then(m => m.SettingsComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
