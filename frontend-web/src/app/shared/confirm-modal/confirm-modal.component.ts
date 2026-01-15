import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface ConfirmModalConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  showInput?: boolean;
  inputLabel?: string;
  inputPlaceholder?: string;
  type?: 'danger' | 'warning' | 'info';
}

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (isOpen()) {
      <div class="modal-overlay" (click)="cancel()"></div>
      <div class="modal" [class]="'modal--' + config().type">
        <div class="modal__header">
          <h3>{{ config().title }}</h3>
          <button class="modal__close" (click)="cancel()">×</button>
        </div>
        <div class="modal__body">
          <p>{{ config().message }}</p>
          @if (config().showInput) {
            <div class="form-group">
              <label class="form-group__label">{{ config().inputLabel }}</label>
              <input
                type="text"
                class="input"
                [(ngModel)]="inputValue"
                [placeholder]="config().inputPlaceholder || ''"
              />
            </div>
          }
        </div>
        <div class="modal__footer">
          <button class="btn btn--ghost" (click)="cancel()">
            {{ config().cancelText || 'Annuler' }}
          </button>
          <button
            class="btn"
            [class.btn--danger]="config().type === 'danger'"
            [class.btn--primary]="config().type !== 'danger'"
            (click)="confirm()"
          >
            {{ config().confirmText || 'Confirmer' }}
          </button>
        </div>
      </div>
    }
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.7);
      z-index: 1000;
    }

    .modal {
      position: fixed;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 90%;
      max-width: 450px;
      background: var(--bg-secondary);
      border-radius: var(--radius-xl);
      border: 1px solid var(--border-subtle);
      z-index: 1001;
      overflow: hidden;

      &__header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: var(--space-lg);
        border-bottom: 1px solid var(--border-subtle);

        h3 {
          font-size: 1.125rem;
          font-weight: 600;
          color: var(--text-primary);
        }
      }

      &__close {
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: transparent;
        border: none;
        border-radius: var(--radius-md);
        color: var(--text-secondary);
        font-size: 1.25rem;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover {
          background: var(--bg-tertiary);
          color: var(--text-primary);
        }
      }

      &__body {
        padding: var(--space-lg);

        p {
          color: var(--text-secondary);
          line-height: 1.5;
          margin-bottom: var(--space-md);
        }

        .form-group {
          margin-top: var(--space-md);
        }

        .form-group__label {
          display: block;
          font-size: 0.875rem;
          font-weight: 500;
          color: var(--text-primary);
          margin-bottom: var(--space-xs);
        }

        .input {
          width: 100%;
          padding: var(--space-sm) var(--space-md);
          background: var(--bg-tertiary);
          border: 1px solid var(--border-subtle);
          border-radius: var(--radius-md);
          color: var(--text-primary);
          font-size: 0.9375rem;

          &:focus {
            outline: none;
            border-color: var(--gold-400);
          }
        }
      }

      &__footer {
        display: flex;
        justify-content: flex-end;
        gap: var(--space-sm);
        padding: var(--space-lg);
        background: var(--bg-tertiary);
        border-top: 1px solid var(--border-subtle);
      }

      &--danger .modal__header h3 {
        color: var(--error);
      }
    }

    .btn {
      padding: var(--space-sm) var(--space-lg);
      border-radius: var(--radius-md);
      font-size: 0.9375rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.15s ease;
      border: none;

      &--primary {
        background: var(--gold-400);
        color: var(--bg-primary);

        &:hover {
          background: var(--gold-500);
        }
      }

      &--danger {
        background: var(--error);
        color: white;

        &:hover {
          opacity: 0.9;
        }
      }

      &--ghost {
        background: transparent;
        color: var(--text-secondary);
        border: 1px solid var(--border-subtle);

        &:hover {
          background: var(--bg-tertiary);
          color: var(--text-primary);
        }
      }
    }
  `]
})
export class ConfirmModalComponent {
  isOpen = signal(false);
  config = signal<ConfirmModalConfig>({
    title: '',
    message: '',
    type: 'info'
  });
  inputValue = '';

  private resolvePromise: ((value: boolean | string | null) => void) | null = null;

  open(config: ConfirmModalConfig): Promise<boolean | string | null> {
    this.config.set({ ...config, type: config.type || 'info' });
    this.inputValue = '';
    this.isOpen.set(true);

    return new Promise((resolve) => {
      this.resolvePromise = resolve;
    });
  }

  confirm(): void {
    this.isOpen.set(false);
    if (this.resolvePromise) {
      if (this.config().showInput) {
        this.resolvePromise(this.inputValue || null);
      } else {
        this.resolvePromise(true);
      }
      this.resolvePromise = null;
    }
  }

  cancel(): void {
    this.isOpen.set(false);
    if (this.resolvePromise) {
      this.resolvePromise(this.config().showInput ? null : false);
      this.resolvePromise = null;
    }
  }
}
