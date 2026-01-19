import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChartBarSquare,
  heroUsers,
  heroCalendarDays,
  heroBanknotes,
  heroCog6Tooth,
  heroArrowRightOnRectangle
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, NgIconComponent],
  viewProviders: [provideIcons({
    heroChartBarSquare,
    heroUsers,
    heroCalendarDays,
    heroBanknotes,
    heroCog6Tooth,
    heroArrowRightOnRectangle
  })],
  template: `
    <div class="admin-layout">
      <aside class="admin-sidebar">
        <div class="admin-sidebar__header">
          <a routerLink="/" class="admin-sidebar__logo">ChessConnect</a>
          <span class="admin-sidebar__badge">Admin</span>
        </div>

        <nav class="admin-sidebar__nav">
          <a routerLink="/admin/dashboard" routerLinkActive="active" class="admin-sidebar__link">
            <ng-icon name="heroChartBarSquare"></ng-icon>
            <span>Tableau de bord</span>
          </a>
          <a routerLink="/admin/users" routerLinkActive="active" class="admin-sidebar__link">
            <ng-icon name="heroUsers"></ng-icon>
            <span>Utilisateurs</span>
          </a>
          <a routerLink="/admin/lessons" routerLinkActive="active" class="admin-sidebar__link">
            <ng-icon name="heroCalendarDays"></ng-icon>
            <span>Cours</span>
          </a>
          <a routerLink="/admin/accounting" routerLinkActive="active" class="admin-sidebar__link">
            <ng-icon name="heroBanknotes"></ng-icon>
            <span>Comptabilite</span>
          </a>
        </nav>

        <div class="admin-sidebar__footer">
          <a routerLink="/settings" class="admin-sidebar__link">
            <ng-icon name="heroCog6Tooth"></ng-icon>
            <span>Parametres</span>
          </a>
          <button (click)="logout()" class="admin-sidebar__link">
            <ng-icon name="heroArrowRightOnRectangle"></ng-icon>
            <span>Deconnexion</span>
          </button>
        </div>
      </aside>

      <main class="admin-main">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .admin-layout {
      display: grid;
      grid-template-columns: 240px 1fr;
      min-height: 100vh;
    }

    .admin-sidebar {
      background: var(--bg-secondary);
      border-right: 1px solid var(--border-subtle);
      display: flex;
      flex-direction: column;
      padding: var(--space-lg);

      &__header {
        display: flex;
        flex-direction: column;
        gap: var(--space-xs);
        padding-bottom: var(--space-lg);
        border-bottom: 1px solid var(--border-subtle);
        margin-bottom: var(--space-lg);
      }

      &__logo {
        font-family: var(--font-display);
        font-size: 1.25rem;
        font-weight: 700;
        background: linear-gradient(135deg, var(--gold-400), var(--gold-500));
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }

      &__badge {
        font-size: 0.6875rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--text-muted);
        background: var(--bg-tertiary);
        padding: 2px 8px;
        border-radius: var(--radius-sm);
        width: fit-content;
      }

      &__nav {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: var(--space-xs);
      }

      &__link {
        display: flex;
        align-items: center;
        gap: var(--space-sm);
        padding: var(--space-sm) var(--space-md);
        color: var(--text-secondary);
        border-radius: var(--radius-md);
        font-size: 0.875rem;
        text-decoration: none;
        background: transparent;
        border: none;
        cursor: pointer;
        width: 100%;
        text-align: left;
        transition: all var(--transition-fast);

        &:hover {
          background: var(--bg-tertiary);
          color: var(--text-primary);
        }

        &.active {
          background: rgba(212, 168, 75, 0.1);
          color: var(--gold-400);
        }

        ng-icon {
          font-size: 1.125rem;
        }
      }

      &__footer {
        padding-top: var(--space-lg);
        border-top: 1px solid var(--border-subtle);
        display: flex;
        flex-direction: column;
        gap: var(--space-xs);
      }
    }

    .admin-main {
      padding: var(--space-xl);
      background: var(--bg-primary);
      overflow-y: auto;
    }
  `]
})
export class AdminLayoutComponent {
  constructor(private authService: AuthService) {}

  logout(): void {
    this.authService.logout();
  }
}
