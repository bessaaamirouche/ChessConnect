import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError } from 'rxjs';
import { LearningPath, Course, GradeWithCourses } from '../models/learning-path.model';
import { StudentProfile } from '../models/student-profile.model';
import { ChessLevel } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class LearningPathService {
  private readonly apiUrl = '/api/learning-path';

  private learningPathSignal = signal<LearningPath | null>(null);
  private currentCourseSignal = signal<Course | null>(null);
  private studentProfileSignal = signal<StudentProfile | null>(null);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);

  readonly learningPath = this.learningPathSignal.asReadonly();
  readonly currentCourse = this.currentCourseSignal.asReadonly();
  readonly studentProfile = this.studentProfileSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly grades = computed(() => this.learningPathSignal()?.grades ?? []);

  readonly totalCourses = computed(() =>
    this.learningPathSignal()?.totalCourses ?? 0
  );

  readonly completedCourses = computed(() =>
    this.learningPathSignal()?.completedCourses ?? 0
  );

  readonly overallProgress = computed(() =>
    this.learningPathSignal()?.overallProgressPercentage ?? 0
  );

  constructor(private http: HttpClient) {}

  loadLearningPath(): Observable<LearningPath> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.get<LearningPath>(this.apiUrl).pipe(
      tap(learningPath => {
        this.learningPathSignal.set(learningPath);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.errorSignal.set('Impossible de charger le parcours');
        this.loadingSignal.set(false);
        throw error;
      })
    );
  }

  getCourse(id: number): Observable<Course> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.get<Course>(`${this.apiUrl}/courses/${id}`).pipe(
      tap(course => {
        this.currentCourseSignal.set(course);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.errorSignal.set('Impossible de charger le cours');
        this.loadingSignal.set(false);
        throw error;
      })
    );
  }

  startCourse(id: number): Observable<Course> {
    return this.http.post<Course>(`${this.apiUrl}/courses/${id}/start`, {}).pipe(
      tap(course => {
        this.currentCourseSignal.set(course);
        this.updateCourseInPath(course);
      })
    );
  }

  /**
   * Validate a course for a student (Teacher only)
   */
  validateCourse(studentId: number, courseId: number): Observable<Course> {
    return this.http.post<Course>(
      `${this.apiUrl}/courses/${courseId}/validate/${studentId}`,
      {}
    ).pipe(
      tap(course => {
        this.updateCourseInStudentProfile(course);
      })
    );
  }

  /**
   * Get student profile with all courses (Teacher only)
   */
  getStudentProfile(studentId: number): Observable<StudentProfile> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.get<StudentProfile>(`${this.apiUrl}/students/${studentId}`).pipe(
      tap(profile => {
        this.studentProfileSignal.set(profile);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.errorSignal.set('Impossible de charger le profil');
        this.loadingSignal.set(false);
        throw error;
      })
    );
  }

  private updateCourseInPath(updatedCourse: Course): void {
    const currentPath = this.learningPathSignal();
    if (!currentPath) return;

    const updatedGrades = currentPath.grades.map(grade => {
      if (grade.grade !== updatedCourse.grade) return grade;

      const updatedCourses = grade.courses.map(course =>
        course.id === updatedCourse.id ? { ...course, status: updatedCourse.status } : course
      );

      const completedCourses = updatedCourses.filter(c => c.status === 'COMPLETED').length;

      return {
        ...grade,
        courses: updatedCourses,
        completedCourses,
        progressPercentage: (completedCourses / grade.totalCourses) * 100,
        isCompleted: completedCourses === grade.totalCourses
      };
    });

    const totalCompleted = updatedGrades.reduce((sum, g) => sum + g.completedCourses, 0);

    this.learningPathSignal.set({
      ...currentPath,
      grades: updatedGrades,
      completedCourses: totalCompleted,
      overallProgressPercentage: (totalCompleted / currentPath.totalCourses) * 100
    });
  }

  private updateCourseInStudentProfile(updatedCourse: Course): void {
    const profile = this.studentProfileSignal();
    if (!profile) return;

    const updatedGrades = profile.courseProgress.map(grade => {
      if (grade.grade !== updatedCourse.grade) return grade;

      const updatedCourses = grade.courses.map(course =>
        course.id === updatedCourse.id ? updatedCourse : course
      );

      const completedCourses = updatedCourses.filter(c => c.status === 'COMPLETED').length;

      return {
        ...grade,
        courses: updatedCourses,
        completedCourses,
        progressPercentage: (completedCourses / grade.totalCourses) * 100,
        isCompleted: completedCourses === grade.totalCourses
      };
    });

    const totalCourses = updatedGrades.reduce((sum, g) => sum + g.totalCourses, 0);
    const totalCompleted = updatedGrades.reduce((sum, g) => sum + g.completedCourses, 0);

    this.studentProfileSignal.set({
      ...profile,
      courseProgress: updatedGrades,
      progressPercentage: totalCourses > 0 ? (totalCompleted / totalCourses) * 100 : 0
    });
  }

  getGradeIcon(grade: ChessLevel): string {
    const icons: Record<ChessLevel, string> = {
      PION: '♟',
      CAVALIER: '♞',
      FOU: '♝',
      TOUR: '♜',
      DAME: '♛'
    };
    return icons[grade];
  }

  getGradeColor(grade: ChessLevel): string {
    const colors: Record<ChessLevel, string> = {
      PION: '#78716c',
      CAVALIER: '#22c55e',
      FOU: '#3b82f6',
      TOUR: '#a855f7',
      DAME: '#eab308'
    };
    return colors[grade];
  }

  clearCurrentCourse(): void {
    this.currentCourseSignal.set(null);
  }

  clearStudentProfile(): void {
    this.studentProfileSignal.set(null);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }
}
