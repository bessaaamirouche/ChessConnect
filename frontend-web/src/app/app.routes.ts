import { Routes } from '@angular/router';
import { authGuard, guestGuard, studentGuard, teacherGuard } from './core/guards/auth.guard';
import { premiumGuard } from './core/guards/premium.guard';
import { libraryGuard } from './core/guards/library.guard';

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
    path: 'mint',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/admin-login/admin-login.component').then(m => m.AdminLoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'verify-email',
    loadComponent: () => import('./features/auth/verify-email/verify-email.component').then(m => m.VerifyEmailComponent)
  },
  // Public coach pages (no authentication required)
  {
    path: 'coaches',
    loadComponent: () => import('./features/teachers/public-teacher-list/public-teacher-list.component').then(m => m.PublicTeacherListComponent)
  },
  {
    path: 'coaches/:uuid',
    loadComponent: () => import('./features/teachers/public-teacher-profile/public-teacher-profile.component').then(m => m.PublicTeacherProfileComponent)
  },
  // User routes with sidebar layout
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/user-layout/user-layout.component').then(m => m.UserLayoutComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'lessons',
        loadComponent: () => import('./features/lessons/lesson-list/lesson-list.component').then(m => m.LessonListComponent)
      },
      {
        path: 'progress',
        redirectTo: 'programme',
        pathMatch: 'full'
      },
      {
        path: 'quiz',
        canActivate: [studentGuard],
        loadComponent: () => import('./features/quiz/quiz.component').then(m => m.QuizComponent)
      },
      {
        path: 'subscription',
        canActivate: [studentGuard],
        loadComponent: () => import('./features/subscription/subscription.component').then(m => m.SubscriptionComponent)
      },
      {
        path: 'availability',
        canActivate: [teacherGuard],
        loadComponent: () => import('./features/availability/availability-management.component').then(m => m.AvailabilityManagementComponent)
      },
      {
        path: 'programme',
        loadComponent: () => import('./features/programme/programme.component').then(m => m.ProgrammeComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/settings/settings.component').then(m => m.SettingsComponent)
      },
      {
        path: 'invoices',
        loadComponent: () => import('./features/invoices/invoices.component').then(m => m.InvoicesComponent)
      },
      {
        path: 'wallet',
        canActivate: [studentGuard],
        loadComponent: () => import('./features/wallet/wallet.component').then(m => m.WalletComponent)
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
        path: 'library',
        canActivate: [libraryGuard],
        loadComponent: () => import('./features/library/library.component').then(m => m.LibraryComponent)
      }
    ]
  },
  {
    path: 'book/:teacherId',
    canActivate: [authGuard, studentGuard],
    loadComponent: () => import('./features/lessons/book-lesson/book-lesson.component').then(m => m.BookLessonComponent)
  },
  {
    path: 'exercise/:lessonId',
    canActivate: [premiumGuard],
    loadComponent: () => import('./features/exercise/exercise.component').then(m => m.ExerciseComponent)
  },
  {
    path: 'lessons/payment/success',
    // No auth guard - Stripe redirects here
    loadComponent: () => import('./features/lessons/payment-success/payment-success.component').then(m => m.PaymentSuccessComponent)
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
    path: 'admin',
    canActivate: [authGuard],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes)
  },
  {
    path: 'blog',
    loadComponent: () => import('./features/blog/blog-list/blog-list.component').then(m => m.BlogListComponent)
  },
  {
    path: 'blog/:slug',
    loadComponent: () => import('./features/blog/blog-article/blog-article.component').then(m => m.BlogArticleComponent)
  },
  // Info pages
  {
    path: 'how-it-works',
    loadComponent: () => import('./features/how-it-works/how-it-works.component').then(m => m.HowItWorksComponent)
  },
  {
    path: 'pricing',
    loadComponent: () => import('./features/pricing/pricing.component').then(m => m.PricingComponent)
  },
  {
    path: 'faq',
    loadComponent: () => import('./features/faq/faq.component').then(m => m.FaqComponent)
  },
  // Legal pages
  {
    path: 'terms',
    loadComponent: () => import('./features/legal/terms/terms.component').then(m => m.TermsComponent)
  },
  {
    path: 'privacy',
    loadComponent: () => import('./features/legal/privacy/privacy.component').then(m => m.PrivacyComponent)
  },
  {
    path: 'legal-notice',
    loadComponent: () => import('./features/legal/legal-notice/legal-notice.component').then(m => m.LegalNoticeComponent)
  },
  {
    path: '**',
    loadComponent: () => import('./features/not-found/not-found.component').then(m => m.NotFoundComponent)
  }
];
