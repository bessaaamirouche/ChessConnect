import { Injectable, inject, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { interval, Subscription, filter, switchMap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PresenceService implements OnDestroy {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private heartbeatSubscription?: Subscription;
  private readonly HEARTBEAT_INTERVAL = 30000; // 30 seconds

  constructor() {
    // Start heartbeat when service is created if user is logged in
    this.startHeartbeat();
  }

  startHeartbeat(): void {
    // Stop any existing heartbeat
    this.stopHeartbeat();

    // Start new heartbeat that only fires when user is authenticated
    this.heartbeatSubscription = interval(this.HEARTBEAT_INTERVAL).pipe(
      filter(() => this.authService.isAuthenticated()),
      switchMap(() => this.sendHeartbeat())
    ).subscribe();

    // Also send an immediate heartbeat if authenticated
    if (this.authService.isAuthenticated()) {
      this.sendHeartbeat().subscribe();
    }
  }

  stopHeartbeat(): void {
    if (this.heartbeatSubscription) {
      this.heartbeatSubscription.unsubscribe();
      this.heartbeatSubscription = undefined;
    }
  }

  private sendHeartbeat() {
    return this.http.post<void>('/api/users/me/heartbeat', {});
  }

  ngOnDestroy(): void {
    this.stopHeartbeat();
  }
}
