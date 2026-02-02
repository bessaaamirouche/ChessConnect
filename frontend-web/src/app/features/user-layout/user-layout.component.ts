import { Component, computed, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { WalletService } from '../../core/services/wallet.service';
import { AppSidebarComponent, SidebarSection, SidebarItem } from '../../shared/components/app-sidebar/app-sidebar.component';

@Component({
  selector: 'app-user-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, AppSidebarComponent, TranslateModule],
  templateUrl: './user-layout.component.html',
  styleUrl: './user-layout.component.scss'
})
export class UserLayoutComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private walletService = inject(WalletService);
  private translate = inject(TranslateService);
  private langSubscription?: Subscription;

  sidebarCollapsed = signal(false);
  private currentLang = signal(this.translate.currentLang || this.translate.defaultLang);

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }

  ngOnInit(): void {
    // Load wallet balance for students
    if (this.authService.isStudent()) {
      this.walletService.loadBalance().subscribe();
    }

    // Subscribe to language changes to trigger sidebar recomputation
    this.langSubscription = this.translate.onLangChange.subscribe(event => {
      this.currentLang.set(event.lang);
    });
  }

  ngOnDestroy(): void {
    this.langSubscription?.unsubscribe();
  }

  sidebarSections = computed<SidebarSection[]>(() => {
    // Access currentLang signal to trigger recomputation on language change
    this.currentLang();

    const sections: SidebarSection[] = [];

    // Admin menu
    if (this.authService.isAdmin()) {
      sections.push({
        title: this.translate.instant('sidebar.admin'),
        items: [
          { label: this.translate.instant('sidebar.overview'), icon: 'heroChartBarSquare', route: '/admin/dashboard' },
          { label: this.translate.instant('sidebar.users'), icon: 'heroUsers', route: '/admin/users' },
          { label: this.translate.instant('sidebar.lessons'), icon: 'heroCalendarDays', route: '/admin/lessons' },
          { label: this.translate.instant('sidebar.accounting'), icon: 'heroBanknotes', route: '/admin/accounting' },
          { label: this.translate.instant('sidebar.invoices'), icon: 'heroDocumentText', route: '/admin/invoices' }
        ]
      });
    }

    // Menu section for non-admin users
    if (!this.authService.isAdmin()) {
      const menuItems: SidebarItem[] = [
        { label: this.translate.instant('sidebar.dashboard'), icon: 'heroChartBarSquare', route: '/dashboard' },
        { label: this.translate.instant('sidebar.myLessons'), icon: 'heroCalendarDays', route: '/lessons' }
      ];

      if (this.authService.isTeacher()) {
        menuItems.push({ label: this.translate.instant('sidebar.myAvailability'), icon: 'heroClipboardDocumentList', route: '/availability' });
        menuItems.push({ label: this.translate.instant('sidebar.programme'), icon: 'heroBookOpen', route: '/programme' });
      }

      if (this.authService.isStudent()) {
        menuItems.push({ label: this.translate.instant('sidebar.myProgress'), icon: 'heroBookOpen', route: '/programme' });
        menuItems.push({ label: this.translate.instant('sidebar.myLibrary'), icon: 'heroFilm', route: '/library' });
        menuItems.push({ label: this.translate.instant('sidebar.findCoach'), icon: 'heroAcademicCap', route: '/teachers' });
      }

      sections.push({ title: this.translate.instant('sidebar.menu'), items: menuItems });
    }

    // Compte section
    const compteItems: SidebarItem[] = [
      { label: this.translate.instant('sidebar.profile'), icon: 'heroUserCircle', route: '/settings' }
    ];

    compteItems.push({ label: this.translate.instant('sidebar.invoices'), icon: 'heroDocumentText', route: '/invoices' });
    compteItems.push({ label: this.translate.instant('sidebar.logout'), icon: 'heroArrowRightOnRectangle', action: () => this.logout() });

    sections.push({
      title: this.translate.instant('sidebar.account'),
      items: compteItems
    });

    return sections;
  });

  logout(): void {
    this.authService.logout();
  }
}
