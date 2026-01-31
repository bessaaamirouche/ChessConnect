import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Authentication is now handled via HttpOnly cookies set by the server.
  // The browser automatically includes cookies with requests, so no need
  // to manually add Authorization headers. This is more secure as tokens
  // are not accessible to JavaScript (prevents XSS token theft).

  // Ensure credentials (cookies) are included with requests
  const authReq = req.clone({
    withCredentials: true
  });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Don't logout/redirect for auth endpoints (login, register, etc.)
      const isAuthEndpoint = req.url.includes('/api/auth/');
      if (error.status === 401 && !isAuthEndpoint) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
