import { Injectable, signal, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export interface CookieConsent {
  analytics: boolean;
  timestamp: number;
}

const CONSENT_KEY = 'mychess_cookie_consent';
const GA_MEASUREMENT_ID = 'G-ZXWY5QSCTX';

@Injectable({
  providedIn: 'root'
})
export class CookieConsentService {
  private platformId = inject(PLATFORM_ID);
  private consentGiven = signal<CookieConsent | null>(null);
  private bannerVisible = signal(false);

  readonly consent = this.consentGiven.asReadonly();
  readonly showBanner = this.bannerVisible.asReadonly();

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadConsent();
    }
  }

  private loadConsent(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const stored = localStorage.getItem(CONSENT_KEY);
    if (stored) {
      try {
        const consent: CookieConsent = JSON.parse(stored);
        this.consentGiven.set(consent);
        if (consent.analytics) {
          this.loadGoogleAnalytics();
        }
      } catch {
        this.bannerVisible.set(true);
      }
    } else {
      this.bannerVisible.set(true);
    }
  }

  acceptAll(): void {
    const consent: CookieConsent = {
      analytics: true,
      timestamp: Date.now()
    };
    this.saveConsent(consent);
    this.loadGoogleAnalytics();
  }

  rejectAll(): void {
    const consent: CookieConsent = {
      analytics: false,
      timestamp: Date.now()
    };
    this.saveConsent(consent);
    this.removeGoogleAnalytics();
  }

  private saveConsent(consent: CookieConsent): void {
    if (!isPlatformBrowser(this.platformId)) return;

    localStorage.setItem(CONSENT_KEY, JSON.stringify(consent));
    this.consentGiven.set(consent);
    this.bannerVisible.set(false);
  }

  private loadGoogleAnalytics(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    // Check if already loaded
    if (document.querySelector(`script[src*="googletagmanager.com/gtag"]`)) {
      return;
    }

    // Load gtag.js script
    const script = document.createElement('script');
    script.async = true;
    script.src = `https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}`;
    document.head.appendChild(script);

    // Initialize gtag
    script.onload = () => {
      (window as any).dataLayer = (window as any).dataLayer || [];
      function gtag(...args: any[]) {
        (window as any).dataLayer.push(arguments);
      }
      (window as any).gtag = gtag;
      gtag('js', new Date());
      gtag('config', GA_MEASUREMENT_ID);
    };
  }

  private removeGoogleAnalytics(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    // Remove GA cookies
    const cookies = document.cookie.split(';');
    for (const cookie of cookies) {
      const name = cookie.split('=')[0].trim();
      if (name.startsWith('_ga') || name.startsWith('_gid')) {
        document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; domain=.${window.location.hostname}`;
        document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
      }
    }
  }

  // Allow user to change preferences later
  resetConsent(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    localStorage.removeItem(CONSENT_KEY);
    this.consentGiven.set(null);
    this.bannerVisible.set(true);
  }
}
