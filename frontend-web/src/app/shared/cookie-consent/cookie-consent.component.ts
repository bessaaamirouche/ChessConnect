import { Component, inject, ChangeDetectionStrategy } from '@angular/core';

import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { CookieConsentService } from '../../core/services/cookie-consent.service';

@Component({
    selector: 'app-cookie-consent',
    imports: [RouterLink, TranslateModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    @if (cookieService.showBanner()) {
      <div class="cookie-banner" [@slideIn]>
        <div class="cookie-content">
          <div class="cookie-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5"/>
              <circle cx="8" cy="9" r="1.5" fill="currentColor"/>
              <circle cx="15" cy="8" r="1" fill="currentColor"/>
              <circle cx="10" cy="14" r="1" fill="currentColor"/>
              <circle cx="16" cy="13" r="1.5" fill="currentColor"/>
              <circle cx="13" cy="17" r="1" fill="currentColor"/>
            </svg>
          </div>
          <p class="cookie-text">
            {{ 'cookies.message' | translate }}
            <a routerLink="/privacy" (click)="closeBanner()">{{ 'cookies.learnMore' | translate }}</a>
          </p>
        </div>
        <div class="cookie-actions">
          <button class="btn btn--ghost" (click)="reject()">{{ 'cookies.reject' | translate }}</button>
          <button class="btn btn--primary" (click)="accept()">{{ 'cookies.accept' | translate }}</button>
        </div>
      </div>
    }
  `,
    styles: [`
    .cookie-banner {
      position: fixed;
      bottom: 1.5rem;
      left: 50%;
      transform: translateX(-50%);
      z-index: 350;

      display: flex;
      align-items: center;
      gap: 1.5rem;

      max-width: min(600px, calc(100vw - 2rem));
      padding: 1rem 1.25rem;

      background: rgba(22, 22, 26, 0.85);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border: 1px solid rgba(212, 168, 75, 0.15);
      border-radius: 1rem;
      box-shadow:
        0 4px 24px rgba(0, 0, 0, 0.4),
        0 0 0 1px rgba(255, 255, 255, 0.03) inset;

      animation: slideInUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    }

    @keyframes slideInUp {
      from {
        opacity: 0;
        transform: translateX(-50%) translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateX(-50%) translateY(0);
      }
    }

    .cookie-content {
      display: flex;
      align-items: center;
      gap: 0.875rem;
      flex: 1;
      min-width: 0;
    }

    .cookie-icon {
      flex-shrink: 0;
      color: var(--gold-400, #d4a84b);
      opacity: 0.9;
    }

    .cookie-text {
      margin: 0;
      font-size: 0.875rem;
      line-height: 1.5;
      color: var(--text-secondary, #a8a5a0);

      a {
        color: var(--gold-400, #d4a84b);
        text-decoration: none;
        transition: color 0.2s ease;

        &:hover {
          color: var(--gold-300, #ffd666);
          text-decoration: underline;
        }
      }
    }

    .cookie-actions {
      display: flex;
      gap: 0.5rem;
      flex-shrink: 0;
    }

    .btn {
      padding: 0.5rem 1rem;
      font-size: 0.8125rem;
      font-weight: 500;
      border-radius: 0.5rem;
      cursor: pointer;
      transition: all 0.2s ease;
      border: none;
      white-space: nowrap;
    }

    .btn--ghost {
      background: transparent;
      color: var(--text-secondary, #a8a5a0);

      &:hover {
        background: rgba(255, 255, 255, 0.05);
        color: var(--text-primary, #f5f3eb);
      }
    }

    .btn--primary {
      background: linear-gradient(135deg, var(--gold-400, #d4a84b) 0%, var(--gold-500, #b8923a) 100%);
      color: var(--bg-primary, #0d0d0f);
      font-weight: 600;

      &:hover {
        background: linear-gradient(135deg, var(--gold-300, #ffd666) 0%, var(--gold-400, #d4a84b) 100%);
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(212, 168, 75, 0.3);
      }
    }

    /* Mobile responsive */
    @media (max-width: 640px) {
      .cookie-banner {
        bottom: 1rem;
        left: 1rem;
        right: 1rem;
        transform: none;
        flex-direction: column;
        gap: 1rem;
        max-width: none;
      }

      @keyframes slideInUp {
        from {
          opacity: 0;
          transform: translateY(20px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .cookie-content {
        width: 100%;
      }

      .cookie-actions {
        width: 100%;
        justify-content: flex-end;
      }

      .btn {
        padding: 0.625rem 1.25rem;
      }
    }

    /* Reduced motion */
    @media (prefers-reduced-motion: reduce) {
      .cookie-banner {
        animation: none;
        opacity: 1;
      }
    }
  `]
})
export class CookieConsentComponent {
  cookieService = inject(CookieConsentService);

  accept(): void {
    this.cookieService.acceptAll();
  }

  reject(): void {
    this.cookieService.rejectAll();
  }

  closeBanner(): void {
    // Just hide the banner temporarily when navigating to privacy page
    // User will still need to make a choice when they return
  }
}
