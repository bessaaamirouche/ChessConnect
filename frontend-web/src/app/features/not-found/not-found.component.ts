import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="not-found">
      <div class="not-found__content">
        <div class="not-found__icon" aria-hidden="true">
          <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <path d="M16 16s-1.5-2-4-2-4 2-4 2"></path>
            <line x1="9" y1="9" x2="9.01" y2="9"></line>
            <line x1="15" y1="9" x2="15.01" y2="9"></line>
          </svg>
        </div>

        <h1 class="not-found__title">Page introuvable</h1>
        <p class="not-found__code">Erreur 404</p>
        <p class="not-found__message">
          Oups ! La page que vous recherchez n'existe pas ou a ete deplacee.
        </p>

        <div class="not-found__actions">
          <a routerLink="/" class="btn btn--primary btn--lg">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
              <polyline points="9 22 9 12 15 12 15 22"></polyline>
            </svg>
            Retour a l'accueil
          </a>
          <a routerLink="/teachers" class="btn btn--ghost btn--lg">
            Voir nos coachs
          </a>
        </div>

        <div class="not-found__suggestions">
          <p>Suggestions :</p>
          <ul>
            <li><a routerLink="/blog">Consulter nos articles</a></li>
            <li><a routerLink="/register">Creer un compte</a></li>
            <li><a routerLink="/login">Se connecter</a></li>
          </ul>
        </div>
      </div>

      <a routerLink="/" class="not-found__logo">
        <img src="assets/logo.png" alt="mychess" />
      </a>
    </div>
  `,
  styles: [`
    .not-found {
      min-height: 100vh;
      min-height: 100dvh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      background:
        radial-gradient(ellipse at top, rgba(212, 168, 75, 0.08) 0%, transparent 50%),
        var(--bg-primary);
      position: relative;
    }

    .not-found__logo {
      position: absolute;
      top: 1.5rem;
      left: 1.5rem;

      img {
        height: 40px;
        width: auto;
      }
    }

    .not-found__content {
      text-align: center;
      max-width: 480px;
    }

    .not-found__icon {
      color: var(--gold-400);
      margin-bottom: 1.5rem;
      opacity: 0.8;

      svg {
        filter: drop-shadow(0 0 20px rgba(212, 168, 75, 0.3));
      }
    }

    .not-found__title {
      font-size: 2rem;
      font-weight: 700;
      color: var(--text-primary);
      margin-bottom: 0.5rem;

      @media (min-width: 640px) {
        font-size: 2.5rem;
      }
    }

    .not-found__code {
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--gold-400);
      text-transform: uppercase;
      letter-spacing: 0.1em;
      margin-bottom: 1rem;
    }

    .not-found__message {
      font-size: 1rem;
      color: var(--text-secondary);
      line-height: 1.6;
      margin-bottom: 2rem;

      @media (min-width: 640px) {
        font-size: 1.125rem;
      }
    }

    .not-found__actions {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      margin-bottom: 2.5rem;

      @media (min-width: 480px) {
        flex-direction: row;
        justify-content: center;
      }

      .btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
      }
    }

    .not-found__suggestions {
      padding-top: 1.5rem;
      border-top: 1px solid var(--border-subtle);

      p {
        font-size: 0.8125rem;
        color: var(--text-muted);
        margin-bottom: 0.75rem;
      }

      ul {
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
        gap: 0.5rem 1.5rem;
      }

      a {
        font-size: 0.875rem;
        color: var(--gold-400);
        text-decoration: none;
        transition: opacity var(--transition-fast);

        &:hover {
          opacity: 0.8;
          text-decoration: underline;
        }
      }
    }

    .btn {
      padding: 0.75rem 1.5rem;
      font-size: 0.9375rem;
      font-weight: 600;
      border-radius: var(--radius-md);
      text-decoration: none;
      transition: all var(--transition-fast);
      cursor: pointer;
      border: none;

      &--primary {
        background: linear-gradient(135deg, var(--gold-500) 0%, var(--gold-600) 100%);
        color: var(--bg-primary);

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(212, 168, 75, 0.3);
        }
      }

      &--ghost {
        background: transparent;
        color: var(--text-secondary);
        border: 1px solid var(--border-default);

        &:hover {
          background: var(--bg-tertiary);
          color: var(--text-primary);
        }
      }

      &--lg {
        padding: 0.875rem 1.75rem;
      }
    }
  `]
})
export class NotFoundComponent {}
