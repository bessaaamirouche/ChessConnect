import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap, catchError } from 'rxjs';
import { Availability, AvailabilityRequest, TimeSlot } from '../models/availability.model';

@Injectable({
  providedIn: 'root'
})
export class AvailabilityService {
  private readonly apiUrl = '/api/availabilities';

  private myAvailabilitiesSignal = signal<Availability[]>([]);
  private teacherSlotsSignal = signal<TimeSlot[]>([]);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);

  readonly myAvailabilities = this.myAvailabilitiesSignal.asReadonly();
  readonly teacherSlots = this.teacherSlotsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly availableSlots = () => this.teacherSlotsSignal().filter(s => s.isAvailable);

  constructor(private http: HttpClient) {}

  // Teacher: Create availability
  createAvailability(request: AvailabilityRequest): Observable<Availability> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.post<Availability>(this.apiUrl, request).pipe(
      tap(availability => {
        this.myAvailabilitiesSignal.update(list => [...list, availability]);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        const message = error.error?.error || error.error?.message || 'Erreur lors de la creation du creneau';
        this.errorSignal.set(message);
        throw error;
      })
    );
  }

  // Teacher: Load my availabilities
  loadMyAvailabilities(): Observable<Availability[]> {
    this.loadingSignal.set(true);

    return this.http.get<Availability[]>(`${this.apiUrl}/me`).pipe(
      tap(availabilities => {
        this.myAvailabilitiesSignal.set(availabilities);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        this.errorSignal.set('Erreur lors du chargement des disponibilités');
        throw error;
      })
    );
  }

  // Teacher: Delete availability
  deleteAvailability(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => {
        this.myAvailabilitiesSignal.update(list => list.filter(a => a.id !== id));
      })
    );
  }

  // Public: Load teacher's availabilities
  loadTeacherAvailabilities(teacherId: number): Observable<Availability[]> {
    return this.http.get<Availability[]>(`${this.apiUrl}/teacher/${teacherId}`);
  }

  // Public: Load available time slots for a teacher
  loadAvailableSlots(teacherId: number, startDate: string, endDate: string): Observable<TimeSlot[]> {
    this.loadingSignal.set(true);

    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<TimeSlot[]>(`${this.apiUrl}/teacher/${teacherId}/slots`, { params }).pipe(
      tap(slots => {
        this.teacherSlotsSignal.set(slots);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        this.errorSignal.set('Erreur lors du chargement des créneaux');
        throw error;
      })
    );
  }

  // Group slots by date
  getSlotsByDate(): Map<string, TimeSlot[]> {
    const grouped = new Map<string, TimeSlot[]>();

    for (const slot of this.teacherSlotsSignal()) {
      const dateKey = slot.date;
      if (!grouped.has(dateKey)) {
        grouped.set(dateKey, []);
      }
      grouped.get(dateKey)!.push(slot);
    }

    return grouped;
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  clearSlots(): void {
    this.teacherSlotsSignal.set([]);
  }
}
