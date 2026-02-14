import { Component, input } from '@angular/core';

import { RouterLink } from '@angular/router';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroPlus } from '@ng-icons/heroicons/outline';

@Component({
    selector: 'app-page-header',
    imports: [RouterLink, NgIconComponent],
    viewProviders: [provideIcons({ heroPlus })],
    template: `
    <header class="page-header">
      <div>
        <h1 class="page-header__title" [class.page-header__title--sm]="smallTitle()">
          {{ title() }}
        </h1>
        @if (subtitle()) {
          <p class="page-header__subtitle">{{ subtitle() }}</p>
        }
      </div>
      @if (actionLabel() && actionRoute()) {
        <a [routerLink]="actionRoute()" class="btn btn--primary" [class.btn--sm]="smallAction()">
          @if (showActionIcon()) {
            <ng-icon name="heroPlus" size="16"></ng-icon>
          }
          {{ actionLabel() }}
        </a>
      }
      @if (actionLabel() && !actionRoute()) {
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
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
  readonly actionLabel = input<string>();
  readonly actionRoute = input<string>();
  readonly showActionIcon = input(true);
  readonly smallTitle = input(false);
  readonly smallAction = input(false);
}
