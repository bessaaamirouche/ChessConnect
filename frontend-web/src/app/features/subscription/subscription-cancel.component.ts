import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-subscription-cancel',
    imports: [RouterLink, TranslateModule],
    template: `
    <div class="result-page">
      <div class="result-card">
        <div class="result-card__icon">✗</div>
        <h1>{{ 'subscriptionCancelled.title' | translate }}</h1>
        <p>{{ 'subscriptionCancelled.description' | translate }}</p>
        <p class="result-card__details">
          {{ 'subscriptionCancelled.details' | translate }}
        </p>
        <div class="result-card__actions">
          <a routerLink="/subscription" class="btn btn--primary btn--lg">{{ 'subscriptionCancelled.viewPlans' | translate }}</a>
          <a routerLink="/dashboard" class="btn btn--ghost">{{ 'subscriptionCancelled.backToDashboard' | translate }}</a>
        </div>
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

      &__icon {
        width: 80px;
        height: 80px;
        margin: 0 auto 1.5rem;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(107, 114, 128, 0.1);
        border-radius: 50%;
        font-size: 2.5rem;
        color: var(--text-muted);
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

      &__actions {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        margin-top: 2rem;
      }
    }
  `]
})
export class SubscriptionCancelComponent {}
