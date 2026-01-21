import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, AdminLessonResponse } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin-lessons',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  template: `
    <div class="admin-lessons">
      <header class="page-header">
        <h1>Cours</h1>
        <div class="header-controls">
          <div class="search-box">
            <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <path d="m21 21-4.3-4.3"></path>
            </svg>
            <input
              type="text"
              [(ngModel)]="searchQuery"
              placeholder="Rechercher coach ou joueur..."
              class="search-input"
            >
            @if (searchQuery) {
              <button class="search-clear" (click)="searchQuery = ''">×</button>
            }
          </div>
          <div class="tabs">
            <button
              class="tab"
              [class.tab--active]="activeTab() === 'upcoming'"
              (click)="setTab('upcoming')"
            >
              A venir ({{ upcomingLessons().length }})
            </button>
            <button
              class="tab"
              [class.tab--active]="activeTab() === 'completed'"
              (click)="setTab('completed')"
            >
              Effectues ({{ completedLessons().length }})
            </button>
          </div>
        </div>
      </header>

      @if (loading()) {
        <div class="loading">Chargement...</div>
      } @else {
        <div class="table-container">
          <div class="table-header">
            <span class="results-count">{{ filteredLessons().length }} resultat(s)</span>
          </div>
          <table class="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Heure</th>
                <th>Coach</th>
                <th>Joueur</th>
                <th>Statut</th>
                <th>Prix</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (lesson of filteredLessons(); track lesson.id) {
                <tr>
                  <td>{{ lesson.scheduledAt | date:'dd/MM/yyyy' }}</td>
                  <td>{{ lesson.scheduledAt | date:'HH:mm' }}</td>
                  <td>{{ lesson.teacherName }}</td>
                  <td>
                    <div class="student-info">
                      <span>{{ lesson.studentName }}</span>
                      @if (lesson.studentLevel) {
                        <span class="badge badge--level">{{ lesson.studentLevel }}</span>
                      }
                    </div>
                  </td>
                  <td>
                    <span class="badge" [class]="getStatusClass(lesson.status)">
                      {{ getStatusLabel(lesson.status) }}
                    </span>
                  </td>
                  <td>
                    @if (lesson.priceCents) {
                      {{ formatPrice(lesson.priceCents) }}
                      @if (lesson.isFromSubscription) {
                        <span class="badge badge--subscription">Abo</span>
                      }
                    } @else {
                      -
                    }
                  </td>
                  <td>
                    @if (lesson.status === 'COMPLETED' && lesson.recordingUrl) {
                      <button class="btn-recording" (click)="openRecording(lesson)" title="Voir l'enregistrement">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <polygon points="23 7 16 12 23 17 23 7"></polygon>
                          <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                        </svg>
                      </button>
                    } @else {
                      -
                    }
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7" class="empty-state">
                    Aucun cours {{ activeTab() === 'upcoming' ? 'a venir' : 'effectue' }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        @if (activeTab() === 'completed' && completedLessons().length > 0) {
          <div class="summary-card">
            <h3>Resume des cours effectues</h3>
            <div class="summary-stats">
              <div class="stat">
                <span class="stat__value">{{ completedLessons().length }}</span>
                <span class="stat__label">Cours</span>
              </div>
              <div class="stat">
                <span class="stat__value">{{ formatPrice(totalRevenue()) }}</span>
                <span class="stat__label">CA Total</span>
              </div>
              <div class="stat">
                <span class="stat__value">{{ formatPrice(totalCommissions()) }}</span>
                <span class="stat__label">Commissions</span>
              </div>
            </div>
          </div>
        }
      }

      <!-- Video Player Modal -->
      @if (showVideoPlayer()) {
        <div class="video-player-overlay" (click)="closeVideoPlayer()">
          <div class="video-player-modal" (click)="$event.stopPropagation()">
            <div class="video-player-header">
              <h3>Enregistrement du cours</h3>
              <button class="video-player-close" (click)="closeVideoPlayer()">✕</button>
            </div>
            <div class="video-player-content">
              <video
                [src]="videoPlayerUrl()"
                controls
                autoplay
                class="video-player"
              >
                Votre navigateur ne supporte pas la lecture de vidéos.
              </video>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--space-lg);
      flex-wrap: wrap;
      gap: var(--space-md);

      h1 {
        font-size: 1.5rem;
        font-weight: 700;
      }

      @media (max-width: 767px) {
        h1 {
          font-size: 1.25rem;
        }
      }
    }

    .header-controls {
      display: flex;
      align-items: center;
      gap: var(--space-md);
      flex-wrap: wrap;

      @media (max-width: 767px) {
        width: 100%;
        flex-direction: column;
        align-items: stretch;
      }
    }

    .search-box {
      position: relative;
      display: flex;
      align-items: center;
    }

    .search-icon {
      position: absolute;
      left: 12px;
      color: var(--text-muted);
      pointer-events: none;
    }

    .search-input {
      padding: 8px 36px 8px 36px;
      font-size: 0.875rem;
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      background: var(--bg-tertiary);
      color: var(--text-primary);
      min-width: 240px;
      transition: all var(--transition-fast);

      &::placeholder {
        color: var(--text-muted);
      }

      &:focus {
        outline: none;
        border-color: var(--gold-500);
        background: var(--bg-secondary);
      }

      @media (max-width: 767px) {
        min-width: 100%;
      }
    }

    .search-clear {
      position: absolute;
      right: 8px;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg-tertiary);
      border: none;
      border-radius: 50%;
      color: var(--text-muted);
      cursor: pointer;
      font-size: 14px;
      line-height: 1;

      &:hover {
        background: var(--border-subtle);
        color: var(--text-primary);
      }
    }

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--space-sm) var(--space-md);
      background: var(--bg-tertiary);
      border-bottom: 1px solid var(--border-subtle);
    }

    .results-count {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .tabs {
      display: flex;
      gap: var(--space-xs);

      @media (max-width: 480px) {
        width: 100%;
      }
    }

    .tab {
      padding: 8px 16px;
      font-size: 0.875rem;
      font-weight: 500;
      color: var(--text-secondary);
      background: transparent;
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all var(--transition-fast);
      min-height: 40px;

      @media (max-width: 480px) {
        flex: 1;
        padding: 8px 12px;
        font-size: 0.8125rem;
      }

      &:hover {
        background: var(--bg-tertiary);
      }

      &--active {
        background: rgba(212, 168, 75, 0.1);
        border-color: var(--gold-500);
        color: var(--gold-400);
      }
    }

    .table-container {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;

      @media (max-width: 767px) {
        border-radius: var(--radius-md);
      }
    }

    .table {
      width: 100%;
      border-collapse: collapse;
      min-width: 650px;

      th, td {
        padding: var(--space-md);
        text-align: left;
        border-bottom: 1px solid var(--border-subtle);
        white-space: nowrap;

        @media (max-width: 767px) {
          padding: var(--space-sm);
          font-size: 0.8125rem;
        }
      }

      th {
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--text-muted);
        background: var(--bg-tertiary);

        @media (max-width: 767px) {
          font-size: 0.6875rem;
        }
      }

      tbody tr:hover {
        background: var(--bg-tertiary);
      }

      tbody tr:last-child td {
        border-bottom: none;
      }
    }

    .student-info {
      display: flex;
      align-items: center;
      gap: var(--space-sm);
    }

    .badge {
      display: inline-block;
      padding: 4px 8px;
      font-size: 0.6875rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      border-radius: var(--radius-sm);

      &--pending {
        background: rgba(251, 191, 36, 0.1);
        color: #fbbf24;
      }

      &--confirmed {
        background: rgba(59, 130, 246, 0.1);
        color: #3b82f6;
      }

      &--completed {
        background: var(--success-muted);
        color: var(--success);
      }

      &--cancelled {
        background: var(--error-muted);
        color: var(--error);
      }

      &--level {
        background: rgba(139, 92, 246, 0.1);
        color: #8b5cf6;
      }

      &--subscription {
        background: rgba(212, 168, 75, 0.1);
        color: var(--gold-500);
        margin-left: var(--space-xs);
      }
    }

    .empty-state {
      text-align: center;
      color: var(--text-muted);
      padding: var(--space-2xl) !important;
    }

    .loading {
      text-align: center;
      padding: var(--space-2xl);
      color: var(--text-muted);
    }

    .btn-recording {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: rgba(34, 197, 94, 0.1);
      border: none;
      border-radius: var(--radius-md);
      color: #22c55e;
      cursor: pointer;
      transition: all var(--transition-fast);
      min-height: 36px;

      &:hover {
        background: rgba(34, 197, 94, 0.2);
      }
    }

    .video-player-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.9);
      z-index: 1100;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;

      @media (max-width: 767px) {
        padding: 0.5rem;
      }
    }

    .video-player-modal {
      width: 100%;
      max-width: 1000px;
      background: var(--bg-secondary);
      border-radius: var(--radius-xl);
      border: 1px solid var(--border-subtle);
      overflow: hidden;

      @media (max-width: 767px) {
        border-radius: var(--radius-md);
      }
    }

    .video-player-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.5rem;
      background: var(--bg-tertiary);
      border-bottom: 1px solid var(--border-subtle);

      @media (max-width: 767px) {
        padding: 0.75rem 1rem;
      }

      h3 {
        font-size: 1.125rem;
        font-weight: 600;
        color: var(--text-primary);

        @media (max-width: 767px) {
          font-size: 1rem;
        }
      }
    }

    .video-player-close {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: transparent;
      border: none;
      border-radius: var(--radius-md);
      color: var(--text-secondary);
      cursor: pointer;
      font-size: 1.25rem;
      transition: all var(--transition-fast);

      &:hover {
        background: var(--bg-secondary);
        color: var(--text-primary);
      }
    }

    .video-player-content {
      background: #000;
    }

    .video-player {
      width: 100%;
      max-height: 70vh;
      display: block;
    }

    .summary-card {
      margin-top: var(--space-lg);
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      padding: var(--space-lg);

      @media (max-width: 767px) {
        padding: var(--space-md);
        border-radius: var(--radius-md);
      }

      h3 {
        font-size: 1rem;
        font-weight: 600;
        margin-bottom: var(--space-md);
        color: var(--text-primary);
      }
    }

    .summary-stats {
      display: flex;
      gap: var(--space-xl);

      @media (max-width: 480px) {
        flex-wrap: wrap;
        gap: var(--space-md);
      }
    }

    .stat {
      display: flex;
      flex-direction: column;
      gap: 4px;

      @media (max-width: 480px) {
        flex: 1 1 45%;
      }

      &__value {
        font-size: 1.25rem;
        font-weight: 700;
        color: var(--gold-400);

        @media (max-width: 767px) {
          font-size: 1.125rem;
        }
      }

      &__label {
        font-size: 0.75rem;
        color: var(--text-muted);
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
    }
  `]
})
export class AdminLessonsComponent implements OnInit {
  upcomingLessons = signal<AdminLessonResponse[]>([]);
  completedLessons = signal<AdminLessonResponse[]>([]);
  loading = signal(true);
  activeTab = signal<'upcoming' | 'completed'>('upcoming');
  searchQuery = '';

  // Video player
  showVideoPlayer = signal(false);
  videoPlayerUrl = signal('');

  // Filtered lessons based on search query and active tab
  filteredLessons = computed(() => {
    const lessons = this.activeTab() === 'upcoming' ? this.upcomingLessons() : this.completedLessons();
    const query = this.searchQuery.toLowerCase().trim();

    if (!query) return lessons;

    return lessons.filter(lesson => {
      const teacherName = lesson.teacherName?.toLowerCase() || '';
      const studentName = lesson.studentName?.toLowerCase() || '';
      return teacherName.includes(query) || studentName.includes(query);
    });
  });

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadLessons();
  }

  loadLessons(): void {
    this.loading.set(true);

    // Load both upcoming and completed lessons
    this.adminService.getUpcomingLessons().subscribe({
      next: (lessons) => this.upcomingLessons.set(lessons),
      error: () => this.upcomingLessons.set([])
    });

    this.adminService.getCompletedLessons().subscribe({
      next: (lessons) => {
        this.completedLessons.set(lessons);
        this.loading.set(false);
      },
      error: () => {
        this.completedLessons.set([]);
        this.loading.set(false);
      }
    });
  }

  setTab(tab: 'upcoming' | 'completed'): void {
    this.activeTab.set(tab);
  }

  currentLessons(): AdminLessonResponse[] {
    return this.activeTab() === 'upcoming' ? this.upcomingLessons() : this.completedLessons();
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      PENDING: 'badge--pending',
      CONFIRMED: 'badge--confirmed',
      COMPLETED: 'badge--completed',
      CANCELLED: 'badge--cancelled'
    };
    return classes[status] || '';
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente',
      CONFIRMED: 'Confirme',
      COMPLETED: 'Termine',
      CANCELLED: 'Annule'
    };
    return labels[status] || status;
  }

  formatPrice(cents: number): string {
    return (cents / 100).toFixed(2) + ' €';
  }

  totalRevenue(): number {
    return this.completedLessons().reduce((sum, l) => sum + (l.priceCents || 0), 0);
  }

  totalCommissions(): number {
    return this.completedLessons().reduce((sum, l) => sum + (l.commissionCents || 0), 0);
  }

  openRecording(lesson: AdminLessonResponse): void {
    if (lesson.recordingUrl) {
      this.videoPlayerUrl.set(lesson.recordingUrl);
      this.showVideoPlayer.set(true);
    }
  }

  closeVideoPlayer(): void {
    this.showVideoPlayer.set(false);
    this.videoPlayerUrl.set('');
  }
}
