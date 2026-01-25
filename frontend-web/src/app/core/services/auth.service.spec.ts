import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { AuthResponse, User, LoginRequest, RegisterRequest } from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockAuthResponse: AuthResponse = {
    token: 'jwt_token_123',
    userId: 1,
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    role: 'STUDENT'
  };

  const mockTeacherAuthResponse: AuthResponse = {
    token: 'jwt_teacher_token',
    userId: 2,
    email: 'teacher@example.com',
    firstName: 'Jane',
    lastName: 'Smith',
    role: 'TEACHER'
  };

  const mockAdminAuthResponse: AuthResponse = {
    token: 'jwt_admin_token',
    userId: 3,
    email: 'admin@example.com',
    firstName: 'Admin',
    lastName: 'User',
    role: 'ADMIN'
  };

  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear();

    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('Initial State', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should have null currentUser when no stored user', () => {
      expect(service.currentUser()).toBeNull();
    });

    it('should have null token when no stored token', () => {
      expect(service.token()).toBeNull();
    });

    it('should have isAuthenticated as false initially', () => {
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('should have isStudent as false initially', () => {
      expect(service.isStudent()).toBeFalse();
    });

    it('should have isTeacher as false initially', () => {
      expect(service.isTeacher()).toBeFalse();
    });

    it('should have isAdmin as false initially', () => {
      expect(service.isAdmin()).toBeFalse();
    });
  });

  describe('login', () => {
    const loginRequest: LoginRequest = {
      email: 'test@example.com',
      password: 'password123'
    };

    it('should login successfully', fakeAsync(() => {
      let response: AuthResponse | null = null;
      service.login(loginRequest).subscribe(r => response = r);

      const req = httpMock.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(loginRequest);
      req.flush(mockAuthResponse);

      tick();

      expect(response).toEqual(mockAuthResponse);
      expect(service.isAuthenticated()).toBeTrue();
      expect(service.currentUser()?.email).toBe('test@example.com');
      expect(service.token()).toBe('jwt_token_123');
    }));

    it('should store token in localStorage', fakeAsync(() => {
      service.login(loginRequest).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      expect(localStorage.getItem('chess_token')).toBe('jwt_token_123');
    }));

    it('should store user in localStorage', fakeAsync(() => {
      service.login(loginRequest).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      const storedUser = JSON.parse(localStorage.getItem('chess_user') || '{}');
      expect(storedUser.email).toBe('test@example.com');
    }));

    it('should set isStudent when role is STUDENT', fakeAsync(() => {
      service.login(loginRequest).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      expect(service.isStudent()).toBeTrue();
      expect(service.isTeacher()).toBeFalse();
      expect(service.isAdmin()).toBeFalse();
    }));

    it('should set isTeacher when role is TEACHER', fakeAsync(() => {
      service.login(loginRequest).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockTeacherAuthResponse);
      tick();

      expect(service.isStudent()).toBeFalse();
      expect(service.isTeacher()).toBeTrue();
      expect(service.isAdmin()).toBeFalse();
    }));

    it('should handle login error', fakeAsync(() => {
      let error: any;
      service.login(loginRequest).subscribe({
        error: e => error = e
      });

      const req = httpMock.expectOne('/api/auth/login');
      req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });

      tick();

      expect(error).toBeTruthy();
      expect(service.isAuthenticated()).toBeFalse();
    }));
  });

  describe('adminLogin', () => {
    const adminLoginRequest = {
      username: 'admin',
      password: 'admin123'
    };

    it('should login admin successfully', fakeAsync(() => {
      let response: AuthResponse | null = null;
      service.adminLogin(adminLoginRequest).subscribe(r => response = r);

      const req = httpMock.expectOne('/api/auth/admin-login');
      expect(req.request.method).toBe('POST');
      req.flush(mockAdminAuthResponse);

      tick();

      expect(service.isAdmin()).toBeTrue();
      expect(service.currentUser()?.role).toBe('ADMIN');
    }));
  });

  describe('register', () => {
    const registerRequest: RegisterRequest = {
      email: 'new@example.com',
      password: 'password123',
      firstName: 'New',
      lastName: 'User',
      role: 'STUDENT'
    };

    it('should register successfully', fakeAsync(() => {
      let response: AuthResponse | null = null;
      service.register(registerRequest).subscribe(r => response = r);

      const req = httpMock.expectOne('/api/auth/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(registerRequest);
      req.flush(mockAuthResponse);

      tick();

      expect(response).toBeTruthy();
      expect(service.isAuthenticated()).toBeTrue();
    }));

    it('should handle registration error', fakeAsync(() => {
      let error: any;
      service.register(registerRequest).subscribe({
        error: e => error = e
      });

      const req = httpMock.expectOne('/api/auth/register');
      req.flush({ message: 'Email already exists' }, { status: 400, statusText: 'Bad Request' });

      tick();

      expect(error).toBeTruthy();
    }));
  });

  describe('logout', () => {
    it('should clear token from localStorage', fakeAsync(() => {
      // First login
      service.login({ email: 'test@example.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      expect(localStorage.getItem('chess_token')).toBeTruthy();

      // Then logout
      service.logout();

      expect(localStorage.getItem('chess_token')).toBeNull();
    }));

    it('should clear user from localStorage', fakeAsync(() => {
      service.login({ email: 'test@example.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      service.logout();

      expect(localStorage.getItem('chess_user')).toBeNull();
    }));

    it('should set currentUser to null', fakeAsync(() => {
      service.login({ email: 'test@example.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      service.logout();

      expect(service.currentUser()).toBeNull();
    }));

    it('should set token to null', fakeAsync(() => {
      service.login({ email: 'test@example.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      service.logout();

      expect(service.token()).toBeNull();
    }));

    it('should set isAuthenticated to false', fakeAsync(() => {
      service.login({ email: 'test@example.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      service.logout();

      expect(service.isAuthenticated()).toBeFalse();
    }));

    it('should navigate to home page', fakeAsync(() => {
      service.login({ email: 'test@example.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      service.logout();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
    }));
  });

  describe('getRedirectRoute', () => {
    it('should return /dashboard for STUDENT', fakeAsync(() => {
      service.login({ email: 'test@example.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      expect(service.getRedirectRoute()).toBe('/dashboard');
    }));

    it('should return /dashboard for TEACHER', fakeAsync(() => {
      service.login({ email: 'teacher@example.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockTeacherAuthResponse);
      tick();

      expect(service.getRedirectRoute()).toBe('/dashboard');
    }));

    it('should return /admin for ADMIN', fakeAsync(() => {
      service.adminLogin({ username: 'admin', password: 'admin' }).subscribe();
      httpMock.expectOne('/api/auth/admin-login').flush(mockAdminAuthResponse);
      tick();

      expect(service.getRedirectRoute()).toBe('/admin');
    }));

    it('should return / when not authenticated', () => {
      expect(service.getRedirectRoute()).toBe('/');
    });
  });

  describe('loadCurrentUserProfile', () => {
    it('should load and update user profile', fakeAsync(() => {
      const mockUser: User = {
        id: 1,
        email: 'test@example.com',
        firstName: 'John',
        lastName: 'Doe',
        role: 'STUDENT',
        chessLevel: 'CAVALIER',
        hasUsedFreeTrial: false
      };

      let result: User | null = null;
      service.loadCurrentUserProfile().subscribe(u => result = u);

      const req = httpMock.expectOne('/api/users/me');
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);

      tick();

      expect(result).toEqual(mockUser);
      expect(service.currentUser()?.chessLevel).toBe('CAVALIER');
    }));
  });

  describe('updateTeacherProfile', () => {
    const updateRequest = {
      bio: 'Experienced chess coach',
      hourlyRateCents: 5000,
      specialties: ['Openings', 'Tactics']
    };

    it('should update teacher profile', fakeAsync(() => {
      const updatedUser: User = {
        id: 2,
        email: 'teacher@example.com',
        firstName: 'Jane',
        lastName: 'Smith',
        role: 'TEACHER',
        bio: 'Experienced chess coach',
        hourlyRateCents: 5000
      };

      let result: User | null = null;
      service.updateTeacherProfile(updateRequest).subscribe(u => result = u);

      const req = httpMock.expectOne('/api/users/me/teacher-profile');
      expect(req.request.method).toBe('PUT');
      req.flush(updatedUser);

      tick();

      expect(result?.bio).toBe('Experienced chess coach');
      expect(service.currentUser()?.hourlyRateCents).toBe(5000);
    }));
  });

  describe('updateCurrentUser', () => {
    it('should update current user and localStorage', () => {
      const updatedUser: User = {
        id: 1,
        email: 'updated@example.com',
        firstName: 'Updated',
        lastName: 'User',
        role: 'STUDENT'
      };

      service.updateCurrentUser(updatedUser);

      expect(service.currentUser()).toEqual(updatedUser);
      const storedUser = JSON.parse(localStorage.getItem('chess_user') || '{}');
      expect(storedUser.email).toBe('updated@example.com');
    });
  });

  describe('Load from Storage', () => {
    it('should load user from localStorage on service creation', () => {
      const storedUser: User = {
        id: 1,
        email: 'stored@example.com',
        firstName: 'Stored',
        lastName: 'User',
        role: 'STUDENT'
      };
      localStorage.setItem('chess_user', JSON.stringify(storedUser));
      localStorage.setItem('chess_token', 'stored_token');

      // Re-create service to test loading from storage
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule],
        providers: [
          AuthService,
          { provide: Router, useValue: routerSpy }
        ]
      });
      const newService = TestBed.inject(AuthService);

      expect(newService.currentUser()?.email).toBe('stored@example.com');
      expect(newService.token()).toBe('stored_token');
      expect(newService.isAuthenticated()).toBeTrue();
    });
  });

  describe('Role Computed Signals', () => {
    it('should correctly identify STUDENT role', fakeAsync(() => {
      service.login({ email: 'student@example.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockAuthResponse);
      tick();

      expect(service.isStudent()).toBeTrue();
      expect(service.isTeacher()).toBeFalse();
      expect(service.isAdmin()).toBeFalse();
    }));

    it('should correctly identify TEACHER role', fakeAsync(() => {
      service.login({ email: 'teacher@example.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/login').flush(mockTeacherAuthResponse);
      tick();

      expect(service.isStudent()).toBeFalse();
      expect(service.isTeacher()).toBeTrue();
      expect(service.isAdmin()).toBeFalse();
    }));

    it('should correctly identify ADMIN role', fakeAsync(() => {
      service.adminLogin({ username: 'admin', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/auth/admin-login').flush(mockAdminAuthResponse);
      tick();

      expect(service.isStudent()).toBeFalse();
      expect(service.isTeacher()).toBeFalse();
      expect(service.isAdmin()).toBeTrue();
    }));
  });
});
