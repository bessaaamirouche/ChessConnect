import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface FavoriteTeacher {
  id: number;
  teacherId: number;
  teacherFirstName: string;
  teacherLastName: string;
  teacherEmail: string;
  teacherHourlyRateCents: number;
  teacherBio?: string;
  teacherAvatarUrl?: string;
  teacherLanguages: string[];
  notifyNewSlots: boolean;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class FavoriteService {
  private readonly apiUrl = '/api/favorites';

  favorites = signal<FavoriteTeacher[]>([]);
  favoriteTeacherIds = signal<Set<number>>(new Set());

  constructor(private http: HttpClient) {}

  loadFavorites(): Observable<FavoriteTeacher[]> {
    return this.http.get<FavoriteTeacher[]>(this.apiUrl).pipe(
      tap(favorites => {
        this.favorites.set(favorites);
        this.favoriteTeacherIds.set(new Set(favorites.map(f => f.teacherId)));
      })
    );
  }

  addFavorite(teacherId: number): Observable<FavoriteTeacher> {
    return this.http.post<FavoriteTeacher>(`${this.apiUrl}/${teacherId}`, {}).pipe(
      tap(favorite => {
        this.favorites.update(favs => [...favs, favorite]);
        this.favoriteTeacherIds.update(ids => {
          const newIds = new Set(ids);
          newIds.add(teacherId);
          return newIds;
        });
      })
    );
  }

  removeFavorite(teacherId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${teacherId}`).pipe(
      tap(() => {
        this.favorites.update(favs => favs.filter(f => f.teacherId !== teacherId));
        this.favoriteTeacherIds.update(ids => {
          const newIds = new Set(ids);
          newIds.delete(teacherId);
          return newIds;
        });
      })
    );
  }

  isFavorite(teacherId: number): boolean {
    return this.favoriteTeacherIds().has(teacherId);
  }

  getFavoriteStatus(teacherId: number): Observable<{ isFavorite: boolean }> {
    return this.http.get<{ isFavorite: boolean }>(`${this.apiUrl}/${teacherId}/status`);
  }

  updateNotifications(teacherId: number, notify: boolean): Observable<FavoriteTeacher> {
    return this.http.patch<FavoriteTeacher>(`${this.apiUrl}/${teacherId}/notify`, {
      notifyNewSlots: notify
    }).pipe(
      tap(updated => {
        this.favorites.update(favs =>
          favs.map(f => f.teacherId === teacherId ? updated : f)
        );
      })
    );
  }
}
