import { Component, OnInit, PLATFORM_ID, signal, computed, inject, effect, untracked } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroMagnifyingGlass,
  heroPlay,
  heroFilm,
  heroCalendarDays,
  heroXMark,
  heroTrash
} from '@ng-icons/heroicons/outline';
import { heroPlaySolid } from '@ng-icons/heroicons/solid';
import { LibraryService, Video, LibraryFilters } from '../../core/services/library.service';
import { VideoPlayerComponent } from '../../shared/components/video-player/video-player.component';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { paginate } from '../../core/utils/pagination';
import { PaginationComponent } from '../../shared/components/pagination/pagination.component';

@Component({
    selector: 'app-library',
    imports: [FormsModule, NgIconComponent, VideoPlayerComponent, PageHeaderComponent, TranslateModule, PaginationComponent],
    viewProviders: [provideIcons({
            heroMagnifyingGlass,
            heroPlay,
            heroFilm,
            heroCalendarDays,
            heroXMark,
            heroTrash,
            heroPlaySolid
        })],
    templateUrl: './library.component.html',
    styleUrl: './library.component.scss'
})
export class LibraryComponent implements OnInit {
  private libraryService = inject(LibraryService);
  private translate = inject(TranslateService);
  private platformId = inject(PLATFORM_ID);
  private searchSubject = new Subject<string>();

  // Filters
  searchTerm = signal('');
  selectedPeriod = signal<'week' | 'month' | '3months' | 'year' | ''>('');
  dateFrom = signal('');
  dateTo = signal('');
  showCustomDates = signal(false);

  // Video player modal
  selectedVideo = signal<Video | null>(null);

  // Real video durations (probed from metadata)
  videoDurations = signal<Record<number, number>>({});

  // From service
  readonly videos = this.libraryService.videos;
  readonly loading = this.libraryService.loading;
  readonly error = this.libraryService.error;

  // Filtered videos based on search (client-side for instant feedback)
  readonly filteredVideos = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const allVideos = this.videos();
    if (!term) return allVideos;
    return allVideos.filter(v =>
      v.teacherName.toLowerCase().includes(term)
    );
  });

  pagination = paginate(this.filteredVideos, 10);

  // Period options (using translation keys)
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

    // Probe real video durations when videos change
    effect(() => {
      const videos = this.videos();
      if (isPlatformBrowser(this.platformId) && videos.length > 0) {
        untracked(() => this.probeVideoDurations(videos));
      }
    });

    // Debounced search - triggers API call
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe(term => {
      this.loadVideos();
    });
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
    if (period) {
      // Clear custom dates when using period
      this.dateFrom.set('');
      this.dateTo.set('');
      this.showCustomDates.set(false);
    }
    this.loadVideos();
  }

  toggleCustomDates(): void {
    this.showCustomDates.update(v => !v);
    if (this.showCustomDates()) {
      this.selectedPeriod.set('');
    }
  }

  onDateChange(): void {
    // Only reload if both dates are set or both are empty
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

  private loadVideos(): void {
    const filters: LibraryFilters = {};

    if (this.searchTerm()) {
      filters.search = this.searchTerm();
    }
    if (this.selectedPeriod()) {
      filters.period = this.selectedPeriod();
    }
    if (this.dateFrom()) {
      filters.dateFrom = this.dateFrom();
    }
    if (this.dateTo()) {
      filters.dateTo = this.dateTo();
    }

    this.libraryService.loadVideos(filters).subscribe();
  }

  playVideo(video: Video): void {
    this.selectedVideo.set(video);
  }

  closePlayer(): void {
    this.selectedVideo.set(null);
  }

  deleteVideo(video: Video, event: Event): void {
    event.stopPropagation();
    if (confirm(this.translate.instant('library.video.deleteConfirm'))) {
      this.libraryService.deleteVideo(video.lessonId).subscribe();
    }
  }

  getVideoDuration(video: Video): number | null {
    return this.videoDurations()[video.id] ?? null;
  }

  private probeVideoDurations(videos: Video[]): void {
    const known = this.videoDurations();
    for (const video of videos) {
      if (known[video.id] !== undefined) continue;
      if (!video.recordingUrl || video.recordingUrl.includes('mediadelivery.net')) continue;

      const el = document.createElement('video');
      el.preload = 'metadata';
      const videoId = video.id;
      el.onloadedmetadata = () => {
        if (el.duration && isFinite(el.duration)) {
          this.videoDurations.update(d => ({ ...d, [videoId]: Math.round(el.duration) }));
        }
        el.src = '';
      };
      el.onerror = () => { el.src = ''; };
      el.src = video.recordingUrl;
    }
  }

  formatDuration(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  formatRelativeDate(dateStr: string): string {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return this.translate.instant('relativeTime.today');
    if (diffDays === 1) return this.translate.instant('relativeTime.yesterday');
    if (diffDays < 7) return this.translate.instant('relativeTime.daysAgo', { count: diffDays });
    if (diffDays < 30) return this.translate.instant('relativeTime.weeksAgo', { count: Math.floor(diffDays / 7) });
    if (diffDays < 365) return this.translate.instant('relativeTime.monthsAgo', { count: Math.floor(diffDays / 30) });
    return this.translate.instant('relativeTime.yearsAgo', { count: Math.floor(diffDays / 365) });
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  }

  hasActiveFilters(): boolean {
    return !!(this.searchTerm() || this.selectedPeriod() || this.dateFrom() || this.dateTo());
  }
}
