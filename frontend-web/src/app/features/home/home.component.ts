import {
  Component,
  signal,
  ChangeDetectionStrategy,
  inject,
  OnInit,
  OnDestroy,
  PLATFORM_ID
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { StructuredDataService } from '../../core/services/structured-data.service';
import { ScrollRevealDirective, StaggerRevealDirective } from '../../shared/directives/scroll-reveal.directive';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroUserGroup,
  heroAcademicCap,
  heroVideoCamera,
  heroClock,
  heroShieldCheck,
  heroTrophy,
  heroArrowRight,
  heroGift
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    RouterLink,
    NgIconComponent,
    ScrollRevealDirective,
    StaggerRevealDirective
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroUserGroup,
    heroAcademicCap,
    heroVideoCamera,
    heroClock,
    heroShieldCheck,
    heroTrophy,
    heroArrowRight,
    heroGift
  })],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit, OnDestroy {
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);
  private platformId = inject(PLATFORM_ID);

  // Signals
  mobileMenuOpen = signal(false);
  isScrolled = signal(false);
  scrollProgress = signal(0);

  // Scroll listener reference for cleanup
  private scrollListener: (() => void) | null = null;
  private rafId: number | null = null;

  constructor(public authService: AuthService) {
    this.seoService.setHomePage();
    this.structuredDataService.setOrganizationSchema();
    this.structuredDataService.setCourseSchema();
  }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.setupScrollListener();
    }
  }

  ngOnDestroy(): void {
    this.removeScrollListener();
  }

  private setupScrollListener(): void {
    this.scrollListener = () => {
      if (this.rafId) {
        cancelAnimationFrame(this.rafId);
      }
      this.rafId = requestAnimationFrame(() => this.handleScroll());
    };

    window.addEventListener('scroll', this.scrollListener, { passive: true });
    // Initial check
    this.handleScroll();
  }

  private removeScrollListener(): void {
    if (this.scrollListener) {
      window.removeEventListener('scroll', this.scrollListener);
      this.scrollListener = null;
    }
    if (this.rafId) {
      cancelAnimationFrame(this.rafId);
      this.rafId = null;
    }
  }

  private handleScroll(): void {
    const scrollY = window.scrollY;
    const documentHeight = document.documentElement.scrollHeight - window.innerHeight;

    // Update isScrolled for navbar background
    this.isScrolled.set(scrollY > 50);

    // Update scroll progress (0 to 1)
    const progress = documentHeight > 0 ? Math.min(scrollY / documentHeight, 1) : 0;
    this.scrollProgress.set(progress);
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  scrollToSection(sectionId: string): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const element = document.getElementById(sectionId);
    if (element) {
      const navHeight = 80; // Account for fixed navbar
      const elementPosition = element.getBoundingClientRect().top + window.scrollY;
      const offsetPosition = elementPosition - navHeight;

      window.scrollTo({
        top: offsetPosition,
        behavior: 'smooth'
      });
    }
  }
}
