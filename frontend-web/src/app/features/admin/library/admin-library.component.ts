import { Component, OnInit, signal, computed, inject, effect, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroMagnifyingGlass,
  heroPlay,
  heroFilm,
  heroCalendarDays,
  heroXMark
} from '@ng-icons/heroicons/outline';
import { heroPlaySolid } from '@ng-icons/heroicons/solid';
import { VideoPlayerComponent } from '../../../shared/components/video-player/video-player.component';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { paginate } from '../../../core/utils/pagination';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

interface AdminVideo {
  id: number;
  lessonId: number;
  teacherName: string;
  teacherAvatar: string | null;
  studentName: string | null;
  scheduledAt: string;
  recordingUrl: string;
  durationSeconds: number;
  thumbnailUrl: string;
  courseTitle?: string;
}

@Component({
  selector: 'app-admin-library',
  imports: [FormsModule, NgIconComponent, VideoPlayerComponent, TranslateModule, PaginationComponent],
  viewProviders: [provideIcons({
    heroMagnifyingGlass, heroPlay, heroFilm, heroCalendarDays, heroXMark, heroPlaySolid
  })],
  template: `
    <div class="admin-library">
      <div class="page-header">
        <h1>{{ 'adminLibrary.title' | translate }}</h1>
        <p class="subtitle">{{ 'adminLibrary.subtitle' | translate }}</p>
      </div>

      <!-- Stats -->
      @if (!loading() && videos().length > 0) {
        <div class="stats-row">
          <div class="stat-card">
            <span class="stat-value">{{ videos().length }}</span>
            <span class="stat-label">{{ 'adminLibrary.totalVideos' | translate }}</span>
          </div>
          <div class="stat-card">
            <span class="stat-value">{{ totalDuration() }}</span>
            <span class="stat-label">{{ 'adminLibrary.totalDuration' | translate }}</span>
          </div>
        </div>
      }

      <!-- Filters -->
      <div class="filters-section">
        <div class="search-bar">
          <ng-icon name="heroMagnifyingGlass" class="search-icon"></ng-icon>
          <input
            type="text"
            [placeholder]="'adminLibrary.searchPlaceholder' | translate"
            [ngModel]="searchTerm()"
            (ngModelChange)="onSearchChange($event)"
            class="search-input"
          />
          @if (searchTerm()) {
            <button class="clear-search" (click)="onSearchChange('')">
              <ng-icon name="heroXMark"></ng-icon>
            </button>
          }
        </div>

        <div class="filter-row">
          <select
            [ngModel]="selectedPeriod()"
            (ngModelChange)="onPeriodChange($event)"
            class="period-select"
          >
            @for (option of periodOptions; track option.value) {
              <option [value]="option.value">{{ option.labelKey | translate }}</option>
            }
          </select>

          <button
            class="custom-dates-toggle"
            [class.active]="showCustomDates()"
            (click)="toggleCustomDates()"
          >
            <ng-icon name="heroCalendarDays"></ng-icon>
            {{ 'adminLibrary.customDates' | translate }}
          </button>

          @if (hasActiveFilters()) {
            <button class="clear-filters" (click)="clearFilters()">
              <ng-icon name="heroXMark"></ng-icon>
              {{ 'adminLibrary.clearFilters' | translate }}
            </button>
          }
        </div>

        @if (showCustomDates()) {
          <div class="date-range">
            <div class="date-field">
              <label>{{ 'adminLibrary.dateFrom' | translate }}</label>
              <input type="date" [ngModel]="dateFrom()" (ngModelChange)="dateFrom.set($event); onDateChange()" class="date-input" />
            </div>
            <div class="date-field">
              <label>{{ 'adminLibrary.dateTo' | translate }}</label>
              <input type="date" [ngModel]="dateTo()" (ngModelChange)="dateTo.set($event); onDateChange()" class="date-input" />
            </div>
          </div>
        }
      </div>

      <!-- Loading -->
      @if (loading()) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>{{ 'adminLibrary.loading' | translate }}</p>
        </div>
      }

      <!-- Empty -->
      @if (!loading() && filteredVideos().length === 0) {
        <div class="empty-state">
          <ng-icon name="heroFilm" class="empty-icon"></ng-icon>
          @if (hasActiveFilters()) {
            <h3>{{ 'adminLibrary.noResults' | translate }}</h3>
            <button class="clear-filters-btn" (click)="clearFilters()">{{ 'adminLibrary.clearFilters' | translate }}</button>
          } @else {
            <h3>{{ 'adminLibrary.empty' | translate }}</h3>
          }
        </div>
      }

      <!-- Video Grid -->
      @if (!loading() && filteredVideos().length > 0) {
        <div class="videos-grid">
          @for (video of pagination.paginatedItems(); track video.id) {
            <div class="video-card" (click)="playVideo(video)">
              <div class="thumbnail-container">
                <img
                  [src]="video.thumbnailUrl || '/assets/images/video-thumbnail-placeholder.svg'"
                  [alt]="video.teacherName"
                  class="thumbnail"
                  loading="lazy"
                />
                <div class="play-overlay">
                  <ng-icon name="heroPlaySolid" class="play-icon"></ng-icon>
                </div>
                <div class="duration-badge">{{ formatDuration(video.durationSeconds) }}</div>
              </div>
              <div class="video-info">
                @if (video.courseTitle) {
                  <div class="course-title">{{ video.courseTitle }}</div>
                }
                <div class="participants-row">
                  @if (video.teacherAvatar) {
                    <img [src]="video.teacherAvatar" [alt]="video.teacherName" class="avatar" />
                  } @else {
                    <div class="avatar-placeholder">{{ video.teacherName.charAt(0) }}</div>
                  }
                  <div class="participants-names">
                    <span class="teacher-name">{{ video.teacherName }}</span>
                    @if (video.studentName) {
                      <span class="student-name">{{ video.studentName }}</span>
                    }
                  </div>
                </div>
                <div class="date-row">
                  <span class="relative-date">{{ formatRelativeDate(video.scheduledAt) }}</span>
                  <span class="absolute-date">{{ formatDate(video.scheduledAt) }}</span>
                </div>
              </div>
            </div>
          }
        </div>
        <app-pagination
          [currentPage]="pagination.currentPage()"
          [totalPages]="pagination.totalPages()"
          [totalItems]="pagination.totalItems()"
          [startItem]="pagination.startItem()"
          [endItem]="pagination.endItem()"
          [visiblePages]="pagination.visiblePages()"
          (pageChange)="pagination.goToPage($event)"
        />
      }
    </div>

    <!-- Video Player Modal -->
    @if (selectedVideo()) {
      <div class="video-modal-overlay" (click)="closePlayer()">
        <div class="video-modal" (click)="$event.stopPropagation()">
          <app-video-player
            [url]="selectedVideo()!.recordingUrl"
            [videoId]="selectedVideo()!.lessonId"
            [title]="selectedVideo()!.courseTitle || selectedVideo()!.teacherName + ' & ' + (selectedVideo()!.studentName || 'Groupe')"
            [teacherName]="selectedVideo()!.teacherName"
            [lessonDate]="formatDate(selectedVideo()!.scheduledAt)"
            (close)="closePlayer()"
          ></app-video-player>
        </div>
      </div>
    }
  `,
  styles: [`
    .admin-library {
      padding: var(--space-lg);
      max-width: 1400px;
      margin: 0 auto;
    }

    .page-header {
      margin-bottom: var(--space-xl);
      h1 { font-size: 1.5rem; font-weight: 700; color: var(--text-primary); margin: 0 0 var(--space-xs); }
      .subtitle { color: var(--text-tertiary); margin: 0; font-size: 0.9375rem; }
    }

    .stats-row {
      display: flex; gap: var(--space-md); margin-bottom: var(--space-xl);
    }
    .stat-card {
      background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: var(--radius-lg);
      padding: var(--space-md) var(--space-lg); display: flex; flex-direction: column; gap: var(--space-xs);
    }
    .stat-value { font-size: 1.5rem; font-weight: 700; color: var(--accent-color); }
    .stat-label { font-size: 0.8125rem; color: var(--text-tertiary); }

    .filters-section { margin-bottom: var(--space-xl); display: flex; flex-direction: column; gap: var(--space-md); }
    .search-bar {
      position: relative; max-width: 400px;
      .search-icon { position: absolute; left: var(--space-md); top: 50%; transform: translateY(-50%); color: var(--text-tertiary); font-size: 1.25rem; }
      .search-input {
        width: 100%; padding: var(--space-sm) var(--space-md); padding-left: 2.75rem; padding-right: 2.5rem;
        background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: var(--radius-md);
        color: var(--text-primary); font-size: 0.9375rem;
        &::placeholder { color: var(--text-tertiary); }
        &:focus { outline: none; border-color: var(--accent-color); box-shadow: 0 0 0 3px rgba(var(--accent-rgb), 0.1); }
      }
      .clear-search {
        position: absolute; right: var(--space-sm); top: 50%; transform: translateY(-50%);
        background: none; border: none; color: var(--text-tertiary); cursor: pointer; padding: var(--space-xs);
        border-radius: var(--radius-sm); display: flex; align-items: center;
        &:hover { color: var(--text-primary); background: var(--bg-tertiary); }
      }
    }

    .filter-row { display: flex; align-items: center; gap: var(--space-md); flex-wrap: wrap; }
    .period-select {
      padding: var(--space-sm) var(--space-md); background: var(--bg-secondary); border: 1px solid var(--border-color);
      border-radius: var(--radius-md); color: var(--text-primary); font-size: 0.875rem; cursor: pointer; min-width: 160px;
      &:focus { outline: none; border-color: var(--accent-color); }
    }
    .custom-dates-toggle {
      display: flex; align-items: center; gap: var(--space-xs); padding: var(--space-sm) var(--space-md);
      background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: var(--radius-md);
      color: var(--text-secondary); font-size: 0.875rem; cursor: pointer; transition: all 0.2s;
      &:hover { border-color: var(--accent-color); color: var(--text-primary); }
      &.active { background: var(--accent-color); border-color: var(--accent-color); color: var(--bg-primary); }
    }
    .clear-filters {
      display: flex; align-items: center; gap: var(--space-xs); padding: var(--space-sm) var(--space-md);
      background: transparent; border: none; color: var(--text-tertiary); font-size: 0.875rem; cursor: pointer;
      &:hover { color: var(--danger-color); }
    }
    .date-range {
      display: flex; gap: var(--space-md); padding: var(--space-md); background: var(--bg-secondary);
      border-radius: var(--radius-md); max-width: 400px;
    }
    .date-field {
      display: flex; flex-direction: column; gap: var(--space-xs); flex: 1;
      label { font-size: 0.75rem; color: var(--text-tertiary); text-transform: uppercase; letter-spacing: 0.05em; }
      .date-input {
        padding: var(--space-sm); background: var(--bg-primary); border: 1px solid var(--border-color);
        border-radius: var(--radius-sm); color: var(--text-primary); font-size: 0.875rem;
        &:focus { outline: none; border-color: var(--accent-color); }
      }
    }

    .loading-state {
      display: flex; flex-direction: column; align-items: center; padding: var(--space-3xl); color: var(--text-tertiary);
      .spinner { width: 40px; height: 40px; border: 3px solid var(--border-color); border-top-color: var(--accent-color); border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: var(--space-md); }
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .empty-state {
      display: flex; flex-direction: column; align-items: center; padding: var(--space-3xl); text-align: center;
      .empty-icon { font-size: 4rem; color: var(--text-tertiary); margin-bottom: var(--space-lg); }
      h3 { font-size: 1.25rem; font-weight: 600; color: var(--text-primary); margin: 0 0 var(--space-sm); }
      .clear-filters-btn {
        margin-top: var(--space-lg); padding: var(--space-sm) var(--space-lg); background: var(--bg-secondary);
        border: 1px solid var(--border-color); border-radius: var(--radius-md); color: var(--text-primary);
        font-weight: 500; cursor: pointer; &:hover { border-color: var(--accent-color); }
      }
    }

    .videos-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: var(--space-lg); }
    .video-card {
      background: var(--bg-secondary); border-radius: var(--radius-lg); overflow: hidden; cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
      &:hover { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0, 0, 0, 0.3);
        .play-overlay { opacity: 1; }
      }
    }
    .thumbnail-container {
      position: relative; aspect-ratio: 16/9; background: var(--bg-tertiary);
      .thumbnail { width: 100%; height: 100%; object-fit: cover; }
      .play-overlay {
        position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
        background: rgba(0, 0, 0, 0.4); opacity: 0; transition: opacity 0.2s;
        .play-icon { font-size: 3.5rem; color: white; filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3)); }
      }
      .duration-badge {
        position: absolute; bottom: var(--space-sm); right: var(--space-sm); padding: var(--space-xs) var(--space-sm);
        background: rgba(0, 0, 0, 0.8); border-radius: var(--radius-sm); color: white; font-size: 0.75rem;
        font-weight: 500; font-variant-numeric: tabular-nums;
      }
    }

    .video-info { padding: var(--space-md); }
    .course-title {
      font-size: 0.9375rem; font-weight: 600; color: var(--text-primary); margin-bottom: var(--space-sm);
      line-height: 1.3; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
    }
    .participants-row {
      display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-sm);
      .avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
      .avatar-placeholder {
        width: 32px; height: 32px; border-radius: 50%; background: var(--accent-color); color: var(--bg-primary);
        display: flex; align-items: center; justify-content: center; font-size: 0.875rem; font-weight: 600;
      }
    }
    .participants-names {
      display: flex; flex-direction: column;
      .teacher-name { font-weight: 500; color: var(--text-primary); font-size: 0.875rem; }
      .student-name { color: var(--text-tertiary); font-size: 0.8125rem; }
    }
    .date-row {
      display: flex; align-items: center; gap: var(--space-sm);
      .relative-date { color: var(--text-secondary); font-size: 0.8125rem; }
      .absolute-date { color: var(--text-tertiary); font-size: 0.75rem; &::before { content: '·'; margin-right: var(--space-sm); } }
    }

    .video-modal-overlay {
      position: fixed; inset: 0; background: rgba(0, 0, 0, 0.9); display: flex; align-items: center;
      justify-content: center; z-index: var(--z-overlay); padding: var(--space-lg);
    }
    .video-modal { width: 100%; max-width: 1200px; max-height: 90vh; border-radius: var(--radius-lg); overflow: hidden; }

    @media (max-width: 768px) {
      .admin-library { padding: var(--space-md); }
      .stats-row { flex-direction: column; }
      .filter-row { flex-direction: column; align-items: stretch; }
      .period-select, .custom-dates-toggle { width: 100%; justify-content: center; }
      .date-range { flex-direction: column; max-width: 100%; }
      .videos-grid { grid-template-columns: 1fr; }
      .video-modal-overlay { padding: 0; }
      .video-modal { max-width: 100%; max-height: 100vh; border-radius: 0; }
    }
  `]
})
export class AdminLibraryComponent implements OnInit {
  private http = inject(HttpClient);
  private translate = inject(TranslateService);
  private searchSubject = new Subject<string>();

  searchTerm = signal('');
  selectedPeriod = signal<'week' | 'month' | '3months' | 'year' | ''>('');
  dateFrom = signal('');
  dateTo = signal('');
  showCustomDates = signal(false);
  selectedVideo = signal<AdminVideo | null>(null);

  videos = signal<AdminVideo[]>([]);
  loading = signal(false);

  filteredVideos = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const all = this.videos();
    if (!term) return all;
    return all.filter(v =>
      v.teacherName.toLowerCase().includes(term) ||
      (v.studentName && v.studentName.toLowerCase().includes(term))
    );
  });

  pagination = paginate(this.filteredVideos, 12);

  totalDuration = computed(() => {
    const totalSec = this.videos().reduce((sum, v) => sum + v.durationSeconds, 0);
    const hours = Math.floor(totalSec / 3600);
    const mins = Math.floor((totalSec % 3600) / 60);
    return hours > 0 ? `${hours}h ${mins}min` : `${mins}min`;
  });

  periodOptions = [
    { value: '', labelKey: 'library.periods.all' },
    { value: 'week', labelKey: 'library.periods.week' },
    { value: 'month', labelKey: 'library.periods.month' },
    { value: '3months', labelKey: 'library.periods.3months' },
    { value: 'year', labelKey: 'library.periods.year' }
  ];

  constructor() {
    effect(() => {
      this.filteredVideos();
      untracked(() => this.pagination.currentPage.set(0));
    });

    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe(() => this.loadVideos());
  }

  ngOnInit(): void {
    this.loadVideos();
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.searchSubject.next(value);
  }

  onPeriodChange(period: string): void {
    this.selectedPeriod.set(period as any);
    if (period) { this.dateFrom.set(''); this.dateTo.set(''); this.showCustomDates.set(false); }
    this.loadVideos();
  }

  toggleCustomDates(): void {
    this.showCustomDates.update(v => !v);
    if (this.showCustomDates()) this.selectedPeriod.set('');
  }

  onDateChange(): void {
    if ((this.dateFrom() && this.dateTo()) || (!this.dateFrom() && !this.dateTo())) {
      this.loadVideos();
    }
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedPeriod.set('');
    this.dateFrom.set('');
    this.dateTo.set('');
    this.showCustomDates.set(false);
    this.loadVideos();
  }

  hasActiveFilters(): boolean {
    return !!(this.searchTerm() || this.selectedPeriod() || this.dateFrom() || this.dateTo());
  }

  private loadVideos(): void {
    this.loading.set(true);
    let params = new HttpParams();
    if (this.searchTerm()) params = params.set('search', this.searchTerm());
    if (this.selectedPeriod()) params = params.set('period', this.selectedPeriod());
    if (this.dateFrom()) params = params.set('dateFrom', this.dateFrom());
    if (this.dateTo()) params = params.set('dateTo', this.dateTo());

    this.http.get<AdminVideo[]>('/api/admin/library/videos', { params }).subscribe({
      next: videos => { this.videos.set(videos); this.loading.set(false); },
      error: () => { this.loading.set(false); }
    });
  }

  playVideo(video: AdminVideo): void { this.selectedVideo.set(video); }
  closePlayer(): void { this.selectedVideo.set(null); }

  formatDuration(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  formatRelativeDate(dateStr: string): string {
    const diffDays = Math.floor((Date.now() - new Date(dateStr).getTime()) / 86400000);
    if (diffDays === 0) return this.translate.instant('relativeTime.today');
    if (diffDays === 1) return this.translate.instant('relativeTime.yesterday');
    if (diffDays < 7) return this.translate.instant('relativeTime.daysAgo', { count: diffDays });
    if (diffDays < 30) return this.translate.instant('relativeTime.weeksAgo', { count: Math.floor(diffDays / 7) });
    if (diffDays < 365) return this.translate.instant('relativeTime.monthsAgo', { count: Math.floor(diffDays / 30) });
    return this.translate.instant('relativeTime.yearsAgo', { count: Math.floor(diffDays / 365) });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }
}
