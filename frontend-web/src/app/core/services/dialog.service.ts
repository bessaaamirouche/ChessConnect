import { Injectable, signal } from '@angular/core';

export interface DialogConfig {
  title: string;
  message: string;
  type: 'confirm' | 'alert' | 'prompt';
  confirmText?: string;
  cancelText?: string;
  inputLabel?: string;
  inputPlaceholder?: string;
  inputValue?: string;
  variant?: 'danger' | 'warning' | 'info' | 'success';
}

interface DialogState {
  isOpen: boolean;
  config: DialogConfig | null;
  resolve: ((value: boolean | string | null) => void) | null;
}

@Injectable({
  providedIn: 'root'
})
export class DialogService {
  private state = signal<DialogState>({
    isOpen: false,
    config: null,
    resolve: null
  });

  readonly isOpen = () => this.state().isOpen;
  readonly config = () => this.state().config;

  /**
   * Show a confirmation dialog (Yes/No)
   */
  confirm(message: string, title = 'Confirmation', options?: Partial<DialogConfig>): Promise<boolean> {
    return new Promise((resolve) => {
      this.state.set({
        isOpen: true,
        config: {
          type: 'confirm',
          title,
          message,
          confirmText: options?.confirmText || 'Confirmer',
          cancelText: options?.cancelText || 'Annuler',
          variant: options?.variant || 'info'
        },
        resolve: (value) => resolve(value as boolean)
      });
    });
  }

  /**
   * Show an alert dialog (OK only)
   */
  alert(message: string, title = 'Information', options?: Partial<DialogConfig>): Promise<void> {
    return new Promise((resolve) => {
      this.state.set({
        isOpen: true,
        config: {
          type: 'alert',
          title,
          message,
          confirmText: options?.confirmText || 'OK',
          variant: options?.variant || 'info'
        },
        resolve: () => resolve()
      });
    });
  }

  /**
   * Show a prompt dialog (input + OK/Cancel)
   */
  prompt(message: string, title = 'Saisie', options?: Partial<DialogConfig>): Promise<string | null> {
    return new Promise((resolve) => {
      this.state.set({
        isOpen: true,
        config: {
          type: 'prompt',
          title,
          message,
          confirmText: options?.confirmText || 'Valider',
          cancelText: options?.cancelText || 'Annuler',
          inputLabel: options?.inputLabel,
          inputPlaceholder: options?.inputPlaceholder || '',
          inputValue: options?.inputValue || '',
          variant: options?.variant || 'info'
        },
        resolve: (value) => resolve(value as string | null)
      });
    });
  }

  /**
   * Called by the dialog component when user confirms
   */
  handleConfirm(inputValue?: string): void {
    const { config, resolve } = this.state();
    if (resolve) {
      if (config?.type === 'prompt') {
        resolve(inputValue ?? null);
      } else if (config?.type === 'alert') {
        resolve(null);
      } else {
        resolve(true);
      }
    }
    this.close();
  }

  /**
   * Called by the dialog component when user cancels
   */
  handleCancel(): void {
    const { config, resolve } = this.state();
    if (resolve) {
      if (config?.type === 'prompt') {
        resolve(null);
      } else {
        resolve(false);
      }
    }
    this.close();
  }

  private close(): void {
    this.state.set({
      isOpen: false,
      config: null,
      resolve: null
    });
  }
}
