import { Injectable, signal, inject, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { ToastService } from './toast.service';
import { AuthService } from './auth.service';
import { NotificationCenterService } from './notification-center.service';
import { interval, Subscription, forkJoin, of } from 'rxjs';
import { switchMap, catchError, map } from 'rxjs/operators';
import { Availability } from '../models/availability.model';
import { Lesson } from '../models/lesson.model';

interface AvailabilityInfo {
  id: number;
  teacherId: number;
  teacherName: string;
}

interface LessonInfo {
  id: number;
  status: string;
  teacherName: string;
  studentName: string;
  teacherJoinedAt?: string;
}

interface Teacher {
  id: number;
  firstName: string;
  lastName: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {
  private http = inject(HttpClient);
  private toastService = inject(ToastService);
  private authService = inject(AuthService);
  private notificationCenter = inject(NotificationCenterService);
  private translate = inject(TranslateService);

  private pollingSubscription?: Subscription;
  private lastAvailabilities = signal<Map<number, AvailabilityInfo>>(new Map());
  private lastLessons = signal<Map<number, LessonInfo>>(new Map());
  private teachersCache: Teacher[] = [];
  private isPolling = signal<boolean>(false);
  private initialized = false;
  private userRole: 'student' | 'teacher' | null = null;

  readonly polling = this.isPolling.asReadonly();

  private readonly POLL_INTERVAL = 8000; // 8 secondes

  startPolling(): void {
    if (this.isPolling()) {
      return;
    }

    // Determine user role
    if (this.authService.isStudent()) {
      this.userRole = 'student';
      this.startStudentPolling();
    } else if (this.authService.isTeacher()) {
      this.userRole = 'teacher';
      this.startTeacherPolling();
    }
  }

  private startStudentPolling(): void {
    console.log('[Notification] Starting student polling...');
    this.isPolling.set(true);
    this.initialized = false;

    // Fetch teachers first
    this.http.get<Teacher[]>('/api/teachers').pipe(
      catchError(err => {
        console.error('[Notification] Error fetching teachers:', err);
        return of([]);
      })
    ).subscribe(teachers => {
      console.log('[Notification] Found', teachers.length, 'teachers');
      this.teachersCache = teachers;

      // Fetch initial data
      forkJoin({
        availabilities: this.fetchAllAvailabilities(),
        lessons: this.fetchMyLessons()
      }).subscribe(({ availabilities, lessons }) => {
        this.lastAvailabilities.set(availabilities);
        this.lastLessons.set(lessons);
        this.initialized = true;
        console.log('[Notification] Initialized with', availabilities.size, 'availabilities,', lessons.size, 'lessons');
      });

      // Start polling
      this.pollingSubscription = interval(this.POLL_INTERVAL).pipe(
        switchMap(() => {
          return forkJoin({
            availabilities: this.fetchAllAvailabilities(),
            lessons: this.fetchMyLessons()
          });
        })
      ).subscribe(({ availabilities: newAvailabilities, lessons: newLessons }) => {
        if (!this.initialized) return;

        // Check for new availabilities
        this.checkNewAvailabilities(newAvailabilities);

        // Check for lesson status changes (for student)
        this.checkStudentLessonChanges(newLessons);

        this.lastAvailabilities.set(newAvailabilities);
        this.lastLessons.set(newLessons);
      });
    });
  }

  private startTeacherPolling(): void {
    console.log('[Notification] Starting teacher polling...');
    this.isPolling.set(true);
    this.initialized = false;

    // Fetch initial lessons
    this.fetchMyLessons().subscribe(lessons => {
      this.lastLessons.set(lessons);
      this.initialized = true;
      console.log('[Notification] Teacher initialized with', lessons.size, 'lessons');
    });

    // Start polling for new reservations
    this.pollingSubscription = interval(this.POLL_INTERVAL).pipe(
      switchMap(() => this.fetchMyLessons())
    ).subscribe(newLessons => {
      if (!this.initialized) return;

      // Check for new reservations (new PENDING lessons)
      this.checkTeacherNewReservations(newLessons);

      // Check for cancelled lessons by student
      this.checkTeacherLessonChanges(newLessons);

      this.lastLessons.set(newLessons);
    });
  }

  private checkNewAvailabilities(newAvailabilities: Map<number, AvailabilityInfo>): void {
    const previousMap = this.lastAvailabilities();

    const newOnes: AvailabilityInfo[] = [];
    newAvailabilities.forEach((info, id) => {
      if (!previousMap.has(id)) {
        console.log('[Notification] NEW availability:', id, info.teacherName);
        newOnes.push(info);
      }
    });

    if (newOnes.length > 0) {
      const byTeacher = new Map<string, { count: number; teacherId: number }>();
      newOnes.forEach(a => {
        const existing = byTeacher.get(a.teacherName);
        if (existing) {
          existing.count++;
        } else {
          byTeacher.set(a.teacherName, { count: 1, teacherId: a.teacherId });
        }
      });

      byTeacher.forEach(({ teacherId }, teacherName) => {
        const message = this.translate.instant('realTimeNotifications.newAvailabilityMsg', { name: teacherName });
        const link = `/book/${teacherId}`;
        this.toastService.info(message, 8000, link);
        this.notificationCenter.info(this.translate.instant('realTimeNotifications.newAvailability'), message, link);
      });
    }
  }

  private checkStudentLessonChanges(newLessons: Map<number, LessonInfo>): void {
    const previousMap = this.lastLessons();

    newLessons.forEach((newLesson, id) => {
      const previousLesson = previousMap.get(id);

      if (previousLesson && previousLesson.status !== newLesson.status) {
        console.log('[Notification] Lesson status changed:', id, previousLesson.status, '->', newLesson.status);

        if (newLesson.status === 'CONFIRMED') {
          const message = this.translate.instant('realTimeNotifications.lessonConfirmedMsg', { name: newLesson.teacherName });
          this.toastService.success(message, 8000, '/lessons');
          this.notificationCenter.success(this.translate.instant('realTimeNotifications.lessonConfirmed'), message, '/lessons');
        } else if (newLesson.status === 'CANCELLED') {
          const message = this.translate.instant('realTimeNotifications.lessonCancelledByCoach', { name: newLesson.teacherName });
          this.toastService.error(message, 10000, '/lessons');
          this.notificationCenter.error(this.translate.instant('realTimeNotifications.lessonCancelled'), message, '/lessons');
        }
      }

      // Check if teacher just joined the video call
      if (previousLesson && !previousLesson.teacherJoinedAt && newLesson.teacherJoinedAt) {
        console.log('[Notification] Teacher joined video call for lesson:', id);
        const message = this.translate.instant('realTimeNotifications.videoCallMsg', { name: newLesson.teacherName });
        this.toastService.success(message, 15000, `/lessons?openCall=${id}`);
        this.notificationCenter.success(this.translate.instant('realTimeNotifications.videoCall'), message, `/lessons?openCall=${id}`);
      }
    });
  }

  private checkTeacherNewReservations(newLessons: Map<number, LessonInfo>): void {
    const previousMap = this.lastLessons();

    // Find new lessons that weren't in the previous list
    const newReservations: LessonInfo[] = [];
    newLessons.forEach((lesson, id) => {
      if (!previousMap.has(id)) {
        newReservations.push(lesson);
      }
    });

    // Show notification for each new reservation
    newReservations.forEach(lesson => {
      console.log('[Notification] New reservation from:', lesson.studentName);
      const message = this.translate.instant('realTimeNotifications.newBookingMsg', { name: lesson.studentName });
      this.toastService.info(message, 8000, '/lessons');
      this.notificationCenter.info(this.translate.instant('realTimeNotifications.newBooking'), message, '/lessons');
    });
  }

  private checkTeacherLessonChanges(newLessons: Map<number, LessonInfo>): void {
    const previousMap = this.lastLessons();

    newLessons.forEach((newLesson, id) => {
      const previousLesson = previousMap.get(id);

      if (previousLesson && previousLesson.status !== newLesson.status) {
        console.log('[Notification] Teacher lesson status changed:', id, previousLesson.status, '->', newLesson.status);

        if (newLesson.status === 'CANCELLED') {
          const message = this.translate.instant('realTimeNotifications.lessonCancelledByPlayer', { name: newLesson.studentName });
          this.toastService.error(message, 10000, '/lessons');
          this.notificationCenter.error(this.translate.instant('realTimeNotifications.lessonCancelled'), message, '/lessons');
        }
      }
    });
  }

  stopPolling(): void {
    if (!this.isPolling()) {
      return;
    }
    console.log('[Notification] Stopping polling');
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = undefined;
    this.isPolling.set(false);
    this.initialized = false;
    this.userRole = null;
    this.lastLessons.set(new Map());
    this.lastAvailabilities.set(new Map());
    this.teachersCache = []; // Clear teachers cache to prevent stale data
  }

  private fetchAllAvailabilities() {
    if (this.teachersCache.length === 0) {
      return of(new Map<number, AvailabilityInfo>());
    }

    const requests = this.teachersCache.map(teacher =>
      this.http.get<Availability[]>(`/api/availabilities/teacher/${teacher.id}`).pipe(
        map(availabilities => ({
          teacher,
          availabilities
        })),
        catchError(() => of({ teacher, availabilities: [] as Availability[] }))
      )
    );

    return forkJoin(requests).pipe(
      map(results => {
        const allAvailabilities = new Map<number, AvailabilityInfo>();
        results.forEach(({ teacher, availabilities }) => {
          availabilities.forEach(a => {
            allAvailabilities.set(a.id, {
              id: a.id,
              teacherId: teacher.id,
              teacherName: teacher.lastName
            });
          });
        });
        return allAvailabilities;
      }),
      catchError(() => of(this.lastAvailabilities()))
    );
  }

  private fetchMyLessons() {
    return this.http.get<Lesson[]>('/api/lessons/upcoming').pipe(
      map(lessons => {
        const lessonsMap = new Map<number, LessonInfo>();
        lessons.forEach(l => {
          lessonsMap.set(l.id, {
            id: l.id,
            status: l.status,
            teacherName: l.teacherName?.split(' ').pop() || this.translate.instant('realTimeNotifications.coach'), // Get last name
            studentName: l.studentName || this.translate.instant('realTimeNotifications.player'),
            teacherJoinedAt: l.teacherJoinedAt || undefined
          });
        });
        return lessonsMap;
      }),
      catchError(() => of(this.lastLessons()))
    );
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }
}
