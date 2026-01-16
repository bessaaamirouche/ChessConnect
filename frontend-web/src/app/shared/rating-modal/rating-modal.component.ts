import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RatingService, CreateRatingRequest } from '../../core/services/rating.service';

@Component({
  selector: 'app-rating-modal',
  standalone: true,
  imports: [FormsModule],
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

  constructor(private ratingService: RatingService) {}

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
      this.error.set('Veuillez selectionner une note');
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
        this.error.set(err.error?.error || err.error?.message || 'Une erreur est survenue');
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
