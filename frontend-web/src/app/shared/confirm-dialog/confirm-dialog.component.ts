import { Component, signal, ChangeDetectionStrategy, HostListener, inject } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { FocusTrapDirective } from '../directives/focus-trap.directive';

export interface ConfirmDialogConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  showInput?: boolean;
  inputLabel?: string;
  inputPlaceholder?: string;
  type?: 'danger' | 'warning' | 'info';
  icon?: 'trash' | 'warning' | 'info' | 'question';
}

@Component({
    selector: 'app-confirm-dialog',
    imports: [FormsModule, FocusTrapDirective],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    @if (isOpen()) {
      <div class="dialog-backdrop" (click)="cancel()">
        <div
          class="dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="dialog-title"
          appFocusTrap
          (click)="$event.stopPropagation()">
          <!-- Icon -->
          <div class="dialog__icon" [class]="'dialog__icon--' + config().type">
            @switch (config().icon || 'question') {
              @case ('trash') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
                </svg>
              }
              @case ('warning') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
                </svg>
              }
              @case ('info') {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="m11.25 11.25.041-.02a.75.75 0 0 1 1.063.852l-.708 2.836a.75.75 0 0 0 1.063.853l.041-.021M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-3.75h.008v.008H12V8.25Z" />
                </svg>
              }
              @default {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 5.25h.008v.008H12v-.008Z" />
                </svg>
              }
            }
          </div>

          <!-- Content -->
          <div class="dialog__content">
            <h3 id="dialog-title" class="dialog__title">{{ config().title }}</h3>
            <p class="dialog__message">{{ config().message }}</p>

            @if (config().showInput) {
              <div class="dialog__input-group">
                @if (config().inputLabel) {
                  <label class="dialog__input-label">{{ config().inputLabel }}</label>
                }
                <input
                  type="text"
                  class="dialog__input"
                  [(ngModel)]="inputValue"
                  [placeholder]="config().inputPlaceholder || ''"
                  (keyup.enter)="confirm()"
                />
              </div>
            }
          </div>

          <!-- Actions -->
          <div class="dialog__actions">
            <button class="dialog__btn dialog__btn--secondary" (click)="cancel()">
              {{ config().cancelText || defaultCancelText }}
            </button>
            <button
              class="dialog__btn"
              [class.dialog__btn--danger]="config().type === 'danger'"
              [class.dialog__btn--primary]="config().type !== 'danger'"
              (click)="confirm()"
            >
              {{ config().confirmText || defaultConfirmText }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
    styles: [`
    .dialog-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.6);
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      z-index: 10000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
      animation: fadeIn 0.15s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .dialog {
      background: rgba(22, 22, 26, 0.95);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 16px;
      width: 100%;
      max-width: 320px;
      box-shadow:
        0 0 0 1px rgba(255, 255, 255, 0.05),
        0 24px 64px rgba(0, 0, 0, 0.4);
      overflow: hidden;
      animation: scaleIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      text-align: center;
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
    }

    @keyframes scaleIn {
      from {
        opacity: 0;
        transform: scale(0.95) translateY(10px);
      }
      to {
        opacity: 1;
        transform: scale(1) translateY(0);
      }
    }

    .dialog__icon {
      width: 48px;
      height: 48px;
      margin: 24px auto 16px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;

      svg {
        width: 26px;
        height: 26px;
      }

      &--danger {
        background: rgba(239, 68, 68, 0.12);
        color: #ef4444;
      }

      &--warning {
        background: rgba(245, 158, 11, 0.12);
        color: #f59e0b;
      }

      &--info {
        background: rgba(59, 130, 246, 0.12);
        color: #3b82f6;
      }
    }

    .dialog__content {
      padding: 0 24px 20px;
    }

    .dialog__title {
      font-size: 15px;
      font-weight: 600;
      color: var(--text-primary);
      margin: 0 0 6px;
      letter-spacing: -0.01em;
    }

    .dialog__message {
      font-size: 13px;
      color: var(--text-secondary);
      margin: 0;
      line-height: 1.5;
    }

    .dialog__input-group {
      margin-top: 16px;
      text-align: left;
    }

    .dialog__input-label {
      display: block;
      font-size: 12px;
      font-weight: 500;
      color: var(--text-secondary);
      margin-bottom: 6px;
    }

    .dialog__input {
      width: 100%;
      padding: 10px 12px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border-subtle);
      border-radius: 8px;
      color: var(--text-primary);
      font-size: 14px;
      transition: all 0.15s ease;

      &::placeholder {
        color: var(--text-muted);
      }

      &:focus {
        outline: none;
        border-color: var(--gold-400);
        box-shadow: 0 0 0 3px rgba(212, 168, 75, 0.15);
      }
    }

    .dialog__actions {
      display: flex;
      border-top: 1px solid rgba(255, 255, 255, 0.08);
    }

    .dialog__btn {
      flex: 1;
      padding: 14px 16px;
      font-size: 14px;
      font-weight: 500;
      border: none;
      background: transparent;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
      position: relative;

      &:not(:last-child)::after {
        content: '';
        position: absolute;
        right: 0;
        top: 0;
        bottom: 0;
        width: 1px;
        background: rgba(255, 255, 255, 0.08);
      }

      &--secondary {
        color: rgba(255, 255, 255, 0.6);

        &:hover {
          background: rgba(255, 255, 255, 0.04);
          color: #ffffff;
        }

        &:active {
          background: rgba(255, 255, 255, 0.08);
        }
      }

      &--primary {
        color: var(--gold-400);
        font-weight: 600;

        &:hover {
          background: rgba(212, 168, 75, 0.1);
        }

        &:active {
          background: rgba(212, 168, 75, 0.15);
        }
      }

      &--danger {
        color: var(--gold-400);
        font-weight: 600;

        &:hover {
          background: rgba(212, 168, 75, 0.1);
        }

        &:active {
          background: rgba(212, 168, 75, 0.15);
        }
      }
    }
  `]
})
export class ConfirmDialogComponent {
  isOpen = signal(false);
  config = signal<ConfirmDialogConfig>({
    title: '',
    message: '',
    type: 'info'
  });
  inputValue = '';

  private translateService = inject(TranslateService);
  private resolvePromise: ((value: boolean | string | null) => void) | null = null;

  get defaultCancelText(): string {
    return this.translateService.instant('common.cancel');
  }

  get defaultConfirmText(): string {
    return this.translateService.instant('common.confirm');
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isOpen()) {
      this.cancel();
    }
  }

  open(config: ConfirmDialogConfig): Promise<boolean | string | null> {
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
        this.resolvePromise(this.inputValue || '');
      } else {
        this.resolvePromise(true);
      }
      this.resolvePromise = null;
    }
  }

  cancel(): void {
    this.isOpen.set(false);
    if (this.resolvePromise) {
      this.resolvePromise(null);
      this.resolvePromise = null;
    }
  }
}
