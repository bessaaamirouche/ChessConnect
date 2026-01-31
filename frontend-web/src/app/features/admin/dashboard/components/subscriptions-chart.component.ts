import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';

export interface DataPoint {
  date: string;
  value: number;
}

@Component({
  selector: 'app-subscriptions-chart',
  standalone: true,
  imports: [CommonModule, NgChartsModule],
  template: `
    <div class="chart-card">
      <div class="chart-header">
        <h3>Abonnements Premium</h3>
        <div class="legend-inline">
          <span class="legend-item legend-new">Nouveaux</span>
          <span class="legend-item legend-renewal">Renouvellements</span>
          <span class="legend-item legend-cancel">Désabonnements</span>
        </div>
      </div>
      <div class="chart-container">
        <canvas baseChart
          [data]="chartData()"
          [options]="chartOptions"
          [type]="'line'">
        </canvas>
      </div>
    </div>
  `,
  styles: [`
    .chart-card {
      background: var(--surface-secondary, #1a1a1a);
      border-radius: 12px;
      padding: 1.5rem;
      border: 1px solid var(--border-color, #333);
    }

    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
      flex-wrap: wrap;
      gap: 0.75rem;
    }

    .chart-header h3 {
      margin: 0;
      font-size: 1.125rem;
      font-weight: 600;
      color: var(--text-primary, #fff);
    }

    .legend-inline {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
    }

    .legend-item {
      display: flex;
      align-items: center;
      font-size: 0.75rem;
      color: var(--text-secondary, #888);
    }

    .legend-item::before {
      content: '';
      width: 10px;
      height: 10px;
      border-radius: 50%;
      margin-right: 0.375rem;
    }

    .legend-new::before {
      background: #10B981;
    }

    .legend-renewal::before {
      background: #d4af37;
    }

    .legend-cancel::before {
      background: #EF4444;
    }

    .chart-container {
      position: relative;
      height: 250px;
    }
  `]
})
export class SubscriptionsChartComponent {
  newSubscriptions = input.required<DataPoint[]>();
  renewals = input.required<DataPoint[]>();
  cancellations = input.required<DataPoint[]>();

  chartData = computed<ChartConfiguration<'line'>['data']>(() => {
    const newSubs = this.newSubscriptions();
    const renewalData = this.renewals();
    const cancelData = this.cancellations();

    return {
      labels: newSubs.map(d => this.formatDate(d.date)),
      datasets: [
        {
          label: 'Nouveaux',
          data: newSubs.map(d => d.value),
          borderColor: '#10B981',
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          fill: true,
          tension: 0.4
        },
        {
          label: 'Renouvellements',
          data: renewalData.map(d => d.value),
          borderColor: '#d4af37',
          backgroundColor: 'rgba(212, 175, 55, 0.1)',
          fill: true,
          tension: 0.4
        },
        {
          label: 'Désabonnements',
          data: cancelData.map(d => d.value),
          borderColor: '#EF4444',
          backgroundColor: 'rgba(239, 68, 68, 0.1)',
          fill: true,
          tension: 0.4
        }
      ]
    };
  });

  chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          stepSize: 1,
          color: '#666'
        },
        grid: {
          color: 'rgba(255,255,255,0.05)'
        }
      },
      x: {
        ticks: {
          color: '#666',
          maxRotation: 45,
          minRotation: 0
        },
        grid: {
          display: false
        }
      }
    }
  };

  private formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    const day = date.getDate();
    const month = date.toLocaleDateString('fr-FR', { month: 'short' });
    return `${day} ${month}`;
  }
}
