import { Component, inject } from '@angular/core';
import { ToastService } from '../../core/services/toast.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroCheckCircle,
  heroInformationCircle,
  heroExclamationTriangle,
  heroXCircle,
  heroXMark,
  heroAcademicCap
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [NgIconComponent],
  viewProviders: [provideIcons({
    heroCheckCircle,
    heroInformationCircle,
    heroExclamationTriangle,
    heroXCircle,
    heroXMark,
    heroAcademicCap
  })],
  template: `
    <div class="toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast toast--{{ toast.type }}" (click)="toastService.dismiss(toast.id)">
          <div class="toast__icon-wrapper">
            <ng-icon [name]="getIcon(toast.type)" class="toast__icon" size="22"></ng-icon>
          </div>
          <span class="toast__message">{{ toast.message }}</span>
          <button class="toast__close" (click)="toastService.dismiss(toast.id); $event.stopPropagation()">
            <ng-icon name="heroXMark" size="18"></ng-icon>
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 1.5rem;
      right: 1.5rem;
      z-index: var(--z-toast, 400);
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      max-width: 420px;
    }

    .toast {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem 1.25rem;
      border-radius: var(--radius-lg, 12px);
      background: var(--bg-elevated, #252530);
      border: 1px solid var(--border-default, rgba(255, 255, 255, 0.12));
      box-shadow: var(--shadow-lg, 0 8px 24px rgba(0, 0, 0, 0.6));
      cursor: pointer;
      animation: slideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      transition: transform var(--transition-fast, 150ms ease), opacity var(--transition-fast, 150ms ease);
    }

    .toast:hover {
      transform: translateX(-4px);
    }

    @keyframes slideIn {
      from {
        transform: translateX(120%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }

    .toast__icon-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: var(--radius-md, 8px);
      flex-shrink: 0;
    }

    .toast--success .toast__icon-wrapper {
      background: var(--success-muted, #166534);
    }
    .toast--success .toast__icon {
      color: var(--success, #4ade80);
    }

    .toast--info .toast__icon-wrapper {
      background: linear-gradient(135deg, rgba(212, 168, 75, 0.2) 0%, rgba(184, 146, 58, 0.2) 100%);
      border: 1px solid var(--border-gold, rgba(212, 168, 75, 0.5));
    }
    .toast--info .toast__icon {
      color: var(--gold-300, #ffd666);
    }

    .toast--warning .toast__icon-wrapper {
      background: var(--warning-muted, #92400e);
    }
    .toast--warning .toast__icon {
      color: var(--warning, #fbbf24);
    }

    .toast--error .toast__icon-wrapper {
      background: var(--error-muted, #991b1b);
    }
    .toast--error .toast__icon {
      color: var(--error, #f87171);
    }

    .toast__message {
      flex: 1;
      font-family: var(--font-sans, 'Inter', sans-serif);
      font-size: 0.9rem;
      font-weight: 500;
      color: var(--text-primary, #f5f3eb);
      line-height: 1.4;
    }

    .toast__close {
      background: none;
      border: none;
      padding: 0.375rem;
      cursor: pointer;
      color: var(--text-muted, #6b6965);
      transition: color var(--transition-fast, 150ms ease);
      border-radius: var(--radius-sm, 4px);
    }

    .toast__close:hover {
      color: var(--text-primary, #f5f3eb);
      background: var(--bg-tertiary, #1e1e24);
    }

    @media (max-width: 480px) {
      .toast-container {
        left: 1rem;
        right: 1rem;
        top: 1rem;
        max-width: none;
      }

      .toast {
        padding: 0.875rem 1rem;
      }

      .toast__icon-wrapper {
        width: 36px;
        height: 36px;
      }

      .toast__message {
        font-size: 0.85rem;
      }
    }
  `]
})
export class ToastComponent {
  toastService = inject(ToastService);

  getIcon(type: string): string {
    switch (type) {
      case 'success': return 'heroCheckCircle';
      case 'warning': return 'heroExclamationTriangle';
      case 'error': return 'heroXCircle';
      default: return 'heroAcademicCap'; // Chess teacher icon for info
    }
  }
}
