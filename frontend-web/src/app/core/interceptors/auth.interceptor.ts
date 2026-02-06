import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformServer } from '@angular/common';
import { Router } from '@angular/router';
import { catchError, throwError, EMPTY } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  // Authentication is now handled via HttpOnly cookies set by the server.
  // The browser automatically includes cookies with requests, so no need
  // to manually add Authorization headers. This is more secure as tokens
  // are not accessible to JavaScript (prevents XSS token theft).

  let modifiedReq = req;

  // In SSR context, convert relative API URLs to absolute URLs
  // The SSR server proxy handles /api routes, but needs absolute URLs for Node.js HTTP
  if (isPlatformServer(platformId) && req.url.startsWith('/api')) {
    // API_URL is http://backend:8282/api, so we strip /api from req.url to avoid duplication
    const apiBaseUrl = process.env['API_URL'] || 'http://backend:8282/api';
    const apiPath = req.url.replace(/^\/api/, ''); // Remove leading /api
    const absoluteUrl = `${apiBaseUrl}${apiPath}`;
    modifiedReq = req.clone({ url: absoluteUrl });
  }

  // Ensure credentials (cookies) are included with requests
  const authReq = modifiedReq.clone({
    withCredentials: true
  });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Silently ignore heartbeat errors (background requests that can fail)
      if (req.url.includes('/heartbeat') || req.url.includes('/notifications/stream')) {
        return EMPTY;
      }

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
