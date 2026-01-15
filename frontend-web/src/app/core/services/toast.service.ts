import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'info' | 'warning' | 'error';
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private counter = 0;

  toasts = signal<Toast[]>([]);

  show(message: string, type: Toast['type'] = 'info', duration = 5000): void {
    const id = ++this.counter;
    const toast: Toast = { id, message, type };

    this.toasts.update(current => [...current, toast]);

    if (duration > 0) {
      setTimeout(() => this.dismiss(id), duration);
    }
  }

  dismiss(id: number): void {
    this.toasts.update(current => current.filter(t => t.id !== id));
  }

  success(message: string, duration = 5000): void {
    this.show(message, 'success', duration);
  }

  info(message: string, duration = 5000): void {
    this.show(message, 'info', duration);
  }

  warning(message: string, duration = 5000): void {
    this.show(message, 'warning', duration);
  }

  error(message: string, duration = 5000): void {
    this.show(message, 'error', duration);
  }
}
