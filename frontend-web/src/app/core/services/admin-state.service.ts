import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export type AdminDataChangeType = 'user' | 'lesson' | 'subscription' | 'payment' | 'all';

export interface AdminDataChange {
  type: AdminDataChangeType;
  action: 'create' | 'update' | 'delete';
  id?: number;
}

/**
 * Service to synchronize data changes across admin components.
 * When data is modified in one component (e.g., user deleted in user list),
 * other components (e.g., dashboard stats) can subscribe to be notified.
 */
@Injectable({
  providedIn: 'root'
})
export class AdminStateService {
  private dataChange$ = new Subject<AdminDataChange>();

  /**
   * Observable that emits when admin data changes
   */
  get onDataChange$() {
    return this.dataChange$.asObservable();
  }

  /**
   * Notify all subscribers that data has changed
   */
  notifyChange(change: AdminDataChange): void {
    this.dataChange$.next(change);
  }

  /**
   * Convenience method for user changes
   */
  notifyUserChange(action: 'create' | 'update' | 'delete', userId?: number): void {
    this.notifyChange({ type: 'user', action, id: userId });
  }

  /**
   * Convenience method for lesson changes
   */
  notifyLessonChange(action: 'create' | 'update' | 'delete', lessonId?: number): void {
    this.notifyChange({ type: 'lesson', action, id: lessonId });
  }

  /**
   * Convenience method to refresh all admin data
   */
  refreshAll(): void {
    this.notifyChange({ type: 'all', action: 'update' });
  }
}
