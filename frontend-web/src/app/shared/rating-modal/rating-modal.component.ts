import { Component, EventEmitter, Input, Output, signal, HostListener, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { RatingService, CreateRatingRequest } from '../../core/services/rating.service';
import { FocusTrapDirective } from '../directives/focus-trap.directive';

@Component({
  selector: 'app-rating-modal',
  standalone: true,
  imports: [FormsModule, FocusTrapDirective, TranslateModule],
  templateUrl: './rating-modal.component.html',
  styleUrl: './rating-modal.component.scss'
})
export class RatingModalComponent {
  @Input() lessonId!: number;
  @Input() teacherName = '';
  @Output() close = new EventEmitter<void>();
  @Output() rated = new EventEmitter<void>();

  selectedStars = signal(0);
  hoveredStars = signal(0);
  comment = '';
  loading = signal(false);
  error = signal<string | null>(null);

  private translateService = inject(TranslateService);

  constructor(private ratingService: RatingService) {}

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.onClose();
  }

  setHoveredStars(stars: number): void {
    this.hoveredStars.set(stars);
  }

  clearHoveredStars(): void {
    this.hoveredStars.set(0);
  }

  selectStars(stars: number): void {
    this.selectedStars.set(stars);
  }

  getDisplayStars(): number {
    return this.hoveredStars() > 0 ? this.hoveredStars() : this.selectedStars();
  }

  onSubmit(): void {
    if (this.selectedStars() === 0) {
      this.error.set(this.translateService.instant('errors.selectRating'));
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const request: CreateRatingRequest = {
      lessonId: this.lessonId,
      stars: this.selectedStars(),
      comment: this.comment.trim() || undefined
    };

    this.ratingService.createRating(request).subscribe({
      next: () => {
        this.loading.set(false);
        this.rated.emit();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.error || err.error?.message || this.translateService.instant('errors.generic'));
      }
    });
  }

  onClose(): void {
    this.close.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.onClose();
    }
  }
}
