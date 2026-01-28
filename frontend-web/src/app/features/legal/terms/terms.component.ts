import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { SeoService } from '../../../core/services/seo.service';

@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './terms.component.html',
  styleUrl: './terms.component.scss'
})
export class TermsComponent {
  authService = inject(AuthService);
  private seoService = inject(SeoService);

  mobileMenuOpen = signal(false);
  lastUpdate = '28 janvier 2026';

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Conditions Generales d\'Utilisation - mychess',
      description: 'Consultez les conditions generales d\'utilisation de la plateforme mychess, service de cours d\'echecs en ligne.',
      keywords: 'CGU, conditions utilisation, mychess, echecs'
    });
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(open => !open);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}
