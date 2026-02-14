import { Component, input, output, computed, signal, effect } from '@angular/core';

export interface PillTab {
  id: string;
  label: string;
}

@Component({
  selector: 'app-pill-tabs',
  standalone: true,
  imports: [],
  template: `
    <div class="pill-tabs" [class.pill-tabs--compact]="compact()">
      @for (tab of tabs(); track tab.id; let i = $index) {
        <button
          type="button"
          class="pill-tab"
          [class.active]="activeTab() === tab.id"
          (click)="selectTab(tab.id)"
        >
          {{ tab.label }}
        </button>
      }
    </div>
  `,
  styles: [`
    /* Disney+ style pill tabs */
    .pill-tabs {
      display: inline-flex;
      gap: 0;
      background: rgba(255, 255, 255, 0.08);
      border-radius: 9999px;
      padding: 4px;
      border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .pill-tabs--compact {
      padding: 3px;
    }

    .pill-tabs--compact .pill-tab {
      padding: 6px 12px;
      font-size: 0.75rem;
    }

    .pill-tab {
      position: relative;
      padding: 8px 18px;
      font-size: 0.8125rem;
      font-weight: 500;
      border: none;
      background: transparent;
      color: rgba(255, 255, 255, 0.6);
      cursor: pointer;
      border-radius: 9999px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      white-space: nowrap;
      font-family: inherit;
    }

    .pill-tab:hover:not(.active) {
      color: rgba(255, 255, 255, 0.9);
      background: rgba(255, 255, 255, 0.05);
    }

    .pill-tab.active {
      background: #fff;
      color: #0d0d0f;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    }

    /* Responsive */
    @media (max-width: 480px) {
      .pill-tab {
        padding: 6px 12px;
        font-size: 0.75rem;
      }
    }
  `]
})
export class PillTabsComponent {
  tabs = input.required<PillTab[]>();
  activeTab = input<string>('');
  compact = input<boolean>(false);
  tabChange = output<string>();

  selectTab(tabId: string): void {
    this.tabChange.emit(tabId);
  }
}
