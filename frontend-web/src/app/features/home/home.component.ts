import { Component, signal, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { StructuredDataService } from '../../core/services/structured-data.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroUserGroup,
  heroAcademicCap,
  heroGlobeAlt,
  heroTrophy,
  heroChatBubbleLeftRight,
  heroPlayCircle,
  heroVideoCamera,
  heroClock,
  heroShieldCheck,
  heroCheck,
  heroArrowRight
} from '@ng-icons/heroicons/outline';

type Section = 'home' | 'profils' | 'apprentis' | 'fonctionnement' | 'contact';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, NgIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroUserGroup,
    heroAcademicCap,
    heroGlobeAlt,
    heroTrophy,
    heroChatBubbleLeftRight,
    heroPlayCircle,
    heroVideoCamera,
    heroClock,
    heroShieldCheck,
    heroCheck,
    heroArrowRight
  })],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);

  activeSection = signal<Section>('home');
  isTransitioning = signal(false);
  mobileMenuOpen = signal(false);

  constructor(public authService: AuthService) {
    this.seoService.setHomePage();
    this.structuredDataService.setOrganizationSchema();
    this.structuredDataService.setCourseSchema();
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  navigateTo(section: Section): void {
    if (this.activeSection() === section || this.isTransitioning()) return;

    this.isTransitioning.set(true);

    // Petite pause pour l'animation
    setTimeout(() => {
      this.activeSection.set(section);
      setTimeout(() => {
        this.isTransitioning.set(false);
      }, 300);
    }, 150);
  }

  isActive(section: Section): boolean {
    return this.activeSection() === section;
  }
}
