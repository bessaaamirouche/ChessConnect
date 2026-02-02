import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { PaymentService } from '../services/payment.service';
import { map, catchError, of, forkJoin } from 'rxjs';

/**
 * Guard for the library page.
 * Allows access if user is a student with active subscription OR active premium trial.
 */
export const libraryGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const paymentService = inject(PaymentService);
  const router = inject(Router);

  // First check if user is authenticated
  if (!authService.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  // Check if user is a student (only students can access the library)
  if (!authService.isStudent()) {
    router.navigate(['/dashboard']);
    return false;
  }

  // Check for active subscription OR active trial
  return forkJoin([
    paymentService.loadActiveSubscription(),
    paymentService.loadFreeTrialStatus()
  ]).pipe(
    map(() => {
      if (paymentService.hasActiveSubscription()) {
        return true;
      }
      // Redirect to subscription page if not premium
      router.navigate(['/subscription'], {
        queryParams: { required: 'library' }
      });
      return false;
    }),
    catchError(() => {
      router.navigate(['/subscription'], {
        queryParams: { required: 'library' }
      });
      return of(false);
    })
  );
};
