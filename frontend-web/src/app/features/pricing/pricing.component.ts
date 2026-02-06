import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/services/auth.service';
import { PublicNavbarComponent, NavLink } from '../../shared/components/public-navbar/public-navbar.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { SeoService } from '../../core/services/seo.service';
import { StructuredDataService } from '../../core/services/structured-data.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroCheck,
  heroXMark,
  heroSparkles,
  heroVideoCamera,
  heroBell,
  heroCpuChip,
  heroStar,
  heroCalendarDays,
  heroShieldCheck,
  heroClock,
  heroArrowTrendingUp,
  heroGift
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [RouterLink, NgIconComponent, TranslateModule, PublicNavbarComponent, FooterComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroCheck,
    heroXMark,
    heroSparkles,
    heroVideoCamera,
    heroBell,
    heroCpuChip,
    heroStar,
    heroCalendarDays,
    heroShieldCheck,
    heroClock,
    heroArrowTrendingUp,
    heroGift
  })],
  templateUrl: './pricing.component.html',
  styleUrl: './pricing.component.scss'
})
export class PricingComponent {
  authService = inject(AuthService);
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);

  navLinks: NavLink[] = [
    { route: '/', labelKey: 'nav.home' },
    { route: '/teachers', labelKey: 'teachers.title' },
    { route: '/how-it-works', labelKey: 'nav.howItWorks' },
    { route: '/blog', labelKey: 'nav.blog' }
  ];

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Tarifs - mychess | Cours d\'echecs et abonnement Premium',
      description: 'Decouvrez nos tarifs : cours d\'echecs avec coachs qualifies et abonnement Premium a 4,99E/mois. Essai gratuit 14 jours sans carte bancaire !',
      keywords: 'mychess, tarifs, prix, cours echecs, premium, abonnement'
    });
    this.structuredDataService.setBreadcrumbSchema([
      { name: 'Accueil', url: 'https://mychess.fr/' },
      { name: 'Tarifs', url: 'https://mychess.fr/pricing' }
    ]);
  }
}
