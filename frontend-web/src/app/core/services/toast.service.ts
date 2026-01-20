import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'info' | 'warning' | 'error';
  link?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private counter = 0;

  toasts = signal<Toast[]>([]);

  show(message: string, type: Toast['type'] = 'info', duration = 5000, link?: string): void {
    const id = ++this.counter;
    const toast: Toast = { id, message, type, link };

    this.toasts.update(current => [...current, toast]);

    if (duration > 0) {
      setTimeout(() => this.dismiss(id), duration);
    }
  }

  dismiss(id: number): void {
    this.toasts.update(current => current.filter(t => t.id !== id));
  }

  success(message: string, duration = 5000, link?: string): void {
    this.show(message, 'success', duration, link);
  }

  info(message: string, duration = 5000, link?: string): void {
    this.show(message, 'info', duration, link);
  }

  warning(message: string, duration = 5000, link?: string): void {
    this.show(message, 'warning', duration, link);
  }

  error(message: string, duration = 5000, link?: string): void {
    this.show(message, 'error', duration, link);
  }
}
