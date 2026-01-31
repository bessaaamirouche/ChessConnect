import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';

export interface ProgrammeCourse {
  id: number;
  levelCode: string;
  levelName: string;
  courseOrder: number;
  title: string;
  isCurrent: boolean;
  isCompleted: boolean;
}

export interface ProgrammeLevel {
  code: string;
  name: string;
  description: string;
  color: string;
  courses: ProgrammeCourse[];
}

@Injectable({
  providedIn: 'root'
})
export class ProgrammeService {
  private readonly apiUrl = '/api/programme';

  private coursesSignal = signal<ProgrammeCourse[]>([]);
  private currentCourseSignal = signal<ProgrammeCourse | null>(null);
  private loadingSignal = signal(false);
  private errorSignal = signal<string | null>(null);

  readonly courses = this.coursesSignal.asReadonly();
  readonly currentCourse = this.currentCourseSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  // Group courses by level
  readonly coursesByLevel = computed(() => {
    const courses = this.coursesSignal();
    const levels: ProgrammeLevel[] = [
      { code: 'A', name: 'Pion - Débutant', description: 'Les fondamentaux des échecs (50h)', color: '#78716c', courses: [] },
      { code: 'B', name: 'Cavalier - Intermédiaire', description: 'Approfondissement tactique et stratégique (60h)', color: '#22c55e', courses: [] },
      { code: 'C', name: 'Reine - Avancé', description: 'Maîtrise stratégique et préparation approfondie (70h)', color: '#a855f7', courses: [] },
      { code: 'D', name: 'Roi - Expert', description: 'Perfectionnement et préparation professionnelle (80h)', color: '#eab308', courses: [] }
    ];

    courses.forEach(course => {
      const level = levels.find(l => l.code === course.levelCode);
      if (level) {
        level.courses.push(course);
      }
    });

    return levels;
  });

  constructor(private http: HttpClient) {}

  loadCourses(): Observable<ProgrammeCourse[]> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.get<ProgrammeCourse[]>(`${this.apiUrl}/courses`).pipe(
      tap(courses => {
        this.coursesSignal.set(courses);
        const current = courses.find(c => c.isCurrent);
        if (current) {
          this.currentCourseSignal.set(current);
        }
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.errorSignal.set(error.error?.message || 'Erreur de chargement');
        this.loadingSignal.set(false);
        return of([]);
      })
    );
  }

  loadPublicCourses(): Observable<ProgrammeCourse[]> {
    return this.http.get<ProgrammeCourse[]>(`${this.apiUrl}/public/courses`).pipe(
      tap(courses => {
        this.coursesSignal.set(courses);
      }),
      catchError(() => of([]))
    );
  }

  getCurrentCourse(): Observable<ProgrammeCourse> {
    return this.http.get<ProgrammeCourse>(`${this.apiUrl}/current`).pipe(
      tap(course => {
        this.currentCourseSignal.set(course);
      })
    );
  }

  setCurrentCourse(courseId: number): Observable<ProgrammeCourse> {
    return this.http.post<ProgrammeCourse>(`${this.apiUrl}/current`, { courseId }).pipe(
      tap(course => {
        this.currentCourseSignal.set(course);
        // Update the courses list to reflect the new current
        this.coursesSignal.update(courses =>
          courses.map(c => ({
            ...c,
            isCurrent: c.id === courseId,
            isCompleted: c.id < courseId
          }))
        );
      })
    );
  }

  advanceToNextCourse(): Observable<ProgrammeCourse> {
    return this.http.post<ProgrammeCourse>(`${this.apiUrl}/advance`, {}).pipe(
      tap(course => {
        this.currentCourseSignal.set(course);
        this.loadCourses().subscribe(); // Refresh the list
      })
    );
  }

  goBackToPreviousCourse(): Observable<ProgrammeCourse> {
    return this.http.post<ProgrammeCourse>(`${this.apiUrl}/go-back`, {}).pipe(
      tap(course => {
        this.currentCourseSignal.set(course);
        this.loadCourses().subscribe(); // Refresh the list
      })
    );
  }

  getLevelColor(levelCode: string): string {
    const colors: Record<string, string> = {
      'A': '#4CAF50',
      'B': '#2196F3',
      'C': '#9C27B0',
      'D': '#FF9800'
    };
    return colors[levelCode] || '#666';
  }

  getLevelName(levelCode: string): string {
    const names: Record<string, string> = {
      'A': 'Débutant',
      'B': 'Intermédiaire',
      'C': 'Avancé',
      'D': 'Expert'
    };
    return names[levelCode] || levelCode;
  }

  clearError(): void {
    this.errorSignal.set(null);
  }
}
