import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface Rating {
  id: number;
  lessonId: number;
  studentId: number;
  studentFirstName: string;
  studentLastName: string;
  teacherId: number;
  teacherFirstName: string;
  teacherLastName: string;
  stars: number;
  comment?: string;
  createdAt: string;
}

export interface CreateRatingRequest {
  lessonId: number;
  stars: number;
  comment?: string;
}

export interface RatingSummary {
  averageRating: number;
  reviewCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class RatingService {
  private readonly apiUrl = '/api/ratings';

  teacherRatings = signal<Rating[]>([]);
  ratingSummary = signal<RatingSummary | null>(null);

  constructor(private http: HttpClient) {}

  createRating(request: CreateRatingRequest): Observable<Rating> {
    return this.http.post<Rating>(this.apiUrl, request);
  }

  getTeacherRatings(teacherId: number): Observable<Rating[]> {
    return this.http.get<Rating[]>(`${this.apiUrl}/teacher/${teacherId}`).pipe(
      tap(ratings => this.teacherRatings.set(ratings))
    );
  }

  getLessonRating(lessonId: number): Observable<{ isRated: boolean; rating: Rating | null }> {
    return this.http.get<{ isRated: boolean; rating: Rating | null }>(`${this.apiUrl}/lesson/${lessonId}`);
  }

  getTeacherRatingSummary(teacherId: number): Observable<RatingSummary> {
    return this.http.get<RatingSummary>(`${this.apiUrl}/teacher/${teacherId}/summary`).pipe(
      tap(summary => this.ratingSummary.set(summary))
    );
  }
}
