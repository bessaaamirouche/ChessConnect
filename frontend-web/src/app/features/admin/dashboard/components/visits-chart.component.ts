import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';

export interface DataPoint {
  date: string;
  value: number;
}

export interface HourlyDataPoint {
  hour: number;
  value: number;
}

@Component({
  selector: 'app-visits-chart',
  standalone: true,
  imports: [CommonModule, NgChartsModule],
  template: `
    <div class="chart-card">
      <div class="chart-header">
        <h3>Visites du site</h3>
        <div class="total-visits">
          <span class="total-label">Total période :</span>
          <span class="total-value">{{ totalVisits() }}</span>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-section">
          <h4>Visites par jour</h4>
          <div class="chart-container">
            <canvas baseChart
              [data]="dailyChartData()"
              [options]="lineChartOptions"
              [type]="'line'">
            </canvas>
          </div>
        </div>

        <div class="chart-section">
          <h4>Heures de pointe</h4>
          <div class="chart-container">
            <canvas baseChart
              [data]="hourlyChartData()"
              [options]="barChartOptions"
              [type]="'bar'">
            </canvas>
          </div>
        </div>
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

    .total-visits {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .total-label {
      font-size: 0.875rem;
      color: var(--text-secondary, #888);
    }

    .total-value {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--gold, #d4af37);
    }

    .charts-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.5rem;
    }

    @media (max-width: 900px) {
      .charts-row {
        grid-template-columns: 1fr;
      }
    }

    .chart-section h4 {
      font-size: 0.875rem;
      color: var(--text-secondary, #888);
      margin: 0 0 0.75rem 0;
      font-weight: 500;
    }

    .chart-container {
      position: relative;
      height: 200px;
    }
  `]
})
export class VisitsChartComponent {
  dailyVisits = input.required<DataPoint[]>();
  hourlyVisits = input.required<HourlyDataPoint[]>();

  totalVisits = computed(() => {
    return this.dailyVisits().reduce((sum, d) => sum + d.value, 0);
  });

  dailyChartData = computed<ChartConfiguration<'line'>['data']>(() => {
    const data = this.dailyVisits();
    return {
      labels: data.map(d => this.formatDate(d.date)),
      datasets: [
        {
          label: 'Visites',
          data: data.map(d => d.value),
          borderColor: '#06B6D4',
          backgroundColor: 'rgba(6, 182, 212, 0.1)',
          fill: true,
          tension: 0.4
        }
      ]
    };
  });

  hourlyChartData = computed<ChartConfiguration<'bar'>['data']>(() => {
    const data = this.hourlyVisits();
    return {
      labels: data.map(d => `${d.hour}h`),
      datasets: [
        {
          label: 'Visites',
          data: data.map(d => d.value),
          backgroundColor: data.map(d => {
            // Highlight peak hours
            const max = Math.max(...data.map(x => x.value));
            return d.value === max && d.value > 0
              ? 'rgba(212, 175, 55, 0.8)'
              : 'rgba(6, 182, 212, 0.6)';
          }),
          borderRadius: 4
        }
      ]
    };
  });

  lineChartOptions: ChartOptions<'line'> = {
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

  barChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        callbacks: {
          title: (items) => `${items[0].label}`,
          label: (item) => `${item.raw} visites`
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          color: '#666'
        },
        grid: {
          color: 'rgba(255,255,255,0.05)'
        }
      },
      x: {
        ticks: {
          color: '#666',
          font: {
            size: 10
          }
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
