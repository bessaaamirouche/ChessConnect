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
          <p>Veuillez patienter pendant que nous activons votre abonnement Premium.</p>
        } @else if (verified()) {
          <div class="result-card__icon result-card__icon--premium">✨</div>
          <h1>Bienvenue chez les Premium !</h1>
          <p>Félicitations ! Votre abonnement <strong>{{ planName() }}</strong> est maintenant actif.</p>
          <div class="result-card__features">
            <p><strong>Vos avantages exclusifs :</strong></p>
            <ul>
              <li>✓ Revisionnage illimité de vos cours</li>
              <li>✓ Notifications prioritaires des coachs favoris</li>
              <li>✓ Accès anticipé aux créneaux (24h avant)</li>
              <li>✓ Statistiques avancées de progression</li>
              <li>✓ Badge Premium sur votre profil</li>
            </ul>
          </div>
          <div class="result-card__actions">
            <a routerLink="/teachers" class="btn btn--primary btn--lg">Réserver un cours</a>
            <a routerLink="/stats" class="btn btn--outline">Voir mes statistiques</a>
            <a routerLink="/dashboard" class="btn btn--ghost">Aller au dashboard</a>
          </div>
        } @else {
          <div class="result-card__icon result-card__icon--error">✕</div>
          <h1>Erreur</h1>
          <p>{{ error() || 'Une erreur est survenue lors de l\\'activation de l\\'abonnement.' }}</p>
          <div class="result-card__actions">
            <a routerLink="/subscription" class="btn btn--primary">Réessayer</a>
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
