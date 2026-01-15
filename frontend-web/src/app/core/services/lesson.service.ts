import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Lesson, BookLessonRequest, UpdateLessonStatusRequest } from '../models/lesson.model';

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

  readonly upcomingCount = computed(() => this.upcomingLessonsSignal().length);
  readonly completedCount = computed(() =>
    this.lessonHistorySignal().filter(l => l.status === 'COMPLETED').length
  );

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
          this.errorSignal.set(err.error?.message || 'Erreur lors de la réservation');
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
    return this.updateLessonStatus(lessonId, {
      status: 'CANCELLED',
      cancellationReason: reason
    });
  }

  completeLesson(lessonId: number, teacherObservations?: string): Observable<Lesson> {
    return this.updateLessonStatus(lessonId, {
      status: 'COMPLETED',
      teacherObservations: teacherObservations || undefined
    });
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
}
