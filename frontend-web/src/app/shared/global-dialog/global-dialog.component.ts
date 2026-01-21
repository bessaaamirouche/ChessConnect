import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogService } from '../../core/services/dialog.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroExclamationTriangle,
  heroInformationCircle,
  heroCheckCircle,
  heroXCircle
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-global-dialog',
  standalone: true,
  imports: [FormsModule, NgIconComponent],
  viewProviders: [provideIcons({
    heroExclamationTriangle,
    heroInformationCircle,
    heroCheckCircle,
    heroXCircle
  })],
  template: `
    @if (dialogService.isOpen()) {
      <div class="dialog-overlay" (click)="onOverlayClick()"></div>
      <div class="dialog" [class]="'dialog--' + (dialogService.config()?.variant || 'info')">
        <div class="dialog__icon">
          <ng-icon [name]="getIcon()" size="28"></ng-icon>
        </div>
        <div class="dialog__content">
          <h3 class="dialog__title">{{ dialogService.config()?.title }}</h3>
          <p class="dialog__message">{{ dialogService.config()?.message }}</p>

          @if (dialogService.config()?.type === 'prompt') {
            <div class="dialog__input-group">
              @if (dialogService.config()?.inputLabel) {
                <label class="dialog__label">{{ dialogService.config()?.inputLabel }}</label>
              }
              <input
                type="text"
                class="dialog__input"
                [(ngModel)]="inputValue"
                [placeholder]="dialogService.config()?.inputPlaceholder || ''"
                (keyup.enter)="onConfirm()"
                #inputField
              />
            </div>
          }
        </div>

        <div class="dialog__actions">
          @if (dialogService.config()?.type !== 'alert') {
            <button class="dialog__btn dialog__btn--cancel" (click)="onCancel()">
              {{ dialogService.config()?.cancelText || 'Annuler' }}
            </button>
          }
          <button
            class="dialog__btn dialog__btn--confirm"
            [class.dialog__btn--danger]="dialogService.config()?.variant === 'danger'"
            (click)="onConfirm()"
          >
            {{ dialogService.config()?.confirmText || 'OK' }}
          </button>
        </div>
      </div>
    }
  `,
  styles: [`
    .dialog-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.75);
      backdrop-filter: blur(4px);
      z-index: 9998;
      animation: fadeIn 0.15s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translate(-50%, -50%) scale(0.95);
      }
      to {
        opacity: 1;
        transform: translate(-50%, -50%) scale(1);
      }
    }

    .dialog {
      position: fixed;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 90%;
      max-width: 420px;
      background: var(--bg-secondary, #1a1a1f);
      border-radius: var(--radius-xl, 16px);
      border: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.1));
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
      z-index: 9999;
      padding: var(--space-xl, 1.5rem);
      animation: slideIn 0.2s ease-out;
    }

    .dialog__icon {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto var(--space-lg, 1rem);
    }

    .dialog--info .dialog__icon {
      background: rgba(212, 168, 75, 0.15);
      color: var(--gold-400, #d4a84b);
    }

    .dialog--success .dialog__icon {
      background: rgba(74, 222, 128, 0.15);
      color: #4ade80;
    }

    .dialog--warning .dialog__icon {
      background: rgba(251, 191, 36, 0.15);
      color: #fbbf24;
    }

    .dialog--danger .dialog__icon {
      background: rgba(248, 113, 113, 0.15);
      color: #f87171;
    }

    .dialog__content {
      text-align: center;
      margin-bottom: var(--space-xl, 1.5rem);
    }

    .dialog__title {
      font-size: 1.25rem;
      font-weight: 600;
      color: var(--text-primary, #f5f3eb);
      margin-bottom: var(--space-sm, 0.5rem);
    }

    .dialog__message {
      font-size: 0.9375rem;
      color: var(--text-secondary, #a8a8a8);
      line-height: 1.5;
    }

    .dialog__input-group {
      margin-top: var(--space-lg, 1rem);
      text-align: left;
    }

    .dialog__label {
      display: block;
      font-size: 0.875rem;
      font-weight: 500;
      color: var(--text-primary, #f5f3eb);
      margin-bottom: var(--space-xs, 0.25rem);
    }

    .dialog__input {
      width: 100%;
      padding: var(--space-sm, 0.5rem) var(--space-md, 0.75rem);
      background: var(--bg-tertiary, #252530);
      border: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.1));
      border-radius: var(--radius-md, 8px);
      color: var(--text-primary, #f5f3eb);
      font-size: 0.9375rem;
      transition: border-color 0.15s ease;

      &:focus {
        outline: none;
        border-color: var(--gold-400, #d4a84b);
      }

      &::placeholder {
        color: var(--text-muted, #6b6965);
      }
    }

    .dialog__actions {
      display: flex;
      gap: var(--space-sm, 0.5rem);
      justify-content: center;
    }

    .dialog__btn {
      flex: 1;
      padding: var(--space-sm, 0.5rem) var(--space-lg, 1rem);
      border-radius: var(--radius-md, 8px);
      font-size: 0.9375rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.15s ease;
      border: none;
    }

    .dialog__btn--cancel {
      background: var(--bg-tertiary, #252530);
      color: var(--text-secondary, #a8a8a8);
      border: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.1));

      &:hover {
        background: var(--bg-elevated, #2a2a35);
        color: var(--text-primary, #f5f3eb);
      }
    }

    .dialog__btn--confirm {
      background: var(--gold-400, #d4a84b);
      color: var(--bg-primary, #0f0f12);

      &:hover {
        background: var(--gold-500, #c9a043);
      }
    }

    .dialog__btn--danger {
      background: #ef4444;
      color: white;

      &:hover {
        background: #dc2626;
      }
    }

    @media (max-width: 480px) {
      .dialog {
        width: 95%;
        padding: var(--space-lg, 1rem);
      }

      .dialog__actions {
        flex-direction: column-reverse;
      }

      .dialog__btn {
        width: 100%;
      }
    }
  `]
})
export class GlobalDialogComponent {
  dialogService = inject(DialogService);
  inputValue = '';

  getIcon(): string {
    const variant = this.dialogService.config()?.variant || 'info';
    switch (variant) {
      case 'success': return 'heroCheckCircle';
      case 'warning': return 'heroExclamationTriangle';
      case 'danger': return 'heroXCircle';
      default: return 'heroInformationCircle';
    }
  }

  onConfirm(): void {
    this.dialogService.handleConfirm(this.inputValue);
    this.inputValue = '';
  }

  onCancel(): void {
    this.dialogService.handleCancel();
    this.inputValue = '';
  }

  onOverlayClick(): void {
    // Only close on overlay click for alerts
    if (this.dialogService.config()?.type === 'alert') {
      this.onConfirm();
    }
  }
}
