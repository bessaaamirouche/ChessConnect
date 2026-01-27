import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class TrackingService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly sessionId: string;

  constructor() {
    this.sessionId = this.getOrCreateSessionId();
    if (isPlatformBrowser(this.platformId)) {
      this.initPageTracking();
    }
  }

  /**
   * Initialize page view tracking on navigation events.
   */
  private initPageTracking(): void {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event) => {
      const navEnd = event as NavigationEnd;
      this.trackPageView(navEnd.urlAfterRedirects);
    });
  }

  /**
   * Track a page view.
   */
  private trackPageView(url: string): void {
    this.http.post('/api/tracking/pageview', {
      pageUrl: url,
      sessionId: this.sessionId
    }).subscribe({
      error: () => {
        // Silently fail - tracking should not affect user experience
      }
    });
  }

  /**
   * Get or create a session ID stored in localStorage.
   */
  private getOrCreateSessionId(): string {
    if (!isPlatformBrowser(this.platformId)) {
      return this.generateUUID(); // SSR: generate temporary ID
    }

    const key = 'tracking_session_id';
    let sessionId = localStorage.getItem(key);

    if (!sessionId) {
      sessionId = this.generateUUID();
      localStorage.setItem(key, sessionId);
    }

    return sessionId;
  }

  /**
   * Generate a UUID v4.
   */
  private generateUUID(): string {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return crypto.randomUUID();
    }

    // Fallback for older browsers
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}
