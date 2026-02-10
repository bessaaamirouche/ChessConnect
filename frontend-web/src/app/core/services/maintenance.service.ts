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

  private maintenanceMode = signal(true);
  private maintenanceMessage = signal('mychess est actuellement en maintenance. Les réservations et paiements sont temporairement suspendus.');

  readonly isMaintenanceMode = this.maintenanceMode.asReadonly();
  readonly message = this.maintenanceMessage.asReadonly();
}
