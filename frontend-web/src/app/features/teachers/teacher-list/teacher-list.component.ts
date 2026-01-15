import { Component, OnInit, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TeacherService } from '../../../core/services/teacher.service';
import { AuthService } from '../../../core/services/auth.service';
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
  searchQuery = signal('');
  minRate = signal<number | null>(null);
  maxRate = signal<number | null>(null);

  filteredTeachers = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const min = this.minRate();
    const max = this.maxRate();

    return this.teacherService.teachers().filter(teacher => {
      // Filter by name
      const fullName = `${teacher.firstName} ${teacher.lastName}`.toLowerCase();
      const matchesName = !query || fullName.includes(query);

      // Filter by rate
      const rate = teacher.hourlyRateCents ? teacher.hourlyRateCents / 100 : 0;
      const matchesMinRate = min === null || rate >= min;
      const matchesMaxRate = max === null || rate <= max;

      return matchesName && matchesMinRate && matchesMaxRate;
    });
  });

  constructor(
    public teacherService: TeacherService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.teacherService.loadTeachers().subscribe();
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
