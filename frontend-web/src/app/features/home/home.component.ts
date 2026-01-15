import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
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
  activeSection = signal<Section>('home');
  isTransitioning = signal(false);

  constructor(public authService: AuthService) {}

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
