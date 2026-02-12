import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { Subscription, SubscriptionPlan, SubscriptionPlanDetails } from '../models/subscription.model';

export interface CheckoutSessionResponse {
  sessionId: string;
  url?: string;
  publishableKey: string;
  clientSecret?: string;
}

export interface PaymentConfig {
  publishableKey: string;
}

export interface Payment {
  id: number;
  payerId: number;
  payerName: string;
  teacherId?: number;
  teacherName?: string;
  lessonId?: number;
  subscriptionId?: number;
  paymentType: 'SUBSCRIPTION' | 'ONE_TIME_LESSON';
  amountCents: number;
  commissionCents: number;
  teacherPayoutCents?: number;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';
  processedAt?: string;
  createdAt: string;
}

export interface SubscriptionPlanResponseDto {
  code: SubscriptionPlan;
  name: string;
  priceCents: number;
  features: string[];
  popular: boolean;
}

export interface LessonCheckoutRequest {
  teacherId: number;
  scheduledAt: string;
  durationMinutes?: number;
  notes?: string;
  courseId?: number;
}

export interface FreeTrialStatus {
  hasActiveTrial: boolean;
  eligible: boolean;
  trialEndDate?: string;
  daysRemaining?: number;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private readonly apiUrl = '/api/payments';

  private plansSignal = signal<SubscriptionPlanResponseDto[]>([]);
  private activeSubscriptionSignal = signal<Subscription | null>(null);
  private paymentHistorySignal = signal<Payment[]>([]);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);
  private trialEligibleSignal = signal<boolean>(false);
  private freeTrialStatusSignal = signal<FreeTrialStatus | null>(null);

  readonly plans = this.plansSignal.asReadonly();
  readonly activeSubscription = this.activeSubscriptionSignal.asReadonly();
  readonly paymentHistory = this.paymentHistorySignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly trialEligible = this.trialEligibleSignal.asReadonly();
  readonly freeTrialStatus = this.freeTrialStatusSignal.asReadonly();

  // Check if user has premium access (subscription OR active trial)
  readonly hasActiveSubscription = () => {
    if (this.activeSubscriptionSignal() !== null) return true;
    const trial = this.freeTrialStatusSignal();
    return trial?.hasActiveTrial ?? false;
  };

  // Check if user has an active free trial
  readonly hasActiveTrial = () => this.freeTrialStatusSignal()?.hasActiveTrial ?? false;

  // Check if user is eligible for free trial
  readonly canStartFreeTrial = () => this.freeTrialStatusSignal()?.eligible ?? false;

  private translate = inject(TranslateService);

  constructor(private http: HttpClient) {}

  loadPlans(): Observable<SubscriptionPlanResponseDto[]> {
    return this.http.get<SubscriptionPlanResponseDto[]>(`${this.apiUrl}/plans`).pipe(
      tap(plans => this.plansSignal.set(plans))
    );
  }

  getStripeConfig(): Observable<PaymentConfig> {
    return this.http.get<PaymentConfig>(`${this.apiUrl}/config`);
  }

  createSubscriptionCheckout(plan: SubscriptionPlan, embedded = true): Observable<CheckoutSessionResponse> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/checkout/subscription`, { plan, embedded }).pipe(
      tap(() => this.loadingSignal.set(false)),
      catchError(error => {
        this.loadingSignal.set(false);
        this.errorSignal.set(this.translate.instant('errors.checkoutCreate'));
        throw error;
      })
    );
  }

  createLessonCheckout(request: LessonCheckoutRequest, embedded = true): Observable<CheckoutSessionResponse> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/checkout/lesson`, { ...request, embedded }).pipe(
      tap(() => this.loadingSignal.set(false)),
      catchError(error => {
        this.loadingSignal.set(false);
        this.errorSignal.set(this.translate.instant('errors.checkoutCreate'));
        throw error;
      })
    );
  }

  loadActiveSubscription(): Observable<Subscription | null> {
    this.loadingSignal.set(true);

    return this.http.get<Subscription>(`${this.apiUrl}/subscription`).pipe(
      tap(subscription => {
        this.activeSubscriptionSignal.set(subscription);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        // 204 No Content means no active subscription
        if (error.status === 204) {
          this.activeSubscriptionSignal.set(null);
        }
        this.loadingSignal.set(false);
        return [];
      })
    );
  }

  loadPaymentHistory(): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.apiUrl}/history`).pipe(
      tap(payments => this.paymentHistorySignal.set(payments))
    );
  }

  cancelSubscription(): Observable<Subscription> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.post<Subscription>(`${this.apiUrl}/subscription/cancel`, {}).pipe(
      tap(subscription => {
        this.activeSubscriptionSignal.set(subscription);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        this.errorSignal.set(this.translate.instant('errors.subscriptionCancel'));
        throw error;
      })
    );
  }

  verifyCheckoutSession(sessionId: string): Observable<{ status: string; paymentStatus: string }> {
    return this.http.get<{ status: string; paymentStatus: string }>(
      `${this.apiUrl}/checkout/verify?sessionId=${sessionId}`
    );
  }

  confirmLessonPayment(sessionId: string): Observable<{ success: boolean; lessonId?: number; error?: string }> {
    return this.http.post<{ success: boolean; lessonId?: number; error?: string }>(
      `${this.apiUrl}/checkout/lesson/confirm?sessionId=${sessionId}`,
      {}
    );
  }

  confirmSubscriptionPayment(sessionId: string): Observable<{
    success: boolean;
    subscriptionId?: number;
    planName?: string;
    error?: string;
  }> {
    return this.http.post<{
      success: boolean;
      subscriptionId?: number;
      planName?: string;
      error?: string;
    }>(
      `${this.apiUrl}/checkout/subscription/confirm?sessionId=${sessionId}`,
      {}
    ).pipe(
      tap((response) => {
        if (response.success) {
          // Reload active subscription after confirmation
          this.loadActiveSubscription().subscribe();
        }
      })
    );
  }

  formatPrice(cents: number): string {
    return (cents / 100).toFixed(2).replace('.', ',') + ' €';
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  /**
   * Check if the user is eligible for the 14-day free trial (Stripe - requires card).
   * First-time subscribers get a free trial.
   */
  checkTrialEligibility(): Observable<{ eligible: boolean }> {
    return this.http.get<{ eligible: boolean }>(`${this.apiUrl}/subscription/trial-eligible`).pipe(
      tap(response => this.trialEligibleSignal.set(response.eligible)),
      catchError(() => {
        this.trialEligibleSignal.set(false);
        return [];
      })
    );
  }

  /**
   * Get free trial status (without credit card).
   */
  loadFreeTrialStatus(): Observable<FreeTrialStatus> {
    return this.http.get<FreeTrialStatus>(`${this.apiUrl}/subscription/free-trial`).pipe(
      tap(status => this.freeTrialStatusSignal.set(status)),
      catchError(() => {
        this.freeTrialStatusSignal.set({ hasActiveTrial: false, eligible: false });
        return of({ hasActiveTrial: false, eligible: false });
      })
    );
  }

  /**
   * Start 14-day free premium trial (no credit card required).
   */
  startFreeTrial(): Observable<FreeTrialStatus & { success: boolean; message?: string; error?: string }> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.post<FreeTrialStatus & { success: boolean; message?: string; error?: string }>(
      `${this.apiUrl}/subscription/free-trial/start`,
      {}
    ).pipe(
      tap(response => {
        this.loadingSignal.set(false);
        if (response.success) {
          this.freeTrialStatusSignal.set({
            hasActiveTrial: response.hasActiveTrial,
            eligible: response.eligible,
            trialEndDate: response.trialEndDate,
            daysRemaining: response.daysRemaining
          });
        }
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        this.errorSignal.set(this.translate.instant('errors.freeTrialStart'));
        throw error;
      })
    );
  }
}
