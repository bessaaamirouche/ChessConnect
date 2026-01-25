import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { Exercise } from '../models/exercise.model';

@Injectable({
  providedIn: 'root'
})
export class ExerciseService {
  private readonly apiUrl = '/api/exercises';

  private currentExerciseSignal = signal<Exercise | null>(null);
  private exercisesSignal = signal<Exercise[]>([]);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);

  readonly currentExercise = this.currentExerciseSignal.asReadonly();
  readonly exercises = this.exercisesSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  constructor(private http: HttpClient) {}

  loadExerciseForLesson(lessonId: number): Observable<Exercise> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.get<Exercise>(`${this.apiUrl}/lesson/${lessonId}`).pipe(
      tap({
        next: (exercise) => {
          this.currentExerciseSignal.set(exercise);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(err.error?.error || 'Erreur lors du chargement de l\'exercice');
          this.loadingSignal.set(false);
        }
      }),
      catchError((err) => {
        this.errorSignal.set(err.error?.error || 'Erreur lors du chargement de l\'exercice');
        this.loadingSignal.set(false);
        throw err;
      })
    );
  }

  loadExerciseById(exerciseId: number): Observable<Exercise> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.get<Exercise>(`${this.apiUrl}/${exerciseId}`).pipe(
      tap({
        next: (exercise) => {
          this.currentExerciseSignal.set(exercise);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(err.error?.error || 'Erreur lors du chargement de l\'exercice');
          this.loadingSignal.set(false);
        }
      }),
      catchError((err) => {
        this.errorSignal.set(err.error?.error || 'Erreur lors du chargement de l\'exercice');
        this.loadingSignal.set(false);
        throw err;
      })
    );
  }

  loadAllExercises(): Observable<Exercise[]> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.get<Exercise[]>(this.apiUrl).pipe(
      tap({
        next: (exercises) => {
          this.exercisesSignal.set(exercises);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(err.error?.error || 'Erreur lors du chargement des exercices');
          this.loadingSignal.set(false);
        }
      }),
      catchError((err) => {
        this.errorSignal.set(err.error?.error || 'Erreur lors du chargement des exercices');
        this.loadingSignal.set(false);
        return of([]);
      })
    );
  }

  clearExercise(): void {
    this.currentExerciseSignal.set(null);
    this.errorSignal.set(null);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }
}
