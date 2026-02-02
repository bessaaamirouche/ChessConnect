import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroXMark } from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [CommonModule, NgIconComponent],
  viewProviders: [provideIcons({ heroXMark })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (isOpen) {
      <div class="modal-overlay" (click)="onOverlayClick()"></div>
      <div class="modal" [class.modal--sm]="size === 'sm'" [class.modal--lg]="size === 'lg'">
        <div class="modal__header">
          <h3 class="modal__title">{{ title }}</h3>
          <button class="modal__close" (click)="close.emit()" type="button">
            <ng-icon name="heroXMark" size="20"></ng-icon>
          </button>
        </div>
        <div class="modal__content">
          <ng-content></ng-content>
        </div>
        @if (showFooter) {
          <div class="modal__footer">
            <ng-content select="[footer]"></ng-content>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      z-index: 1000;
    }

    .modal {
      position: fixed;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      background: #1a1a1a;
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 16px;
      width: 90%;
      max-width: 480px;
      max-height: 90vh;
      overflow: hidden;
      z-index: 1001;
      display: flex;
      flex-direction: column;

      &--sm {
        max-width: 360px;
      }

      &--lg {
        max-width: 640px;
      }

      &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 20px 24px;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      }

      &__title {
        font-size: 1.125rem;
        font-weight: 600;
        color: #fff;
        margin: 0;
      }

      &__close {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        height: 32px;
        background: transparent;
        border: none;
        border-radius: 8px;
        color: rgba(255, 255, 255, 0.6);
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: rgba(255, 255, 255, 0.1);
          color: #fff;
        }
      }

      &__content {
        padding: 24px;
        overflow-y: auto;
        flex: 1;
      }

      &__footer {
        display: flex;
        gap: 12px;
        justify-content: flex-end;
        padding: 16px 24px;
        border-top: 1px solid rgba(255, 255, 255, 0.1);
        background: rgba(0, 0, 0, 0.2);
      }
    }
  `]
})
export class ModalComponent {
  @Input() isOpen = false;
  @Input() title = '';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() showFooter = true;
  @Input() closeOnOverlay = true;

  @Output() close = new EventEmitter<void>();

  onOverlayClick(): void {
    if (this.closeOnOverlay) {
      this.close.emit();
    }
  }
}
