import { Injectable, signal, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface MaintenanceStatus {
  enabled: boolean;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class MaintenanceService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly http = inject(HttpClient);

  private maintenanceMode = signal(false);
  private maintenanceMessage = signal('');

  readonly isMaintenanceMode = this.maintenanceMode.asReadonly();
  readonly message = this.maintenanceMessage.asReadonly();

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.checkStatus();
    }
  }

  private checkStatus(): void {
    this.http.get<MaintenanceStatus>('/api/maintenance/status').subscribe({
      next: (status) => {
        this.maintenanceMode.set(status.enabled);
        this.maintenanceMessage.set(status.message);
      },
      error: () => {
        // If we can't reach the API, don't show maintenance banner
      }
    });
  }
}
