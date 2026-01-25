import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { CheckoutSessionResponse } from './payment.service';

export interface WalletInfo {
  id: number | null;
  balanceCents: number;
  totalTopUpsCents: number;
  totalUsedCents: number;
  totalRefundedCents: number;
}

export interface CreditTransaction {
  id: number;
  transactionType: 'TOPUP' | 'LESSON_PAYMENT' | 'REFUND';
  amountCents: number;
  description: string;
  lessonId?: number;
  createdAt: string;
}

export interface TopUpRequest {
  amountCents: number;
  embedded: boolean;
}

export interface BookWithCreditRequest {
  teacherId: number;
  scheduledAt: string;
  durationMinutes: number;
  notes?: string;
}

export interface BookWithCreditResponse {
  success: boolean;
  lessonId?: number;
  remainingBalanceCents?: number;
  message?: string;
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private readonly apiUrl = '/api/wallet';

  private walletSignal = signal<WalletInfo | null>(null);
  private transactionsSignal = signal<CreditTransaction[]>([]);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);

  readonly wallet = this.walletSignal.asReadonly();
  readonly transactions = this.transactionsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  // Use computed signals for proper reactivity
  readonly balance = computed(() => this.walletSignal()?.balanceCents ?? 0);
  readonly hasCredit = computed(() => this.balance() > 0);

  constructor(private http: HttpClient) {}

  loadWallet(): Observable<WalletInfo> {
    this.loadingSignal.set(true);
    return this.http.get<WalletInfo>(this.apiUrl).pipe(
      tap(wallet => {
        this.walletSignal.set(wallet);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        // Return empty wallet if not found
        const emptyWallet: WalletInfo = {
          id: null,
          balanceCents: 0,
          totalTopUpsCents: 0,
          totalUsedCents: 0,
          totalRefundedCents: 0
        };
        this.walletSignal.set(emptyWallet);
        return of(emptyWallet);
      })
    );
  }

  loadBalance(): Observable<{ balanceCents: number }> {
    return this.http.get<{ balanceCents: number }>(`${this.apiUrl}/balance`).pipe(
      tap(response => {
        const currentWallet = this.walletSignal();
        if (currentWallet) {
          this.walletSignal.set({ ...currentWallet, balanceCents: response.balanceCents });
        } else {
          this.walletSignal.set({
            id: null,
            balanceCents: response.balanceCents,
            totalTopUpsCents: 0,
            totalUsedCents: 0,
            totalRefundedCents: 0
          });
        }
      }),
      catchError(() => of({ balanceCents: 0 }))
    );
  }

  loadTransactions(): Observable<CreditTransaction[]> {
    return this.http.get<CreditTransaction[]>(`${this.apiUrl}/transactions`).pipe(
      tap(transactions => this.transactionsSignal.set(transactions))
    );
  }

  createTopUpSession(amountCents: number, embedded = true): Observable<CheckoutSessionResponse> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    const request: TopUpRequest = { amountCents, embedded };
    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/topup`, request).pipe(
      tap(() => this.loadingSignal.set(false)),
      catchError(error => {
        this.loadingSignal.set(false);
        this.errorSignal.set('Erreur lors de la création de la session de paiement');
        throw error;
      })
    );
  }

  confirmTopUp(sessionId: string): Observable<{ success: boolean; balanceCents?: number; message?: string; error?: string }> {
    return this.http.post<{ success: boolean; balanceCents?: number; message?: string; error?: string }>(
      `${this.apiUrl}/topup/confirm?sessionId=${sessionId}`,
      {}
    ).pipe(
      tap(response => {
        if (response.success && response.balanceCents !== undefined) {
          const currentWallet = this.walletSignal();
          if (currentWallet) {
            this.walletSignal.set({ ...currentWallet, balanceCents: response.balanceCents });
          }
        }
      })
    );
  }

  bookWithCredit(request: BookWithCreditRequest): Observable<BookWithCreditResponse> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.post<BookWithCreditResponse>(`${this.apiUrl}/book-with-credit`, request).pipe(
      tap(response => {
        this.loadingSignal.set(false);
        if (response.success && response.remainingBalanceCents !== undefined) {
          const currentWallet = this.walletSignal();
          if (currentWallet) {
            this.walletSignal.set({ ...currentWallet, balanceCents: response.remainingBalanceCents });
          }
        }
        if (!response.success && response.error) {
          this.errorSignal.set(response.error);
        }
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        this.errorSignal.set(error.error?.error || 'Erreur lors de la réservation');
        throw error;
      })
    );
  }

  hasEnoughCredit(amountCents: number): boolean {
    return this.balance() >= amountCents;
  }

  formatPrice(cents: number): string {
    return (cents / 100).toFixed(2).replace('.', ',') + ' €';
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  getTransactionTypeLabel(type: string): string {
    switch (type) {
      case 'TOPUP': return 'Recharge';
      case 'LESSON_PAYMENT': return 'Cours';
      case 'REFUND': return 'Remboursement';
      default: return type;
    }
  }

  getTransactionSign(type: string): string {
    switch (type) {
      case 'TOPUP': return '+';
      case 'LESSON_PAYMENT': return '-';
      case 'REFUND': return '+';
      default: return '';
    }
  }
}
