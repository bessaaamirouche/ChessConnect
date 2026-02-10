import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LanguageService, Language } from '../../../core/services/language.service';

@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="language-selector">
      <button
        class="language-btn"
        (click)="toggleDropdown()"
        [attr.aria-expanded]="isOpen"
        aria-haspopup="true"
      >
        <span class="language-flag">{{ getCurrentFlag() }}</span>
        <svg class="chevron" [class.open]="isOpen" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
        </svg>
      </button>

      @if (isOpen) {
        <div class="language-dropdown" role="menu">
          @for (lang of languageService.availableLanguages; track lang.code) {
            <button
              class="language-option"
              [class.active]="lang.code === languageService.currentLang()"
              (click)="selectLanguage(lang.code)"
              role="menuitem"
            >
              <span class="language-flag">{{ lang.flag }}</span>
              <span class="language-name">{{ lang.name }}</span>
              @if (lang.code === languageService.currentLang()) {
                <svg class="check" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                </svg>
              }
            </button>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .language-selector {
      position: relative;
    }

    .language-btn {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: transparent;
      border: none;
      color: inherit;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        opacity: 0.8;
      }
    }

    .language-flag {
      font-size: 1.25rem;
      line-height: 1;
    }


    .chevron {
      width: 1rem;
      height: 1rem;
      transition: transform 0.2s ease;

      &.open {
        transform: rotate(180deg);
      }
    }

    .language-dropdown {
      position: absolute;
      top: calc(100% + 0.5rem);
      right: 0;
      min-width: 150px;
      background: var(--color-surface, #1a1a2e);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 0.5rem;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
      overflow: hidden;
      z-index: 100;
    }

    .language-option {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      width: 100%;
      padding: 0.75rem 1rem;
      background: transparent;
      border: none;
      color: inherit;
      cursor: pointer;
      transition: background 0.2s ease;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
      }

      &.active {
        background: rgba(212, 175, 55, 0.1);
        color: var(--color-gold, #d4af37);
      }
    }

    .language-name {
      flex: 1;
      text-align: left;
      font-size: 0.875rem;
    }

    .check {
      width: 1rem;
      height: 1rem;
      color: var(--color-gold, #d4af37);
    }
  `]
})
export class LanguageSelectorComponent {
  languageService = inject(LanguageService);
  isOpen = false;

  toggleDropdown(): void {
    this.isOpen = !this.isOpen;
  }

  selectLanguage(lang: Language): void {
    this.languageService.setLanguage(lang);
    this.isOpen = false;
  }

  getCurrentFlag(): string {
    const currentLang = this.languageService.availableLanguages.find(
      l => l.code === this.languageService.currentLang()
    );
    return currentLang?.flag || '🌐';
  }
}
