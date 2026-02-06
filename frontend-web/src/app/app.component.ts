import { Component, inject, OnInit, OnDestroy, ChangeDetectionStrategy, effect } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './shared/toast/toast.component';
import { GlobalDialogComponent } from './shared/global-dialog/global-dialog.component';
import { CookieConsentComponent } from './shared/cookie-consent/cookie-consent.component';
import { AuthService } from './core/services/auth.service';
import { SseService } from './core/services/sse.service';
import { InactivityService } from './core/services/inactivity.service';
import { PresenceService } from './core/services/presence.service';
import { TrackingService } from './core/services/tracking.service';
import { PushNotificationService } from './core/services/push-notification.service';
import { LanguageService } from './core/services/language.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent, GlobalDialogComponent, CookieConsentComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main id="main-content">
      <router-outlet></router-outlet>
    </main>
    <app-toast></app-toast>
    <app-global-dialog></app-global-dialog>
    <app-cookie-consent></app-cookie-consent>
  `,
  styles: []
})
export class AppComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private sseService = inject(SseService);
  private inactivityService = inject(InactivityService);
  private presenceService = inject(PresenceService);
  private trackingService = inject(TrackingService); // Initialize page tracking
  private pushNotificationService = inject(PushNotificationService);
  private languageService = inject(LanguageService); // Initialize i18n translations

  constructor() {
    // Use effect to react to auth state changes
    // Connect/disconnect SSE based on user role
    effect(() => {
      const user = this.authService.currentUser();
      const isStudent = this.authService.isStudent();
      const isTeacher = this.authService.isTeacher();
      const isAuthenticated = this.authService.isAuthenticated();

      if (user && isStudent) {
        this.sseService.connect('student');
      } else if (user && isTeacher) {
        this.sseService.connect('teacher');
      } else {
        this.sseService.disconnect();
      }

      // Start/stop inactivity tracking and presence based on auth state
      if (isAuthenticated) {
        this.inactivityService.startWatching();
        this.presenceService.startHeartbeat();
        // Initialize push notifications
        this.pushNotificationService.init();
      } else {
        this.inactivityService.stopWatching();
        this.presenceService.stopHeartbeat();
      }
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    // Initial setup is handled by the effect
  }

  ngOnDestroy(): void {
    this.sseService.disconnect();
    this.inactivityService.stopWatching();
    this.presenceService.stopHeartbeat();
  }
}
