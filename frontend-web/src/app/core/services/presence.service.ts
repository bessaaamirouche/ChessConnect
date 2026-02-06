import { Injectable, inject, OnDestroy, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { interval, Subscription, filter, switchMap, catchError, EMPTY } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PresenceService implements OnDestroy {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private platformId = inject(PLATFORM_ID);
  private heartbeatSubscription?: Subscription;
  private readonly HEARTBEAT_INTERVAL = 30000; // 30 seconds

  constructor() {
    // Don't auto-start - let app.component control when to start
    // This prevents heartbeat during SSR and before proper auth
  }

  startHeartbeat(): void {
    // Only run in browser
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    // Stop any existing heartbeat
    this.stopHeartbeat();

    // Start new heartbeat that only fires when user is authenticated
    this.heartbeatSubscription = interval(this.HEARTBEAT_INTERVAL).pipe(
      filter(() => this.authService.isAuthenticated()),
      switchMap(() => this.sendHeartbeat())
    ).subscribe();

    // Also send an immediate heartbeat if authenticated (with small delay)
    if (this.authService.isAuthenticated()) {
      setTimeout(() => {
        this.sendHeartbeat().subscribe();
      }, 1000);
    }
  }

  stopHeartbeat(): void {
    if (this.heartbeatSubscription) {
      this.heartbeatSubscription.unsubscribe();
      this.heartbeatSubscription = undefined;
    }
  }

  private sendHeartbeat() {
    return this.http.post<void>('/api/users/me/heartbeat', {}).pipe(
      catchError(() => {
        // Silently ignore heartbeat errors (user might not be fully authenticated yet)
        return EMPTY;
      })
    );
  }

  ngOnDestroy(): void {
    this.stopHeartbeat();
  }
}
