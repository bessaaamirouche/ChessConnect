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
        <span>M'exercer</span>
      </a>
    } @else {
      <a
        routerLink="/subscription"
        [queryParams]="{ required: 'exercise' }"
        class="exercise-btn exercise-btn--locked"
        title="Fonctionnalité Premium - Abonnez-vous pour accéder"
      >
        <ng-icon name="heroLockClosed" size="14"></ng-icon>
        <span>Premium</span>
      </a>
    }
  `,
  styles: [`
    .exercise-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      height: 26px;
      padding: 0 10px;
      background: var(--bg-tertiary, rgba(255, 255, 255, 0.05));
      border: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.1));
      color: var(--text-secondary, #9ca3af);
      border-radius: var(--radius-sm, 4px);
      font-size: 0.6875rem;
      font-weight: 400;
      text-decoration: none;
      transition: all 0.15s ease;
      white-space: nowrap;
      flex-shrink: 0;

      @media (max-width: 480px) {
        width: 26px;
        height: 26px;
        padding: 0;

        span {
          display: none;
        }
      }

      &:hover {
        background: var(--bg-secondary, rgba(255, 255, 255, 0.08));
        border-color: var(--border-default, rgba(255, 255, 255, 0.15));
        color: var(--text-primary, #fff);
      }

      ng-icon {
        color: var(--text-muted, #6b7280);
        flex-shrink: 0;
      }

      &:hover ng-icon {
        color: var(--text-secondary, #9ca3af);
      }

      &--locked {
        background: rgba(107, 105, 101, 0.1);
        border-color: rgba(107, 105, 101, 0.2);
        color: #6b7280;

        ng-icon {
          color: #6b7280;
        }

        &:hover {
          background: rgba(107, 105, 101, 0.15);
          border-color: rgba(107, 105, 101, 0.3);
        }
      }
    }
  `]
})
export class ExerciseButtonComponent {
  @Input({ required: true }) lessonId!: number;

  constructor(public paymentService: PaymentService) {}
}
