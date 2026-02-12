import { Injectable, signal, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { loadStripe, Stripe } from '@stripe/stripe-js';
import { firstValueFrom } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root'
})
export class StripeService {
  private stripe: Stripe | null = null;
  private publishableKey: string | null = null;
  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);
  private http = inject(HttpClient);
  private translate = inject(TranslateService);

  loading = signal(false);
  error = signal<string | null>(null);

  async getStripe(): Promise<Stripe | null> {
    if (!this.isBrowser) {
      return null;
    }

    if (this.stripe) {
      return this.stripe;
    }

    if (!this.publishableKey) {
      try {
        const config = await firstValueFrom(
          this.http.get<{ publishableKey: string }>('/api/payments/config')
        );
        this.publishableKey = config.publishableKey;
      } catch (error) {
        console.error('Failed to load Stripe config:', error);
        this.error.set(this.translate.instant('errors.loadPaymentConfig'));
        return null;
      }
    }

    this.stripe = await loadStripe(this.publishableKey);
    return this.stripe;
  }

  getPublishableKey(): string | null {
    return this.publishableKey;
  }
}
