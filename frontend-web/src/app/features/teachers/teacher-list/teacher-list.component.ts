import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject, effect, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TeacherService } from '../../../core/services/teacher.service';
import { AuthService } from '../../../core/services/auth.service';
import { FavoriteService } from '../../../core/services/favorite.service';
import { SeoService } from '../../../core/services/seo.service';
import { AppSidebarComponent, SidebarSection } from '../../../shared/components/app-sidebar/app-sidebar.component';
import { paginate } from '../../../core/utils/pagination';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
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
  heroBookOpen,
  heroWallet
} from '@ng-icons/heroicons/outline';
import { heroStarSolid } from '@ng-icons/heroicons/solid';

@Component({
    selector: 'app-teacher-list',
    imports: [RouterLink, NgIconComponent, FormsModule, AppSidebarComponent, TranslateModule, PaginationComponent],
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
            heroBookOpen,
            heroWallet,
            heroStarSolid
        })],
    templateUrl: './teacher-list.component.html',
    styleUrl: './teacher-list.component.scss'
})
export class TeacherListComponent implements OnInit {
  private seoService = inject(SeoService);
  private translate = inject(TranslateService);

  sidebarCollapsed = signal(false);

  sidebarSections = computed<SidebarSection[]>(() => {
    const menuItems: any[] = [
      { label: this.translate.instant('sidebar.dashboard'), icon: 'heroChartBarSquare', route: '/dashboard' },
      { label: this.translate.instant('sidebar.myLessons'), icon: 'heroCalendarDays', route: '/lessons' }
    ];

    if (this.authService.isTeacher()) {
      menuItems.push({ label: this.translate.instant('sidebar.myAvailability'), icon: 'heroClipboardDocumentList', route: '/availability' });
    }

    if (this.authService.isStudent()) {
      menuItems.push({ label: this.translate.instant('sidebar.myProgress'), icon: 'heroBookOpen', route: '/programme' });
      menuItems.push({ label: this.translate.instant('sidebar.findCoach'), icon: 'heroAcademicCap', route: '/teachers', active: true });
    }

    const compteItems: any[] = [
      { label: this.translate.instant('sidebar.settings'), icon: 'heroUserCircle', route: '/settings' }
    ];

    if (this.authService.isStudent()) {
      compteItems.push({ label: this.translate.instant('sidebar.wallet'), icon: 'heroWallet', route: '/wallet' });
      compteItems.push({ label: this.translate.instant('sidebar.subscription'), icon: 'heroCreditCard', route: '/subscription' });
    }

    compteItems.push({ label: this.translate.instant('sidebar.invoices'), icon: 'heroDocumentText', route: '/invoices' });
    compteItems.push({ label: this.translate.instant('sidebar.logout'), icon: 'heroArrowRightOnRectangle', action: () => this.logout() });

    return [
      { title: this.translate.instant('sidebar.menu'), items: menuItems },
      { title: this.translate.instant('sidebar.account'), items: compteItems }
    ];
  });

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }

  searchQuery = signal('');
  minRate = signal<number | null>(null);
  maxRate = signal<number | null>(null);

  // Lesson type filter
  lessonTypeFilter = signal<'INDIVIDUAL' | 'GROUP'>('INDIVIDUAL');

  // Free trial mode: true = show only coaches accepting free trial, false = show all
  useFreeTrialMode = signal(true);

  // Favorite teachers (separate list)
  favoriteTeachers = computed(() => {
    const favoriteIds = this.favoriteService.favoriteTeacherIds();
    const query = this.searchQuery().toLowerCase().trim();
    const min = this.minRate();
    const max = this.maxRate();

    return this.teacherService.teachers().filter(teacher => {
      if (!favoriteIds.has(teacher.id)) return false;

      // Apply same filters
      const fullName = `${teacher.firstName} ${teacher.lastName}`.toLowerCase();
      const matchesName = !query || fullName.includes(query);
      const rate = teacher.hourlyRateCents ? teacher.hourlyRateCents / 100 : 0;
      const matchesMinRate = min === null || rate >= min;
      const matchesMaxRate = max === null || rate <= max;

      return matchesName && matchesMinRate && matchesMaxRate;
    });
  });

  // Non-favorite teachers (all others)
  filteredTeachers = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const min = this.minRate();
    const max = this.maxRate();
    const favoriteIds = this.favoriteService.favoriteTeacherIds();

    return this.teacherService.teachers().filter(teacher => {
      // Exclude favorites (they're shown separately)
      if (favoriteIds.has(teacher.id)) return false;

      // Filter by name
      const fullName = `${teacher.firstName} ${teacher.lastName}`.toLowerCase();
      const matchesName = !query || fullName.includes(query);

      // Filter by rate
      const rate = teacher.hourlyRateCents ? teacher.hourlyRateCents / 100 : 0;
      const matchesMinRate = min === null || rate >= min;
      const matchesMaxRate = max === null || rate <= max;

      return matchesName && matchesMinRate && matchesMaxRate;
    }).sort((a, b) => a.firstName.localeCompare(b.firstName));
  });

  // Total count for display
  totalFilteredCount = computed(() => {
    return this.favoriteTeachers().length + this.filteredTeachers().length;
  });

  favoritePagination = paginate(this.favoriteTeachers, 10);
  teacherPagination = paginate(this.filteredTeachers, 10);

  constructor(
    public teacherService: TeacherService,
    public authService: AuthService,
    public favoriteService: FavoriteService
  ) {
    effect(() => {
      this.favoriteTeachers();
      untracked(() => this.favoritePagination.currentPage.set(0));
    });
    effect(() => {
      this.filteredTeachers();
      untracked(() => this.teacherPagination.currentPage.set(0));
    });
  }

  ngOnInit(): void {
    this.seoService.setTeachersListPage();

    // Load teachers filtered by lesson type
    this.teacherService.loadTeachers(this.lessonTypeFilter()).subscribe();

    // Load favorites if user is a student
    if (this.authService.isStudent()) {
      this.favoriteService.loadFavorites().subscribe();
    }
  }

  setLessonType(type: 'INDIVIDUAL' | 'GROUP'): void {
    this.lessonTypeFilter.set(type);
    this.teacherService.loadTeachers(type).subscribe();
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
