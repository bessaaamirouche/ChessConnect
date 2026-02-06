import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'app-links',
  standalone: true,
  imports: [TranslateModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="links-page">
      <div class="links-page__container">
        <img src="assets/logo.webp" alt="myChess" class="links-page__logo" loading="lazy">
        <p class="links-page__tagline">{{ 'links.tagline' | translate }}</p>

        <div class="links-page__buttons">
          <a href="https://www.instagram.com/mychess_learning" target="_blank" rel="noopener noreferrer" class="links-page__btn links-page__btn--instagram">
            <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
              <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/>
            </svg>
            <span>Instagram</span>
          </a>

          <a href="https://www.facebook.com/share/1PmSHzcb8F/" target="_blank" rel="noopener noreferrer" class="links-page__btn links-page__btn--facebook">
            <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
              <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
            </svg>
            <span>Facebook</span>
          </a>
        </div>

        <a routerLink="/" class="links-page__home">{{ 'nav.home' | translate }}</a>

        <p class="links-page__footer">&copy; 2026 mychess</p>
      </div>
    </div>
  `,
  styles: [`
    @use '../../../styles/variables' as *;

    .links-page {
      min-height: 100vh;
      min-height: 100dvh;
      background: var(--bg-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: var(--space-xl);

      &__container {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--space-xl);
        max-width: 400px;
        width: 100%;
      }

      &__logo {
        height: 48px;
        object-fit: contain;
      }

      &__tagline {
        font-size: 0.9375rem;
        color: var(--text-secondary);
        text-align: center;
      }

      &__buttons {
        display: flex;
        flex-direction: column;
        gap: var(--space-md);
        width: 100%;
      }

      &__btn {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: var(--space-sm);
        padding: var(--space-md) var(--space-lg);
        border-radius: var(--radius-lg);
        font-size: 1rem;
        font-weight: 600;
        text-decoration: none;
        transition: all var(--transition-base);

        svg {
          width: 22px;
          height: 22px;
          flex-shrink: 0;
        }

        &:hover {
          transform: translateY(-2px);
          box-shadow: var(--shadow-lg);
        }

        &:active {
          transform: translateY(0);
        }

        &--instagram {
          background: linear-gradient(135deg, #833ab4, #fd1d1d, #fcb045);
          color: #fff;

          &:hover {
            box-shadow: 0 8px 24px rgba(131, 58, 180, 0.4);
          }
        }

        &--facebook {
          background: #1877f2;
          color: #fff;

          &:hover {
            box-shadow: 0 8px 24px rgba(24, 119, 242, 0.4);
          }
        }
      }

      &__home {
        font-size: 0.9375rem;
        color: var(--gold-400);
        transition: opacity var(--transition-fast);

        &:hover {
          opacity: 0.8;
        }
      }

      &__footer {
        font-size: 0.8125rem;
        color: var(--text-muted);
        margin-top: var(--space-lg);
      }
    }
  `]
})
export class LinksComponent {
  private seoService = inject(SeoService);

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Liens - mychess | Retrouvez-nous sur les réseaux',
      description: 'Suivez mychess sur Instagram et Facebook. Cours d\'échecs en ligne avec des coachs experts.',
      keywords: 'mychess, instagram, facebook, réseaux sociaux, échecs'
    });
  }
}
