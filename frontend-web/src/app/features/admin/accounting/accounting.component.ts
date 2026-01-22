import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, AccountingResponse, TeacherBalanceResponse } from '../../../core/services/admin.service';
import { DialogService } from '../../../core/services/dialog.service';

@Component({
  selector: 'app-accounting',
  standalone: true,
  imports: [CommonModule, DecimalPipe, FormsModule],
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
                <span class="stat-card__label">Gains coachs</span>
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
          <div class="section-header">
            <h2>Soldes des coachs - {{ currentMonthLabel() }}</h2>
            <div class="search-box">
              <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"></circle>
                <path d="m21 21-4.3-4.3"></path>
              </svg>
              <input
                type="text"
                [(ngModel)]="searchQuery"
                placeholder="Rechercher un coach..."
                class="search-input"
              >
              @if (searchQuery) {
                <button class="search-clear" (click)="searchQuery = ''">×</button>
              }
            </div>
          </div>
          @if (balances().length === 0) {
            <p class="empty">Aucun coach avec un solde.</p>
          } @else {
            <div class="table-container">
              <div class="table-header">
                <span class="results-count">{{ filteredBalances().length }} resultat(s)</span>
              </div>
              <table class="table">
                <thead>
                  <tr>
                    <th>Coach</th>
                    <th>Ce mois</th>
                    <th>A virer</th>
                    <th>Total gagne</th>
                    <th>Compte</th>
                    <th>Statut</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  @for (balance of filteredBalances(); track balance.teacherId) {
                    <tr>
                      <td>
                        <div class="user-info">
                          <strong>{{ balance.firstName }} {{ balance.lastName }}</strong>
                          <span class="text-muted">{{ balance.email }}</span>
                          @if (balance.companyName) {
                            <span class="text-muted">{{ balance.companyName }}</span>
                          }
                        </div>
                      </td>
                      <td>
                        <div class="month-info">
                          <span class="amount amount--primary">{{ formatCents(balance.currentMonthEarningsCents) }}</span>
                          <span class="text-muted small">{{ balance.currentMonthLessonsCount }} cours</span>
                        </div>
                      </td>
                      <td class="amount amount--success">{{ formatCents(balance.availableBalanceCents) }}</td>
                      <td class="amount">{{ formatCents(balance.totalEarnedCents) }}</td>
                      <td>
                        @if (balance.stripeConnectReady) {
                          <span class="badge badge--success">Pret</span>
                        } @else if (balance.stripeConnectEnabled) {
                          <span class="badge badge--warning">Incomplet</span>
                        } @else if (balance.iban) {
                          <div class="banking-info">
                            <span class="iban">{{ maskIban(balance.iban) }}</span>
                            @if (balance.siret) {
                              <span class="text-muted small">SIRET: {{ balance.siret }}</span>
                            }
                          </div>
                        } @else {
                          <span class="badge badge--error">Non configure</span>
                        }
                      </td>
                      <td>
                        @if (balance.currentMonthPaid) {
                          <span class="badge badge--success">Paye</span>
                        } @else if (balance.currentMonthEarningsCents > 0) {
                          <span class="badge badge--pending">A payer</span>
                        } @else {
                          <span class="badge badge--muted">-</span>
                        }
                      </td>
                      <td>
                        @if (balance.availableBalanceCents > 0) {
                          @if (balance.stripeConnectReady) {
                            <div class="transfer-action">
                              <input
                                type="number"
                                class="transfer-input"
                                [value]="balance.availableBalanceCents / 100"
                                (input)="setTransferAmount(balance.teacherId, $event)"
                                [max]="balance.availableBalanceCents / 100"
                                min="1"
                                step="0.01"
                                placeholder="Montant"
                              >
                              <button
                                class="btn btn--sm btn--primary"
                                (click)="markAsPaid(balance)"
                                [disabled]="payingTeacher() === balance.teacherId"
                              >
                                @if (payingTeacher() === balance.teacherId) {
                                  ...
                                } @else {
                                  Virer
                                }
                              </button>
                            </div>
                          } @else {
                            <span class="text-muted small">Compte non configure</span>
                          }
                        } @else {
                          <span class="text-muted small">Rien a virer</span>
                        }
                      </td>
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

      @media (max-width: 767px) {
        margin-bottom: var(--space-lg);

        h1 {
          font-size: 1.25rem;
        }
      }
    }

    .section {
      margin-bottom: var(--space-2xl);

      h2 {
        font-size: 1.125rem;
        font-weight: 600;
        margin-bottom: var(--space-md);
      }

      @media (max-width: 767px) {
        margin-bottom: var(--space-xl);
      }
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--space-md);
      flex-wrap: wrap;
      gap: var(--space-md);

      h2 {
        margin-bottom: 0;
      }

      @media (max-width: 767px) {
        flex-direction: column;
        align-items: stretch;
      }
    }

    .search-box {
      position: relative;
      display: flex;
      align-items: center;
    }

    .search-icon {
      position: absolute;
      left: 12px;
      color: var(--text-muted);
      pointer-events: none;
    }

    .search-input {
      padding: 8px 36px 8px 36px;
      font-size: 0.875rem;
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      background: var(--bg-tertiary);
      color: var(--text-primary);
      min-width: 220px;
      transition: all var(--transition-fast);

      &::placeholder {
        color: var(--text-muted);
      }

      &:focus {
        outline: none;
        border-color: var(--gold-500);
        background: var(--bg-secondary);
      }

      @media (max-width: 767px) {
        min-width: 100%;
      }
    }

    .search-clear {
      position: absolute;
      right: 8px;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg-tertiary);
      border: none;
      border-radius: 50%;
      color: var(--text-muted);
      cursor: pointer;
      font-size: 14px;
      line-height: 1;

      &:hover {
        background: var(--border-subtle);
        color: var(--text-primary);
      }
    }

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--space-sm) var(--space-md);
      background: var(--bg-tertiary);
      border-bottom: 1px solid var(--border-subtle);
    }

    .results-count {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: var(--space-md);
      margin-bottom: var(--space-lg);

      @media (max-width: 1023px) {
        grid-template-columns: repeat(2, 1fr);
      }

      @media (max-width: 480px) {
        grid-template-columns: 1fr;
        gap: var(--space-sm);
      }
    }

    .stat-card {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: var(--space-md);

      @media (max-width: 767px) {
        padding: var(--space-sm);
      }

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

        @media (max-width: 767px) {
          font-size: 1.125rem;
        }
      }
    }

    .lessons-stats {
      display: flex;
      gap: var(--space-lg);
      padding: var(--space-md);
      background: var(--bg-secondary);
      border-radius: var(--radius-md);
      border: 1px solid var(--border-subtle);

      @media (max-width: 480px) {
        flex-wrap: wrap;
        gap: var(--space-md);
      }
    }

    .lessons-stat {
      text-align: center;

      @media (max-width: 480px) {
        flex: 1 1 45%;
      }

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

        @media (max-width: 767px) {
          font-size: 1.25rem;
        }
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
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;

      @media (max-width: 767px) {
        border-radius: var(--radius-md);
      }
    }

    .table {
      width: 100%;
      border-collapse: collapse;
      min-width: 600px;

      th, td {
        padding: var(--space-md);
        text-align: left;
        border-bottom: 1px solid var(--border-subtle);
        white-space: nowrap;

        @media (max-width: 767px) {
          padding: var(--space-sm);
          font-size: 0.8125rem;
        }
      }

      th {
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--text-muted);
        background: var(--bg-tertiary);

        @media (max-width: 767px) {
          font-size: 0.6875rem;
        }
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

      &--primary {
        color: var(--gold-400);
      }
    }

    .month-info {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .banking-info {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .iban {
      font-family: var(--font-mono, monospace);
      font-size: 0.8125rem;
      color: var(--text-secondary);
    }

    .small {
      font-size: 0.75rem;
    }

    .badge {
      display: inline-block;
      padding: 4px 8px;
      font-size: 0.6875rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      border-radius: var(--radius-sm);

      &--success {
        background: var(--success-muted);
        color: var(--success);
      }

      &--pending {
        background: rgba(251, 191, 36, 0.1);
        color: #fbbf24;
      }

      &--warning {
        background: rgba(251, 146, 60, 0.1);
        color: #fb923c;
      }

      &--muted {
        background: var(--bg-tertiary);
        color: var(--text-muted);
      }

      &--error {
        background: rgba(248, 113, 113, 0.1);
        color: #f87171;
      }
    }

    .btn--sm {
      padding: 6px 12px;
      font-size: 0.75rem;
      min-height: 36px;
    }

    .btn--primary {
      background: var(--gold-500);
      color: var(--bg-primary);
      border: none;

      &:hover:not(:disabled) {
        background: var(--gold-400);
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }

    .empty {
      text-align: center;
      padding: var(--space-xl);
      color: var(--text-muted);
      background: var(--bg-secondary);
      border-radius: var(--radius-md);

      @media (max-width: 767px) {
        padding: var(--space-lg);
      }
    }

    .loading {
      text-align: center;
      padding: var(--space-2xl);
      color: var(--text-muted);
    }

    .transfer-action {
      display: flex;
      align-items: center;
      gap: var(--space-xs);
    }

    .transfer-input {
      width: 80px;
      padding: 6px 8px;
      font-size: 0.8125rem;
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      background: var(--bg-tertiary);
      color: var(--text-primary);
      text-align: right;

      &:focus {
        outline: none;
        border-color: var(--gold-500);
      }

      &::-webkit-inner-spin-button,
      &::-webkit-outer-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }
    }
  `]
})
export class AccountingComponent implements OnInit {
  accounting = signal<AccountingResponse | null>(null);
  balances = signal<TeacherBalanceResponse[]>([]);
  loading = signal(true);
  payingTeacher = signal<number | null>(null);
  searchQuery = '';
  transferAmounts: Map<number, number> = new Map(); // teacherId -> amount in cents
  private dialogService = inject(DialogService);

  // Filtered balances based on search query
  filteredBalances = computed(() => {
    const allBalances = this.balances();
    const query = this.searchQuery.toLowerCase().trim();

    if (!query) return allBalances;

    return allBalances.filter(balance => {
      const fullName = `${balance.firstName} ${balance.lastName}`.toLowerCase();
      const email = balance.email?.toLowerCase() || '';
      const companyName = balance.companyName?.toLowerCase() || '';
      return fullName.includes(query) ||
             balance.firstName.toLowerCase().includes(query) ||
             balance.lastName.toLowerCase().includes(query) ||
             email.includes(query) ||
             companyName.includes(query);
    });
  });

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

  currentMonthLabel(): string {
    const date = new Date();
    return date.toLocaleString('fr-FR', { month: 'long', year: 'numeric' });
  }

  maskIban(iban: string): string {
    if (!iban) return '';
    // Show first 4 and last 4 characters, mask the rest
    if (iban.length <= 8) return iban;
    return iban.slice(0, 4) + '****' + iban.slice(-4);
  }

  setTransferAmount(teacherId: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const amountEuros = parseFloat(input.value) || 0;
    const amountCents = Math.round(amountEuros * 100);
    this.transferAmounts.set(teacherId, amountCents);
  }

  getTransferAmount(balance: TeacherBalanceResponse): number {
    return this.transferAmounts.get(balance.teacherId) ?? balance.availableBalanceCents;
  }

  async markAsPaid(balance: TeacherBalanceResponse): Promise<void> {
    const amountCents = this.getTransferAmount(balance);

    if (amountCents <= 0) {
      await this.dialogService.alert('Le montant doit etre superieur a 0', 'Erreur', { variant: 'danger' });
      return;
    }

    if (amountCents > balance.availableBalanceCents) {
      await this.dialogService.alert(
        `Le montant ne peut pas depasser le solde disponible (${this.formatCents(balance.availableBalanceCents)})`,
        'Erreur',
        { variant: 'danger' }
      );
      return;
    }

    const confirmed = await this.dialogService.confirm(
      `Confirmer le virement de ${this.formatCents(amountCents)} a ${balance.firstName} ${balance.lastName} ?`,
      'Confirmer le virement',
      { confirmText: 'Effectuer le virement', cancelText: 'Annuler', variant: 'info' }
    );
    if (!confirmed) return;

    this.payingTeacher.set(balance.teacherId);
    this.adminService.markTeacherPaid(balance.teacherId, undefined, undefined, undefined, amountCents).subscribe({
      next: (response) => {
        this.payingTeacher.set(null);
        if (response.success) {
          const msg = response.stripeTransferId
            ? `Virement effectue avec succes ! Ref: ${response.stripeTransferId}`
            : 'Virement effectue avec succes !';
          this.dialogService.alert(msg, 'Succes', { variant: 'success' });
          this.loadData();
        } else {
          this.dialogService.alert(response.message || 'Erreur lors du virement', 'Erreur', { variant: 'danger' });
        }
      },
      error: (err) => {
        this.payingTeacher.set(null);
        this.dialogService.alert(err.error?.message || 'Erreur lors du virement', 'Erreur', { variant: 'danger' });
      }
    });
  }
}
