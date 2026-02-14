import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map } from 'rxjs';
import { Lesson, BookLessonRequest, UpdateLessonStatusRequest } from '../models/lesson.model';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root'
})
export class LessonService {
  private readonly apiUrl = '/api/lessons';

  private upcomingLessonsSignal = signal<Lesson[]>([]);
  private lessonHistorySignal = signal<Lesson[]>([]);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);

  readonly upcomingLessons = this.upcomingLessonsSignal.asReadonly();
  readonly lessonHistory = this.lessonHistorySignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly upcomingCount = computed(() =>
    this.upcomingLessonsSignal().filter(l => l.status !== 'CANCELLED' && l.status !== 'COMPLETED').length
  );
  readonly completedCount = computed(() =>
    this.lessonHistorySignal().filter(l => l.status === 'COMPLETED').length
  );

  private translate = inject(TranslateService);

  constructor(private http: HttpClient) {}

  bookLesson(request: BookLessonRequest): Observable<Lesson> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.post<Lesson>(`${this.apiUrl}/book`, request).pipe(
      tap({
        next: (lesson) => {
          this.upcomingLessonsSignal.update(lessons => [lesson, ...lessons]);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(this.translate.instant(err.error?.error || err.error?.message || 'errors.bookingCreate'));
          this.loadingSignal.set(false);
        }
      })
    );
  }

  loadUpcomingLessons(): Observable<Lesson[]> {
    this.loadingSignal.set(true);

    return this.http.get<Lesson[]>(`${this.apiUrl}/upcoming`).pipe(
      tap({
        next: (lessons) => {
          this.upcomingLessonsSignal.set(lessons);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  loadLessonHistory(): Observable<Lesson[]> {
    this.loadingSignal.set(true);

    return this.http.get<Lesson[]>(`${this.apiUrl}/history`).pipe(
      tap({
        next: (lessons) => {
          this.lessonHistorySignal.set(lessons);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  getLesson(lessonId: number): Observable<Lesson> {
    return this.http.get<Lesson>(`${this.apiUrl}/${lessonId}`);
  }

  updateLessonStatus(lessonId: number, request: UpdateLessonStatusRequest): Observable<Lesson> {
    return this.http.patch<Lesson>(`${this.apiUrl}/${lessonId}/status`, request).pipe(
      tap((updatedLesson) => {
        this.upcomingLessonsSignal.update(lessons =>
          lessons.map(l => l.id === lessonId ? updatedLesson : l)
        );
        this.lessonHistorySignal.update(lessons =>
          lessons.map(l => l.id === lessonId ? updatedLesson : l)
        );
      })
    );
  }

  confirmLesson(lessonId: number): Observable<Lesson> {
    return this.updateLessonStatus(lessonId, { status: 'CONFIRMED' });
  }

  cancelLesson(lessonId: number, reason?: string): Observable<Lesson> {
    return this.http.patch<Lesson>(`${this.apiUrl}/${lessonId}/status`, {
      status: 'CANCELLED',
      cancellationReason: reason
    }).pipe(
      tap((updatedLesson) => {
        // Remove from upcoming and add to history
        this.upcomingLessonsSignal.update(lessons =>
          lessons.filter(l => l.id !== lessonId)
        );
        this.lessonHistorySignal.update(lessons => {
          // Check if already in history
          const exists = lessons.some(l => l.id === lessonId);
          if (exists) {
            return lessons.map(l => l.id === lessonId ? updatedLesson : l);
          }
          return [updatedLesson, ...lessons];
        });
      })
    );
  }

  completeLesson(lessonId: number, teacherObservations?: string): Observable<Lesson> {
    return this.http.patch<Lesson>(`${this.apiUrl}/${lessonId}/status`, {
      status: 'COMPLETED',
      teacherObservations: teacherObservations || undefined
    }).pipe(
      tap((updatedLesson) => {
        // Remove from upcoming and add to history
        this.upcomingLessonsSignal.update(lessons =>
          lessons.filter(l => l.id !== lessonId)
        );
        this.lessonHistorySignal.update(lessons => {
          // Check if already in history
          const exists = lessons.some(l => l.id === lessonId);
          if (exists) {
            return lessons.map(l => l.id === lessonId ? updatedLesson : l);
          }
          return [updatedLesson, ...lessons];
        });
      })
    );
  }

  deleteLesson(lessonId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${lessonId}`).pipe(
      tap(() => {
        this.lessonHistorySignal.update(lessons =>
          lessons.filter(l => l.id !== lessonId)
        );
        this.upcomingLessonsSignal.update(lessons =>
          lessons.filter(l => l.id !== lessonId)
        );
      })
    );
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  /**
   * Mark that the teacher has joined the video call
   */
  markTeacherJoined(lessonId: number): Observable<Lesson> {
    return this.http.patch<Lesson>(`${this.apiUrl}/${lessonId}/teacher-joined`, {}).pipe(
      tap((updatedLesson) => {
        this.upcomingLessonsSignal.update(lessons =>
          lessons.map(l => l.id === lessonId ? updatedLesson : l)
        );
      })
    );
  }

  /**
   * Check if the teacher has joined the video call
   */
  checkTeacherJoined(lessonId: number): Observable<boolean> {
    return this.http.get<{ teacherJoined: boolean }>(`${this.apiUrl}/${lessonId}/teacher-joined`).pipe(
      map(response => response.teacherJoined)
    );
  }

  /**
   * Refresh a specific lesson from the server
   */
  refreshLesson(lessonId: number): Observable<Lesson> {
    return this.http.get<Lesson>(`${this.apiUrl}/${lessonId}`).pipe(
      tap((lesson) => {
        this.upcomingLessonsSignal.update(lessons =>
          lessons.map(l => l.id === lessonId ? lesson : l)
        );
      })
    );
  }

  /**
   * Update teacher comment on a lesson (teachers only)
   */
  updateTeacherComment(lessonId: number, comment: string): Observable<Lesson> {
    return this.http.patch<Lesson>(`${this.apiUrl}/${lessonId}/comment`, { comment }).pipe(
      tap((updatedLesson) => {
        this.lessonHistorySignal.update(lessons =>
          lessons.map(l => l.id === lessonId ? updatedLesson : l)
        );
      })
    );
  }
}
