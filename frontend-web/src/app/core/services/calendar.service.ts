import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface CalendarStatus {
  configured: boolean;
  connected: boolean;
  enabled: boolean;
}

export interface CalendarAuthUrl {
  configured: boolean;
  authUrl?: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CalendarService {
  private readonly apiUrl = '/api/calendar';

  status = signal<CalendarStatus>({
    configured: false,
    connected: false,
    enabled: false
  });

  constructor(private http: HttpClient) {}

  /**
   * Get Google Calendar connection status
   */
  getStatus(): Observable<CalendarStatus> {
    return this.http.get<CalendarStatus>(`${this.apiUrl}/google/status`).pipe(
      tap(status => this.status.set(status))
    );
  }

  /**
   * Get Google OAuth authorization URL
   */
  getAuthUrl(): Observable<CalendarAuthUrl> {
    return this.http.get<CalendarAuthUrl>(`${this.apiUrl}/google/auth-url`);
  }

  /**
   * Handle OAuth callback with authorization code
   */
  handleCallback(code: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.apiUrl}/google/callback`,
      { code }
    ).pipe(
      tap(() => {
        // Refresh status after connecting
        this.getStatus().subscribe();
      })
    );
  }

  /**
   * Disconnect Google Calendar
   */
  disconnect(): Observable<{ success: boolean; message: string }> {
    return this.http.delete<{ success: boolean; message: string }>(
      `${this.apiUrl}/google/disconnect`
    ).pipe(
      tap(() => {
        this.status.set({
          configured: this.status().configured,
          connected: false,
          enabled: false
        });
      })
    );
  }
}
