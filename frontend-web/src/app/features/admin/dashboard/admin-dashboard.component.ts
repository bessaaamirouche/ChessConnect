import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService, AdminStatsResponse, AnalyticsResponse } from '../../../core/services/admin.service';
import { RegistrationsChartComponent } from './components/registrations-chart.component';
import { SubscriptionsChartComponent } from './components/subscriptions-chart.component';
import { VisitsChartComponent } from './components/visits-chart.component';

// Register Chart.js components
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    DecimalPipe,
    RegistrationsChartComponent,
    SubscriptionsChartComponent,
    VisitsChartComponent
  ],
  template: `
    <div class="admin-dashboard">
      <header class="page-header">
        <h1>Tableau de bord</h1>
        <p class="text-secondary">Vue d'ensemble de la plateforme</p>
      </header>

      @if (loading()) {
        <div class="loading">Chargement...</div>
      } @else if (stats()) {
        <div class="stats-grid">
          <div class="stat-card">
            <span class="stat-card__label">Utilisateurs</span>
            <span class="stat-card__value">{{ stats()!.totalUsers | number }}</span>
            <span class="stat-card__detail">
              {{ stats()!.totalStudents }} joueurs / {{ stats()!.totalTeachers }} profs
            </span>
          </div>

          <div class="stat-card">
            <span class="stat-card__label">Abonnements actifs</span>
            <span class="stat-card__value">{{ stats()!.activeSubscriptions | number }}</span>
          </div>

          <div class="stat-card">
            <span class="stat-card__label">Cours total</span>
            <span class="stat-card__value">{{ stats()!.totalLessons | number }}</span>
            <span class="stat-card__detail">
              {{ stats()!.lessonsThisMonth }} ce mois
            </span>
          </div>

          <div class="stat-card stat-card--gold">
            <span class="stat-card__label">Chiffre d'affaires total</span>
            <span class="stat-card__value">{{ formatCents(stats()!.totalRevenueCents) }}</span>
            <span class="stat-card__detail">
              {{ formatCents(stats()!.revenueThisMonthCents) }} ce mois
            </span>
          </div>
        </div>

        <!-- Analytics Charts Section -->
        <section class="analytics-section">
          <h2>Analytiques</h2>

          @if (analyticsLoading()) {
            <div class="analytics-loading">Chargement des graphiques...</div>
          } @else if (analytics()) {
            <div class="charts-grid">
              <app-registrations-chart
                [students]="analytics()!.studentRegistrations"
                [teachers]="analytics()!.teacherRegistrations"
                [period]="selectedPeriod()"
                (periodChange)="onPeriodChange($event)"
              />

              <app-subscriptions-chart
                [newSubscriptions]="analytics()!.newSubscriptions"
                [renewals]="analytics()!.renewals"
                [cancellations]="analytics()!.cancellations"
              />

              <div class="chart-full-width">
                <app-visits-chart
                  [dailyVisits]="analytics()!.dailyVisits"
                  [hourlyVisits]="analytics()!.hourlyVisits"
                />
              </div>
            </div>
          }
        </section>

        <div class="quick-actions">
          <h2>Actions rapides</h2>
          <div class="action-grid">
            <a routerLink="/admin/users" class="action-card">
              <span class="action-card__title">Gerer les utilisateurs</span>
              <span class="action-card__desc">Voir, suspendre ou reactiver des comptes</span>
            </a>
            <a routerLink="/admin/accounting" class="action-card">
              <span class="action-card__title">Comptabilite</span>
              <span class="action-card__desc">Revenus, commissions et soldes coachs</span>
            </a>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .admin-dashboard {
      max-width: 1200px;
    }

    .page-header {
      margin-bottom: var(--space-xl);

      h1 {
        font-size: 1.5rem;
        font-weight: 700;
        margin-bottom: var(--space-xs);
      }

      @media (max-width: 767px) {
        margin-bottom: var(--space-lg);

        h1 {
          font-size: 1.25rem;
        }
      }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: var(--space-lg);
      margin-bottom: var(--space-2xl);

      @media (max-width: 1023px) {
        grid-template-columns: repeat(2, 1fr);
        gap: var(--space-md);
      }

      @media (max-width: 480px) {
        grid-template-columns: 1fr;
        gap: var(--space-sm);
        margin-bottom: var(--space-xl);
      }
    }

    .stat-card {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      padding: var(--space-lg);
      display: flex;
      flex-direction: column;

      @media (max-width: 767px) {
        padding: var(--space-md);
      }

      &--gold {
        border-color: var(--gold-500);
        background: linear-gradient(135deg, rgba(212, 168, 75, 0.1), transparent);
      }

      &__label {
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--text-muted);
        margin-bottom: var(--space-xs);
      }

      &__value {
        font-size: 1.75rem;
        font-weight: 700;
        color: var(--text-primary);

        @media (max-width: 767px) {
          font-size: 1.5rem;
        }
      }

      &__detail {
        font-size: 0.8125rem;
        color: var(--text-secondary);
        margin-top: var(--space-xs);
      }
    }

    .analytics-section {
      margin-bottom: var(--space-2xl);

      h2 {
        font-size: 1.125rem;
        font-weight: 600;
        margin-bottom: var(--space-lg);
      }
    }

    .charts-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: var(--space-lg);

      @media (max-width: 900px) {
        grid-template-columns: 1fr;
      }
    }

    .chart-full-width {
      grid-column: 1 / -1;
    }

    .analytics-loading {
      text-align: center;
      padding: var(--space-xl);
      color: var(--text-muted);
      background: var(--bg-secondary);
      border-radius: var(--radius-lg);
    }

    .quick-actions {
      h2 {
        font-size: 1.125rem;
        font-weight: 600;
        margin-bottom: var(--space-md);
      }
    }

    .action-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: var(--space-md);

      @media (max-width: 640px) {
        grid-template-columns: 1fr;
        gap: var(--space-sm);
      }
    }

    .action-card {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: var(--space-lg);
      text-decoration: none;
      transition: all var(--transition-fast);

      @media (max-width: 767px) {
        padding: var(--space-md);
      }

      &:hover {
        border-color: var(--gold-500);
        transform: translateY(-2px);
      }

      &__title {
        display: block;
        font-weight: 600;
        color: var(--text-primary);
        margin-bottom: var(--space-xs);
      }

      &__desc {
        font-size: 0.8125rem;
        color: var(--text-secondary);
      }
    }

    .loading {
      text-align: center;
      padding: var(--space-2xl);
      color: var(--text-muted);
    }

    .text-secondary {
      color: var(--text-secondary);
    }
  `]
})
export class AdminDashboardComponent implements OnInit {
  stats = signal<AdminStatsResponse | null>(null);
  analytics = signal<AnalyticsResponse | null>(null);
  loading = signal(true);
  analyticsLoading = signal(true);
  selectedPeriod = signal<'day' | 'week' | 'month'>('day');

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadAnalytics();
  }

  loadStats(): void {
    this.adminService.getStats().subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  loadAnalytics(): void {
    this.analyticsLoading.set(true);
    this.adminService.getAnalytics(this.selectedPeriod()).subscribe({
      next: (data) => {
        this.analytics.set(data);
        this.analyticsLoading.set(false);
      },
      error: () => {
        this.analyticsLoading.set(false);
      }
    });
  }

  onPeriodChange(period: 'day' | 'week' | 'month'): void {
    this.selectedPeriod.set(period);
    this.loadAnalytics();
  }

  formatCents(cents: number): string {
    return (cents / 100).toFixed(2) + ' EUR';
  }
}
