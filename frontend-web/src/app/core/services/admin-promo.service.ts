import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PromoCodeResponse {
  id: number;
  code: string;
  codeType: 'PROMO' | 'REFERRAL';
  discountType: 'COMMISSION_REDUCTION' | 'STUDENT_DISCOUNT' | null;
  discountPercent: number | null;
  referrerName: string | null;
  referrerEmail: string | null;
  premiumDays: number;
  revenueSharePercent: number;
  maxUses: number | null;
  currentUses: number;
  firstLessonOnly: boolean;
  minAmountCents: number | null;
  isActive: boolean;
  expiresAt: string | null;
  createdAt: string;
  totalDiscountCents: number;
  totalEarningsCents: number;
  unpaidEarningsCents: number;
}

export interface CreatePromoCodeRequest {
  code: string;
  codeType: 'PROMO' | 'REFERRAL';
  discountType?: 'COMMISSION_REDUCTION' | 'STUDENT_DISCOUNT';
  discountPercent?: number;
  referrerName?: string;
  referrerEmail?: string;
  premiumDays?: number;
  revenueSharePercent?: number;
  maxUses?: number;
  firstLessonOnly?: boolean;
  minAmountCents?: number;
  expiresAt?: string;
}

export interface PromoCodeUsageResponse {
  id: number;
  userId: number;
  userName: string;
  lessonId: number | null;
  originalAmountCents: number;
  discountAmountCents: number;
  commissionSavedCents: number;
  usedAt: string;
}

export interface ReferralEarningResponse {
  id: number;
  referredUserId: number;
  referredUserName: string;
  lessonId: number | null;
  lessonAmountCents: number;
  platformCommissionCents: number;
  referrerEarningCents: number;
  isPaid: boolean;
  paidAt: string | null;
  paymentReference: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AdminPromoService {
  private http = inject(HttpClient);
  private readonly apiUrl = '/api/admin/promo-codes';

  getAll(): Observable<PromoCodeResponse[]> {
    return this.http.get<PromoCodeResponse[]>(this.apiUrl);
  }

  getById(id: number): Observable<PromoCodeResponse> {
    return this.http.get<PromoCodeResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: CreatePromoCodeRequest): Observable<PromoCodeResponse> {
    return this.http.post<PromoCodeResponse>(this.apiUrl, request);
  }

  update(id: number, request: Partial<CreatePromoCodeRequest>): Observable<PromoCodeResponse> {
    return this.http.put<PromoCodeResponse>(`${this.apiUrl}/${id}`, request);
  }

  toggleActive(id: number, active: boolean): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/toggle`, { active });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  generateCode(): Observable<{ code: string }> {
    return this.http.post<{ code: string }>(`${this.apiUrl}/generate`, {});
  }

  getUsages(id: number): Observable<PromoCodeUsageResponse[]> {
    return this.http.get<PromoCodeUsageResponse[]>(`${this.apiUrl}/${id}/usages`);
  }

  getEarnings(id: number): Observable<ReferralEarningResponse[]> {
    return this.http.get<ReferralEarningResponse[]>(`${this.apiUrl}/${id}/earnings`);
  }

  markEarningsAsPaid(id: number, paymentReference?: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/mark-paid`, { paymentReference });
  }
}
