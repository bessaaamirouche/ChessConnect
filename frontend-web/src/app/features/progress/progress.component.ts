import { Component, OnInit, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LearningPathService } from '../../core/services/learning-path.service';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { Course, GradeWithCourses } from '../../core/models/learning-path.model';
import { ChessLevel, CHESS_LEVELS } from '../../core/models/user.model';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroCheckCircle,
  heroChevronDown,
  heroChevronUp,
  heroLockClosed,
  heroClock,
  heroCheckBadge,
  heroExclamationTriangle,
  heroAcademicCap
} from '@ng-icons/heroicons/outline';

interface LevelGroup {
  code: ChessLevel;
  name: string;
  color: string;
  courses: Course[];
  totalCourses: number;
  completedCourses: number;
  isUnlocked: boolean;
}

@Component({
  selector: 'app-progress',
  standalone: true,
  imports: [RouterLink, NgIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroCheckCircle,
    heroChevronDown,
    heroChevronUp,
    heroLockClosed,
    heroClock,
    heroCheckBadge,
    heroExclamationTriangle,
    heroAcademicCap
  })],
  templateUrl: './progress.component.html',
  styleUrl: './progress.component.scss'
})
export class ProgressComponent implements OnInit {
  expandedLevel = signal<ChessLevel | null>('A');

  constructor(
    public learningPathService: LearningPathService,
    public authService: AuthService,
    private seoService: SeoService
  ) {}

  ngOnInit(): void {
    this.seoService.setProgressPage();
    this.learningPathService.loadLearningPath().subscribe();
  }

  // Now grades come directly as A/B/C/D from backend
  programmeLevels = computed<LevelGroup[]>(() => {
    const grades = this.learningPathService.grades();
    if (!grades.length) return [];

    return grades.map(grade => ({
      code: grade.grade,
      name: CHESS_LEVELS[grade.grade]?.label || grade.displayName,
      color: this.getLevelColor(grade.grade),
      courses: grade.courses,
      totalCourses: grade.totalCourses,
      completedCourses: grade.completedCourses,
      isUnlocked: grade.isUnlocked
    }));
  });

  totalValidatedCourses = computed(() => this.learningPathService.completedCourses());
  totalCourses = computed(() => this.learningPathService.totalCourses());
  loading = computed(() => this.learningPathService.loading());
  error = computed(() => this.learningPathService.error());

  toggleLevel(code: ChessLevel): void {
    if (this.expandedLevel() === code) {
      this.expandedLevel.set(null);
    } else {
      this.expandedLevel.set(code);
    }
  }

  isExpanded(code: ChessLevel): boolean {
    return this.expandedLevel() === code;
  }

  getLevelIcon(code: ChessLevel): string {
    return CHESS_LEVELS[code]?.icon || '🎓';
  }

  getLevelColor(code: ChessLevel): string {
    const colors: Record<ChessLevel, string> = {
      'A': '#4CAF50',  // Green for beginners
      'B': '#2196F3',  // Blue for intermediate
      'C': '#9C27B0',  // Purple for advanced
      'D': '#FF9800'   // Orange for expert
    };
    return colors[code] || '#4CAF50';
  }

  isLevelCompleted(level: LevelGroup): boolean {
    return level.completedCourses === level.totalCourses && level.totalCourses > 0;
  }

  isLevelInProgress(level: LevelGroup): boolean {
    return level.completedCourses > 0 && level.completedCourses < level.totalCourses;
  }

  logout(): void {
    this.authService.logout();
  }
}
