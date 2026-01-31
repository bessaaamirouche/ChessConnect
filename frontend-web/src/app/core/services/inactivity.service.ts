import { Injectable, NgZone, OnDestroy, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AuthService } from './auth.service';
import { DialogService } from './dialog.service';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class InactivityService implements OnDestroy {
  private readonly INACTIVITY_TIMEOUT = 60 * 60 * 1000; // 1 hour
  private readonly WARNING_BEFORE = 5 * 60 * 1000; // 5 minutes before logout
  private readonly LAST_ACTIVITY_KEY = 'chess_last_activity';

  private timeoutId: any;
  private warningTimeoutId: any;
  private isWarningShown = false;
  private isWatching = false;
  private boundResetTimer: () => void;
  private dialogService = inject(DialogService);
  private platformId = inject(PLATFORM_ID);

  constructor(
    private authService: AuthService,
    private router: Router,
    private ngZone: NgZone
  ) {
    this.boundResetTimer = this.resetTimer.bind(this);
  }

  startWatching(): void {
    if (!isPlatformBrowser(this.platformId) || !this.authService.isAuthenticated()) {
      return;
    }

    // Prevent multiple watchers
    if (this.isWatching) {
      return;
    }

    // Check if user was inactive while away (tab closed, browser closed)
    if (this.checkStoredActivity()) {
      // User was inactive for too long - logout immediately
      return;
    }

    this.isWatching = true;

    // Listen for user activity
    const events = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart', 'click'];

    this.ngZone.runOutsideAngular(() => {
      events.forEach(event => {
        document.addEventListener(event, this.boundResetTimer, { passive: true });
      });
    });

    this.resetTimer();
  }

  /**
   * Check if user was inactive while the app was closed
   * Returns true if user should be logged out
   */
  private checkStoredActivity(): boolean {
    if (!isPlatformBrowser(this.platformId)) return false;

    const lastActivity = localStorage.getItem(this.LAST_ACTIVITY_KEY);
    if (lastActivity) {
      const lastActivityTime = parseInt(lastActivity, 10);
      const timeSinceLastActivity = Date.now() - lastActivityTime;

      if (timeSinceLastActivity >= this.INACTIVITY_TIMEOUT) {
        // User was inactive for too long - logout
        this.ngZone.run(() => {
          this.logoutDueToInactivity();
        });
        return true;
      }
    }
    return false;
  }

  /**
   * Store the last activity timestamp
   */
  private storeLastActivity(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.LAST_ACTIVITY_KEY, Date.now().toString());
    }
  }

  /**
   * Clear stored activity timestamp
   */
  private clearStoredActivity(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.LAST_ACTIVITY_KEY);
    }
  }

  stopWatching(): void {
    this.isWatching = false;

    if (isPlatformBrowser(this.platformId)) {
      const events = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart', 'click'];
      events.forEach(event => {
        document.removeEventListener(event, this.boundResetTimer);
      });
    }

    this.clearTimers();
  }

  private resetTimer(): void {
    if (!this.authService.isAuthenticated()) {
      this.stopWatching();
      return;
    }

    // Store the current activity time
    this.storeLastActivity();

    this.clearTimers();
    this.isWarningShown = false;

    // Set warning timeout
    this.warningTimeoutId = setTimeout(() => {
      this.showWarning();
    }, this.INACTIVITY_TIMEOUT - this.WARNING_BEFORE);

    // Set logout timeout
    this.timeoutId = setTimeout(() => {
      this.ngZone.run(() => {
        this.logoutDueToInactivity();
      });
    }, this.INACTIVITY_TIMEOUT);
  }

  private showWarning(): void {
    if (this.isWarningShown) return;
    this.isWarningShown = true;

    this.ngZone.run(async () => {
      const stay = await this.dialogService.confirm(
        'Vous allez être déconnecté dans 5 minutes pour inactivité. Voulez-vous rester connecté ?',
        'Session inactive',
        { confirmText: 'Rester connecté', cancelText: 'Se déconnecter', variant: 'warning' }
      );
      if (stay) {
        this.resetTimer();
      } else {
        this.logoutDueToInactivity();
      }
    });
  }

  private logoutDueToInactivity(): void {
    this.stopWatching();
    this.clearStoredActivity();
    this.authService.logout();
    this.dialogService.alert(
      'Vous avez été déconnecté pour inactivité (plus d\'une heure sans activité).',
      'Déconnexion',
      { variant: 'warning' }
    );
  }

  private clearTimers(): void {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
    if (this.warningTimeoutId) {
      clearTimeout(this.warningTimeoutId);
      this.warningTimeoutId = null;
    }
  }

  ngOnDestroy(): void {
    this.stopWatching();
  }
}
