import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { AppSidebarComponent, SidebarSection } from '../../shared/components/app-sidebar/app-sidebar.component';

@Component({
  selector: 'app-user-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, AppSidebarComponent],
  templateUrl: './user-layout.component.html',
  styleUrl: './user-layout.component.scss'
})
export class UserLayoutComponent {
  private authService = inject(AuthService);

  sidebarSections = computed<SidebarSection[]>(() => {
    const sections: SidebarSection[] = [];

    // Admin menu
    if (this.authService.isAdmin()) {
      sections.push({
        title: 'Administration',
        items: [
          { label: 'Vue d\'ensemble', icon: 'heroChartBarSquare', route: '/admin/dashboard' },
          { label: 'Utilisateurs', icon: 'heroUsers', route: '/admin/users' },
          { label: 'Cours', icon: 'heroCalendarDays', route: '/admin/lessons' },
          { label: 'Comptabilite', icon: 'heroBanknotes', route: '/admin/accounting' },
          { label: 'Factures', icon: 'heroDocumentText', route: '/admin/invoices' }
        ]
      });
    }

    // Menu section for non-admin users
    if (!this.authService.isAdmin()) {
      const menuItems: any[] = [
        { label: 'Mon Espace', icon: 'heroChartBarSquare', route: '/dashboard' },
        { label: 'Mes Cours', icon: 'heroCalendarDays', route: '/lessons' }
      ];

      if (this.authService.isTeacher()) {
        menuItems.push({ label: 'Mes Disponibilites', icon: 'heroClipboardDocumentList', route: '/availability' });
      }

      if (this.authService.isStudent()) {
        menuItems.push({ label: 'Ma Progression', icon: 'heroTrophy', route: '/progress' });
        menuItems.push({ label: 'Abonnement', icon: 'heroCreditCard', route: '/subscription' });
        menuItems.push({ label: 'Trouver un Coach', icon: 'heroAcademicCap', route: '/teachers' });
      }

      sections.push({ title: 'Menu', items: menuItems });
    }

    // Compte section
    sections.push({
      title: 'Compte',
      items: [
        { label: 'Mon Profil', icon: 'heroUserCircle', route: '/settings' },
        { label: 'Mes Factures', icon: 'heroDocumentText', route: '/invoices' },
        { label: 'Deconnexion', icon: 'heroArrowRightOnRectangle', action: () => this.logout() }
      ]
    });

    return sections;
  });

  logout(): void {
    this.authService.logout();
  }
}
