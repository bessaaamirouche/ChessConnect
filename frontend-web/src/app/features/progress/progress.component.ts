import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ProgressService, LevelInfo } from '../../core/services/progress.service';
import { AuthService } from '../../core/services/auth.service';
import { LearningPathService } from '../../core/services/learning-path.service';
import { CHESS_LEVELS, ChessLevel } from '../../core/models/user.model';
import { Course } from '../../core/models/learning-path.model';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChartBarSquare,
  heroCalendarDays,
  heroTrophy,
  heroAcademicCap,
  heroUserCircle,
  heroArrowRightOnRectangle,
  heroExclamationTriangle,
  heroBookOpen,
  heroArrowTrendingUp,
  heroCheckCircle,
  heroCheck,
  heroChevronDown,
  heroChevronUp,
  heroLockClosed,
  heroClock,
  heroCheckBadge
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-progress',
  standalone: true,
  imports: [RouterLink, DatePipe, DecimalPipe, NgIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroChartBarSquare,
    heroCalendarDays,
    heroTrophy,
    heroAcademicCap,
    heroUserCircle,
    heroArrowRightOnRectangle,
    heroExclamationTriangle,
    heroBookOpen,
    heroArrowTrendingUp,
    heroCheckCircle,
    heroCheck,
    heroChevronDown,
    heroChevronUp,
    heroLockClosed,
    heroClock,
    heroCheckBadge
  })],
  templateUrl: './progress.component.html',
  styleUrl: './progress.component.scss'
})
export class ProgressComponent implements OnInit {
  levels: ChessLevel[] = ['PION', 'CAVALIER', 'FOU', 'TOUR', 'DAME'];
  CHESS_LEVELS = CHESS_LEVELS;

  expandedLevel = signal<ChessLevel | null>(null);

  constructor(
    public progressService: ProgressService,
    public authService: AuthService,
    public learningPathService: LearningPathService
  ) {}

  ngOnInit(): void {
    this.progressService.loadMyProgress().subscribe();
    this.progressService.loadAllLevels().subscribe();
    this.learningPathService.loadLearningPath().subscribe();
  }

  toggleLevel(level: ChessLevel): void {
    if (this.expandedLevel() === level) {
      this.expandedLevel.set(null);
    } else {
      this.expandedLevel.set(level);
    }
  }

  isExpanded(level: ChessLevel): boolean {
    return this.expandedLevel() === level;
  }

  getCoursesForLevel(level: ChessLevel): Course[] {
    const grades = this.learningPathService.grades();
    const grade = grades.find(g => g.grade === level);
    return grade?.courses ?? [];
  }

  getCompletedCoursesCount(level: ChessLevel): number {
    const grades = this.learningPathService.grades();
    const grade = grades.find(g => g.grade === level);
    return grade?.completedCourses ?? 0;
  }

  getTotalCoursesCount(level: ChessLevel): number {
    const grades = this.learningPathService.grades();
    const grade = grades.find(g => g.grade === level);
    return grade?.totalCourses ?? 0;
  }

  getLevelIcon(level: ChessLevel): string {
    return this.progressService.getLevelIcon(level);
  }

  getLevelColor(level: ChessLevel): string {
    return this.progressService.getLevelColor(level);
  }

  isLevelCompleted(level: ChessLevel): boolean {
    const currentLevel = this.progressService.currentLevel();
    if (!currentLevel) return false;
    return CHESS_LEVELS[level].order < CHESS_LEVELS[currentLevel].order;
  }

  isCurrentLevel(level: ChessLevel): boolean {
    return this.progressService.currentLevel() === level;
  }

  isLevelLocked(level: ChessLevel): boolean {
    const currentLevel = this.progressService.currentLevel();
    if (!currentLevel) return true;
    return CHESS_LEVELS[level].order > CHESS_LEVELS[currentLevel].order;
  }

  getLessonsRequired(level: ChessLevel): number {
    const requirements: Record<ChessLevel, number> = {
      PION: 10,
      CAVALIER: 15,
      FOU: 20,
      TOUR: 25,
      DAME: 0
    };
    return requirements[level];
  }

  logout(): void {
    this.authService.logout();
  }
}
