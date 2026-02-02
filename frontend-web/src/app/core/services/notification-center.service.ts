import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';

export interface AppNotification {
  id: string;
  type: 'info' | 'success' | 'warning' | 'error' | 'refund';
  title: string;
  message: string;
  timestamp: Date;
  read: boolean;
  link?: string;
  fromBackend?: boolean; // Track if notification came from backend
}

export interface BackendNotification {
  id: number;
  type: string;
  title: string;
  message: string;
  link: string | null;
  isRead?: boolean;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationCenterService {
  private readonly STORAGE_KEY_PREFIX = 'mychess_notifications_';
  private readonly MAX_NOTIFICATIONS = 50;

  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  private notificationsSignal = signal<AppNotification[]>([]);
  private currentUserId: number | null = null;
  private knownBackendIds = new Set<number>();

  readonly notifications = this.notificationsSignal.asReadonly();

  readonly unreadCount = computed(() =>
    this.notificationsSignal().filter(n => !n.read).length
  );

  readonly hasNotifications = computed(() =>
    this.notificationsSignal().length > 0
  );

  constructor() {
    // Don't load from storage in constructor - wait for user initialization
  }

  /**
   * Initialize notifications for a specific user
   * Called when user logs in
   */
  initializeForUser(userId: number): void {
    // Always clear current state when initializing for a user
    // This handles both switching users and fresh logins after logout
    if (this.currentUserId !== userId) {
      this.notificationsSignal.set([]);
      this.knownBackendIds.clear();
    }

    this.currentUserId = userId;
    this.loadFromStorage();
    // Fetch initial backend notifications (SSE will handle real-time updates)
    this.fetchBackendNotifications();
  }

  /**
   * Clear notifications and reset state when user logs out
   */
  clearOnLogout(): void {
    this.notificationsSignal.set([]);
    this.currentUserId = null;
    this.knownBackendIds.clear();
  }

  /**
   * Add a notification received from SSE.
   * This replaces the polling mechanism.
   */
  addFromBackend(notification: BackendNotification): void {
    if (this.knownBackendIds.has(notification.id)) {
      return; // Already have this notification
    }

    this.knownBackendIds.add(notification.id);

    const appNotification: AppNotification = {
      id: `backend-${notification.id}`,
      type: this.mapBackendType(notification.type),
      title: notification.title,
      message: notification.message,
      timestamp: new Date(notification.createdAt),
      read: notification.isRead || false,
      link: notification.link || undefined,
      fromBackend: true
    };

    this.notificationsSignal.update(notifications => {
      const updated = [appNotification, ...notifications];
      return updated.slice(0, this.MAX_NOTIFICATIONS);
    });

    this.saveToStorage();
  }

  /**
   * Fetch notifications from backend and merge with local ones
   */
  private fetchBackendNotifications(): void {
    if (this.currentUserId === null) return;

    this.http.get<BackendNotification[]>('/api/notifications/unread').subscribe({
      next: (backendNotifications) => {
        // Add new backend notifications that we haven't seen before
        for (const bn of backendNotifications) {
          if (!this.knownBackendIds.has(bn.id)) {
            this.knownBackendIds.add(bn.id);

            // Convert backend notification to app notification
            const appNotification: AppNotification = {
              id: `backend-${bn.id}`,
              type: this.mapBackendType(bn.type),
              title: bn.title,
              message: bn.message,
              timestamp: new Date(bn.createdAt),
              read: bn.isRead ?? false,
              link: bn.link || undefined,
              fromBackend: true
            };

            // Add to the beginning of the list
            this.notificationsSignal.update(notifications => {
              const updated = [appNotification, ...notifications];
              return updated.slice(0, this.MAX_NOTIFICATIONS);
            });
          }
        }
        this.saveToStorage();
      },
      error: (err) => {
        // Silently fail - notifications are not critical
        console.debug('Failed to fetch backend notifications:', err);
      }
    });
  }

  /**
   * Map backend notification type to frontend type
   */
  private mapBackendType(backendType: string): AppNotification['type'] {
    const typeMap: Record<string, AppNotification['type']> = {
      'info': 'info',
      'success': 'success',
      'warning': 'warning',
      'error': 'error',
      'refund': 'success', // Show refunds as success
      'lesson_confirmed': 'success',
      'lesson_cancelled': 'warning',
      'new_booking': 'info',
      'pending_validation': 'warning' // Remind coach to validate courses
    };
    return typeMap[backendType.toLowerCase()] || 'info';
  }

  private getStorageKey(): string {
    if (this.currentUserId === null) {
      return this.STORAGE_KEY_PREFIX + 'anonymous';
    }
    return this.STORAGE_KEY_PREFIX + this.currentUserId;
  }

  /**
   * Add a new notification
   */
  add(notification: Omit<AppNotification, 'id' | 'timestamp' | 'read'>): void {
    const newNotification: AppNotification = {
      ...notification,
      id: this.generateId(),
      timestamp: new Date(),
      read: false
    };

    this.notificationsSignal.update(notifications => {
      const updated = [newNotification, ...notifications];
      // Keep only the most recent notifications
      return updated.slice(0, this.MAX_NOTIFICATIONS);
    });

    this.saveToStorage();
  }

  /**
   * Add info notification
   */
  info(title: string, message: string, link?: string): void {
    this.add({ type: 'info', title, message, link });
  }

  /**
   * Add success notification
   */
  success(title: string, message: string, link?: string): void {
    this.add({ type: 'success', title, message, link });
  }

  /**
   * Add warning notification
   */
  warning(title: string, message: string, link?: string): void {
    this.add({ type: 'warning', title, message, link });
  }

  /**
   * Add error notification
   */
  error(title: string, message: string, link?: string): void {
    this.add({ type: 'error', title, message, link });
  }

  /**
   * Mark a notification as read (deletes it immediately)
   */
  markAsRead(id: string): void {
    // If it's a backend notification, mark as read on the server (which deletes it)
    if (id.startsWith('backend-')) {
      const backendId = id.replace('backend-', '');
      this.http.patch(`/api/notifications/${backendId}/read`, {}).subscribe({
        error: (err) => console.debug('Failed to mark notification as read on server:', err)
      });
    }

    // Remove from local list immediately
    this.notificationsSignal.update(notifications =>
      notifications.filter(n => n.id !== id)
    );
    this.saveToStorage();
  }

  /**
   * Mark all notifications as read (deletes them all)
   */
  markAllAsRead(): void {
    // Mark all as read on the server (which deletes them)
    this.http.patch('/api/notifications/read-all', {}).subscribe({
      error: (err) => console.debug('Failed to mark all notifications as read on server:', err)
    });

    // Clear all notifications locally
    this.notificationsSignal.set([]);
    this.saveToStorage();
  }

  /**
   * Remove a single notification
   */
  remove(id: string): void {
    this.notificationsSignal.update(notifications =>
      notifications.filter(n => n.id !== id)
    );
    this.saveToStorage();
  }

  /**
   * Clear all notifications
   */
  clearAll(): void {
    this.notificationsSignal.set([]);
    this.saveToStorage();
  }

  /**
   * Format relative time (like iPhone)
   */
  formatRelativeTime(date: Date): string {
    const now = new Date();
    const diff = now.getTime() - new Date(date).getTime();

    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return "A l'instant";
    if (minutes < 60) return `Il y a ${minutes} min`;
    if (hours < 24) return `Il y a ${hours}h`;
    if (days === 1) return 'Hier';
    if (days < 7) return `Il y a ${days} jours`;

    return new Date(date).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short'
    });
  }

  /**
   * Format full date time
   */
  formatDateTime(date: Date): string {
    return new Date(date).toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  private generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
  }

  private loadFromStorage(): void {
    if (this.currentUserId === null || !isPlatformBrowser(this.platformId)) {
      return; // Don't load if no user is set or on server
    }

    try {
      const stored = localStorage.getItem(this.getStorageKey());
      if (stored) {
        const notifications = JSON.parse(stored) as AppNotification[];
        // Convert date strings back to Date objects
        notifications.forEach(n => n.timestamp = new Date(n.timestamp));
        this.notificationsSignal.set(notifications);
      } else {
        // No stored notifications for this user - start fresh
        this.notificationsSignal.set([]);
      }
    } catch (e) {
      console.error('Error loading notifications from storage:', e);
      this.notificationsSignal.set([]);
    }
  }

  private saveToStorage(): void {
    if (this.currentUserId === null || !isPlatformBrowser(this.platformId)) {
      return; // Don't save if no user is set or on server
    }

    try {
      localStorage.setItem(this.getStorageKey(), JSON.stringify(this.notificationsSignal()));
    } catch (e) {
      console.error('Error saving notifications to storage:', e);
    }
  }
}
