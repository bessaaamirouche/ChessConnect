import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AppSidebarComponent, SidebarSection } from '../../../shared/components/app-sidebar/app-sidebar.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, AppSidebarComponent],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss'
})
export class AdminLayoutComponent {
  private authService = inject(AuthService);

  sidebarCollapsed = signal(false);

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }

  sidebarSections: SidebarSection[] = [
    {
      title: 'Tableau de bord',
      items: [
        { label: 'Vue d\'ensemble', icon: 'heroChartBarSquare', route: '/admin/dashboard' },
        { label: 'Utilisateurs', icon: 'heroUsers', route: '/admin/users' },
        { label: 'Cours', icon: 'heroCalendarDays', route: '/admin/lessons' },
        { label: 'Comptabilité', icon: 'heroBanknotes', route: '/admin/accounting' },
        { label: 'Factures', icon: 'heroDocumentText', route: '/admin/invoices' },
        { label: 'Blog', icon: 'heroNewspaper', route: '/admin/blog' }
      ]
    },
    {
      title: 'Compte',
      items: [
        { label: 'Déconnexion', icon: 'heroArrowRightOnRectangle', action: () => this.logout() }
      ]
    }
  ];

  logout(): void {
    this.authService.logout();
  }
}
