import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { SeoService } from '../../../core/services/seo.service';
import { PublicNavbarComponent, NavLink } from '../../../shared/components/public-navbar/public-navbar.component';
import { FooterComponent } from '../../../shared/components/footer/footer.component';

@Component({
    selector: 'app-legal-notice',
    imports: [RouterLink, TranslateModule, PublicNavbarComponent, FooterComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './legal-notice.component.html',
    styleUrl: './legal-notice.component.scss'
})
export class LegalNoticeComponent {
  private seoService = inject(SeoService);

  lastUpdate = '28 janvier 2026';

  navLinks: NavLink[] = [
    { route: '/', labelKey: 'legal.home' },
    { route: '/teachers', labelKey: 'legal.ourCoaches' },
    { route: '/blog', labelKey: 'nav.blog' }
  ];

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Mentions Légales - mychess',
      description: 'Mentions légales du site mychess.fr - Informations sur l\'éditeur, l\'hébergeur et les conditions d\'utilisation.',
      keywords: 'mentions légales, éditeur, hébergeur, mychess, échecs'
    });
  }
}
