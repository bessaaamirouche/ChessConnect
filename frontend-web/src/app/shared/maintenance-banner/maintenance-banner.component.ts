import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { MaintenanceService } from '../../core/services/maintenance.service';

@Component({
  selector: 'app-maintenance-banner',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (maintenanceService.isMaintenanceMode()) {
      <div class="maintenance-banner">
        <div class="maintenance-content">
          <div class="maintenance-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6-3.7 3.7-1.6-1.6a1 1 0 0 0-1.4 0l-4.3 4.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l4.3-4.3a1 1 0 0 0 0-1.4L11 11.4l3.7-3.7 1.6 1.6a1 1 0 0 0 1.4 0l2-2a1 1 0 0 0 0-1.4l-1.6-1.6a1 1 0 0 0-1.4 0l-2 2z" fill="currentColor"/>
              <path d="M3.5 21.5l3-3m-3 3l-1-1m1 1l1 1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <path d="M20.5 2.5l-3 3m3-3l1 1m-1-1l-1-1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <p class="maintenance-text">
            {{ maintenanceService.message() || 'mychess est actuellement en maintenance. Les réservations et paiements sont temporairement suspendus.' }}
          </p>
        </div>
      </div>
    }
  `,
  styles: [`
    .maintenance-banner {
      position: fixed;
      top: 72px;
      left: 0;
      right: 0;
      z-index: 999;

      display: none;
      justify-content: center;

      padding: 1rem 1.5rem;

      background: rgba(22, 22, 26, 0.95);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border-bottom: 1px solid rgba(212, 168, 75, 0.3);
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
    }

    .maintenance-content {
      display: flex;
      align-items: center;
      gap: 0.875rem;
      max-width: 800px;
    }

    .maintenance-icon {
      flex-shrink: 0;
      color: var(--gold-400, #d4a84b);
      filter: drop-shadow(0 0 6px rgba(212, 168, 75, 0.4));
    }

    .maintenance-text {
      margin: 0;
      font-size: 0.9375rem;
      line-height: 1.5;
      font-weight: 500;
      color: var(--gold-400, #d4a84b);
    }

    @media (min-width: 1024px) {
      .maintenance-banner {
        top: 80px;
      }
    }

    @media (max-width: 640px) {
      .maintenance-banner {
        top: 64px;
        padding: 0.75rem 1rem;
      }

      .maintenance-text {
        font-size: 0.8125rem;
      }
    }
  `]
})
export class MaintenanceBannerComponent {
  maintenanceService = inject(MaintenanceService);
}
