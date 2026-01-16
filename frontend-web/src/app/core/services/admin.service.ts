import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserListResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  isSuspended: boolean;
  createdAt: string;
  hourlyRateCents?: number;
  languages?: string;
  averageRating?: number;
  reviewCount?: number;
  lessonsCount: number;
}

export interface AccountingResponse {
  totalRevenueCents: number;
  totalCommissionsCents: number;
  totalTeacherEarningsCents: number;
  totalRefundedCents: number;
  totalLessons: number;
  completedLessons: number;
  cancelledLessons: number;
}

export interface AdminStatsResponse {
  totalUsers: number;
  totalStudents: number;
  totalTeachers: number;
  activeSubscriptions: number;
  totalLessons: number;
  lessonsThisMonth: number;
  totalRevenueCents: number;
  revenueThisMonthCents: number;
}

export interface TeacherBalanceResponse {
  teacherId: number;
  firstName: string;
  lastName: string;
  email: string;
  availableBalanceCents: number;
  pendingBalanceCents: number;
  totalEarnedCents: number;
  totalWithdrawnCents: number;
  lessonsCompleted: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

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

  suspendUser(id: number, reason: string): Observable<any> {
    return this.http.patch(`${this.apiUrl}/users/${id}/suspend`, { reason });
  }

  activateUser(id: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/users/${id}/activate`, {});
  }

  // Accounting
  getAccountingOverview(): Observable<AccountingResponse> {
    return this.http.get<AccountingResponse>(`${this.apiUrl}/accounting/revenue`);
  }

  getTeacherBalances(): Observable<TeacherBalanceResponse[]> {
    return this.http.get<TeacherBalanceResponse[]>(`${this.apiUrl}/accounting/teachers`);
  }

  getPayments(page = 0, size = 20): Observable<Page<any>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<any>>(`${this.apiUrl}/accounting/payments`, { params });
  }

  // Subscriptions
  getSubscriptions(page = 0, size = 20): Observable<Page<any>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<any>>(`${this.apiUrl}/subscriptions`, { params });
  }

  cancelSubscription(id: number, reason: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/subscriptions/${id}/cancel`, { reason });
  }

  // Refunds
  refundPayment(paymentIntentId: string, percentage: number, reason: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/payments/${paymentIntentId}/refund`, {
      percentage,
      reason
    });
  }

  // Stats
  getStats(): Observable<AdminStatsResponse> {
    return this.http.get<AdminStatsResponse>(`${this.apiUrl}/stats`);
  }
}
