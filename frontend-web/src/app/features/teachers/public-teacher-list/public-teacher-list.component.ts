import { Component, OnInit, signal, computed, ChangeDetectionStrategy, inject, effect, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { TeacherService } from '../../../core/services/teacher.service';
import { SeoService } from '../../../core/services/seo.service';
import { StructuredDataService } from '../../../core/services/structured-data.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroMagnifyingGlass, heroXMark, heroAcademicCap } from '@ng-icons/heroicons/outline';
import { LanguageSelectorComponent } from '../../../shared/components/language-selector/language-selector.component';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { paginate } from '../../../core/utils/pagination';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
    selector: 'app-public-teacher-list',
    imports: [RouterLink, NgIconComponent, FormsModule, TranslateModule, LanguageSelectorComponent, FooterComponent, PaginationComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [provideIcons({ heroMagnifyingGlass, heroXMark, heroAcademicCap })],
    templateUrl: './public-teacher-list.component.html',
    styleUrl: './public-teacher-list.component.scss'
})
export class PublicTeacherListComponent implements OnInit {
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);

  searchQuery = signal('');
  minRate = signal<number | null>(null);
  maxRate = signal<number | null>(null);

  filteredTeachers = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const min = this.minRate();
    const max = this.maxRate();

    return this.teacherService.teachers().filter(teacher => {
      const fullName = `${teacher.firstName} ${teacher.lastName}`.toLowerCase();
      const matchesName = !query || fullName.includes(query);
      const rate = teacher.hourlyRateCents ? teacher.hourlyRateCents / 100 : 0;
      const matchesMinRate = min === null || rate >= min;
      const matchesMaxRate = max === null || rate <= max;

      return matchesName && matchesMinRate && matchesMaxRate;
    }).sort((a, b) => a.firstName.localeCompare(b.firstName));
  });

  pagination = paginate(this.filteredTeachers, 10);

  constructor(public teacherService: TeacherService) {
    effect(() => {
      this.filteredTeachers();
      untracked(() => this.pagination.currentPage.set(0));
    });
  }

  ngOnInit(): void {
    this.seoService.setTeachersListPage();
    this.structuredDataService.setBreadcrumbSchema([
      { name: 'Accueil', url: 'https://mychess.fr/' },
      { name: 'Coachs', url: 'https://mychess.fr/coaches' }
    ]);
    this.teacherService.loadTeachers().subscribe();
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
}
