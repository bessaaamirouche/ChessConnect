import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { LessonService } from './lesson.service';
import { Lesson, BookLessonRequest } from '../models/lesson.model';

describe('LessonService', () => {
  let service: LessonService;
  let httpMock: HttpTestingController;

  const mockLesson: Lesson = {
    id: 1,
    studentId: 1,
    studentName: 'John Doe',
    teacherId: 2,
    teacherName: 'Coach Smith',
    scheduledAt: '2024-02-01T14:00:00',
    durationMinutes: 60,
    status: 'PENDING',
    priceCents: 5000,
    createdAt: '2024-01-01T00:00:00'
  };

  const mockUpcomingLessons: Lesson[] = [
    mockLesson,
    {
      ...mockLesson,
      id: 2,
      status: 'CONFIRMED',
      scheduledAt: '2024-02-02T10:00:00'
    }
  ];

  const mockHistoryLessons: Lesson[] = [
    {
      ...mockLesson,
      id: 10,
      status: 'COMPLETED',
      scheduledAt: '2024-01-15T14:00:00'
    },
    {
      ...mockLesson,
      id: 11,
      status: 'CANCELLED',
      scheduledAt: '2024-01-10T14:00:00',
      cancellationReason: 'Student cancelled'
    }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [LessonService]
    });

    service = TestBed.inject(LessonService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Initial State', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should have empty upcoming lessons initially', () => {
      expect(service.upcomingLessons()).toEqual([]);
    });

    it('should have empty lesson history initially', () => {
      expect(service.lessonHistory()).toEqual([]);
    });

    it('should have loading as false initially', () => {
      expect(service.loading()).toBeFalse();
    });

    it('should have error as null initially', () => {
      expect(service.error()).toBeNull();
    });

    it('should have freeTrialEligible as false initially', () => {
      expect(service.freeTrialEligible()).toBeFalse();
    });

    it('should have upcomingCount as 0 initially', () => {
      expect(service.upcomingCount()).toBe(0);
    });

    it('should have completedCount as 0 initially', () => {
      expect(service.completedCount()).toBe(0);
    });
  });

  describe('bookLesson', () => {
    const bookRequest: BookLessonRequest = {
      teacherId: 2,
      scheduledAt: '2024-02-01T14:00:00',
      durationMinutes: 60,
      notes: 'First lesson'
    };

    it('should book lesson successfully', fakeAsync(() => {
      let result: Lesson | null = null;
      service.bookLesson(bookRequest).subscribe(l => result = l);

      expect(service.loading()).toBeTrue();

      const req = httpMock.expectOne('/api/lessons/book');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(bookRequest);
      req.flush(mockLesson);

      tick();

      expect(result).toEqual(mockLesson);
      expect(service.loading()).toBeFalse();
      expect(service.upcomingLessons()).toContain(mockLesson);
    }));

    it('should handle booking error', fakeAsync(() => {
      let error: any;
      service.bookLesson(bookRequest).subscribe({
        error: e => error = e
      });

      const req = httpMock.expectOne('/api/lessons/book');
      req.flush({ error: 'Slot no longer available' }, { status: 400, statusText: 'Bad Request' });

      tick();

      expect(service.error()).toBe('Slot no longer available');
      expect(service.loading()).toBeFalse();
    }));
  });

  describe('loadUpcomingLessons', () => {
    it('should load upcoming lessons', fakeAsync(() => {
      let result: Lesson[] = [];
      service.loadUpcomingLessons().subscribe(l => result = l);

      expect(service.loading()).toBeTrue();

      const req = httpMock.expectOne('/api/lessons/upcoming');
      expect(req.request.method).toBe('GET');
      req.flush(mockUpcomingLessons);

      tick();

      expect(result).toHaveLength(2);
      expect(service.upcomingLessons()).toEqual(mockUpcomingLessons);
      expect(service.loading()).toBeFalse();
    }));

    it('should calculate upcomingCount correctly', fakeAsync(() => {
      service.loadUpcomingLessons().subscribe();
      httpMock.expectOne('/api/lessons/upcoming').flush(mockUpcomingLessons);
      tick();

      // Both PENDING and CONFIRMED count
      expect(service.upcomingCount()).toBe(2);
    }));

    it('should exclude cancelled and completed from upcomingCount', fakeAsync(() => {
      const lessonsWithCancelled = [
        ...mockUpcomingLessons,
        { ...mockLesson, id: 3, status: 'CANCELLED' as const },
        { ...mockLesson, id: 4, status: 'COMPLETED' as const }
      ];

      service.loadUpcomingLessons().subscribe();
      httpMock.expectOne('/api/lessons/upcoming').flush(lessonsWithCancelled);
      tick();

      expect(service.upcomingCount()).toBe(2);
    }));
  });

  describe('loadLessonHistory', () => {
    it('should load lesson history', fakeAsync(() => {
      let result: Lesson[] = [];
      service.loadLessonHistory().subscribe(l => result = l);

      expect(service.loading()).toBeTrue();

      const req = httpMock.expectOne('/api/lessons/history');
      expect(req.request.method).toBe('GET');
      req.flush(mockHistoryLessons);

      tick();

      expect(result).toHaveLength(2);
      expect(service.lessonHistory()).toEqual(mockHistoryLessons);
      expect(service.loading()).toBeFalse();
    }));

    it('should calculate completedCount correctly', fakeAsync(() => {
      service.loadLessonHistory().subscribe();
      httpMock.expectOne('/api/lessons/history').flush(mockHistoryLessons);
      tick();

      // Only COMPLETED counts
      expect(service.completedCount()).toBe(1);
    }));
  });

  describe('getLesson', () => {
    it('should get lesson by ID', fakeAsync(() => {
      let result: Lesson | null = null;
      service.getLesson(1).subscribe(l => result = l);

      const req = httpMock.expectOne('/api/lessons/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockLesson);

      tick();

      expect(result).toEqual(mockLesson);
    }));
  });

  describe('confirmLesson', () => {
    it('should confirm lesson', fakeAsync(() => {
      // First load lessons
      service.loadUpcomingLessons().subscribe();
      httpMock.expectOne('/api/lessons/upcoming').flush(mockUpcomingLessons);
      tick();

      const confirmedLesson = { ...mockLesson, status: 'CONFIRMED' as const };
      let result: Lesson | null = null;
      service.confirmLesson(1).subscribe(l => result = l);

      const req = httpMock.expectOne('/api/lessons/1/status');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ status: 'CONFIRMED' });
      req.flush(confirmedLesson);

      tick();

      expect(result?.status).toBe('CONFIRMED');
    }));
  });

  describe('cancelLesson', () => {
    it('should cancel lesson with reason', fakeAsync(() => {
      // First load lessons
      service.loadUpcomingLessons().subscribe();
      httpMock.expectOne('/api/lessons/upcoming').flush([mockLesson]);
      tick();

      const cancelledLesson = { ...mockLesson, status: 'CANCELLED' as const, cancellationReason: 'Schedule conflict' };
      let result: Lesson | null = null;
      service.cancelLesson(1, 'Schedule conflict').subscribe(l => result = l);

      const req = httpMock.expectOne('/api/lessons/1/status');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body.cancellationReason).toBe('Schedule conflict');
      req.flush(cancelledLesson);

      tick();

      expect(result?.status).toBe('CANCELLED');
      // Should be removed from upcoming
      expect(service.upcomingLessons().find(l => l.id === 1)).toBeUndefined();
      // Should be added to history
      expect(service.lessonHistory().find(l => l.id === 1)).toBeTruthy();
    }));
  });

  describe('completeLesson', () => {
    it('should complete lesson with observations', fakeAsync(() => {
      service.loadUpcomingLessons().subscribe();
      httpMock.expectOne('/api/lessons/upcoming').flush([{ ...mockLesson, status: 'CONFIRMED' }]);
      tick();

      const completedLesson = {
        ...mockLesson,
        status: 'COMPLETED' as const,
        teacherObservations: 'Good progress'
      };

      let result: Lesson | null = null;
      service.completeLesson(1, 'Good progress').subscribe(l => result = l);

      const req = httpMock.expectOne('/api/lessons/1/status');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body.teacherObservations).toBe('Good progress');
      req.flush(completedLesson);

      tick();

      expect(result?.status).toBe('COMPLETED');
      expect(service.upcomingLessons().find(l => l.id === 1)).toBeUndefined();
      expect(service.lessonHistory().find(l => l.id === 1)).toBeTruthy();
    }));
  });

  describe('deleteLesson', () => {
    it('should delete lesson from history', fakeAsync(() => {
      service.loadLessonHistory().subscribe();
      httpMock.expectOne('/api/lessons/history').flush(mockHistoryLessons);
      tick();

      expect(service.lessonHistory().find(l => l.id === 10)).toBeTruthy();

      service.deleteLesson(10).subscribe();
      httpMock.expectOne('/api/lessons/10').flush(null);
      tick();

      expect(service.lessonHistory().find(l => l.id === 10)).toBeUndefined();
    }));

    it('should delete lesson from upcoming', fakeAsync(() => {
      service.loadUpcomingLessons().subscribe();
      httpMock.expectOne('/api/lessons/upcoming').flush(mockUpcomingLessons);
      tick();

      service.deleteLesson(1).subscribe();
      httpMock.expectOne('/api/lessons/1').flush(null);
      tick();

      expect(service.upcomingLessons().find(l => l.id === 1)).toBeUndefined();
    }));
  });

  describe('Free Trial', () => {
    describe('checkFreeTrialEligibility', () => {
      it('should check eligibility and return true', fakeAsync(() => {
        let result: boolean | null = null;
        service.checkFreeTrialEligibility().subscribe(r => result = r);

        const req = httpMock.expectOne('/api/lessons/free-trial/eligible');
        expect(req.request.method).toBe('GET');
        req.flush({ eligible: true });

        tick();

        expect(result).toBeTrue();
        expect(service.freeTrialEligible()).toBeTrue();
      }));

      it('should return false when not eligible', fakeAsync(() => {
        let result: boolean | null = null;
        service.checkFreeTrialEligibility().subscribe(r => result = r);

        httpMock.expectOne('/api/lessons/free-trial/eligible').flush({ eligible: false });
        tick();

        expect(result).toBeFalse();
        expect(service.freeTrialEligible()).toBeFalse();
      }));
    });

    describe('bookFreeTrialLesson', () => {
      const bookRequest: BookLessonRequest = {
        teacherId: 2,
        scheduledAt: '2024-02-01T14:00:00',
        durationMinutes: 60
      };

      it('should book free trial lesson', fakeAsync(() => {
        const freeLesson = { ...mockLesson, priceCents: 0 };
        let result: Lesson | null = null;

        service.bookFreeTrialLesson(bookRequest).subscribe(l => result = l);

        expect(service.loading()).toBeTrue();

        const req = httpMock.expectOne('/api/lessons/free-trial/book');
        expect(req.request.method).toBe('POST');
        req.flush(freeLesson);

        tick();

        expect(result?.priceCents).toBe(0);
        expect(service.freeTrialEligible()).toBeFalse();
        expect(service.upcomingLessons()).toContain(freeLesson);
      }));

      it('should set freeTrialEligible to false after booking', fakeAsync(() => {
        // First set eligible to true
        service.checkFreeTrialEligibility().subscribe();
        httpMock.expectOne('/api/lessons/free-trial/eligible').flush({ eligible: true });
        tick();

        expect(service.freeTrialEligible()).toBeTrue();

        // Then book free trial
        service.bookFreeTrialLesson(bookRequest).subscribe();
        httpMock.expectOne('/api/lessons/free-trial/book').flush(mockLesson);
        tick();

        expect(service.freeTrialEligible()).toBeFalse();
      }));
    });
  });

  describe('Teacher Joined', () => {
    it('should mark teacher joined', fakeAsync(() => {
      service.loadUpcomingLessons().subscribe();
      httpMock.expectOne('/api/lessons/upcoming').flush([mockLesson]);
      tick();

      const updatedLesson = { ...mockLesson, teacherJoined: true };
      service.markTeacherJoined(1).subscribe();

      const req = httpMock.expectOne('/api/lessons/1/teacher-joined');
      expect(req.request.method).toBe('PATCH');
      req.flush(updatedLesson);

      tick();

      expect(service.upcomingLessons().find(l => l.id === 1)?.teacherJoined).toBeTrue();
    }));

    it('should check if teacher joined', fakeAsync(() => {
      let result: boolean | null = null;
      service.checkTeacherJoined(1).subscribe(r => result = r);

      const req = httpMock.expectOne('/api/lessons/1/teacher-joined');
      expect(req.request.method).toBe('GET');
      req.flush({ teacherJoined: true });

      tick();

      expect(result).toBeTrue();
    }));
  });

  describe('refreshLesson', () => {
    it('should refresh lesson and update state', fakeAsync(() => {
      service.loadUpcomingLessons().subscribe();
      httpMock.expectOne('/api/lessons/upcoming').flush([mockLesson]);
      tick();

      const updatedLesson = { ...mockLesson, status: 'CONFIRMED' as const };
      service.refreshLesson(1).subscribe();

      const req = httpMock.expectOne('/api/lessons/1');
      expect(req.request.method).toBe('GET');
      req.flush(updatedLesson);

      tick();

      expect(service.upcomingLessons().find(l => l.id === 1)?.status).toBe('CONFIRMED');
    }));
  });

  describe('clearError', () => {
    it('should clear error', fakeAsync(() => {
      // Cause an error first
      service.bookLesson({ teacherId: 1, scheduledAt: '2024-01-01', durationMinutes: 60 }).subscribe({
        error: () => {}
      });
      httpMock.expectOne('/api/lessons/book').flush({ error: 'Error' }, { status: 400, statusText: 'Error' });
      tick();

      expect(service.error()).toBeTruthy();

      service.clearError();
      expect(service.error()).toBeNull();
    }));
  });

  describe('API Endpoint URLs', () => {
    it('should use correct URL for booking', () => {
      service.bookLesson({ teacherId: 1, scheduledAt: '2024-01-01', durationMinutes: 60 }).subscribe({ error: () => {} });
      const req = httpMock.expectOne('/api/lessons/book');
      expect(req.request.url).toBe('/api/lessons/book');
      req.flush(mockLesson);
    });

    it('should use correct URL for upcoming lessons', () => {
      service.loadUpcomingLessons().subscribe();
      const req = httpMock.expectOne('/api/lessons/upcoming');
      expect(req.request.url).toBe('/api/lessons/upcoming');
      req.flush([]);
    });

    it('should use correct URL for history', () => {
      service.loadLessonHistory().subscribe();
      const req = httpMock.expectOne('/api/lessons/history');
      expect(req.request.url).toBe('/api/lessons/history');
      req.flush([]);
    });

    it('should use correct URL for free trial eligibility', () => {
      service.checkFreeTrialEligibility().subscribe();
      const req = httpMock.expectOne('/api/lessons/free-trial/eligible');
      expect(req.request.url).toBe('/api/lessons/free-trial/eligible');
      req.flush({ eligible: true });
    });
  });
});
