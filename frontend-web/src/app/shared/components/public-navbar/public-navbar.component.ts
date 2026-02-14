import { Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { LanguageSelectorComponent } from '../language-selector/language-selector.component';

export interface NavLink {
  route: string;
  labelKey: string;
  active?: boolean;
}

@Component({
    selector: 'app-public-navbar',
    imports: [RouterLink, TranslateModule, LanguageSelectorComponent],
    templateUrl: './public-navbar.component.html',
    styleUrl: './public-navbar.component.scss'
})
export class PublicNavbarComponent {
  authService = inject(AuthService);

  links = input.required<NavLink[]>();
  dashboardLabelKey = input('sidebar.dashboard');
  isScrolled = input(false);
  scrollProgress = input<number | null>(null);
  showDesktopLanguageSelector = input(false);
  staticBackground = input(true);

  mobileMenuOpen = signal(false);

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}
