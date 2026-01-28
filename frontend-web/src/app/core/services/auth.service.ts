import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, RegisterResponse, UpdateTeacherProfileRequest, User, UserRole } from '../models/user.model';
import { NotificationCenterService } from './notification-center.service';

const TOKEN_KEY = 'chess_token';
const USER_KEY = 'chess_user';
const LAST_ACTIVITY_KEY = 'chess_last_activity';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = '/api/auth';
  private readonly platformId = inject(PLATFORM_ID);

  private currentUserSignal = signal<User | null>(null);
  private tokenSignal = signal<string | null>(null);
  private notificationCenterService = inject(NotificationCenterService);

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly token = this.tokenSignal.asReadonly();

  readonly isAuthenticated = computed(() => !!this.tokenSignal());
  readonly isStudent = computed(() => this.currentUserSignal()?.role === 'STUDENT');
  readonly isTeacher = computed(() => this.currentUserSignal()?.role === 'TEACHER');
  readonly isAdmin = computed(() => this.currentUserSignal()?.role === 'ADMIN');

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    // Initialize from storage only in browser
    if (isPlatformBrowser(this.platformId)) {
      this.currentUserSignal.set(this.loadUserFromStorage());
      this.tokenSignal.set(this.loadTokenFromStorage());
    }
    // Initialize notifications for current user if already logged in
    const user = this.loadUserFromStorage();
    if (user?.id) {
      this.notificationCenterService.initializeForUser(user.id);
    }
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  adminLogin(request: { username: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/admin-login`, request).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  register(request: RegisterRequest): Observable<RegisterResponse> {
    // Registration now returns a message to verify email, not a token
    return this.http.post<RegisterResponse>(`${this.apiUrl}/register`, request);
  }

  verifyEmail(token: string): Observable<{ success: boolean; message: string }> {
    return this.http.get<{ success: boolean; message: string }>(`${this.apiUrl}/verify-email`, {
      params: { token }
    });
  }

  resendVerificationEmail(email: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.apiUrl}/resend-verification`, { email });
  }

  logout(): void {
    // Call server logout to clear HttpOnly cookie
    this.http.post('/api/auth/logout', {}).subscribe({
      complete: () => {
        this.clearLocalAuth();
      },
      error: () => {
        // Even if server call fails, clear local state
        this.clearLocalAuth();
      }
    });
  }

  private clearLocalAuth(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      localStorage.removeItem(LAST_ACTIVITY_KEY);
    }
    this.currentUserSignal.set(null);
    this.tokenSignal.set(null);
    // Clear notifications when logging out
    this.notificationCenterService.clearOnLogout();
    this.router.navigate(['/']);
  }

  private handleAuthResponse(response: AuthResponse): void {
    const user: User = {
      id: response.userId,
      email: response.email,
      firstName: response.firstName,
      lastName: response.lastName,
      role: response.role
    };

    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(TOKEN_KEY, response.token);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
      // Reset last activity timestamp on login to prevent false inactivity logout
      localStorage.setItem(LAST_ACTIVITY_KEY, Date.now().toString());
    }

    this.tokenSignal.set(response.token);
    this.currentUserSignal.set(user);

    // Initialize notifications for this user (loads their own notifications, not previous user's)
    this.notificationCenterService.initializeForUser(response.userId);
  }

  private loadTokenFromStorage(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(TOKEN_KEY);
  }

  private loadUserFromStorage(): User | null {
    if (typeof window === 'undefined') return null;
    const userJson = localStorage.getItem(USER_KEY);
    return userJson ? JSON.parse(userJson) : null;
  }

  getRedirectRoute(): string {
    const role = this.currentUserSignal()?.role;
    switch (role) {
      case 'STUDENT': return '/dashboard';
      case 'TEACHER': return '/dashboard';
      case 'ADMIN': return '/admin';
      default: return '/';
    }
  }

  loadCurrentUserProfile(): Observable<User> {
    return this.http.get<User>('/api/users/me').pipe(
      tap(user => {
        this.currentUserSignal.set(user);
        if (isPlatformBrowser(this.platformId)) {
          localStorage.setItem(USER_KEY, JSON.stringify(user));
        }
      })
    );
  }

  updateTeacherProfile(request: UpdateTeacherProfileRequest): Observable<User> {
    return this.http.put<User>('/api/users/me/teacher-profile', request).pipe(
      tap(user => {
        this.currentUserSignal.set(user);
        if (isPlatformBrowser(this.platformId)) {
          localStorage.setItem(USER_KEY, JSON.stringify(user));
        }
      })
    );
  }

  updateCurrentUser(user: User): void {
    this.currentUserSignal.set(user);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    }
  }
}
