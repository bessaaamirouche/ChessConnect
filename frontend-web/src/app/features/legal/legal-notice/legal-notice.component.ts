import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { SeoService } from '../../../core/services/seo.service';

@Component({
  selector: 'app-legal-notice',
  standalone: true,
  imports: [RouterLink, TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './legal-notice.component.html',
  styleUrl: './legal-notice.component.scss'
})
export class LegalNoticeComponent {
  authService = inject(AuthService);
  private seoService = inject(SeoService);

  mobileMenuOpen = signal(false);
  lastUpdate = '28 janvier 2026';

  constructor() {
    this.seoService.updateMetaTags({
      title: 'Mentions Légales - mychess',
      description: 'Mentions légales du site mychess.fr - Informations sur l\'éditeur, l\'hébergeur et les conditions d\'utilisation.',
      keywords: 'mentions légales, éditeur, hébergeur, mychess, échecs'
    });
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(open => !open);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}
