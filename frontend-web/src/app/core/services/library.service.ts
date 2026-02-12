import { Injectable, signal, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

export interface Video {
  id: number;
  lessonId: number;
  teacherName: string;
  teacherAvatar: string | null;
  scheduledAt: string;
  recordingUrl: string;
  durationSeconds: number;
  thumbnailUrl: string;
  courseTitle?: string;
}

export interface LibraryFilters {
  search?: string;
  period?: 'week' | 'month' | '3months' | 'year' | '';
  dateFrom?: string;
  dateTo?: string;
}

@Injectable({
  providedIn: 'root'
})
export class LibraryService {
  private readonly apiUrl = '/api/library';

  private videosSignal = signal<Video[]>([]);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);

  readonly videos = this.videosSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  private translate = inject(TranslateService);

  constructor(private http: HttpClient) {}

  loadVideos(filters?: LibraryFilters): Observable<Video[]> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    let params = new HttpParams();
    if (filters) {
      if (filters.search) {
        params = params.set('search', filters.search);
      }
      if (filters.period) {
        params = params.set('period', filters.period);
      }
      if (filters.dateFrom) {
        params = params.set('dateFrom', filters.dateFrom);
      }
      if (filters.dateTo) {
        params = params.set('dateTo', filters.dateTo);
      }
    }

    return this.http.get<Video[]>(`${this.apiUrl}/videos`, { params }).pipe(
      tap({
        next: (videos) => {
          this.videosSignal.set(videos);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(err.error?.message || this.translate.instant('errors.videoLoad'));
          this.loadingSignal.set(false);
        }
      })
    );
  }

  deleteVideo(lessonId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/videos/${lessonId}`).pipe(
      tap(() => {
        this.videosSignal.update(videos => videos.filter(v => v.lessonId !== lessonId));
      })
    );
  }

  clearError(): void {
    this.errorSignal.set(null);
  }
}
