import { Component, computed, DestroyRef, inject, signal, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { AppSidebarComponent, SidebarSection } from '../../../shared/components/app-sidebar/app-sidebar.component';

@Component({
    selector: 'app-admin-layout',
    imports: [RouterOutlet, AppSidebarComponent, TranslateModule],
    templateUrl: './admin-layout.component.html',
    styleUrl: './admin-layout.component.scss'
})
export class AdminLayoutComponent implements OnInit {
  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);

  sidebarCollapsed = signal(false);
  private currentLang = signal(this.translate.currentLang || this.translate.defaultLang);

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }

  ngOnInit(): void {
    // Subscribe to language changes to trigger sidebar recomputation
    this.translate.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      this.currentLang.set(event.lang);
    });
  }

  sidebarSections = computed<SidebarSection[]>(() => {
    // Access currentLang signal to trigger recomputation on language change
    this.currentLang();

    return [
      {
        title: this.translate.instant('sidebar.adminDashboard'),
        items: [
          { label: this.translate.instant('sidebar.overview'), icon: 'heroChartBarSquare', route: '/mint/dashboard' },
          { label: this.translate.instant('sidebar.users'), icon: 'heroUsers', route: '/mint/users' },
          { label: this.translate.instant('sidebar.lessons'), icon: 'heroCalendarDays', route: '/mint/lessons' },
          { label: this.translate.instant('sidebar.accounting'), icon: 'heroBanknotes', route: '/mint/accounting' },
          { label: this.translate.instant('sidebar.stripeConnect'), icon: 'heroCreditCard', route: '/mint/stripe-connect' },
          { label: this.translate.instant('sidebar.invoices'), icon: 'heroDocumentText', route: '/mint/invoices' },
          { label: this.translate.instant('sidebar.blogAdmin'), icon: 'heroNewspaper', route: '/mint/blog' },
          { label: this.translate.instant('sidebar.messages'), icon: 'heroChatBubbleLeftRight', route: '/mint/messages' }
        ]
      },
      {
        title: this.translate.instant('sidebar.account'),
        items: [
          { label: this.translate.instant('sidebar.logout'), icon: 'heroArrowRightOnRectangle', action: () => this.logout() }
        ]
      }
    ];
  });

  logout(): void {
    this.authService.logout();
  }
}
