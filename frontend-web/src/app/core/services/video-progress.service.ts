import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, catchError, tap } from 'rxjs';

export interface VideoProgress {
  lessonId: number;
  watchPosition: number;
  duration: number;
  completed: boolean;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class VideoProgressService {
  private http = inject(HttpClient);
  private readonly apiUrl = '/api/video-progress';

  getProgress(lessonId: number): Observable<VideoProgress> {
    console.log('[VideoProgress] Getting progress for lesson:', lessonId);
    return this.http.get<VideoProgress>(`${this.apiUrl}/${lessonId}`).pipe(
      tap(response => console.log('[VideoProgress] Get success:', response)),
      catchError(error => {
        console.error('[VideoProgress] Get error:', error);
        return of({
          lessonId,
          watchPosition: 0,
          duration: 0,
          completed: false
        });
      })
    );
  }

  saveProgress(lessonId: number, watchPosition: number, duration?: number, completed?: boolean): Observable<any> {
    console.log('[VideoProgress] Saving progress:', { lessonId, watchPosition, duration, completed });
    return this.http.post(`${this.apiUrl}/${lessonId}`, {
      watchPosition,
      duration,
      completed: completed ?? false
    }).pipe(
      tap(response => console.log('[VideoProgress] Save success:', response)),
      catchError(error => {
        console.error('[VideoProgress] Save error:', error);
        return of({ success: false });
      })
    );
  }

  deleteProgress(lessonId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${lessonId}`).pipe(
      catchError(() => of({ success: false }))
    );
  }
}
