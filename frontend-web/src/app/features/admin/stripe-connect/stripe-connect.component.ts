import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService, StripeConnectAccount } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroCheckCircle,
  heroXCircle,
  heroArrowTopRightOnSquare,
  heroExclamationTriangle,
  heroBanknotes,
  heroArrowPath
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-stripe-connect',
  standalone: true,
  imports: [CommonModule, NgIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroCheckCircle,
    heroXCircle,
    heroArrowTopRightOnSquare,
    heroExclamationTriangle,
    heroBanknotes,
    heroArrowPath
  })],
  template: `
    <div class="stripe-connect-page">
      <div class="page-header">
        <h1>Stripe Connect</h1>
        <p class="page-subtitle">Gerez les comptes Stripe des coachs</p>
        <button class="btn btn--ghost btn--sm" (click)="loadAccounts()">
          <ng-icon name="heroArrowPath" size="16"></ng-icon>
          Actualiser
        </button>
      </div>

      @if (loading()) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>Chargement des comptes Stripe...</p>
        </div>
      } @else if (error()) {
        <div class="error-state">
          <ng-icon name="heroExclamationTriangle" size="48"></ng-icon>
          <p>{{ error() }}</p>
          <button class="btn btn--primary" (click)="loadAccounts()">Reessayer</button>
        </div>
      } @else {
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-value">{{ accounts().length }}</div>
            <div class="stat-label">Total coachs</div>
          </div>
          <div class="stat-card stat-card--success">
            <div class="stat-value">{{ accountsReady }}</div>
            <div class="stat-label">Comptes actifs</div>
          </div>
          <div class="stat-card stat-card--warning">
            <div class="stat-value">{{ accountsPending }}</div>
            <div class="stat-label">En attente</div>
          </div>
          <div class="stat-card stat-card--danger">
            <div class="stat-value">{{ accountsNotConfigured }}</div>
            <div class="stat-label">Non configures</div>
          </div>
        </div>

        <div class="accounts-table">
          <table>
            <thead>
              <tr>
                <th>Coach</th>
                <th>Email</th>
                <th>Compte Stripe</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (account of accounts(); track account.teacherId) {
                <tr>
                  <td class="coach-name">{{ account.teacherName }}</td>
                  <td class="coach-email">{{ account.teacherEmail }}</td>
                  <td class="stripe-id">
                    @if (account.stripeAccountId) {
                      <code>{{ account.stripeAccountId | slice:0:20 }}...</code>
                    } @else {
                      <span class="not-configured">Non configure</span>
                    }
                  </td>
                  <td>
                    @if (account.isReady) {
                      <span class="status status--success">
                        <ng-icon name="heroCheckCircle" size="16"></ng-icon>
                        Actif
                      </span>
                    } @else if (account.hasStripeAccount && !account.isReady) {
                      <span class="status status--warning">
                        <ng-icon name="heroExclamationTriangle" size="16"></ng-icon>
                        En attente
                      </span>
                    } @else {
                      <span class="status status--danger">
                        <ng-icon name="heroXCircle" size="16"></ng-icon>
                        Non configure
                      </span>
                    }
                  </td>
                  <td class="actions">
                    @if (account.hasStripeAccount) {
                      <button
                        class="btn btn--ghost btn--sm"
                        (click)="openExpressDashboard(account)"
                        [disabled]="loadingDashboard() === account.teacherId"
                        title="Ouvrir Express Dashboard"
                      >
                        @if (loadingDashboard() === account.teacherId) {
                          <span class="spinner spinner--sm"></span>
                        } @else {
                          <ng-icon name="heroArrowTopRightOnSquare" size="16"></ng-icon>
                        }
                        Dashboard
                      </button>
                    }
                    @if (account.pendingRequirements) {
                      <span class="pending-info" [title]="account.pendingRequirements">
                        Documents requis
                      </span>
                    }
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="empty-state">Aucun coach trouve</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: [`
    .stripe-connect-page {
      padding: var(--space-lg);
    }

    .page-header {
      display: flex;
      align-items: center;
      gap: var(--space-md);
      margin-bottom: var(--space-xl);

      h1 {
        font-size: 1.5rem;
        font-weight: 600;
        margin: 0;
      }

      .page-subtitle {
        color: var(--text-muted);
        margin: 0;
        flex: 1;
      }
    }

    .stats-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: var(--space-md);
      margin-bottom: var(--space-xl);

      @media (max-width: 768px) {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    .stat-card {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      padding: var(--space-lg);
      text-align: center;

      .stat-value {
        font-size: 2rem;
        font-weight: 700;
        color: var(--text-primary);
      }

      .stat-label {
        font-size: 0.875rem;
        color: var(--text-muted);
        margin-top: var(--space-xs);
      }

      &--success .stat-value { color: var(--success); }
      &--warning .stat-value { color: var(--warning); }
      &--danger .stat-value { color: var(--error); }
    }

    .accounts-table {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      overflow: hidden;

      table {
        width: 100%;
        border-collapse: collapse;
      }

      th, td {
        padding: var(--space-md);
        text-align: left;
        border-bottom: 1px solid var(--border-subtle);
      }

      th {
        background: var(--bg-tertiary);
        font-weight: 600;
        font-size: 0.875rem;
        color: var(--text-secondary);
      }

      tr:last-child td {
        border-bottom: none;
      }

      tr:hover {
        background: var(--bg-tertiary);
      }
    }

    .coach-name {
      font-weight: 500;
    }

    .coach-email {
      color: var(--text-muted);
      font-size: 0.875rem;
    }

    .stripe-id {
      code {
        background: var(--bg-tertiary);
        padding: 2px 6px;
        border-radius: var(--radius-sm);
        font-size: 0.75rem;
      }

      .not-configured {
        color: var(--text-muted);
        font-style: italic;
      }
    }

    .status {
      display: inline-flex;
      align-items: center;
      gap: var(--space-xs);
      padding: 4px 8px;
      border-radius: var(--radius-full);
      font-size: 0.75rem;
      font-weight: 500;

      &--success {
        background: rgba(34, 197, 94, 0.1);
        color: var(--success);
      }

      &--warning {
        background: rgba(234, 179, 8, 0.1);
        color: var(--warning);
      }

      &--danger {
        background: rgba(239, 68, 68, 0.1);
        color: var(--error);
      }
    }

    .actions {
      display: flex;
      align-items: center;
      gap: var(--space-sm);
    }

    .pending-info {
      font-size: 0.75rem;
      color: var(--warning);
      cursor: help;
    }

    .loading-state, .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--space-3xl);
      color: var(--text-muted);
      gap: var(--space-md);
    }

    .empty-state {
      text-align: center;
      color: var(--text-muted);
      padding: var(--space-xl) !important;
    }

    .spinner--sm {
      width: 16px;
      height: 16px;
    }
  `]
})
export class StripeConnectComponent implements OnInit {
  accounts = signal<StripeConnectAccount[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  loadingDashboard = signal<number | null>(null);

  constructor(
    private adminService: AdminService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loading.set(true);
    this.error.set(null);

    this.adminService.getStripeConnectAccounts().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Erreur lors du chargement');
        this.loading.set(false);
      }
    });
  }

  get accountsReady(): number {
    return this.accounts().filter(a => a.isReady).length;
  }

  get accountsPending(): number {
    return this.accounts().filter(a => a.hasStripeAccount && !a.isReady).length;
  }

  get accountsNotConfigured(): number {
    return this.accounts().filter(a => !a.hasStripeAccount).length;
  }

  openExpressDashboard(account: StripeConnectAccount): void {
    this.loadingDashboard.set(account.teacherId);

    this.adminService.getExpressDashboardLink(account.teacherId).subscribe({
      next: (response) => {
        this.loadingDashboard.set(null);
        if (response.success && response.dashboardUrl) {
          window.open(response.dashboardUrl, '_blank');
        } else {
          this.toastService.error(response.message || 'Erreur');
        }
      },
      error: (err) => {
        this.loadingDashboard.set(null);
        this.toastService.error(err.error?.message || 'Erreur');
      }
    });
  }
}
