import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { PaymentService } from '../services/payment.service';
import { map, catchError, of, forkJoin } from 'rxjs';

export const premiumGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const paymentService = inject(PaymentService);
  const router = inject(Router);

  // First check if user is authenticated
  if (!authService.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  // Check if user is a student (only students can have premium)
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
        queryParams: { required: 'exercise' }
      });
      return false;
    }),
    catchError(() => {
      router.navigate(['/subscription'], {
        queryParams: { required: 'exercise' }
      });
      return of(false);
    })
  );
};
