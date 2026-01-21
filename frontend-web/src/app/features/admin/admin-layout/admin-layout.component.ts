import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChartBarSquare,
  heroUsers,
  heroCalendarDays,
  heroBanknotes,
  heroCog6Tooth,
  heroArrowRightOnRectangle,
  heroXMark
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, NgIconComponent],
  viewProviders: [provideIcons({
    heroChartBarSquare,
    heroUsers,
    heroCalendarDays,
    heroBanknotes,
    heroCog6Tooth,
    heroArrowRightOnRectangle,
    heroXMark
  })],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss'
})
export class AdminLayoutComponent {
  mobileMenuOpen = signal(false);

  constructor(private authService: AuthService) {}

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
  }
}
