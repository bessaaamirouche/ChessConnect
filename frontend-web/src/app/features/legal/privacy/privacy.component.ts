import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { SeoService } from '../../../core/services/seo.service';

@Component({
  selector: 'app-privacy',
  standalone: true,
  imports: [RouterLink, TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './privacy.component.html',
  styleUrl: './privacy.component.scss'
})
export class PrivacyComponent {
  authService = inject(AuthService);
  private seoService = inject(SeoService);

  mobileMenuOpen = signal(false);
  lastUpdate = '28 janvier 2026';

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Politique de Confidentialité - mychess',
      description: 'Découvrez comment mychess protège vos données personnelles conformément au RGPD.',
      keywords: 'RGPD, confidentialité, données personnelles, mychess, vie privée'
    });
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(open => !open);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}
