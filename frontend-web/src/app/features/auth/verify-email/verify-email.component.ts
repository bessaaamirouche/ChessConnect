import { Component, OnInit, signal, inject, ChangeDetectionStrategy, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroCheckCircle, heroXCircle, heroArrowRight } from '@ng-icons/heroicons/outline';

@Component({
    selector: 'app-verify-email',
    imports: [RouterLink, NgIconComponent, TranslateModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [provideIcons({ heroCheckCircle, heroXCircle, heroArrowRight })],
    template: `
    <div class="auth-page">
      <a routerLink="/" class="auth-page__logo">
        <img src="assets/logo.webp" alt="mychess" class="auth-page__logo-img">
      </a>
      <div class="auth-card card">
        @if (loading()) {
          <div class="verify-status">
            <div class="spinner spinner--lg"></div>
            <h2 class="heading-3">Verification en cours...</h2>
            <p class="text-secondary">Veuillez patienter</p>
          </div>
        } @else if (success()) {
          <div class="verify-status verify-status--success">
            <div class="verify-status__icon">
              <ng-icon name="heroCheckCircle" size="64"></ng-icon>
            </div>
            <h2 class="heading-3">Email verifie !</h2>
            <p class="text-secondary">Votre adresse email a ete confirmee avec succes.</p>
            <p class="text-secondary">Vous pouvez maintenant vous connecter a votre compte.</p>
            <a routerLink="/login" class="btn btn--primary btn--lg btn--block">
              <ng-icon name="heroArrowRight"></ng-icon>
              Se connecter
            </a>
          </div>
        } @else {
          <div class="verify-status verify-status--error">
            <div class="verify-status__icon">
              <ng-icon name="heroXCircle" size="64"></ng-icon>
            </div>
            <h2 class="heading-3">Lien invalide</h2>
            <p class="text-secondary">{{ errorMessage() }}</p>
            <div class="verify-status__actions">
              <a routerLink="/login" class="btn btn--primary btn--block">
                Aller a la page de connexion
              </a>
              <p class="text-muted text-sm">
                Vous pourrez demander un nouvel email de verification depuis la page de connexion.
              </p>
            </div>
          </div>
        }
      </div>
    </div>
  `,
    styles: [`
    .auth-page {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--space-lg);
      background: var(--bg-primary);
    }

    .auth-page__logo {
      margin-bottom: var(--space-xl);
    }

    .auth-page__logo-img {
      height: 64px;
      width: auto;
    }

    .auth-card {
      width: 100%;
      max-width: 420px;
      padding: var(--space-2xl);
      text-align: center;
    }

    .verify-status {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--space-md);
    }

    .verify-status__icon {
      margin-bottom: var(--space-sm);
    }

    .verify-status--success .verify-status__icon {
      color: var(--success);
    }

    .verify-status--error .verify-status__icon {
      color: var(--error);
    }

    .verify-status__actions {
      margin-top: var(--space-lg);
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: var(--space-md);
    }

    .heading-3 {
      margin: 0;
    }

    .text-secondary {
      margin: 0;
    }

    .text-muted {
      color: var(--text-muted);
    }

    .text-sm {
      font-size: 0.875rem;
    }

    .btn--block {
      width: 100%;
      margin-top: var(--space-md);
    }
  `]
})
export class VerifyEmailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private platformId = inject(PLATFORM_ID);
  private translateService = inject(TranslateService);

  loading = signal(true);
  success = signal(false);
  errorMessage = signal('');

  ngOnInit(): void {
    // Only run verification in browser (not during SSR)
    // This prevents the token from being used twice (once on server, once on client)
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.loading.set(false);
      this.errorMessage.set(this.translateService.instant('errors.tokenInvalid'));
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.success.set(response.success);
        if (!response.success) {
          this.errorMessage.set(response.message);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.success.set(false);
        this.errorMessage.set(err.error?.message || this.translateService.instant('errors.linkExpired'));
      }
    });
  }
}
