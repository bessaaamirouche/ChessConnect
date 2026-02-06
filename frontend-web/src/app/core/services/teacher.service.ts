import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { User, TeacherBalance } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class TeacherService {
  private readonly apiUrl = '/api/teachers';

  private teachersSignal = signal<User[]>([]);
  private loadingSignal = signal<boolean>(false);
  private selectedTeacherSignal = signal<User | null>(null);
  private balanceSignal = signal<TeacherBalance | null>(null);

  readonly teachers = this.teachersSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly selectedTeacher = this.selectedTeacherSignal.asReadonly();
  readonly balance = this.balanceSignal.asReadonly();

  constructor(private http: HttpClient) {}

  loadTeachers(): Observable<User[]> {
    this.loadingSignal.set(true);

    return this.http.get<User[]>(this.apiUrl).pipe(
      tap({
        next: (teachers) => {
          this.teachersSignal.set(teachers);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  getTeacher(teacherId: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${teacherId}`).pipe(
      tap(teacher => this.selectedTeacherSignal.set(teacher))
    );
  }

  getTeacherByUuid(uuid: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/uuid/${uuid}`).pipe(
      tap(teacher => this.selectedTeacherSignal.set(teacher))
    );
  }

  searchTeachers(query: string): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/search`, {
      params: { q: query }
    });
  }

  getTeachersBySubscription(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/subscription`);
  }

  loadTeachersAcceptingFreeTrial(): Observable<User[]> {
    this.loadingSignal.set(true);

    return this.http.get<User[]>(`${this.apiUrl}/free-trial`).pipe(
      tap({
        next: (teachers) => {
          this.teachersSignal.set(teachers);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  clearSelectedTeacher(): void {
    this.selectedTeacherSignal.set(null);
  }

  setSelectedTeacher(teacher: User): void {
    this.selectedTeacherSignal.set(teacher);
  }

  getMyBalance(): Observable<TeacherBalance> {
    return this.http.get<TeacherBalance>(`${this.apiUrl}/me/balance`).pipe(
      tap(balance => this.balanceSignal.set(balance))
    );
  }
}
