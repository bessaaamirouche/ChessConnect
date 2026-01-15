import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { Progress, ChessLevel, CHESS_LEVELS } from '../models/user.model';

export interface LevelInfo {
  code: string;
  displayName: string;
  description: string;
  order: number;
  lessonsRequired: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProgressService {
  private readonly apiUrl = '/api/progress';

  private progressSignal = signal<Progress | null>(null);
  private levelsSignal = signal<LevelInfo[]>([]);
  private loadingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);

  readonly progress = this.progressSignal.asReadonly();
  readonly levels = this.levelsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly currentLevel = computed(() => this.progressSignal()?.currentLevel);
  readonly currentLevelInfo = computed(() => {
    const level = this.currentLevel();
    return level ? CHESS_LEVELS[level] : null;
  });

  readonly progressPercentage = computed(() =>
    this.progressSignal()?.progressPercentage ?? 0
  );

  readonly totalLessons = computed(() =>
    this.progressSignal()?.totalLessonsCompleted ?? 0
  );

  readonly lessonsToNextLevel = computed(() => {
    const p = this.progressSignal();
    if (!p) return 0;
    return p.lessonsRequiredForNextLevel - p.lessonsAtCurrentLevel;
  });

  readonly isMaxLevel = computed(() =>
    this.currentLevel() === 'DAME'
  );

  readonly overallProgress = computed(() => {
    const p = this.progressSignal();
    if (!p) return 0;

    const levelOrder = CHESS_LEVELS[p.currentLevel].order;
    const baseProgress = (levelOrder - 1) * 20;
    const levelProgress = (p.progressPercentage / 100) * 20;

    return Math.min(100, baseProgress + levelProgress);
  });

  constructor(private http: HttpClient) {}

  loadMyProgress(): Observable<Progress> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.get<Progress>(`${this.apiUrl}/me`).pipe(
      tap(progress => {
        this.progressSignal.set(progress);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.errorSignal.set('Impossible de charger la progression');
        this.loadingSignal.set(false);
        throw error;
      })
    );
  }

  loadAllLevels(): Observable<LevelInfo[]> {
    return this.http.get<LevelInfo[]>(`${this.apiUrl}/levels`).pipe(
      tap(levels => this.levelsSignal.set(levels))
    );
  }

  getLevelIcon(level: ChessLevel): string {
    const icons: Record<ChessLevel, string> = {
      PION: '♟',
      CAVALIER: '♞',
      FOU: '♝',
      TOUR: '♜',
      DAME: '♛'
    };
    return icons[level];
  }

  getLevelColor(level: ChessLevel): string {
    const colors: Record<ChessLevel, string> = {
      PION: '#78716c',
      CAVALIER: '#22c55e',
      FOU: '#3b82f6',
      TOUR: '#a855f7',
      DAME: '#eab308'
    };
    return colors[level];
  }

  clearError(): void {
    this.errorSignal.set(null);
  }
}
