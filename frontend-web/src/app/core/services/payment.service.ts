import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError } from 'rxjs';
import { Subscription, SubscriptionPlan, SubscriptionPlanDetails } from '../models/subscription.model';

export interface CheckoutSessionResponse {
  sessionId: string;
  url: string;
  publishableKey: string;
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
  monthlyQuota: number;
  features: string[];
  popular: boolean;
}

export interface LessonCheckoutRequest {
  teacherId: number;
  scheduledAt: string;
  durationMinutes?: number;
  notes?: string;
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

  readonly plans = this.plansSignal.asReadonly();
  readonly activeSubscription = this.activeSubscriptionSignal.asReadonly();
  readonly paymentHistory = this.paymentHistorySignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly hasActiveSubscription = () => this.activeSubscriptionSignal() !== null;

  constructor(private http: HttpClient) {}

  loadPlans(): Observable<SubscriptionPlanResponseDto[]> {
    return this.http.get<SubscriptionPlanResponseDto[]>(`${this.apiUrl}/plans`).pipe(
      tap(plans => this.plansSignal.set(plans))
    );
  }

  getStripeConfig(): Observable<PaymentConfig> {
    return this.http.get<PaymentConfig>(`${this.apiUrl}/config`);
  }

  createSubscriptionCheckout(plan: SubscriptionPlan): Observable<CheckoutSessionResponse> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/checkout/subscription`, { plan }).pipe(
      tap(() => this.loadingSignal.set(false)),
      catchError(error => {
        this.loadingSignal.set(false);
        this.errorSignal.set('Erreur lors de la création de la session de paiement');
        throw error;
      })
    );
  }

  createLessonCheckout(request: LessonCheckoutRequest): Observable<CheckoutSessionResponse> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/checkout/lesson`, request).pipe(
      tap(() => this.loadingSignal.set(false)),
      catchError(error => {
        this.loadingSignal.set(false);
        this.errorSignal.set('Erreur lors de la création de la session de paiement');
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
        this.errorSignal.set('Erreur lors de l\'annulation de l\'abonnement');
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
    monthlyQuota?: number;
    error?: string;
  }> {
    return this.http.post<{
      success: boolean;
      subscriptionId?: number;
      planName?: string;
      monthlyQuota?: number;
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
}
