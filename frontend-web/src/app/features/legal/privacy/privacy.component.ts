import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { SeoService } from '../../../core/services/seo.service';
import { PublicNavbarComponent, NavLink } from '../../../shared/components/public-navbar/public-navbar.component';
import { SimpleFooterComponent, FooterLink } from '../../../shared/components/simple-footer/simple-footer.component';

@Component({
  selector: 'app-privacy',
  standalone: true,
  imports: [RouterLink, TranslateModule, PublicNavbarComponent, SimpleFooterComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './privacy.component.html',
  styleUrl: './privacy.component.scss'
})
export class PrivacyComponent {
  private seoService = inject(SeoService);

  lastUpdate = '28 janvier 2026';

  navLinks: NavLink[] = [
    { route: '/', labelKey: 'legal.home' },
    { route: '/teachers', labelKey: 'legal.ourCoaches' },
    { route: '/blog', labelKey: 'nav.blog' }
  ];

  footerLinks: FooterLink[] = [
    { route: '/', labelKey: 'legal.home' },
    { route: '/terms', labelKey: 'footer.terms' },
    { route: '/privacy', labelKey: 'footer.privacy', active: true },
    { route: '/legal-notice', labelKey: 'footer.legal' }
  ];

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Politique de Confidentialité - mychess',
      description: 'Découvrez comment mychess protège vos données personnelles conformément au RGPD.',
      keywords: 'RGPD, confidentialité, données personnelles, mychess, vie privée'
    });
  }
}
