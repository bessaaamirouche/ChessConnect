import { Injectable, inject, signal, PLATFORM_ID, Inject } from '@angular/core'; // Ajout de PLATFORM_ID et Inject
import { isPlatformBrowser } from '@angular/common'; // Ajout de isPlatformBrowser
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

export interface PushSubscriptionStatus {
  enabled: boolean;
  subscriptionCount: number;
  hasSubscriptions: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class PushNotificationService {
  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID); // Injection de la plateforme
  private translate = inject(TranslateService);

  // Service worker registration
  private swRegistration: ServiceWorkerRegistration | null = null;

  // Reactive state
  readonly supported = signal(false);
  readonly subscribed = signal(false);
  readonly permission = signal<NotificationPermission>('default');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // VAPID public key from server
  private vapidPublicKey: string | null = null;

  constructor() {
    // 🛡️ PROTECTION SSR : On ne vérifie le support que si on est sur le client
    if (isPlatformBrowser(this.platformId)) {
      this.checkSupport();
    }
  }

  /**
   * Check if push notifications are supported in this browser.
   */
  private checkSupport(): void {
    // Ici, on est sûr d'être dans le navigateur grâce au check du constructeur
    const isSupported =
      'serviceWorker' in navigator &&
      'PushManager' in window &&
      'Notification' in window;

    this.supported.set(isSupported);

    if (isSupported) {
      this.permission.set(Notification.permission);
    }
  }

  /**
   * Initialize the push notification service.
   * Should be called when user is authenticated.
   */
  async init(): Promise<void> {
    if (!this.supported()) {
      console.log('[Push] Not supported in this browser');
      return;
    }

    try {
      // Get VAPID key from server
      await this.fetchVapidKey();

      // Register service worker
      await this.registerServiceWorker();

      // Check existing subscription
      await this.checkSubscription();

      console.log('[Push] Initialized successfully');
    } catch (err) {
      console.error('[Push] Failed to initialize:', err);
      this.error.set(this.translate.instant('errors.pushInitFailed'));
    }
  }

  /**
   * Fetch VAPID public key from the server.
   */
  private async fetchVapidKey(): Promise<void> {
    try {
      const response = await firstValueFrom(
        this.http.get<{ publicKey: string }>('/api/push/vapid-key')
      );
      this.vapidPublicKey = response.publicKey;
    } catch (err) {
      console.error('[Push] Failed to fetch VAPID key:', err);
      throw err;
    }
  }

  /**
   * Register the service worker for push notifications.
   */
  private async registerServiceWorker(): Promise<void> {
    try {
      this.swRegistration = await navigator.serviceWorker.register('/sw-push.js', {
        scope: '/'
      });

      // Wait for the service worker to be ready
      await navigator.serviceWorker.ready;

      console.log('[Push] Service worker registered');
    } catch (err) {
      console.error('[Push] Service worker registration failed:', err);
      throw err;
    }
  }

  /**
   * Check if there's an existing subscription.
   */
  private async checkSubscription(): Promise<void> {
    if (!this.swRegistration) return;

    try {
      const subscription = await this.swRegistration.pushManager.getSubscription();
      this.subscribed.set(subscription !== null);

      if (subscription) {
        console.log('[Push] Existing subscription found');
      }
    } catch (err) {
      console.error('[Push] Failed to check subscription:', err);
    }
  }

  /**
   * Request permission for notifications.
   * Returns true if permission was granted.
   */
  async requestPermission(): Promise<boolean> {
    if (!this.supported()) {
      return false;
    }

    try {
      const permission = await Notification.requestPermission();
      this.permission.set(permission);
      return permission === 'granted';
    } catch (err) {
      console.error('[Push] Permission request failed:', err);
      return false;
    }
  }

  /**
   * Subscribe to push notifications.
   * Returns true if subscription was successful.
   */
  async subscribe(): Promise<boolean> {
    if (!this.supported() || !this.swRegistration || !this.vapidPublicKey) {
      this.error.set(this.translate.instant('errors.pushNotAvailable'));
      return false;
    }

    this.loading.set(true);
    this.error.set(null);

    try {
      // Request permission if not already granted
      if (this.permission() !== 'granted') {
        const granted = await this.requestPermission();
        if (!granted) {
          this.error.set(this.translate.instant('errors.pushPermissionDenied'));
          this.loading.set(false);
          return false;
        }
      }

      // Subscribe to push manager
      const subscription = await this.swRegistration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: this.urlBase64ToUint8Array(this.vapidPublicKey)
      });

      // Extract keys from subscription
      const p256dh = subscription.getKey('p256dh');
      const auth = subscription.getKey('auth');

      if (!p256dh || !auth) {
        throw new Error('Failed to get subscription keys');
      }

      // Send subscription to server
      const response = await firstValueFrom(
        this.http.post<{ success: boolean; message: string }>('/api/push/subscribe', {
          endpoint: subscription.endpoint,
          p256dh: this.arrayBufferToBase64(p256dh),
          auth: this.arrayBufferToBase64(auth),
          userAgent: navigator.userAgent
        })
      );

      if (response.success) {
        this.subscribed.set(true);
        console.log('[Push] Subscribed successfully');
        return true;
      } else {
        throw new Error(response.message || 'Subscription failed');
      }
    } catch (err) {
      console.error('[Push] Subscription failed:', err);
      this.error.set(err instanceof Error ? err.message : 'Subscription failed');
      return false;
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Unsubscribe from push notifications.
   */
  async unsubscribe(): Promise<boolean> {
    if (!this.swRegistration) {
      return false;
    }

    this.loading.set(true);
    this.error.set(null);

    try {
      const subscription = await this.swRegistration.pushManager.getSubscription();

      if (subscription) {
        // Unsubscribe from push manager
        await subscription.unsubscribe();

        // Notify server
        await firstValueFrom(
          this.http.post<{ success: boolean }>('/api/push/unsubscribe', {
            endpoint: subscription.endpoint
          })
        );
      }

      this.subscribed.set(false);
      console.log('[Push] Unsubscribed successfully');
      return true;
    } catch (err) {
      console.error('[Push] Unsubscribe failed:', err);
      this.error.set(err instanceof Error ? err.message : 'Unsubscribe failed');
      return false;
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Get push notification status from server.
   */
  async getStatus(): Promise<PushSubscriptionStatus | null> {
    try {
      return await firstValueFrom(
        this.http.get<PushSubscriptionStatus>('/api/push/status')
      );
    } catch (err) {
      console.error('[Push] Failed to get status:', err);
      return null;
    }
  }

  /**
   * Update push notification preference on server.
   */
  async updatePreference(enabled: boolean): Promise<boolean> {
    try {
      const response = await firstValueFrom(
        this.http.patch<{ success: boolean; enabled: boolean }>('/api/push/preference', { enabled })
      );
      return response.success;
    } catch (err) {
      console.error('[Push] Failed to update preference:', err);
      return false;
    }
  }

  /**
   * Convert URL-safe base64 string to Uint8Array.
   * Used for VAPID public key conversion.
   */
  private urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding)
      .replace(/-/g, '+')
      .replace(/_/g, '/');

    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }

    return outputArray;
  }

  /**
   * Convert ArrayBuffer to base64 string.
   * Used for subscription key encoding.
   */
  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  }

  /**
   * Check if the browser permission is denied.
   */
  isPermissionDenied(): boolean {
    return this.permission() === 'denied';
  }

  /**
   * Check if push is ready to be enabled.
   */
  canEnable(): boolean {
    return this.supported() && this.permission() !== 'denied';
  }
}
