import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface StripeConnectStatus {
  connected: boolean;
  accountExists: boolean;
  isReady: boolean;
  chargesEnabled?: boolean;
  payoutsEnabled?: boolean;
  pendingReason?: string;
  message?: string;
}

export interface StripeConnectOnboardingResponse {
  success: boolean;
  onboardingUrl?: string;
  accountId?: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class StripeConnectService {
  private readonly apiUrl = '/api/stripe-connect';

  status = signal<StripeConnectStatus>({
    connected: false,
    accountExists: false,
    isReady: false
  });

  loading = signal(false);

  constructor(private http: HttpClient) {}

  /**
   * Get Stripe Connect account status
   */
  getStatus(): Observable<StripeConnectStatus> {
    return this.http.get<StripeConnectStatus>(`${this.apiUrl}/status`).pipe(
      tap(status => this.status.set(status))
    );
  }

  /**
   * Start Stripe Connect onboarding - creates account and returns onboarding URL
   */
  startOnboarding(): Observable<StripeConnectOnboardingResponse> {
    this.loading.set(true);
    return this.http.post<StripeConnectOnboardingResponse>(`${this.apiUrl}/onboarding`, {}).pipe(
      tap({
        next: (response) => {
          this.loading.set(false);
          if (response.success && response.onboardingUrl) {
            // Redirect to Stripe onboarding
            window.location.href = response.onboardingUrl;
          }
        },
        error: () => this.loading.set(false)
      })
    );
  }

  /**
   * Refresh onboarding link (for incomplete onboarding)
   */
  refreshOnboardingLink(): Observable<StripeConnectOnboardingResponse> {
    this.loading.set(true);
    return this.http.post<StripeConnectOnboardingResponse>(`${this.apiUrl}/refresh-link`, {}).pipe(
      tap({
        next: (response) => {
          this.loading.set(false);
          if (response.success && response.onboardingUrl) {
            window.location.href = response.onboardingUrl;
          }
        },
        error: () => this.loading.set(false)
      })
    );
  }

  /**
   * Disconnect Stripe Connect account
   */
  disconnect(): Observable<{ success: boolean; message: string }> {
    this.loading.set(true);
    return this.http.delete<{ success: boolean; message: string }>(`${this.apiUrl}/disconnect`).pipe(
      tap({
        next: () => {
          this.loading.set(false);
          this.status.set({
            connected: false,
            accountExists: false,
            isReady: false
          });
        },
        error: () => this.loading.set(false)
      })
    );
  }

  /**
   * Get teacher's balance for withdrawal
   */
  getBalance(): Observable<{ availableBalanceCents: number; totalEarnedCents: number; totalWithdrawnCents: number; lessonsCompleted: number }> {
    return this.http.get<{ availableBalanceCents: number; totalEarnedCents: number; totalWithdrawnCents: number; lessonsCompleted: number }>(`${this.apiUrl}/balance`);
  }

  /**
   * Withdraw available balance to teacher's Stripe Connect account
   * @param amountCents - Amount to withdraw in cents (minimum 10000 = 100€)
   */
  withdraw(amountCents: number): Observable<{ success: boolean; message?: string; amountCents?: number; stripeTransferId?: string }> {
    this.loading.set(true);
    return this.http.post<{ success: boolean; message?: string; amountCents?: number; stripeTransferId?: string }>(`${this.apiUrl}/withdraw`, { amountCents }).pipe(
      tap({
        next: () => this.loading.set(false),
        error: () => this.loading.set(false)
      })
    );
  }
}
