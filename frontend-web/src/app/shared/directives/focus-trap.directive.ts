import { Directive, ElementRef, OnInit, OnDestroy, Input, booleanAttribute } from '@angular/core';

/**
 * Focus Trap Directive
 * Traps keyboard focus within an element (useful for modals/dialogs)
 * Usage: <div appFocusTrap>...</div>
 */
@Directive({
  selector: '[appFocusTrap]',
  standalone: true
})
export class FocusTrapDirective implements OnInit, OnDestroy {
  @Input({ transform: booleanAttribute }) appFocusTrap = true; // Enable/disable the trap

  private focusableElements: HTMLElement[] = [];
  private firstFocusable: HTMLElement | null = null;
  private lastFocusable: HTMLElement | null = null;
  private previouslyFocused: HTMLElement | null = null;

  private readonly focusableSelectors = [
    'button:not([disabled]):not([tabindex="-1"])',
    'a[href]:not([tabindex="-1"])',
    'input:not([disabled]):not([type="hidden"]):not([tabindex="-1"])',
    'select:not([disabled]):not([tabindex="-1"])',
    'textarea:not([disabled]):not([tabindex="-1"])',
    '[tabindex]:not([tabindex="-1"])',
    '[contenteditable="true"]'
  ].join(', ');

  constructor(private el: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    if (!this.appFocusTrap) return;

    // Store the previously focused element
    this.previouslyFocused = document.activeElement as HTMLElement;

    // Wait for the DOM to be ready
    setTimeout(() => {
      this.updateFocusableElements();
      this.focusFirstElement();
      this.addEventListeners();
    }, 0);
  }

  ngOnDestroy(): void {
    this.removeEventListeners();

    // Restore focus to the previously focused element
    if (this.previouslyFocused && this.previouslyFocused.focus) {
      setTimeout(() => {
        this.previouslyFocused?.focus();
      }, 0);
    }
  }

  private updateFocusableElements(): void {
    const elements = this.el.nativeElement.querySelectorAll<HTMLElement>(this.focusableSelectors);
    this.focusableElements = Array.from(elements).filter(el => {
      // Check if element is visible
      const style = window.getComputedStyle(el);
      return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
    });

    this.firstFocusable = this.focusableElements[0] || null;
    this.lastFocusable = this.focusableElements[this.focusableElements.length - 1] || null;
  }

  private focusFirstElement(): void {
    if (this.firstFocusable) {
      this.firstFocusable.focus();
    }
  }

  private handleKeyDown = (event: KeyboardEvent): void => {
    if (event.key !== 'Tab') return;

    // Update focusable elements in case DOM changed
    this.updateFocusableElements();

    if (this.focusableElements.length === 0) {
      event.preventDefault();
      return;
    }

    const activeElement = document.activeElement;

    if (event.shiftKey) {
      // Shift + Tab: moving backwards
      if (activeElement === this.firstFocusable) {
        event.preventDefault();
        this.lastFocusable?.focus();
      }
    } else {
      // Tab: moving forwards
      if (activeElement === this.lastFocusable) {
        event.preventDefault();
        this.firstFocusable?.focus();
      }
    }
  };

  private addEventListeners(): void {
    this.el.nativeElement.addEventListener('keydown', this.handleKeyDown);
  }

  private removeEventListeners(): void {
    this.el.nativeElement.removeEventListener('keydown', this.handleKeyDown);
  }
}
