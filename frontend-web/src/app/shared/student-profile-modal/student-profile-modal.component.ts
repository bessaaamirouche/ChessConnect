import { Component, Input, Output, EventEmitter, signal, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { LearningPathService } from '../../core/services/learning-path.service';
import { Course } from '../../core/models/learning-path.model';
import { ChessLevel, CHESS_LEVELS } from '../../core/models/user.model';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroXMark,
  heroCheckCircle,
  heroLockClosed,
  heroClock,
  heroCheckBadge,
  heroChevronDown,
  heroChevronUp,
  heroAcademicCap
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-student-profile-modal',
  standalone: true,
  imports: [NgIconComponent, DecimalPipe, FormsModule, TranslateModule],
  viewProviders: [provideIcons({
    heroXMark,
    heroCheckCircle,
    heroLockClosed,
    heroClock,
    heroCheckBadge,
    heroChevronDown,
    heroChevronUp,
    heroAcademicCap
  })],
  templateUrl: './student-profile-modal.component.html',
  styleUrl: './student-profile-modal.component.scss'
})
export class StudentProfileModalComponent implements OnInit {
  @Input() studentId!: number;
  @Output() close = new EventEmitter<void>();
  @Output() courseValidated = new EventEmitter<{ studentId: number; courseId: number }>();
  @Output() closedWithoutValidation = new EventEmitter<number>();

  private hasValidatedCourse = false;
  expandedGrade = signal<ChessLevel | null>(null);
  validatingCourse = signal<number | null>(null);
  selectedLevel = signal<ChessLevel>('A');
  settingLevel = signal<boolean>(false);

  // 4 levels mapped to chess pieces: A→Pion, B→Cavalier, C→Reine, D→Roi
  readonly chessLevels: { value: ChessLevel; label: string; icon: string }[] = [
    { value: 'A', label: 'Pion - Débutant', icon: '♟' },
    { value: 'B', label: 'Cavalier - Intermédiaire', icon: '♞' },
    { value: 'C', label: 'Reine - Avancé', icon: '♛' },
    { value: 'D', label: 'Roi - Expert', icon: '♚' }
  ];

  constructor(public learningPathService: LearningPathService) {}

  ngOnInit(): void {
    if (this.studentId) {
      this.learningPathService.getStudentProfile(this.studentId).subscribe();
    }
  }

  closeModal(): void {
    // If no course was validated during this session, emit reminder event
    if (!this.hasValidatedCourse) {
      this.closedWithoutValidation.emit(this.studentId);
    }
    this.learningPathService.clearStudentProfile();
    this.close.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.closeModal();
    }
  }

  toggleGrade(grade: ChessLevel): void {
    if (this.expandedGrade() === grade) {
      this.expandedGrade.set(null);
    } else {
      this.expandedGrade.set(grade);
    }
  }

  isGradeExpanded(grade: ChessLevel): boolean {
    return this.expandedGrade() === grade;
  }

  validateCourse(course: Course): void {
    if (this.validatingCourse() || course.status === 'COMPLETED' || course.status === 'LOCKED') {
      return;
    }

    this.validatingCourse.set(course.id);
    this.learningPathService.validateCourse(this.studentId, course.id).subscribe({
      next: () => {
        this.validatingCourse.set(null);
        this.hasValidatedCourse = true;
        this.courseValidated.emit({ studentId: this.studentId, courseId: course.id });
      },
      error: () => {
        this.validatingCourse.set(null);
      }
    });
  }

  canValidate(course: Course): boolean {
    return course.status === 'IN_PROGRESS' || course.status === 'PENDING_VALIDATION';
  }

  setLevel(): void {
    if (this.settingLevel()) return;

    this.settingLevel.set(true);
    this.learningPathService.setStudentLevel(this.studentId, this.selectedLevel()).subscribe({
      next: () => {
        this.settingLevel.set(false);
      },
      error: () => {
        this.settingLevel.set(false);
      }
    });
  }

  onLevelChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedLevel.set(select.value as ChessLevel);
  }

  getGradeIcon(grade: ChessLevel): string {
    return CHESS_LEVELS[grade]?.icon || '🎓';
  }

  getGradeColor(grade: ChessLevel): string {
    const colors: Record<ChessLevel, string> = {
      'A': '#4CAF50',  // Green for beginners
      'B': '#2196F3',  // Blue for intermediate
      'C': '#9C27B0',  // Purple for advanced
      'D': '#FF9800'   // Orange for expert
    };
    return colors[grade] || '#4CAF50';
  }
}
