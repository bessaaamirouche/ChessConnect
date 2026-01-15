import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { PaymentService } from '../../core/services/payment.service';

@Component({
  selector: 'app-subscription-success',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="result-page">
      <div class="result-card result-card--success">
        @if (loading()) {
          <div class="result-card__icon">
            <span class="spinner spinner--lg"></span>
          </div>
          <h1>Activation de l'abonnement...</h1>
          <p>Veuillez patienter pendant que nous activons votre abonnement.</p>
        } @else if (verified()) {
          <div class="result-card__icon">✓</div>
          <h1>Abonnement active !</h1>
          <p>Felicitations ! Votre abonnement <strong>{{ planName() }}</strong> est maintenant actif.</p>
          <p class="result-card__quota">Vous avez <strong>{{ monthlyQuota() }} cours</strong> disponibles ce mois.</p>
          <p class="result-card__details">
            Vous pouvez desormais reserver vos cours avec les professeurs acceptant les abonnements.
          </p>
          <div class="result-card__actions">
            <a routerLink="/teachers" class="btn btn--primary btn--lg">Reserver un cours</a>
            <a routerLink="/dashboard" class="btn btn--ghost">Aller au dashboard</a>
          </div>
        } @else {
          <div class="result-card__icon result-card__icon--error">✕</div>
          <h1>Erreur</h1>
          <p>{{ error() || 'Une erreur est survenue lors de l\\'activation de l\\'abonnement.' }}</p>
          <div class="result-card__actions">
            <a routerLink="/subscription" class="btn btn--primary">Reessayer</a>
            <a routerLink="/dashboard" class="btn btn--ghost">Aller au dashboard</a>
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
        border-color: var(--success-500);
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

      &__details {
        margin-top: 1rem;
        padding-top: 1rem;
        border-top: 1px solid var(--border-subtle);
      }

      &__quota {
        background: rgba(212, 168, 75, 0.1);
        padding: 0.75rem 1rem;
        border-radius: 0.5rem;
        color: var(--gold-400);
        margin-top: 1rem;

        strong {
          color: var(--gold-300);
        }
      }

      &__actions {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        margin-top: 2rem;
      }
    }
  `]
})
export class SubscriptionSuccessComponent implements OnInit {
  loading = signal(true);
  verified = signal(false);
  planName = signal<string>('');
  monthlyQuota = signal<number>(0);
  error = signal<string | null>(null);

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
            this.planName.set(result.planName || '');
            this.monthlyQuota.set(result.monthlyQuota || 0);
          } else {
            this.error.set(result.error || 'Erreur inconnue');
          }
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.error || 'Erreur lors de l\'activation');
          this.loading.set(false);
        }
      });
    } else {
      this.error.set('Session de paiement invalide');
      this.loading.set(false);
    }
  }
}
