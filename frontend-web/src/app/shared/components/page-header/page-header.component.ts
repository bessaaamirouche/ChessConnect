import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroPlus } from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule, RouterLink, NgIconComponent],
  viewProviders: [provideIcons({ heroPlus })],
  template: `
    <header class="page-header">
      <div>
        <h1 class="page-header__title" [class.page-header__title--sm]="smallTitle">
          {{ title }}
        </h1>
        @if (subtitle) {
          <p class="page-header__subtitle">{{ subtitle }}</p>
        }
      </div>
      @if (actionLabel && actionRoute) {
        <a [routerLink]="actionRoute" class="btn btn--primary" [class.btn--sm]="smallAction">
          @if (showActionIcon) {
            <ng-icon name="heroPlus" size="16"></ng-icon>
          }
          {{ actionLabel }}
        </a>
      }
      @if (actionLabel && !actionRoute) {
        <ng-content select="[action]"></ng-content>
      }
    </header>
  `,
  styles: [`
    :host {
      display: block;
    }
  `]
})
export class PageHeaderComponent {
  @Input({ required: true }) title!: string;
  @Input() subtitle?: string;
  @Input() actionLabel?: string;
  @Input() actionRoute?: string;
  @Input() showActionIcon = true;
  @Input() smallTitle = false;
  @Input() smallAction = false;
}
