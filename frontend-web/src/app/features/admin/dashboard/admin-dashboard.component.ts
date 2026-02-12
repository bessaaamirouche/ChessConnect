import { Component, OnInit, DestroyRef, signal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AdminService, AdminStatsResponse, AnalyticsResponse } from '../../../core/services/admin.service';
import { AdminStateService } from '../../../core/services/admin-state.service';
import { RegistrationsChartComponent } from './components/registrations-chart.component';
import { SubscriptionsChartComponent } from './components/subscriptions-chart.component';
import { VisitsChartComponent } from './components/visits-chart.component';

// Register Chart.js components
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
    selector: 'app-admin-dashboard',
    imports: [
        RouterLink,
        DecimalPipe,
        TranslateModule,
        RegistrationsChartComponent,
        SubscriptionsChartComponent,
        VisitsChartComponent
    ],
    template: `
    <div class="admin-dashboard">
      <header class="page-header">
        <h1>{{ 'admin.dashboard.title' | translate }}</h1>
        <p class="text-secondary">{{ 'admin.dashboard.subtitle' | translate }}</p>
      </header>

      @if (loading()) {
        <div class="loading">{{ 'common.loading' | translate }}</div>
      } @else if (stats()) {
        <div class="stats-grid">
          <div class="stat-card">
            <span class="stat-card__label">{{ 'admin.dashboard.users' | translate }}</span>
            <span class="stat-card__value">{{ stats()!.totalUsers | number }}</span>
            <span class="stat-card__detail">
              {{ stats()!.totalStudents }} {{ 'admin.dashboard.playersCoaches' | translate : { totalTeachers: stats()!.totalTeachers } }}
            </span>
          </div>

          <div class="stat-card">
            <span class="stat-card__label">{{ 'admin.dashboard.activeSubscriptions' | translate }}</span>
            <span class="stat-card__value">{{ stats()!.activeSubscriptions | number }}</span>
          </div>

          <div class="stat-card">
            <span class="stat-card__label">{{ 'admin.dashboard.totalLessons' | translate }}</span>
            <span class="stat-card__value">{{ stats()!.totalLessons | number }}</span>
            <span class="stat-card__detail">
              {{ stats()!.lessonsThisMonth }} {{ 'admin.dashboard.thisMonth' | translate }}
            </span>
          </div>

          <div class="stat-card stat-card--gold">
            <span class="stat-card__label">{{ 'admin.dashboard.totalRevenue' | translate }}</span>
            <span class="stat-card__value">{{ formatCents(stats()!.totalRevenueCents) }}</span>
            <span class="stat-card__detail">
              {{ formatCents(stats()!.revenueThisMonthCents) }} {{ 'admin.dashboard.thisMonth' | translate }}
            </span>
          </div>
        </div>

        <!-- Analytics Charts Section -->
        <section class="analytics-section">
          <h2>{{ 'admin.dashboard.analytics' | translate }}</h2>

          @if (analyticsLoading()) {
            <div class="analytics-loading">{{ 'admin.dashboard.loadingCharts' | translate }}</div>
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
          <h2>{{ 'admin.dashboard.quickActions' | translate }}</h2>
          <div class="action-grid">
            <a routerLink="/mint/users" class="action-card">
              <span class="action-card__title">{{ 'admin.dashboard.manageUsers' | translate }}</span>
              <span class="action-card__desc">{{ 'admin.dashboard.manageUsersDesc' | translate }}</span>
            </a>
            <a routerLink="/mint/accounting" class="action-card">
              <span class="action-card__title">{{ 'admin.dashboard.accounting' | translate }}</span>
              <span class="action-card__desc">{{ 'admin.dashboard.accountingDesc' | translate }}</span>
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
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: var(--radius-lg);
      padding: var(--space-lg);
      display: flex;
      flex-direction: column;
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

      &:hover {
        border-color: var(--card-border-hover);
        transform: translateY(-2px);
      }

      @media (max-width: 767px) {
        padding: var(--space-md);
      }

      &--gold {
        border-color: rgba(212, 168, 75, 0.2);
        background: linear-gradient(135deg, rgba(212, 168, 75, 0.08), var(--card-bg));

        &:hover {
          border-color: rgba(212, 168, 75, 0.4);
          box-shadow: 0 8px 32px rgba(212, 168, 75, 0.1);
        }
      }

      &__label {
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: rgba(255, 255, 255, 0.35);
        margin-bottom: var(--space-xs);
      }

      &__value {
        font-size: 1.75rem;
        font-weight: 700;
        color: #ffffff;

        @media (max-width: 767px) {
          font-size: 1.5rem;
        }
      }

      &__detail {
        font-size: 0.8125rem;
        color: rgba(255, 255, 255, 0.5);
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
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: var(--radius-md);
      padding: var(--space-lg);
      text-decoration: none;
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

      @media (max-width: 767px) {
        padding: var(--space-md);
      }

      &:hover {
        border-color: rgba(212, 168, 75, 0.4);
        transform: translateY(-2px);
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
      }

      &__title {
        display: block;
        font-weight: 600;
        color: #ffffff;
        margin-bottom: var(--space-xs);
      }

      &__desc {
        font-size: 0.8125rem;
        color: rgba(255, 255, 255, 0.5);
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

  private adminStateService = inject(AdminStateService);
  private destroyRef = inject(DestroyRef);

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadAnalytics();

    // Subscribe to data changes from other admin components
    this.adminStateService.onDataChange$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((change) => {
      // Refresh stats when users or other data changes
      if (change.type === 'user' || change.type === 'all') {
        this.loadStats();
      }
      // Refresh analytics when relevant data changes
      if (change.type === 'subscription' || change.type === 'all') {
        this.loadAnalytics();
      }
    });
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
