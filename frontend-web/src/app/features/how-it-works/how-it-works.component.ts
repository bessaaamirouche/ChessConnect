import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroUserGroup } from '@ng-icons/heroicons/outline';
import { heroStarSolid } from '@ng-icons/heroicons/solid';
import { SeoService } from '../../core/services/seo.service';
import { StructuredDataService } from '../../core/services/structured-data.service';
import { PublicNavbarComponent, NavLink } from '../../shared/components/public-navbar/public-navbar.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

@Component({
    selector: 'app-how-it-works',
    imports: [RouterLink, TranslateModule, NgIconComponent, PublicNavbarComponent, FooterComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [provideIcons({ heroStarSolid, heroUserGroup })],
    templateUrl: './how-it-works.component.html',
    styleUrl: './how-it-works.component.scss'
})
export class HowItWorksComponent {
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);

  navLinks: NavLink[] = [
    { route: '/', labelKey: 'nav.home' },
    { route: '/teachers', labelKey: 'teachers.title' },
    { route: '/blog', labelKey: 'nav.blog' }
  ];

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Comment ca marche - mychess',
      description: 'Decouvrez comment fonctionne mychess : reservations, notifications, favoris et abonnement Premium pour les joueurs et les coachs.',
      keywords: 'mychess, comment ca marche, notifications, favoris, premium, cours echecs'
    });
    this.structuredDataService.setBreadcrumbSchema([
      { name: 'Accueil', url: 'https://mychess.fr/' },
      { name: 'Comment ça marche', url: 'https://mychess.fr/how-it-works' }
    ]);
  }
}
