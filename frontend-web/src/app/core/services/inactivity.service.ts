import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class InactivityService implements OnDestroy {
  private readonly INACTIVITY_TIMEOUT = 15 * 60 * 1000; // 15 minutes
  private readonly WARNING_BEFORE = 60 * 1000; // 1 minute before logout

  private timeoutId: any;
  private warningTimeoutId: any;
  private isWarningShown = false;
  private boundResetTimer: () => void;

  constructor(
    private authService: AuthService,
    private router: Router,
    private ngZone: NgZone
  ) {
    this.boundResetTimer = this.resetTimer.bind(this);
  }

  startWatching(): void {
    if (!this.authService.isAuthenticated()) {
      return;
    }

    // Listen for user activity
    const events = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart', 'click'];

    this.ngZone.runOutsideAngular(() => {
      events.forEach(event => {
        document.addEventListener(event, this.boundResetTimer, { passive: true });
      });
    });

    this.resetTimer();
  }

  stopWatching(): void {
    const events = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart', 'click'];
    events.forEach(event => {
      document.removeEventListener(event, this.boundResetTimer);
    });

    this.clearTimers();
  }

  private resetTimer(): void {
    if (!this.authService.isAuthenticated()) {
      this.stopWatching();
      return;
    }

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

    this.ngZone.run(() => {
      const stay = confirm('Vous allez etre deconnecte dans 1 minute pour inactivite. Voulez-vous rester connecte ?');
      if (stay) {
        this.resetTimer();
      }
    });
  }

  private logoutDueToInactivity(): void {
    this.stopWatching();
    this.authService.logout();
    alert('Vous avez ete deconnecte pour inactivite.');
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
