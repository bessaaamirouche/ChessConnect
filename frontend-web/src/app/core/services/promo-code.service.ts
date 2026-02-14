import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ValidatePromoCodeResponse {
  valid: boolean;
  message: string;
  discountType: 'COMMISSION_REDUCTION' | 'STUDENT_DISCOUNT' | null;
  discountPercent: number | null;
  finalPriceCents: number | null;
  discountAmountCents: number | null;
}

@Injectable({ providedIn: 'root' })
export class PromoCodeService {
  private http = inject(HttpClient);
  private readonly apiUrl = '/api/promo';

  validateCode(code: string, amountCents: number): Observable<ValidatePromoCodeResponse> {
    return this.http.get<ValidatePromoCodeResponse>(`${this.apiUrl}/validate`, {
      params: { code, amount: amountCents.toString() }
    });
  }

  applyReferral(code: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/apply-referral`, { code });
  }
}
