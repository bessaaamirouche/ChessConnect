import { Component, Input, Output, EventEmitter, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { LearningPathService } from '../../core/services/learning-path.service';
import { Course } from '../../core/models/learning-path.model';
import { ChessLevel, CHESS_LEVELS } from '../../core/models/user.model';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroXMark,
  heroCheckCircle,
  heroAcademicCap,
  heroChevronDown,
  heroChevronUp,
  heroLockClosed
} from '@ng-icons/heroicons/outline';

interface FirstLessonResponse {
  isFirstLesson: boolean;
  studentName: string;
  studentId: number;
}

@Component({
  selector: 'app-student-evaluation-modal',
  standalone: true,
  imports: [NgIconComponent, FormsModule],
  viewProviders: [provideIcons({
    heroXMark,
    heroCheckCircle,
    heroAcademicCap,
    heroChevronDown,
    heroChevronUp,
    heroLockClosed
  })],
  template: `
    <div class="modal-backdrop" (click)="onBackdropClick($event)">
      <div class="modal">
        <!-- Header -->
        <header class="modal__header">
          <h2 class="modal__title">
            @if (isFirstLesson()) {
              Évaluation du joueur
            } @else {
              Validation des cours
            }
          </h2>
          <button class="modal__close" (click)="closeModal()">
            <ng-icon name="heroXMark" size="24"></ng-icon>
          </button>
        </header>

        @if (loading()) {
          <div class="modal__loading">
            <span class="spinner spinner--lg"></span>
            <p>Chargement...</p>
          </div>
        } @else {
          <div class="modal__content">
            <!-- First Lesson Mode: Evaluate student level -->
            @if (isFirstLesson()) {
              <div class="evaluation-mode">
                <div class="evaluation-mode__header">
                  <ng-icon name="heroAcademicCap" size="48" class="evaluation-mode__icon"></ng-icon>
                  <h3>Premier cours avec {{ studentName() }}</h3>
                  <p>Définissez le niveau de ce joueur après avoir évalué ses compétences.</p>
                </div>

                <div class="level-selection">
                  <label class="level-selection__label">Niveau évalué :</label>
                  <div class="level-selection__options">
                    @for (level of chessLevels; track level.value) {
                      <button
                        type="button"
                        class="level-option"
                        [class.selected]="selectedLevel() === level.value"
                        (click)="selectedLevel.set(level.value)"
                      >
                        <span class="level-option__icon">{{ level.icon }}</span>
                        <span class="level-option__label">{{ level.label }}</span>
                        <span class="level-option__description">{{ level.description }}</span>
                      </button>
                    }
                  </div>
                </div>

                <div class="modal__actions">
                  <button class="btn btn--ghost" (click)="closeModal()">
                    Plus tard
                  </button>
                  <button
                    class="btn btn--primary"
                    (click)="setStudentLevel()"
                    [disabled]="settingLevel()"
                  >
                    @if (settingLevel()) {
                      <span class="spinner spinner--xs"></span>
                    } @else {
                      Définir le niveau
                    }
                  </button>
                </div>
              </div>
            } @else {
              <!-- Returning Student Mode: Validate courses -->
              <div class="validation-mode">
                <div class="validation-mode__header">
                  <h3>Cours avec {{ studentName() }}</h3>
                  <p>Validez les cours que ce joueur a maîtrisés.</p>
                </div>

                @if (learningPathService.studentProfile()) {
                  <div class="grades-list">
                    @for (grade of learningPathService.studentProfile()!.courseProgress; track grade.grade) {
                      <div class="grade-card" [class.expanded]="expandedGrade() === grade.grade">
                        <button class="grade-card__header" (click)="toggleGrade(grade.grade)">
                          <span class="grade-card__icon" [style.color]="getGradeColor(grade.grade)">
                            {{ getGradeIcon(grade.grade) }}
                          </span>
                          <span class="grade-card__name">{{ grade.displayName }}</span>
                          <span class="grade-card__progress">
                            {{ grade.completedCourses }}/{{ grade.totalCourses }}
                          </span>
                          <ng-icon [name]="expandedGrade() === grade.grade ? 'heroChevronUp' : 'heroChevronDown'" class="grade-card__chevron"></ng-icon>
                        </button>

                        @if (expandedGrade() === grade.grade) {
                          <div class="grade-card__courses">
                            @for (course of grade.courses; track course.id) {
                              <div class="course-row" [class.course-row--completed]="course.status === 'COMPLETED'">
                                <span class="course-row__order">{{ course.orderInGrade }}.</span>
                                <span class="course-row__title">{{ course.title }}</span>

                                <div class="course-row__actions">
                                  @switch (course.status) {
                                    @case ('COMPLETED') {
                                      <span class="course-row__status course-row__status--completed">
                                        <ng-icon name="heroCheckCircle" size="18"></ng-icon>
                                        Validé
                                      </span>
                                    }
                                    @case ('IN_PROGRESS') {
                                      <button
                                        class="btn btn--sm btn--primary"
                                        (click)="validateCourse(course)"
                                        [disabled]="validatingCourse() === course.id"
                                      >
                                        @if (validatingCourse() === course.id) {
                                          <span class="spinner spinner--xs"></span>
                                        } @else {
                                          Valider
                                        }
                                      </button>
                                    }
                                    @case ('PENDING_VALIDATION') {
                                      <button
                                        class="btn btn--sm btn--warning"
                                        (click)="validateCourse(course)"
                                        [disabled]="validatingCourse() === course.id"
                                      >
                                        @if (validatingCourse() === course.id) {
                                          <span class="spinner spinner--xs"></span>
                                        } @else {
                                          Valider
                                        }
                                      </button>
                                    }
                                    @case ('LOCKED') {
                                      <span class="course-row__status course-row__status--locked">
                                        <ng-icon name="heroLockClosed" size="16"></ng-icon>
                                      </span>
                                    }
                                  }
                                </div>
                              </div>
                            }
                          </div>
                        }
                      </div>
                    }
                  </div>
                }

                <div class="modal__actions">
                  <button class="btn btn--ghost" (click)="closeModal()">
                    Plus tard
                  </button>
                  <button class="btn btn--primary" (click)="closeModal()">
                    Terminé
                  </button>
                </div>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 1rem;
    }

    .modal {
      background: #1a1a1a;
      border-radius: 16px;
      max-width: 600px;
      width: 100%;
      max-height: 90vh;
      overflow-y: auto;
      border: 1px solid #333;
    }

    .modal__header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.5rem;
      border-bottom: 1px solid #333;
    }

    .modal__title {
      font-size: 1.25rem;
      font-weight: 600;
      color: #fff;
      margin: 0;
    }

    .modal__close {
      background: none;
      border: none;
      color: #999;
      cursor: pointer;
      padding: 0.5rem;
      border-radius: 8px;
      transition: all 0.2s;

      &:hover {
        background: #333;
        color: #fff;
      }
    }

    .modal__content {
      padding: 1.5rem;
    }

    .modal__loading {
      padding: 3rem;
      text-align: center;
      color: #999;

      .spinner {
        margin-bottom: 1rem;
      }
    }

    .modal__actions {
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      margin-top: 1.5rem;
      padding-top: 1.5rem;
      border-top: 1px solid #333;
    }

    // Evaluation Mode
    .evaluation-mode__header {
      text-align: center;
      margin-bottom: 2rem;

      h3 {
        color: #fff;
        margin: 1rem 0 0.5rem;
        font-size: 1.25rem;
      }

      p {
        color: #999;
        margin: 0;
      }
    }

    .evaluation-mode__icon {
      color: #d4af37;
    }

    .level-selection {
      margin-bottom: 1rem;
    }

    .level-selection__label {
      display: block;
      color: #999;
      font-size: 0.875rem;
      margin-bottom: 1rem;
    }

    .level-selection__options {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .level-option {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem;
      background: #222;
      border: 2px solid #333;
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s;
      text-align: left;

      &:hover {
        background: #2a2a2a;
        border-color: #444;
      }

      &.selected {
        border-color: #d4af37;
        background: rgba(212, 175, 55, 0.1);
      }
    }

    .level-option__icon {
      font-size: 1.5rem;
      width: 40px;
      text-align: center;
    }

    .level-option__label {
      font-weight: 600;
      color: #fff;
      flex: 1;
    }

    .level-option__description {
      color: #999;
      font-size: 0.875rem;
    }

    // Validation Mode
    .validation-mode__header {
      margin-bottom: 1.5rem;

      h3 {
        color: #fff;
        margin: 0 0 0.5rem;
        font-size: 1.125rem;
      }

      p {
        color: #999;
        margin: 0;
        font-size: 0.875rem;
      }
    }

    .grades-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .grade-card {
      background: #222;
      border-radius: 12px;
      overflow: hidden;
    }

    .grade-card__header {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      width: 100%;
      padding: 1rem;
      background: none;
      border: none;
      cursor: pointer;
      transition: background 0.2s;
      text-align: left;

      &:hover {
        background: #2a2a2a;
      }
    }

    .grade-card__icon {
      font-size: 1.25rem;
    }

    .grade-card__name {
      flex: 1;
      color: #fff;
      font-weight: 500;
    }

    .grade-card__progress {
      color: #999;
      font-size: 0.875rem;
    }

    .grade-card__chevron {
      color: #666;
    }

    .grade-card__courses {
      padding: 0 1rem 1rem;
    }

    .course-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem;
      border-radius: 8px;
      background: #1a1a1a;
      margin-bottom: 0.5rem;

      &:last-child {
        margin-bottom: 0;
      }

      &--completed {
        opacity: 0.7;
      }
    }

    .course-row__order {
      color: #666;
      font-size: 0.875rem;
      width: 24px;
    }

    .course-row__title {
      flex: 1;
      color: #fff;
      font-size: 0.875rem;
    }

    .course-row__status {
      display: flex;
      align-items: center;
      gap: 0.25rem;
      font-size: 0.75rem;

      &--completed {
        color: #4caf50;
      }

      &--locked {
        color: #666;
      }
    }

    // Buttons
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      padding: 0.75rem 1.5rem;
      border-radius: 8px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
      border: none;

      &--primary {
        background: #d4af37;
        color: #000;

        &:hover:not(:disabled) {
          background: #e5c048;
        }

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      }

      &--ghost {
        background: transparent;
        color: #999;

        &:hover {
          background: #333;
          color: #fff;
        }
      }

      &--warning {
        background: #ff9800;
        color: #000;

        &:hover:not(:disabled) {
          background: #ffa726;
        }
      }

      &--sm {
        padding: 0.5rem 1rem;
        font-size: 0.875rem;
      }
    }

    .spinner {
      display: inline-block;
      width: 20px;
      height: 20px;
      border: 2px solid transparent;
      border-top-color: currentColor;
      border-radius: 50%;
      animation: spin 1s linear infinite;

      &--xs {
        width: 14px;
        height: 14px;
      }

      &--lg {
        width: 32px;
        height: 32px;
      }
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class StudentEvaluationModalComponent implements OnInit {
  @Input() studentId!: number;
  @Input() lessonId!: number;
  @Output() close = new EventEmitter<void>();
  @Output() levelSet = new EventEmitter<{ studentId: number; level: ChessLevel }>();
  @Output() courseValidated = new EventEmitter<{ studentId: number; courseId: number }>();

  loading = signal(true);
  isFirstLesson = signal(false);
  studentName = signal('');
  selectedLevel = signal<ChessLevel>('A');
  settingLevel = signal(false);
  expandedGrade = signal<ChessLevel | null>('A');
  validatingCourse = signal<number | null>(null);

  // 4 levels mapped to chess pieces: A→Pion, B→Cavalier, C→Reine, D→Roi
  readonly chessLevels: { value: ChessLevel; label: string; description: string; icon: string }[] = [
    { value: 'A', label: 'Pion', description: 'Débutant', icon: '♟' },
    { value: 'B', label: 'Cavalier', description: 'Intermédiaire', icon: '♞' },
    { value: 'C', label: 'Reine', description: 'Avancé', icon: '♛' },
    { value: 'D', label: 'Roi', description: 'Expert', icon: '♚' }
  ];

  constructor(
    private http: HttpClient,
    public learningPathService: LearningPathService
  ) {}

  ngOnInit(): void {
    this.checkFirstLesson();
  }

  private checkFirstLesson(): void {
    this.http.get<FirstLessonResponse>(`/api/lessons/is-first-lesson/${this.studentId}`).subscribe({
      next: (response) => {
        this.isFirstLesson.set(response.isFirstLesson);
        this.studentName.set(response.studentName);

        if (!response.isFirstLesson) {
          // Load student profile for course validation
          this.learningPathService.getStudentProfile(this.studentId).subscribe({
            next: () => this.loading.set(false),
            error: () => this.loading.set(false)
          });
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        // Fallback: assume not first lesson and load profile
        this.isFirstLesson.set(false);
        this.learningPathService.getStudentProfile(this.studentId).subscribe({
          next: () => this.loading.set(false),
          error: () => this.loading.set(false)
        });
      }
    });
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

  setStudentLevel(): void {
    if (this.settingLevel()) return;

    this.settingLevel.set(true);
    this.learningPathService.setStudentLevel(this.studentId, this.selectedLevel()).subscribe({
      next: () => {
        this.settingLevel.set(false);
        this.levelSet.emit({ studentId: this.studentId, level: this.selectedLevel() });
        this.closeModal();
      },
      error: () => {
        this.settingLevel.set(false);
      }
    });
  }

  toggleGrade(grade: ChessLevel): void {
    if (this.expandedGrade() === grade) {
      this.expandedGrade.set(null);
    } else {
      this.expandedGrade.set(grade);
    }
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

  getGradeIcon(grade: ChessLevel): string {
    return CHESS_LEVELS[grade]?.icon || '🎓';
  }

  getGradeColor(grade: ChessLevel): string {
    const colors: Record<ChessLevel, string> = {
      'A': '#78716c',  // Pion - gris
      'B': '#22c55e',  // Cavalier - vert
      'C': '#a855f7',  // Reine - violet
      'D': '#eab308'   // Roi - or
    };
    return colors[grade] || '#78716c';
  }
}
