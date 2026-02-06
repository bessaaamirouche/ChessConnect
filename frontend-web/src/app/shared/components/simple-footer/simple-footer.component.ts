import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

export interface FooterLink {
  route: string;
  labelKey: string;
  active?: boolean;
}

@Component({
  selector: 'app-simple-footer',
  standalone: true,
  imports: [RouterLink, TranslateModule],
  template: `
    <footer class="simple-footer">
      <div class="simple-footer__container">
        <p>
          @if (showBrand()) {
            &copy; 2026 mychess.
          }
          {{ copyrightKey() | translate }}
        </p>
        <div class="simple-footer__links">
          @for (link of links(); track link.route) {
            <a [routerLink]="link.route" [class.active]="link.active">{{ link.labelKey | translate }}</a>
          }
        </div>
      </div>
    </footer>
  `,
  styleUrl: './simple-footer.component.scss'
})
export class SimpleFooterComponent {
  links = input<FooterLink[]>([
    { route: '/terms', labelKey: 'footer.terms' },
    { route: '/privacy', labelKey: 'footer.privacy' },
    { route: '/legal-notice', labelKey: 'footer.legal' }
  ]);
  copyrightKey = input('footer.copyright');
  showBrand = input(true);
}
