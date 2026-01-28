import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { WalletService } from '../../core/services/wallet.service';
import { AppSidebarComponent, SidebarSection, SidebarItem } from '../../shared/components/app-sidebar/app-sidebar.component';

@Component({
  selector: 'app-user-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, AppSidebarComponent],
  templateUrl: './user-layout.component.html',
  styleUrl: './user-layout.component.scss'
})
export class UserLayoutComponent implements OnInit {
  private authService = inject(AuthService);
  private walletService = inject(WalletService);

  sidebarCollapsed = signal(false);

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }

  ngOnInit(): void {
    // Load wallet balance for students
    if (this.authService.isStudent()) {
      this.walletService.loadBalance().subscribe();
    }
  }

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
      const menuItems: SidebarItem[] = [
        { label: 'Mon Espace', icon: 'heroChartBarSquare', route: '/dashboard' },
        { label: 'Mes Cours', icon: 'heroCalendarDays', route: '/lessons' }
      ];

      if (this.authService.isTeacher()) {
        menuItems.push({ label: 'Mes Disponibilites', icon: 'heroClipboardDocumentList', route: '/availability' });
        menuItems.push({ label: 'Programme', icon: 'heroBookOpen', route: '/programme' });
      }

      if (this.authService.isStudent()) {
        menuItems.push({ label: 'Ma Progression', icon: 'heroBookOpen', route: '/programme' });
        menuItems.push({ label: 'Trouver un Coach', icon: 'heroAcademicCap', route: '/teachers' });
      }

      sections.push({ title: 'Menu', items: menuItems });
    }

    // Compte section
    const compteItems: SidebarItem[] = [
      { label: 'Mon Profil', icon: 'heroUserCircle', route: '/settings' }
    ];

    if (this.authService.isStudent()) {
      compteItems.push({ label: 'Mon Solde', icon: 'heroWallet', route: '/wallet' });
      compteItems.push({ label: 'Abonnement', icon: 'heroCreditCard', route: '/subscription' });
    }

    compteItems.push({ label: 'Mes Factures', icon: 'heroDocumentText', route: '/invoices' });
    compteItems.push({ label: 'Deconnexion', icon: 'heroArrowRightOnRectangle', action: () => this.logout() });

    sections.push({
      title: 'Compte',
      items: compteItems
    });

    return sections;
  });

  logout(): void {
    this.authService.logout();
  }
}
