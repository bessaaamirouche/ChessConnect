import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'app-how-it-works',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './how-it-works.component.html',
  styleUrl: './how-it-works.component.scss'
})
export class HowItWorksComponent {
  authService = inject(AuthService);
  private seoService = inject(SeoService);

  mobileMenuOpen = signal(false);

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Comment ca marche - mychess',
      description: 'Decouvrez comment fonctionne mychess : reservations, notifications, favoris et abonnement Premium pour les joueurs et les coachs.',
      keywords: 'mychess, comment ca marche, notifications, favoris, premium, cours echecs'
    });
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(open => !open);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}
