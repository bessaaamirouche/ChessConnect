import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  UserListResponse,
  AccountingResponse,
  AdminStatsResponse,
  TeacherBalanceListResponse,
  MarkTeacherPaidResponse,
  LessonResponse,
  Page,
  AnalyticsResponse,
  DataPoint,
  HourlyDataPoint,
  AdminActionResponse,
  PaymentResponse,
  SubscriptionResponse,
  RefundResponse
} from '@contracts';

// Re-export for backward compatibility
export {
  UserListResponse,
  AccountingResponse,
  AdminStatsResponse,
  MarkTeacherPaidResponse,
  Page,
  AnalyticsResponse,
  DataPoint,
  HourlyDataPoint,
  PaymentResponse,
  SubscriptionResponse
} from '@contracts';

// Alias for backward compatibility
export type TeacherBalanceResponse = TeacherBalanceListResponse;
export type AdminLessonResponse = LessonResponse;

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl = '/api/admin';

  constructor(private http: HttpClient) {}

  // Users
  getUsers(page = 0, size = 20, role?: string): Observable<Page<UserListResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (role) {
      params = params.set('role', role);
    }

    return this.http.get<Page<UserListResponse>>(`${this.apiUrl}/users`, { params });
  }

  getUserById(id: number): Observable<UserListResponse> {
    return this.http.get<UserListResponse>(`${this.apiUrl}/users/${id}`);
  }

  suspendUser(id: number, reason: string): Observable<AdminActionResponse> {
    return this.http.patch<AdminActionResponse>(`${this.apiUrl}/users/${id}/suspend`, { reason });
  }

  activateUser(id: number): Observable<AdminActionResponse> {
    return this.http.patch<AdminActionResponse>(`${this.apiUrl}/users/${id}/activate`, {});
  }

  // Lessons
  getUpcomingLessons(): Observable<AdminLessonResponse[]> {
    return this.http.get<AdminLessonResponse[]>(`${this.apiUrl}/lessons/upcoming`);
  }

  getCompletedLessons(): Observable<AdminLessonResponse[]> {
    return this.http.get<AdminLessonResponse[]>(`${this.apiUrl}/lessons/completed`);
  }

  // Get all past lessons (COMPLETED + CANCELLED) for history/investigation
  getPastLessons(): Observable<AdminLessonResponse[]> {
    return this.http.get<AdminLessonResponse[]>(`${this.apiUrl}/lessons/history`);
  }

  // Get ALL lessons (all statuses) for complete overview
  getAllLessons(): Observable<AdminLessonResponse[]> {
    return this.http.get<AdminLessonResponse[]>(`${this.apiUrl}/lessons/all`);
  }

  // Accounting
  getAccountingOverview(): Observable<AccountingResponse> {
    return this.http.get<AccountingResponse>(`${this.apiUrl}/accounting/revenue`);
  }

  getTeacherBalances(): Observable<TeacherBalanceResponse[]> {
    return this.http.get<TeacherBalanceResponse[]>(`${this.apiUrl}/accounting/teachers`);
  }

  markTeacherPaid(teacherId: number, yearMonth?: string, paymentReference?: string, notes?: string, amountCents?: number): Observable<MarkTeacherPaidResponse> {
    return this.http.post<MarkTeacherPaidResponse>(`${this.apiUrl}/accounting/teachers/${teacherId}/pay`, {
      yearMonth: yearMonth || new Date().toISOString().slice(0, 7),
      paymentReference: paymentReference || '',
      notes: notes || '',
      amountCents: amountCents
    });
  }

  getPayments(page = 0, size = 20): Observable<Page<PaymentResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<PaymentResponse>>(`${this.apiUrl}/accounting/payments`, { params });
  }

  // Subscriptions
  getSubscriptions(page = 0, size = 20): Observable<Page<SubscriptionResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<SubscriptionResponse>>(`${this.apiUrl}/subscriptions`, { params });
  }

  cancelSubscription(id: number, reason: string): Observable<AdminActionResponse> {
    return this.http.post<AdminActionResponse>(`${this.apiUrl}/subscriptions/${id}/cancel`, { reason });
  }

  // Refunds
  refundPayment(paymentIntentId: string, percentage: number, reason: string): Observable<RefundResponse> {
    return this.http.post<RefundResponse>(`${this.apiUrl}/payments/${paymentIntentId}/refund`, {
      percentage,
      reason
    });
  }

  // Stats
  getStats(): Observable<AdminStatsResponse> {
    return this.http.get<AdminStatsResponse>(`${this.apiUrl}/stats`);
  }

  // Analytics
  getAnalytics(period: 'day' | 'week' | 'month' = 'day'): Observable<AnalyticsResponse> {
    const params = new HttpParams().set('period', period);
    return this.http.get<AnalyticsResponse>(`${this.apiUrl}/analytics`, { params });
  }
}
