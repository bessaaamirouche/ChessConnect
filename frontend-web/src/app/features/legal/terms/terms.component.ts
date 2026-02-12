import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { SeoService } from '../../../core/services/seo.service';
import { PublicNavbarComponent, NavLink } from '../../../shared/components/public-navbar/public-navbar.component';
import { FooterComponent } from '../../../shared/components/footer/footer.component';

@Component({
    selector: 'app-terms',
    imports: [RouterLink, TranslateModule, PublicNavbarComponent, FooterComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './terms.component.html',
    styleUrl: './terms.component.scss'
})
export class TermsComponent {
  private seoService = inject(SeoService);

  lastUpdate = '28 janvier 2026';

  navLinks: NavLink[] = [
    { route: '/', labelKey: 'legal.home' },
    { route: '/teachers', labelKey: 'legal.ourCoaches' },
    { route: '/blog', labelKey: 'nav.blog' }
  ];

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Conditions Générales d\'Utilisation - mychess',
      description: 'Consultez les conditions générales d\'utilisation de la plateforme mychess, service de cours d\'échecs en ligne.',
      keywords: 'CGU, conditions utilisation, mychess, échecs'
    });
  }
}
