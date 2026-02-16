import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SeoService } from '../../core/services/seo.service';
import { StructuredDataService } from '../../core/services/structured-data.service';
import { PublicNavbarComponent, NavLink } from '../../shared/components/public-navbar/public-navbar.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

@Component({
    selector: 'app-about',
    imports: [RouterLink, PublicNavbarComponent, FooterComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './about.component.html',
    styleUrl: './about.component.scss'
})
export class AboutComponent {
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);

  navLinks: NavLink[] = [
    { route: '/', labelKey: 'nav.home' },
    { route: '/coaches', labelKey: 'teachers.title' },
    { route: '/how-it-works', labelKey: 'nav.howItWorks' },
    { route: '/blog', labelKey: 'nav.blog' }
  ];

  constructor() {
    this.seoService.updateMetaTags({
      title: 'À propos de mychess - Plateforme de cours d\'échecs en ligne',
      description: 'mychess est une plateforme française de cours d\'échecs en ligne. 120 cours structurés, 546 leçons, 4 niveaux de progression. Premier cours gratuit.',
      url: 'https://mychess.fr/about'
    });
    this.structuredDataService.setEducationalOrganizationSchema();
    this.structuredDataService.setBreadcrumbSchema([
      { name: 'Accueil', url: 'https://mychess.fr' },
      { name: 'À propos', url: 'https://mychess.fr/about' }
    ]);
  }
}
