import { Component, computed, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AppSidebarComponent, SidebarSection } from '../../../shared/components/app-sidebar/app-sidebar.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, AppSidebarComponent, TranslateModule],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss'
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  private langSubscription?: Subscription;

  sidebarCollapsed = signal(false);
  private currentLang = signal(this.translate.currentLang || this.translate.defaultLang);

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }

  ngOnInit(): void {
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

    return [
      {
        title: this.translate.instant('sidebar.adminDashboard'),
        items: [
          { label: this.translate.instant('sidebar.overview'), icon: 'heroChartBarSquare', route: '/admin/dashboard' },
          { label: this.translate.instant('sidebar.users'), icon: 'heroUsers', route: '/admin/users' },
          { label: this.translate.instant('sidebar.lessons'), icon: 'heroCalendarDays', route: '/admin/lessons' },
          { label: this.translate.instant('sidebar.accounting'), icon: 'heroBanknotes', route: '/admin/accounting' },
          { label: this.translate.instant('sidebar.stripeConnect'), icon: 'heroCreditCard', route: '/admin/stripe-connect' },
          { label: this.translate.instant('sidebar.invoices'), icon: 'heroDocumentText', route: '/admin/invoices' },
          { label: this.translate.instant('sidebar.blogAdmin'), icon: 'heroNewspaper', route: '/admin/blog' },
          { label: this.translate.instant('sidebar.messages'), icon: 'heroChatBubbleLeftRight', route: '/admin/messages' }
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
