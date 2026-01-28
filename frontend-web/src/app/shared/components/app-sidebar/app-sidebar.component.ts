import { Component, Input, Output, EventEmitter, signal, computed, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
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
  heroNewspaper,
  heroTrash,
  heroCheckCircle,
  heroExclamationCircle,
  heroInformationCircle,
  heroExclamationTriangle,
  heroWallet,
  heroStar,
  heroBookOpen
} from '@ng-icons/heroicons/outline';
import { heroStarSolid } from '@ng-icons/heroicons/solid';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationCenterService, AppNotification } from '../../../core/services/notification-center.service';
import { PaymentService } from '../../../core/services/payment.service';

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
      heroNewspaper,
      heroTrash,
      heroCheckCircle,
      heroExclamationCircle,
      heroInformationCircle,
      heroExclamationTriangle,
      heroWallet,
      heroStar,
      heroStarSolid,
      heroBookOpen
    })
  ],
  template: `
    <!-- Mobile Header -->
    <header class="mobile-header">
      <a routerLink="/" class="mobile-header__logo">
        <img src="assets/logo.png" alt="myChess" class="mobile-header__logo-img">
      </a>
      <button
        class="mobile-header__hamburger"
        [class.active]="mobileOpen()"
        (click)="toggleMobile()"
        aria-label="Menu"
        [attr.aria-expanded]="mobileOpen()">
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
        <button class="sidebar__close-btn" (click)="closeMobile()" aria-label="Fermer le menu">
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

      <!-- Notification Center -->
      @if (!collapsed()) {
        <div class="notification-center">
          <button class="notification-center__toggle" (click)="toggleNotifications()">
            <ng-icon name="heroBell" class="notification-center__icon"></ng-icon>
            <span>Notifications</span>
            @if (notificationCenter.unreadCount() > 0) {
              <span class="notification-center__badge">{{ notificationCenter.unreadCount() }}</span>
            }
            <ng-icon [name]="notificationsOpen() ? 'heroChevronLeft' : 'heroChevronRight'" class="notification-center__chevron" [class.open]="notificationsOpen()"></ng-icon>
          </button>

          @if (notificationsOpen()) {
            <div class="notification-center__panel">
              <div class="notification-center__header">
                <span class="notification-center__title">Centre de notifications</span>
                @if (notificationCenter.hasNotifications()) {
                  <button class="notification-center__clear" (click)="clearAllNotifications()" title="Tout effacer">
                    <ng-icon name="heroTrash" size="16"></ng-icon>
                  </button>
                }
              </div>

              <div class="notification-center__list">
                @if (!notificationCenter.hasNotifications()) {
                  <div class="notification-center__empty">
                    <ng-icon name="heroBell" class="empty-icon"></ng-icon>
                    <p>Aucune notification</p>
                  </div>
                } @else {
                  @for (notification of notificationCenter.notifications(); track notification.id) {
                    <div
                      class="notification-item"
                      [class.unread]="!notification.read"
                      [class.clickable]="notification.link"
                      [class.info]="notification.type === 'info'"
                      [class.success]="notification.type === 'success'"
                      [class.warning]="notification.type === 'warning'"
                      [class.error]="notification.type === 'error'"
                      (click)="handleNotificationClick(notification)"
                    >
                      <div class="notification-item__icon">
                        @switch (notification.type) {
                          @case ('success') {
                            <ng-icon name="heroCheckCircle"></ng-icon>
                          }
                          @case ('error') {
                            <ng-icon name="heroExclamationCircle"></ng-icon>
                          }
                          @case ('warning') {
                            <ng-icon name="heroExclamationTriangle"></ng-icon>
                          }
                          @default {
                            <ng-icon name="heroInformationCircle"></ng-icon>
                          }
                        }
                      </div>
                      <div class="notification-item__content">
                        <div class="notification-item__title">{{ notification.title }}</div>
                        <div class="notification-item__message">{{ notification.message }}</div>
                        <div class="notification-item__time" [title]="notificationCenter.formatDateTime(notification.timestamp)">
                          {{ notificationCenter.formatRelativeTime(notification.timestamp) }}
                        </div>
                      </div>
                      <button
                        class="notification-item__delete"
                        (click)="deleteNotification($event, notification.id)"
                        title="Supprimer"
                      >
                        <ng-icon name="heroXMark" size="14"></ng-icon>
                      </button>
                    </div>
                  }
                }
              </div>
            </div>
          }
        </div>
      } @else {
        <!-- Collapsed: just icon with badge -->
        <div class="notification-center notification-center--collapsed">
          <button class="notification-center__toggle notification-center__toggle--icon" (click)="toggleNotifications()" title="Notifications">
            <ng-icon name="heroBell"></ng-icon>
            @if (notificationCenter.unreadCount() > 0) {
              <span class="notification-center__badge notification-center__badge--small">{{ notificationCenter.unreadCount() > 9 ? '9+' : notificationCenter.unreadCount() }}</span>
            }
          </button>
        </div>
      }

      <!-- Footer -->
      <div class="sidebar__footer">
        @if (!collapsed()) {
          <div class="sidebar__user">
            <div class="sidebar__user-avatar" [class.premium]="isPremium()">
              {{ userInitials() }}
              @if (isPremium()) {
                <span class="sidebar__user-avatar-badge">
                  <ng-icon name="heroStarSolid" size="10"></ng-icon>
                </span>
              }
            </div>
            <div class="sidebar__user-info">
              <div class="sidebar__user-name-row">
                <span class="sidebar__user-name">{{ userName() }}</span>
                @if (isPremium()) {
                  <span class="sidebar__premium-badge">PREMIUM</span>
                }
              </div>
              <span class="sidebar__user-role">{{ userRoleLabel() }}</span>
            </div>
          </div>
        } @else {
          <div class="sidebar__user-avatar sidebar__user-avatar--small" [class.premium]="isPremium()" [title]="userName() + (isPremium() ? ' (Premium)' : '')">
            {{ userInitials() }}
            @if (isPremium()) {
              <span class="sidebar__user-avatar-badge sidebar__user-avatar-badge--small">
                <ng-icon name="heroStarSolid" size="8"></ng-icon>
              </span>
            }
          </div>
        }
      </div>
    </aside>
  `,
  styleUrl: './app-sidebar.component.scss'
})
export class AppSidebarComponent implements OnInit {
  @Input() sections: SidebarSection[] = [];
  @Output() onLogout = new EventEmitter<void>();
  @Output() collapsedChange = new EventEmitter<boolean>();

  private authService = inject(AuthService);
  private router = inject(Router);
  private paymentService = inject(PaymentService);
  private platformId = inject(PLATFORM_ID);
  notificationCenter = inject(NotificationCenterService);

  collapsed = signal(false);
  mobileOpen = signal(false);
  notificationsOpen = signal(false);

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

  isPremium = computed(() => {
    const user = this.authService.currentUser();
    return user?.role === 'STUDENT' && this.paymentService.hasActiveSubscription();
  });

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('sidebar_collapsed', String(this.collapsed()));
    }
    this.collapsedChange.emit(this.collapsed());
  }

  toggleMobile(): void {
    this.mobileOpen.update(v => !v);
  }

  closeMobile(): void {
    this.mobileOpen.set(false);
  }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      const saved = localStorage.getItem('sidebar_collapsed');
      if (saved === 'true') {
        this.collapsed.set(true);
        this.collapsedChange.emit(true);
      }
    }

    // Load subscription status for students
    if (this.authService.isStudent()) {
      this.paymentService.loadActiveSubscription().subscribe();
    }
  }

  toggleNotifications(): void {
    this.notificationsOpen.update(v => !v);
  }

  handleNotificationClick(notification: AppNotification): void {
    this.notificationCenter.markAsRead(notification.id);
    if (notification.link) {
      this.closeMobile();
      this.notificationsOpen.set(false);
      this.router.navigateByUrl(notification.link);
    }
  }

  deleteNotification(event: Event, id: string): void {
    event.stopPropagation();
    this.notificationCenter.remove(id);
  }

  clearAllNotifications(): void {
    this.notificationCenter.clearAll();
  }
}
