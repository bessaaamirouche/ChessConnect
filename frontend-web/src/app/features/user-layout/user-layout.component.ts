import { Component, computed, DestroyRef, inject, signal, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../core/services/auth.service';
import { WalletService } from '../../core/services/wallet.service';
import { AppSidebarComponent, SidebarSection, SidebarItem } from '../../shared/components/app-sidebar/app-sidebar.component';

@Component({
    selector: 'app-user-layout',
    imports: [RouterOutlet, AppSidebarComponent, TranslateModule],
    templateUrl: './user-layout.component.html',
    styleUrl: './user-layout.component.scss'
})
export class UserLayoutComponent implements OnInit {
  private authService = inject(AuthService);
  private walletService = inject(WalletService);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);

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
    this.translate.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      this.currentLang.set(event.lang);
    });
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
          { label: this.translate.instant('sidebar.overview'), icon: 'heroChartBarSquare', route: '/mint/dashboard' },
          { label: this.translate.instant('sidebar.users'), icon: 'heroUsers', route: '/mint/users' },
          { label: this.translate.instant('sidebar.lessons'), icon: 'heroCalendarDays', route: '/mint/lessons' },
          { label: this.translate.instant('sidebar.accounting'), icon: 'heroBanknotes', route: '/mint/accounting' },
          { label: this.translate.instant('sidebar.invoices'), icon: 'heroDocumentText', route: '/mint/invoices' }
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
