import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroCpuChip, heroLockClosed } from '@ng-icons/heroicons/outline';
import { PaymentService } from '../../../core/services/payment.service';

@Component({
  selector: 'app-exercise-button',
  standalone: true,
  imports: [RouterLink, NgIconComponent],
  viewProviders: [provideIcons({ heroCpuChip, heroLockClosed })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (paymentService.hasActiveSubscription()) {
      <a
        [routerLink]="['/exercise', lessonId]"
        class="exercise-btn"
        title="S'entrainer contre myChessBot"
      >
        <ng-icon name="heroCpuChip" size="14"></ng-icon>
        M'exercer
      </a>
    } @else {
      <a
        routerLink="/subscription"
        [queryParams]="{ required: 'exercise' }"
        class="exercise-btn exercise-btn--locked"
        title="Fonctionnalite Premium - Abonnez-vous pour acceder"
      >
        <ng-icon name="heroLockClosed" size="14"></ng-icon>
        Premium
      </a>
    }
  `,
  styles: [`
    .exercise-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      padding: 0.35rem 0.75rem;
      background: rgba(212, 175, 55, 0.15);
      border: 1px solid rgba(212, 175, 55, 0.3);
      color: #d4af37;
      border-radius: var(--radius-md, 6px);
      font-size: 0.75rem;
      font-weight: 500;
      text-decoration: none;
      transition: all 0.2s ease;
      white-space: nowrap;

      &:hover {
        background: rgba(212, 175, 55, 0.25);
        border-color: rgba(212, 175, 55, 0.5);
        transform: translateY(-1px);
      }

      &--locked {
        background: rgba(107, 105, 101, 0.15);
        border-color: rgba(107, 105, 101, 0.3);
        color: #9ca3af;

        &:hover {
          background: rgba(107, 105, 101, 0.25);
          border-color: rgba(107, 105, 101, 0.5);
        }
      }
    }
  `]
})
export class ExerciseButtonComponent {
  @Input({ required: true }) lessonId!: number;

  constructor(public paymentService: PaymentService) {}
}
