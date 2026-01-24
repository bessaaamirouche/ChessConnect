import { Injectable, signal, computed } from '@angular/core';

export interface AppNotification {
  id: string;
  type: 'info' | 'success' | 'warning' | 'error';
  title: string;
  message: string;
  timestamp: Date;
  read: boolean;
  link?: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationCenterService {
  private readonly STORAGE_KEY = 'mychess_notifications';
  private readonly MAX_NOTIFICATIONS = 50;

  private notificationsSignal = signal<AppNotification[]>([]);

  readonly notifications = this.notificationsSignal.asReadonly();

  readonly unreadCount = computed(() =>
    this.notificationsSignal().filter(n => !n.read).length
  );

  readonly hasNotifications = computed(() =>
    this.notificationsSignal().length > 0
  );

  constructor() {
    this.loadFromStorage();
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
   * Mark a notification as read
   */
  markAsRead(id: string): void {
    this.notificationsSignal.update(notifications =>
      notifications.map(n => n.id === id ? { ...n, read: true } : n)
    );
    this.saveToStorage();
  }

  /**
   * Mark all notifications as read
   */
  markAllAsRead(): void {
    this.notificationsSignal.update(notifications =>
      notifications.map(n => ({ ...n, read: true }))
    );
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
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const notifications = JSON.parse(stored) as AppNotification[];
        // Convert date strings back to Date objects
        notifications.forEach(n => n.timestamp = new Date(n.timestamp));
        this.notificationsSignal.set(notifications);
      }
    } catch (e) {
      console.error('Error loading notifications from storage:', e);
    }
  }

  private saveToStorage(): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.notificationsSignal()));
    } catch (e) {
      console.error('Error saving notifications to storage:', e);
    }
  }
}
