import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard for the library page.
 * Allows access if user is an authenticated student.
 * Videos recorded during premium period are kept accessible even after unsubscribing.
 */
export const libraryGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  if (!authService.isStudent()) {
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};
