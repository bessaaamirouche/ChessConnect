import { Component, Input, Output, EventEmitter, signal, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
  imports: [NgIconComponent, DecimalPipe, FormsModule],
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

  expandedGrade = signal<ChessLevel | null>(null);
  validatingCourse = signal<number | null>(null);
  selectedLevel = signal<ChessLevel>('PION');
  settingLevel = signal<boolean>(false);

  readonly chessLevels = Object.entries(CHESS_LEVELS).map(([key, value]) => ({
    value: key as ChessLevel,
    label: value.label,
    icon: value.icon
  }));

  constructor(public learningPathService: LearningPathService) {}

  ngOnInit(): void {
    if (this.studentId) {
      this.learningPathService.getStudentProfile(this.studentId).subscribe();
    }
  }

  closeModal(): void {
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
    return this.learningPathService.getGradeIcon(grade);
  }

  getGradeColor(grade: ChessLevel): string {
    return this.learningPathService.getGradeColor(grade);
  }
}
