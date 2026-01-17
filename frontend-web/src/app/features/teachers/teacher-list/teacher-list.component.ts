import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TeacherService } from '../../../core/services/teacher.service';
import { AuthService } from '../../../core/services/auth.service';
import { FavoriteService } from '../../../core/services/favorite.service';
import { SeoService } from '../../../core/services/seo.service';
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
  imports: [RouterLink, NgIconComponent, FormsModule],
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

  searchQuery = signal('');
  minRate = signal<number | null>(null);
  maxRate = signal<number | null>(null);

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

  constructor(
    public teacherService: TeacherService,
    public authService: AuthService,
    public favoriteService: FavoriteService
  ) {}

  ngOnInit(): void {
    this.seoService.setTeachersListPage();
    this.teacherService.loadTeachers().subscribe();
    if (this.authService.isStudent()) {
      this.favoriteService.loadFavorites().subscribe();
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

  clearFilters(): void {
    this.searchQuery.set('');
    this.minRate.set(null);
    this.maxRate.set(null);
  }

  hasActiveFilters(): boolean {
    return this.searchQuery() !== '' || this.minRate() !== null || this.maxRate() !== null;
  }

  logout(): void {
    this.authService.logout();
  }
}
