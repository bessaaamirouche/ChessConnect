import { Component, OnInit, signal, inject } from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PaymentService } from '../../core/services/payment.service';

@Component({
    selector: 'app-subscription-success',
    imports: [RouterLink, TranslateModule],
    template: `
    <div class="result-page">
      <div class="result-card result-card--success">
        @if (loading()) {
          <div class="result-card__icon">
            <span class="spinner spinner--lg"></span>
          </div>
          <h1>{{ 'subscriptionSuccess.activating' | translate }}</h1>
          <p>{{ 'subscriptionSuccess.pleaseWait' | translate }}</p>
        } @else if (verified()) {
          <div class="result-card__icon result-card__icon--premium">✨</div>
          <h1>{{ 'subscriptionSuccess.welcome' | translate }}</h1>
          <p>{{ 'subscriptionSuccess.congratulations' | translate }}</p>
          <div class="result-card__features">
            <p><strong>{{ 'subscriptionSuccess.exclusiveBenefits' | translate }}</strong></p>
            <ul>
              <li>✓ {{ 'subscriptionSuccess.unlimitedReplay' | translate }}</li>
              <li>✓ {{ 'subscriptionSuccess.priorityNotifications' | translate }}</li>
              <li>✓ {{ 'subscriptionSuccess.premiumBadge' | translate }}</li>
            </ul>
          </div>
          <div class="result-card__actions">
            <a routerLink="/teachers" class="btn btn--primary btn--lg">{{ 'subscriptionSuccess.bookLesson' | translate }}</a>
            <a routerLink="/dashboard" class="btn btn--ghost">{{ 'subscriptionSuccess.goToDashboard' | translate }}</a>
          </div>
        } @else {
          <div class="result-card__icon result-card__icon--error">✕</div>
          <h1>{{ 'status.error' | translate }}</h1>
          <p>{{ error() || ('errors.subscriptionActivate' | translate) }}</p>
          <div class="result-card__actions">
            <a routerLink="/subscription" class="btn btn--primary">{{ 'common.retry' | translate }}</a>
            <a routerLink="/dashboard" class="btn btn--ghost">{{ 'subscriptionSuccess.goToDashboard' | translate }}</a>
          </div>
        }
      </div>
    </div>
  `,
    styles: [`
    .result-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      background: var(--bg-primary);
    }

    .result-card {
      max-width: 500px;
      width: 100%;
      background: var(--bg-secondary);
      border-radius: 1.5rem;
      padding: 3rem;
      text-align: center;
      border: 1px solid var(--border-subtle);

      &--success {
        border-color: var(--gold-400);
      }

      &__icon {
        width: 80px;
        height: 80px;
        margin: 0 auto 1.5rem;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(34, 197, 94, 0.1);
        border-radius: 50%;
        font-size: 2.5rem;
        color: var(--success-500);

        &--premium {
          background: linear-gradient(135deg, rgba(212, 168, 75, 0.2), rgba(212, 168, 75, 0.1));
          color: var(--gold-400);
        }

        &--warning {
          background: rgba(234, 179, 8, 0.1);
          color: var(--warning);
        }

        &--error {
          background: rgba(239, 68, 68, 0.1);
          color: var(--error);
        }
      }

      h1 {
        font-size: 1.5rem;
        font-weight: 700;
        color: var(--text-primary);
        margin: 0 0 1rem;
      }

      p {
        color: var(--text-secondary);
        margin: 0 0 0.5rem;
      }

      &__features {
        margin-top: 1.5rem;
        padding: 1rem;
        background: rgba(212, 168, 75, 0.05);
        border: 1px solid rgba(212, 168, 75, 0.2);
        border-radius: 0.75rem;
        text-align: left;

        p {
          margin-bottom: 0.75rem;
          color: var(--gold-400);
        }

        ul {
          list-style: none;
          padding: 0;
          margin: 0;
          display: grid;
          gap: 0.5rem;

          li {
            font-size: 0.875rem;
            color: var(--text-secondary);
          }
        }
      }

      &__actions {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        margin-top: 2rem;
      }
    }

    .btn--outline {
      background: transparent;
      border: 1px solid var(--gold-400);
      color: var(--gold-400);

      &:hover {
        background: rgba(212, 168, 75, 0.1);
      }
    }
  `]
})
export class SubscriptionSuccessComponent implements OnInit {
  loading = signal(true);
  verified = signal(false);
  planName = signal<string>('Premium');
  error = signal<string | null>(null);

  private translateService = inject(TranslateService);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    const sessionId = this.route.snapshot.queryParamMap.get('session_id');

    if (sessionId) {
      // Confirm and activate subscription
      this.paymentService.confirmSubscriptionPayment(sessionId).subscribe({
        next: (result) => {
          if (result.success) {
            this.verified.set(true);
            this.planName.set(result.planName || 'Premium');
          } else {
            this.error.set(result.error || this.translateService.instant('errors.unknownError'));
          }
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.error || this.translateService.instant('errors.subscriptionActivate'));
          this.loading.set(false);
        }
      });
    } else {
      this.error.set(this.translateService.instant('errors.invalidSession'));
      this.loading.set(false);
    }
  }
}
