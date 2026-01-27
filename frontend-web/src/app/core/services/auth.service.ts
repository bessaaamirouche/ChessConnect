import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, UpdateTeacherProfileRequest, User, UserRole } from '../models/user.model';

const TOKEN_KEY = 'chess_token';
const USER_KEY = 'chess_user';
const LAST_ACTIVITY_KEY = 'chess_last_activity';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = '/api/auth';

  private currentUserSignal = signal<User | null>(this.loadUserFromStorage());
  private tokenSignal = signal<string | null>(this.loadTokenFromStorage());

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly token = this.tokenSignal.asReadonly();

  readonly isAuthenticated = computed(() => !!this.tokenSignal());
  readonly isStudent = computed(() => this.currentUserSignal()?.role === 'STUDENT');
  readonly isTeacher = computed(() => this.currentUserSignal()?.role === 'TEACHER');
  readonly isAdmin = computed(() => this.currentUserSignal()?.role === 'ADMIN');

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

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

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUserSignal.set(null);
    this.tokenSignal.set(null);
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

    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    // Reset last activity timestamp on login to prevent false inactivity logout
    localStorage.setItem(LAST_ACTIVITY_KEY, Date.now().toString());

    this.tokenSignal.set(response.token);
    this.currentUserSignal.set(user);
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
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      })
    );
  }

  updateTeacherProfile(request: UpdateTeacherProfileRequest): Observable<User> {
    return this.http.put<User>('/api/users/me/teacher-profile', request).pipe(
      tap(user => {
        this.currentUserSignal.set(user);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      })
    );
  }

  updateCurrentUser(user: User): void {
    this.currentUserSignal.set(user);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }
}
