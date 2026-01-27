import { Component, inject, OnInit, OnDestroy, ChangeDetectionStrategy, effect } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './shared/toast/toast.component';
import { GlobalDialogComponent } from './shared/global-dialog/global-dialog.component';
import { AuthService } from './core/services/auth.service';
import { NotificationService } from './core/services/notification.service';
import { InactivityService } from './core/services/inactivity.service';
import { PresenceService } from './core/services/presence.service';
import { TrackingService } from './core/services/tracking.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent, GlobalDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <router-outlet></router-outlet>
    <app-toast></app-toast>
    <app-global-dialog></app-global-dialog>
  `,
  styles: []
})
export class AppComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private inactivityService = inject(InactivityService);
  private presenceService = inject(PresenceService);
  private trackingService = inject(TrackingService); // Initialize page tracking

  constructor() {
    // Use effect to react to auth state changes (replaces 2s polling)
    effect(() => {
      const user = this.authService.currentUser();
      const isStudent = this.authService.isStudent();
      const isTeacher = this.authService.isTeacher();
      const isAuthenticated = this.authService.isAuthenticated();

      if (user && (isStudent || isTeacher)) {
        this.notificationService.startPolling();
      } else {
        this.notificationService.stopPolling();
      }

      // Start/stop inactivity tracking and presence based on auth state
      if (isAuthenticated) {
        this.inactivityService.startWatching();
        this.presenceService.startHeartbeat();
      } else {
        this.inactivityService.stopWatching();
        this.presenceService.stopHeartbeat();
      }
    });
  }

  ngOnInit(): void {
    // Initial setup is handled by the effect
  }

  ngOnDestroy(): void {
    this.notificationService.stopPolling();
    this.inactivityService.stopWatching();
    this.presenceService.stopHeartbeat();
  }
}
