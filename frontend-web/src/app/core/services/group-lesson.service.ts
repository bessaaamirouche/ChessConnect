import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  BookGroupLessonRequest,
  JoinGroupLessonRequest,
  GroupInvitationResponse,
  GroupLessonResponse,
  CheckoutSessionResponse
} from '@contracts';

@Injectable({
  providedIn: 'root'
})
export class GroupLessonService {
  private readonly apiUrl = '/api/group-lessons';

  constructor(private http: HttpClient) {}

  createGroupLesson(request: BookGroupLessonRequest): Observable<Record<string, any>> {
    return this.http.post<Record<string, any>>(this.apiUrl, request);
  }

  getInvitationDetails(token: string): Observable<GroupInvitationResponse> {
    return this.http.get<GroupInvitationResponse>(`${this.apiUrl}/invitation/${token}`);
  }

  joinWithCredit(token: string): Observable<Record<string, any>> {
    const request: JoinGroupLessonRequest = { token };
    return this.http.post<Record<string, any>>(`${this.apiUrl}/join/credit`, request);
  }

  createJoinCheckout(token: string): Observable<CheckoutSessionResponse> {
    const request: JoinGroupLessonRequest = { token };
    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/join/checkout`, request);
  }

  confirmJoinPayment(sessionId: string): Observable<Record<string, any>> {
    const params = new HttpParams().set('sessionId', sessionId);
    return this.http.post<Record<string, any>>(`${this.apiUrl}/join/confirm`, null, { params });
  }

  createGroupCheckout(request: BookGroupLessonRequest): Observable<CheckoutSessionResponse> {
    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/checkout`, request);
  }

  confirmCreatePayment(sessionId: string): Observable<Record<string, any>> {
    const params = new HttpParams().set('sessionId', sessionId);
    return this.http.post<Record<string, any>>(`${this.apiUrl}/create/confirm`, null, { params });
  }

  resolveDeadline(lessonId: number, choice: 'PAY_FULL' | 'CANCEL'): Observable<Record<string, any>> {
    return this.http.post<Record<string, any>>(`${this.apiUrl}/${lessonId}/resolve-deadline`, { choice });
  }

  leaveGroupLesson(lessonId: number, reason?: string): Observable<Record<string, any>> {
    const params = reason ? new HttpParams().set('reason', reason) : undefined;
    return this.http.delete<Record<string, any>>(`${this.apiUrl}/${lessonId}/leave`, { params });
  }

  getGroupLessonDetails(lessonId: number): Observable<GroupLessonResponse> {
    return this.http.get<GroupLessonResponse>(`${this.apiUrl}/${lessonId}`);
  }
}
