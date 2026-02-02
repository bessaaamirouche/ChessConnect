import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
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
  imports: [RouterLink, NgIconComponent, TranslateModule],
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

  mobileMenuOpen = signal(false);

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Tarifs - mychess | Cours d\'echecs et abonnement Premium',
      description: 'Decouvrez nos tarifs : cours d\'echecs avec coachs qualifies et abonnement Premium a 4,99E/mois. Essai gratuit 14 jours sans carte bancaire !',
      keywords: 'mychess, tarifs, prix, cours echecs, premium, abonnement'
    });
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}
