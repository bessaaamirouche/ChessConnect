import { Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';

export interface DataPoint {
  date: string;
  value: number;
}

@Component({
  selector: 'app-registrations-chart',
  standalone: true,
  imports: [CommonModule, NgChartsModule],
  template: `
    <div class="chart-card">
      <div class="chart-header">
        <h3>Inscriptions</h3>
        <div class="period-selector">
          <button
            [class.active]="period() === 'day'"
            (click)="periodChange.emit('day')"
          >7 jours</button>
          <button
            [class.active]="period() === 'week'"
            (click)="periodChange.emit('week')"
          >4 semaines</button>
          <button
            [class.active]="period() === 'month'"
            (click)="periodChange.emit('month')"
          >12 mois</button>
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

    .period-selector {
      display: flex;
      gap: 0.25rem;
      background: var(--surface-tertiary, #252525);
      border-radius: 8px;
      padding: 0.25rem;
    }

    .period-selector button {
      padding: 0.375rem 0.75rem;
      font-size: 0.75rem;
      border: none;
      background: transparent;
      color: var(--text-secondary, #888);
      cursor: pointer;
      border-radius: 6px;
      transition: all 0.2s;
    }

    .period-selector button:hover {
      color: var(--text-primary, #fff);
    }

    .period-selector button.active {
      background: var(--gold, #d4af37);
      color: #000;
      font-weight: 500;
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

  chartData = computed<ChartConfiguration<'line'>['data']>(() => {
    const studentData = this.students();
    const teacherData = this.teachers();

    return {
      labels: studentData.map(d => this.formatDate(d.date)),
      datasets: [
        {
          label: 'Joueurs',
          data: studentData.map(d => d.value),
          borderColor: '#4F46E5',
          backgroundColor: 'rgba(79, 70, 229, 0.1)',
          fill: true,
          tension: 0.4
        },
        {
          label: 'Coachs',
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
