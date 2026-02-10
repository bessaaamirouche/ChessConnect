import { Injectable, inject, signal, NgZone, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ToastService } from './toast.service';
import { NotificationCenterService } from './notification-center.service';

/**
 * SSE Event types matching backend NotificationEvent.EventType
 */
export type SseEventType =
  | 'connected'
  | 'notification'
  | 'lesson_status'
  | 'lesson_booked'
  | 'availability'
  | 'teacher_joined';

export interface SseNotificationPayload {
  notificationId: number;
  type: string;
  title: string;
  message: string;
  link: string | null;
  createdAt: string;
}

export interface SseLessonStatusPayload {
  lessonId: number;
  oldStatus: string | null;
  newStatus: string;
  teacherName: string;
  studentName: string;
  scheduledAt: string;
}

export interface SseAvailabilityPayload {
  availabilityId: number;
  teacherId: number;
  teacherName: string;
  dayInfo: string;
  timeRange: string;
}

export interface SseTeacherJoinedPayload {
  lessonId: number;
  teacherName: string;
}

@Injectable({
  providedIn: 'root'
})
export class SseService {
  private toastService = inject(ToastService);
  private notificationCenter = inject(NotificationCenterService);
  private ngZone = inject(NgZone);
  private platformId = inject(PLATFORM_ID);

  private eventSource: EventSource | null = null;
  private reconnectAttempts = 0;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private userRole: 'student' | 'teacher' | null = null;
  private visibilityHandler: (() => void) | null = null;
  private wasConnectedBeforeHidden = false;
  private hiddenDisconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private intentionalDisconnect = false;

  private readonly MAX_RECONNECT_ATTEMPTS = 10;
  private readonly INITIAL_RECONNECT_DELAY = 3000; // 3 seconds
  private readonly MAX_RECONNECT_DELAY = 60000; // 60 seconds
  private readonly HIDDEN_DISCONNECT_DELAY = 30000; // 30s grace before disconnect when tab hidden

  private isConnected = signal(false);
  private isConnecting = signal(false);

  readonly connected = this.isConnected.asReadonly();
  readonly connecting = this.isConnecting.asReadonly();

  /**
   * Connect to SSE stream for the given user role.
   */
  connect(role: 'student' | 'teacher'): void {
    if (!isPlatformBrowser(this.platformId)) {
      return; // SSE only works in browser
    }

    if (this.eventSource) {
      console.log('[SSE] Already connected or connecting');
      return;
    }

    this.userRole = role;
    this.intentionalDisconnect = false;
    this.doConnect();
    this.setupVisibilityHandler();
  }

  private setupVisibilityHandler(): void {
    if (this.visibilityHandler) return;

    this.visibilityHandler = () => {
      if (document.hidden) {
        // Tab hidden — disconnect after grace period to save resources
        if (this.eventSource) {
          this.wasConnectedBeforeHidden = true;
          this.hiddenDisconnectTimeout = setTimeout(() => {
            console.log('[SSE] Tab hidden for too long, disconnecting');
            this.cleanupConnection();
          }, this.HIDDEN_DISCONNECT_DELAY);
        }
      } else {
        // Tab visible again — cancel pending disconnect or reconnect
        if (this.hiddenDisconnectTimeout) {
          clearTimeout(this.hiddenDisconnectTimeout);
          this.hiddenDisconnectTimeout = null;
        }
        if (this.wasConnectedBeforeHidden && !this.eventSource && !this.intentionalDisconnect) {
          console.log('[SSE] Tab visible again, reconnecting');
          this.reconnectAttempts = 0;
          this.doConnect();
        }
        this.wasConnectedBeforeHidden = false;
      }
    };

    document.addEventListener('visibilitychange', this.visibilityHandler);
  }

  private doConnect(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    // Don't reconnect if tab is hidden
    if (document.hidden) {
      console.log('[SSE] Skipping connect — tab is hidden');
      this.wasConnectedBeforeHidden = true;
      return;
    }

    this.isConnecting.set(true);
    console.log('[SSE] Connecting to stream...');

    // Build the SSE URL - use relative path (proxy handles routing to backend)
    const sseUrl = '/api/notifications/stream';

    this.eventSource = new EventSource(sseUrl, { withCredentials: true });

    // Handle connection opened
    this.eventSource.onopen = () => {
      this.ngZone.run(() => {
        console.log('[SSE] Connection opened');
        this.isConnected.set(true);
        this.isConnecting.set(false);
        this.reconnectAttempts = 0;
      });
    };

    // Handle connection error
    this.eventSource.onerror = () => {
      this.ngZone.run(() => {
        console.warn('[SSE] Connection error');
        this.isConnected.set(false);
        this.isConnecting.set(false);
        this.cleanupConnection();
        if (!this.intentionalDisconnect) {
          this.scheduleReconnect();
        }
      });
    };

    // Register event listeners
    this.registerEventListeners();
  }

  private registerEventListeners(): void {
    if (!this.eventSource) return;

    // Connected event (initial handshake)
    this.eventSource.addEventListener('connected', (event) => {
      this.ngZone.run(() => {
        const data = JSON.parse((event as MessageEvent).data);
        console.log('[SSE] Connected:', data);
      });
    });

    // Backend notification created
    this.eventSource.addEventListener('notification', (event) => {
      this.ngZone.run(() => {
        const payload: SseNotificationPayload = JSON.parse((event as MessageEvent).data);
        console.log('[SSE] Notification received:', payload);
        this.handleNotification(payload);
      });
    });

    // Lesson status changed
    this.eventSource.addEventListener('lesson_status', (event) => {
      this.ngZone.run(() => {
        const payload: SseLessonStatusPayload = JSON.parse((event as MessageEvent).data);
        console.log('[SSE] Lesson status changed:', payload);
        this.handleLessonStatusChange(payload);
      });
    });

    // New lesson booked (for teachers)
    this.eventSource.addEventListener('lesson_booked', (event) => {
      this.ngZone.run(() => {
        const payload: SseLessonStatusPayload = JSON.parse((event as MessageEvent).data);
        console.log('[SSE] Lesson booked:', payload);
        this.handleLessonBooked(payload);
      });
    });

    // New availability (for students)
    this.eventSource.addEventListener('availability', (event) => {
      this.ngZone.run(() => {
        const payload: SseAvailabilityPayload = JSON.parse((event as MessageEvent).data);
        console.log('[SSE] New availability:', payload);
        this.handleNewAvailability(payload);
      });
    });

    // Teacher joined call (for students)
    this.eventSource.addEventListener('teacher_joined', (event) => {
      this.ngZone.run(() => {
        const payload: SseTeacherJoinedPayload = JSON.parse((event as MessageEvent).data);
        console.log('[SSE] Teacher joined call:', payload);
        this.handleTeacherJoined(payload);
      });
    });
  }

  private handleNotification(payload: SseNotificationPayload): void {
    // Map backend type to toast type
    const typeMap: Record<string, 'info' | 'success' | 'warning' | 'error'> = {
      'REFUND': 'success',
      'LESSON_CONFIRMED': 'success',
      'LESSON_CANCELLED': 'warning',
      'NEW_BOOKING': 'info',
      'PENDING_VALIDATION': 'warning'
    };

    const toastType = typeMap[payload.type] || 'info';
    const link = payload.link || undefined;

    // Show toast
    switch (toastType) {
      case 'success':
        this.toastService.success(payload.message, 8000, link);
        break;
      case 'warning':
        this.toastService.warning(payload.message, 8000, link);
        break;
      case 'error':
        this.toastService.error(payload.message, 10000, link);
        break;
      default:
        this.toastService.info(payload.message, 8000, link);
    }

    // Add to notification center
    this.notificationCenter.addFromBackend({
      id: payload.notificationId,
      type: payload.type,
      title: payload.title,
      message: payload.message,
      link: payload.link,
      createdAt: payload.createdAt
    });
  }

  private handleLessonStatusChange(payload: SseLessonStatusPayload): void {
    if (this.userRole === 'student') {
      // Student receives notification about their lesson
      if (payload.newStatus === 'CONFIRMED') {
        const message = `M. ${payload.teacherName} a confirme votre cours`;
        this.toastService.success(message, 8000, '/lessons');
        this.notificationCenter.success('Cours confirme', message, '/lessons');
      } else if (payload.newStatus === 'CANCELLED') {
        const message = `M. ${payload.teacherName} a annule votre cours`;
        this.toastService.error(message, 10000, '/lessons');
        this.notificationCenter.error('Cours annule', message, '/lessons');
      }
    } else if (this.userRole === 'teacher') {
      // Teacher receives notification about cancellation by student
      if (payload.newStatus === 'CANCELLED') {
        const message = `${payload.studentName} a annule son cours`;
        this.toastService.error(message, 10000, '/lessons');
        this.notificationCenter.error('Cours annule', message, '/lessons');
      }
    }
  }

  private handleLessonBooked(payload: SseLessonStatusPayload): void {
    // This is only sent to teachers
    const message = `Nouvelle reservation de ${payload.studentName}`;
    this.toastService.info(message, 8000, '/lessons');
    this.notificationCenter.info('Nouvelle reservation', message, '/lessons');
  }

  private handleNewAvailability(payload: SseAvailabilityPayload): void {
    // This is only sent to students who have this teacher as favorite
    const message = `M. ${payload.teacherName} vient d'ajouter une disponibilite`;
    const link = `/book/${payload.teacherId}`;
    this.toastService.info(message, 8000, link);
    this.notificationCenter.info('Nouvelle disponibilite', message, link);
  }

  private handleTeacherJoined(payload: SseTeacherJoinedPayload): void {
    // This is only sent to students
    const message = `M. ${payload.teacherName} vous attend dans l'appel video`;
    const link = `/lessons?openCall=${payload.lessonId}`;
    this.toastService.success(message, 15000, link);
    this.notificationCenter.success('Appel video', message, link);
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.MAX_RECONNECT_ATTEMPTS) {
      console.warn('[SSE] Max reconnect attempts reached. Giving up.');
      return;
    }

    // Don't reconnect if tab is hidden — will reconnect on visibility change
    if (document.hidden) {
      console.log('[SSE] Tab hidden, deferring reconnect');
      this.wasConnectedBeforeHidden = true;
      return;
    }

    // Exponential backoff: 3s, 6s, 12s, 24s, 48s, 60s (max)
    const delay = Math.min(
      this.INITIAL_RECONNECT_DELAY * Math.pow(2, this.reconnectAttempts),
      this.MAX_RECONNECT_DELAY
    );

    this.reconnectAttempts++;
    console.log(`[SSE] Scheduling reconnect attempt ${this.reconnectAttempts}/${this.MAX_RECONNECT_ATTEMPTS} in ${delay}ms`);

    this.reconnectTimeout = setTimeout(() => {
      this.doConnect();
    }, delay);
  }

  /**
   * Disconnect from SSE stream (intentional, no auto-reconnect).
   */
  disconnect(): void {
    console.log('[SSE] Disconnecting...');
    this.intentionalDisconnect = true;
    this.cleanupConnection();
    this.userRole = null;
    this.wasConnectedBeforeHidden = false;

    if (this.hiddenDisconnectTimeout) {
      clearTimeout(this.hiddenDisconnectTimeout);
      this.hiddenDisconnectTimeout = null;
    }

    if (this.visibilityHandler) {
      document.removeEventListener('visibilitychange', this.visibilityHandler);
      this.visibilityHandler = null;
    }
  }

  private cleanupConnection(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    this.isConnected.set(false);
    this.isConnecting.set(false);
  }

  /**
   * Check if SSE is connected.
   */
  isActive(): boolean {
    return this.eventSource !== null && this.eventSource.readyState === EventSource.OPEN;
  }
}
