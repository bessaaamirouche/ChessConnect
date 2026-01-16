import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { AdminService, AccountingResponse, TeacherBalanceResponse } from '../../../core/services/admin.service';

@Component({
  selector: 'app-accounting',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  template: `
    <div class="accounting">
      <header class="page-header">
        <h1>Comptabilite</h1>
      </header>

      @if (loading()) {
        <div class="loading">Chargement...</div>
      } @else {
        <!-- Revenue Overview -->
        @if (accounting()) {
          <section class="section">
            <h2>Vue d'ensemble</h2>
            <div class="stats-grid">
              <div class="stat-card">
                <span class="stat-card__label">Chiffre d'affaires</span>
                <span class="stat-card__value">{{ formatCents(accounting()!.totalRevenueCents) }}</span>
              </div>
              <div class="stat-card stat-card--gold">
                <span class="stat-card__label">Commissions (10%)</span>
                <span class="stat-card__value">{{ formatCents(accounting()!.totalCommissionsCents) }}</span>
              </div>
              <div class="stat-card">
                <span class="stat-card__label">Gains professeurs</span>
                <span class="stat-card__value">{{ formatCents(accounting()!.totalTeacherEarningsCents) }}</span>
              </div>
              <div class="stat-card stat-card--error">
                <span class="stat-card__label">Remboursements</span>
                <span class="stat-card__value">{{ formatCents(accounting()!.totalRefundedCents) }}</span>
              </div>
            </div>

            <div class="lessons-stats">
              <div class="lessons-stat">
                <span class="lessons-stat__value">{{ accounting()!.totalLessons }}</span>
                <span class="lessons-stat__label">Cours total</span>
              </div>
              <div class="lessons-stat lessons-stat--success">
                <span class="lessons-stat__value">{{ accounting()!.completedLessons }}</span>
                <span class="lessons-stat__label">Termines</span>
              </div>
              <div class="lessons-stat lessons-stat--error">
                <span class="lessons-stat__value">{{ accounting()!.cancelledLessons }}</span>
                <span class="lessons-stat__label">Annules</span>
              </div>
            </div>
          </section>
        }

        <!-- Teacher Balances -->
        <section class="section">
          <h2>Soldes des professeurs</h2>
          @if (balances().length === 0) {
            <p class="empty">Aucun professeur avec un solde.</p>
          } @else {
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Professeur</th>
                    <th>Solde disponible</th>
                    <th>En attente</th>
                    <th>Total gagne</th>
                    <th>Cours termines</th>
                  </tr>
                </thead>
                <tbody>
                  @for (balance of balances(); track balance.teacherId) {
                    <tr>
                      <td>
                        <div class="user-info">
                          <strong>{{ balance.firstName }} {{ balance.lastName }}</strong>
                          <span class="text-muted">{{ balance.email }}</span>
                        </div>
                      </td>
                      <td class="amount amount--success">{{ formatCents(balance.availableBalanceCents) }}</td>
                      <td class="amount">{{ formatCents(balance.pendingBalanceCents) }}</td>
                      <td class="amount">{{ formatCents(balance.totalEarnedCents) }}</td>
                      <td>{{ balance.lessonsCompleted }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>
      }
    </div>
  `,
  styles: [`
    .page-header {
      margin-bottom: var(--space-xl);

      h1 {
        font-size: 1.5rem;
        font-weight: 700;
      }
    }

    .section {
      margin-bottom: var(--space-2xl);

      h2 {
        font-size: 1.125rem;
        font-weight: 600;
        margin-bottom: var(--space-md);
      }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: var(--space-md);
      margin-bottom: var(--space-lg);
    }

    .stat-card {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: var(--space-md);

      &--gold {
        border-color: var(--gold-500);
        background: rgba(212, 168, 75, 0.05);
      }

      &--error {
        border-color: var(--error);
        background: rgba(248, 113, 113, 0.05);
      }

      &__label {
        display: block;
        font-size: 0.6875rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--text-muted);
        margin-bottom: var(--space-xs);
      }

      &__value {
        font-size: 1.25rem;
        font-weight: 700;
        color: var(--text-primary);
      }
    }

    .lessons-stats {
      display: flex;
      gap: var(--space-lg);
      padding: var(--space-md);
      background: var(--bg-secondary);
      border-radius: var(--radius-md);
      border: 1px solid var(--border-subtle);
    }

    .lessons-stat {
      text-align: center;

      &--success .lessons-stat__value {
        color: var(--success);
      }

      &--error .lessons-stat__value {
        color: var(--error);
      }

      &__value {
        display: block;
        font-size: 1.5rem;
        font-weight: 700;
        color: var(--text-primary);
      }

      &__label {
        font-size: 0.75rem;
        color: var(--text-muted);
      }
    }

    .table-container {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      overflow: hidden;
    }

    .table {
      width: 100%;
      border-collapse: collapse;

      th, td {
        padding: var(--space-md);
        text-align: left;
        border-bottom: 1px solid var(--border-subtle);
      }

      th {
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--text-muted);
        background: var(--bg-tertiary);
      }

      tbody tr:hover {
        background: var(--bg-tertiary);
      }

      tbody tr:last-child td {
        border-bottom: none;
      }
    }

    .user-info {
      display: flex;
      flex-direction: column;
      gap: 2px;

      strong {
        color: var(--text-primary);
      }

      .text-muted {
        font-size: 0.8125rem;
        color: var(--text-muted);
      }
    }

    .amount {
      font-weight: 600;
      font-family: var(--font-mono, monospace);

      &--success {
        color: var(--success);
      }
    }

    .empty {
      text-align: center;
      padding: var(--space-xl);
      color: var(--text-muted);
      background: var(--bg-secondary);
      border-radius: var(--radius-md);
    }

    .loading {
      text-align: center;
      padding: var(--space-2xl);
      color: var(--text-muted);
    }
  `]
})
export class AccountingComponent implements OnInit {
  accounting = signal<AccountingResponse | null>(null);
  balances = signal<TeacherBalanceResponse[]>([]);
  loading = signal(true);

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    // Load accounting overview
    this.adminService.getAccountingOverview().subscribe({
      next: (data) => this.accounting.set(data)
    });

    // Load teacher balances
    this.adminService.getTeacherBalances().subscribe({
      next: (data) => {
        this.balances.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  formatCents(cents: number): string {
    return (cents / 100).toFixed(2) + ' EUR';
  }
}
