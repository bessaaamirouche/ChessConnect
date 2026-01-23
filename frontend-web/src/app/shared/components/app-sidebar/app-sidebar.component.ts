import { Component, Input, Output, EventEmitter, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChartBarSquare,
  heroCalendarDays,
  heroClipboardDocumentList,
  heroTrophy,
  heroCreditCard,
  heroAcademicCap,
  heroUserCircle,
  heroDocumentText,
  heroArrowRightOnRectangle,
  heroUsers,
  heroBanknotes,
  heroCog6Tooth,
  heroXMark,
  heroChevronLeft,
  heroChevronRight,
  heroBell,
  heroNewspaper
} from '@ng-icons/heroicons/outline';
import { AuthService } from '../../../core/services/auth.service';

export interface SidebarItem {
  label: string;
  icon: string;
  route?: string;
  action?: () => void;
  active?: boolean;
  badge?: string;
}

export interface SidebarSection {
  title: string;
  items: SidebarItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, NgIconComponent],
  providers: [
    provideIcons({
      heroChartBarSquare,
      heroCalendarDays,
      heroClipboardDocumentList,
      heroTrophy,
      heroCreditCard,
      heroAcademicCap,
      heroUserCircle,
      heroDocumentText,
      heroArrowRightOnRectangle,
      heroUsers,
      heroBanknotes,
      heroCog6Tooth,
      heroXMark,
      heroChevronLeft,
      heroChevronRight,
      heroBell,
      heroNewspaper
    })
  ],
  template: `
    <!-- Mobile Header -->
    <header class="mobile-header">
      <a routerLink="/" class="mobile-header__logo">
        <img src="assets/logo.png" alt="myChess" class="mobile-header__logo-img">
      </a>
      <button class="mobile-header__hamburger" [class.active]="mobileOpen()" (click)="toggleMobile()">
        <span></span>
        <span></span>
        <span></span>
      </button>
    </header>

    <!-- Mobile Overlay -->
    <div class="sidebar-overlay" [class.active]="mobileOpen()" (click)="closeMobile()"></div>

    <!-- Sidebar -->
    <aside class="sidebar" [class.collapsed]="collapsed()" [class.mobile-active]="mobileOpen()">
      <!-- Header -->
      <div class="sidebar__header">
        @if (!collapsed()) {
          <a routerLink="/" class="sidebar__logo">
            <img src="assets/logo.png" alt="mychess" class="sidebar__logo-img">
          </a>
        }
        <button class="sidebar__collapse-btn" (click)="toggleCollapse()" [title]="collapsed() ? 'Ouvrir' : 'Fermer'">
          @if (collapsed()) {
            <ng-icon name="heroChevronRight" size="18"></ng-icon>
          } @else {
            <ng-icon name="heroChevronLeft" size="18"></ng-icon>
          }
        </button>
        <button class="sidebar__close-btn" (click)="closeMobile()">
          <ng-icon name="heroXMark" size="20"></ng-icon>
        </button>
      </div>

      <!-- Navigation -->
      <nav class="sidebar__nav">
        @for (section of sections; track section.title) {
          <div class="sidebar__section">
            <span class="sidebar__section-title">{{ section.title }}</span>
            @for (item of section.items; track item.label) {
              @if (item.route) {
                <a
                  [routerLink]="item.route"
                  routerLinkActive="active"
                  [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' || item.route === '/admin/dashboard' }"
                  class="sidebar__link"
                  [class.active]="item.active"
                  [title]="collapsed() ? item.label : ''"
                  (click)="closeMobile()"
                >
                  <ng-icon [name]="item.icon" class="sidebar__icon"></ng-icon>
                  @if (!collapsed()) {
                    <span class="sidebar__link-text">{{ item.label }}</span>
                  }
                  @if (item.badge && !collapsed()) {
                    <span class="sidebar__badge">{{ item.badge }}</span>
                  }
                </a>
              } @else {
                <button
                  class="sidebar__link"
                  [class.active]="item.active"
                  [title]="collapsed() ? item.label : ''"
                  (click)="item.action?.(); closeMobile()"
                >
                  <ng-icon [name]="item.icon" class="sidebar__icon"></ng-icon>
                  @if (!collapsed()) {
                    <span class="sidebar__link-text">{{ item.label }}</span>
                  }
                </button>
              }
            }
          </div>
        }
      </nav>

      <!-- Footer -->
      <div class="sidebar__footer">
        @if (!collapsed()) {
          <div class="sidebar__user">
            <div class="sidebar__user-avatar">
              {{ userInitials() }}
            </div>
            <div class="sidebar__user-info">
              <span class="sidebar__user-name">{{ userName() }}</span>
              <span class="sidebar__user-role">{{ userRoleLabel() }}</span>
            </div>
          </div>
        } @else {
          <div class="sidebar__user-avatar sidebar__user-avatar--small" [title]="userName()">
            {{ userInitials() }}
          </div>
        }
      </div>
    </aside>
  `,
  styleUrl: './app-sidebar.component.scss'
})
export class AppSidebarComponent {
  @Input() sections: SidebarSection[] = [];
  @Output() onLogout = new EventEmitter<void>();
  @Output() collapsedChange = new EventEmitter<boolean>();

  private authService = inject(AuthService);

  collapsed = signal(false);
  mobileOpen = signal(false);

  userName = computed(() => {
    const user = this.authService.currentUser();
    return user ? `${user.firstName} ${user.lastName}` : '';
  });

  userInitials = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return '';
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  });

  userRoleLabel = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return '';
    switch (user.role) {
      case 'ADMIN': return 'Administrateur';
      case 'TEACHER': return 'Coach';
      case 'STUDENT': return 'Joueur';
      default: return '';
    }
  });

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
    localStorage.setItem('sidebar_collapsed', String(this.collapsed()));
    this.collapsedChange.emit(this.collapsed());
  }

  toggleMobile(): void {
    this.mobileOpen.update(v => !v);
  }

  closeMobile(): void {
    this.mobileOpen.set(false);
  }

  ngOnInit(): void {
    const saved = localStorage.getItem('sidebar_collapsed');
    if (saved === 'true') {
      this.collapsed.set(true);
      this.collapsedChange.emit(true);
    }
  }
}
