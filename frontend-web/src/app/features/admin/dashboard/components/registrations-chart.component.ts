import { Component, computed, input, output, inject } from '@angular/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';

export interface DataPoint {
  date: string;
  value: number;
}

@Component({
    selector: 'app-registrations-chart',
    imports: [BaseChartDirective, TranslateModule],
    template: `
    <div class="chart-card">
      <div class="chart-header">
        <h3>{{ 'admin.charts.registrations' | translate }}</h3>
        <div class="period-selector">
          <button
            [class.active]="period() === 'day'"
            (click)="periodChange.emit('day')"
          >{{ 'admin.charts.days7' | translate }}</button>
          <button
            [class.active]="period() === 'week'"
            (click)="periodChange.emit('week')"
          >{{ 'admin.charts.weeks4' | translate }}</button>
          <button
            [class.active]="period() === 'month'"
            (click)="periodChange.emit('month')"
          >{{ 'admin.charts.months12' | translate }}</button>
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

    /* Disney+ style pill selector */
    .period-selector {
      display: flex;
      gap: 0;
      background: rgba(255, 255, 255, 0.08);
      border-radius: 9999px;
      padding: 4px;
      border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .period-selector button {
      position: relative;
      padding: 8px 16px;
      font-size: 0.8125rem;
      font-weight: 500;
      border: none;
      background: transparent;
      color: rgba(255, 255, 255, 0.6);
      cursor: pointer;
      border-radius: 9999px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      white-space: nowrap;
    }

    .period-selector button:hover:not(.active) {
      color: rgba(255, 255, 255, 0.9);
    }

    .period-selector button.active {
      background: #fff;
      color: #000;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    }

    .chart-container {
      position: relative;
      height: 250px;
    }
  `]
})
export class RegistrationsChartComponent {
  students = input.required<DataPoint[]>();
  teachers = input.required<DataPoint[]>();
  period = input<'day' | 'week' | 'month'>('day');
  periodChange = output<'day' | 'week' | 'month'>();
  private translate = inject(TranslateService);

  chartData = computed<ChartConfiguration<'line'>['data']>(() => {
    const studentData = this.students();
    const teacherData = this.teachers();

    return {
      labels: studentData.map(d => this.formatDate(d.date)),
      datasets: [
        {
          label: this.translate.instant('admin.charts.players'),
          data: studentData.map(d => d.value),
          borderColor: '#4F46E5',
          backgroundColor: 'rgba(79, 70, 229, 0.1)',
          fill: true,
          tension: 0.4
        },
        {
          label: this.translate.instant('admin.charts.coaches'),
          data: teacherData.map(d => d.value),
          borderColor: '#8B5CF6',
          backgroundColor: 'rgba(139, 92, 246, 0.1)',
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
        position: 'bottom',
        labels: {
          color: '#888',
          usePointStyle: true,
          padding: 20
        }
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
