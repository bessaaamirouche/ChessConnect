import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TeacherService } from '../../../core/services/teacher.service';
import { AuthService } from '../../../core/services/auth.service';
import { FavoriteService } from '../../../core/services/favorite.service';
import { LessonService } from '../../../core/services/lesson.service';
import { SeoService } from '../../../core/services/seo.service';
import { AppSidebarComponent, SidebarSection } from '../../../shared/components/app-sidebar/app-sidebar.component';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroAcademicCap,
  heroChartBarSquare,
  heroCalendarDays,
  heroClipboardDocumentList,
  heroTrophy,
  heroCreditCard,
  heroUserCircle,
  heroArrowRightOnRectangle,
  heroMagnifyingGlass,
  heroXMark,
  heroBookOpen
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-teacher-list',
  standalone: true,
  imports: [RouterLink, NgIconComponent, FormsModule, AppSidebarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroAcademicCap,
    heroChartBarSquare,
    heroCalendarDays,
    heroClipboardDocumentList,
    heroTrophy,
    heroCreditCard,
    heroUserCircle,
    heroArrowRightOnRectangle,
    heroMagnifyingGlass,
    heroXMark,
    heroBookOpen
  })],
  templateUrl: './teacher-list.component.html',
  styleUrl: './teacher-list.component.scss'
})
export class TeacherListComponent implements OnInit {
  private seoService = inject(SeoService);

  sidebarCollapsed = signal(false);

  sidebarSections = computed<SidebarSection[]>(() => {
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
      menuItems.push({ label: 'Trouver un Coach', icon: 'heroAcademicCap', route: '/teachers', active: true });
    }

    return [
      { title: 'Menu', items: menuItems },
      {
        title: 'Compte',
        items: [
          { label: 'Mon Profil', icon: 'heroUserCircle', route: '/settings' },
          { label: 'Mes Factures', icon: 'heroDocumentText', route: '/invoices' },
          { label: 'Deconnexion', icon: 'heroArrowRightOnRectangle', action: () => this.logout() }
        ]
      }
    ];
  });

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }

  searchQuery = signal('');
  minRate = signal<number | null>(null);
  maxRate = signal<number | null>(null);

  // Free trial mode: true = show only coaches accepting free trial, false = show all
  useFreeTrialMode = signal(true);

  // Sort teachers: favorites first, then by name
  filteredTeachers = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const min = this.minRate();
    const max = this.maxRate();
    const favoriteIds = this.favoriteService.favoriteTeacherIds();

    const filtered = this.teacherService.teachers().filter(teacher => {
      // Filter by name
      const fullName = `${teacher.firstName} ${teacher.lastName}`.toLowerCase();
      const matchesName = !query || fullName.includes(query);

      // Filter by rate
      const rate = teacher.hourlyRateCents ? teacher.hourlyRateCents / 100 : 0;
      const matchesMinRate = min === null || rate >= min;
      const matchesMaxRate = max === null || rate <= max;

      return matchesName && matchesMinRate && matchesMaxRate;
    });

    // Sort: favorites first
    return filtered.sort((a, b) => {
      const aIsFav = favoriteIds.has(a.id) ? 0 : 1;
      const bIsFav = favoriteIds.has(b.id) ? 0 : 1;
      if (aIsFav !== bIsFav) return aIsFav - bIsFav;
      return a.firstName.localeCompare(b.firstName);
    });
  });

  // Free trial eligibility signal
  freeTrialEligible = this.lessonService.freeTrialEligible;

  constructor(
    public teacherService: TeacherService,
    public authService: AuthService,
    public favoriteService: FavoriteService,
    private lessonService: LessonService
  ) {}

  ngOnInit(): void {
    this.seoService.setTeachersListPage();

    // If user is a student, check free trial eligibility first
    if (this.authService.isStudent()) {
      this.lessonService.checkFreeTrialEligibility().subscribe(eligible => {
        if (eligible) {
          // Student is eligible for free trial, only show teachers accepting free trials
          this.teacherService.loadTeachersAcceptingFreeTrial().subscribe();
        } else {
          // Student already used free trial, show all teachers
          this.teacherService.loadTeachers().subscribe();
        }
      });
      this.favoriteService.loadFavorites().subscribe();
    } else {
      // Not a student (or not logged in), show all teachers
      this.teacherService.loadTeachers().subscribe();
    }
  }

  toggleFavorite(teacherId: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    if (this.favoriteService.isFavorite(teacherId)) {
      this.favoriteService.removeFavorite(teacherId).subscribe();
    } else {
      this.favoriteService.addFavorite(teacherId).subscribe();
    }
  }

  isFavorite(teacherId: number): boolean {
    return this.favoriteService.isFavorite(teacherId);
  }

  formatPrice(cents: number | undefined): string {
    if (!cents) return '-';
    return (cents / 100).toFixed(0) + '€/h';
  }

  getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }

  formatCoachName(firstName: string, lastName: string): string {
    return `${firstName} ${lastName.charAt(0)}.`;
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.minRate.set(null);
    this.maxRate.set(null);
  }

  hasActiveFilters(): boolean {
    return this.searchQuery() !== '' || this.minRate() !== null || this.maxRate() !== null;
  }

  toggleFreeTrialMode(useFreeTrial: boolean): void {
    this.useFreeTrialMode.set(useFreeTrial);
    if (useFreeTrial) {
      this.teacherService.loadTeachersAcceptingFreeTrial().subscribe();
    } else {
      this.teacherService.loadTeachers().subscribe();
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
