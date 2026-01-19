import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './shared/toast/toast.component';
import { AuthService } from './core/services/auth.service';
import { NotificationService } from './core/services/notification.service';
import { InactivityService } from './core/services/inactivity.service';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent],
  template: `
    <router-outlet></router-outlet>
    <app-toast></app-toast>
  `,
  styles: []
})
export class AppComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private inactivityService = inject(InactivityService);
  private checkSubscription?: Subscription;

  ngOnInit(): void {
    // Check auth state periodically and start/stop polling
    this.checkAndStartPolling();

    // Re-check every 2 seconds for auth state changes
    this.checkSubscription = interval(2000).subscribe(() => {
      this.checkAndStartPolling();
    });
  }

  private checkAndStartPolling(): void {
    const user = this.authService.currentUser();
    const isStudent = this.authService.isStudent();
    const isTeacher = this.authService.isTeacher();
    const isAuthenticated = this.authService.isAuthenticated();

    if (user && (isStudent || isTeacher)) {
      this.notificationService.startPolling();
    } else {
      this.notificationService.stopPolling();
    }

    // Start/stop inactivity tracking based on auth state
    if (isAuthenticated) {
      this.inactivityService.startWatching();
    } else {
      this.inactivityService.stopWatching();
    }
  }

  ngOnDestroy(): void {
    this.checkSubscription?.unsubscribe();
    this.notificationService.stopPolling();
    this.inactivityService.stopWatching();
  }
}
