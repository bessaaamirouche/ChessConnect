import { Component, OnInit, signal, computed, effect, untracked, ChangeDetectionStrategy, inject } from '@angular/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroMagnifyingGlass,
  heroFunnel,
  heroArrowPath,
  heroExclamationTriangle,
  heroUserCircle,
  heroAcademicCap,
  heroChatBubbleLeftRight,
  heroXMark
} from '@ng-icons/heroicons/outline';
import { paginate } from '../../../core/utils/pagination';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

interface Message {
  id: number;
  type: string;
  from: string;
  fromId: number;
  fromRole: string;
  to: string;
  toId: number;
  toRole: string;
  content: string;
  date: string;
  lessonDate: string;
  lessonId: number;
}

interface FilterOption {
  id: number;
  name: string;
}

@Component({
    selector: 'app-admin-messages',
    imports: [FormsModule, NgIconComponent, TranslateModule, PaginationComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [provideIcons({
            heroMagnifyingGlass,
            heroFunnel,
            heroArrowPath,
            heroExclamationTriangle,
            heroUserCircle,
            heroAcademicCap,
            heroChatBubbleLeftRight,
            heroXMark
        })],
    template: `
    <div class="messages-page">
      <div class="page-header">
        <div class="page-title">
          <ng-icon name="heroChatBubbleLeftRight" size="24"></ng-icon>
          <h1>{{ 'admin.messages.title' | translate }}</h1>
        </div>
        <p class="page-subtitle">{{ 'admin.messages.subtitle' | translate }}</p>
      </div>

      <!-- Search Bar -->
      <div class="search-bar">
        <ng-icon name="heroMagnifyingGlass" size="18" class="search-bar__icon"></ng-icon>
        <input
          type="text"
          [(ngModel)]="searchQuery"
          [placeholder]="'admin.messages.searchPlaceholder' | translate"
          class="search-bar__input"
        >
        @if (searchQuery) {
          <button class="search-bar__clear" (click)="searchQuery = ''">
            <ng-icon name="heroXMark" size="16"></ng-icon>
          </button>
        }
      </div>

      <!-- Filters -->
      <div class="filters-card">
        <div class="filters-row">
          <div class="filter-group">
            <label>{{ 'admin.messages.dateFrom' | translate }}</label>
            <input type="date" [(ngModel)]="dateFrom" class="filter-input">
          </div>
          <div class="filter-group">
            <label>{{ 'admin.messages.dateTo' | translate }}</label>
            <input type="date" [(ngModel)]="dateTo" class="filter-input">
          </div>
          <div class="filter-group">
            <label>{{ 'admin.messages.coach' | translate }}</label>
            <select [(ngModel)]="selectedTeacherId" class="filter-input">
              <option [ngValue]="null">{{ 'admin.messages.all' | translate }}</option>
              @for (teacher of teachers(); track teacher.id) {
                <option [ngValue]="teacher.id">{{ teacher.name }}</option>
              }
            </select>
          </div>
          <div class="filter-group">
            <label>{{ 'admin.messages.player' | translate }}</label>
            <select [(ngModel)]="selectedStudentId" class="filter-input">
              <option [ngValue]="null">{{ 'admin.messages.all' | translate }}</option>
              @for (student of students(); track student.id) {
                <option [ngValue]="student.id">{{ student.name }}</option>
              }
            </select>
          </div>
          <div class="filter-actions">
            <button class="btn btn--primary btn--sm" (click)="loadMessages()">
              <ng-icon name="heroMagnifyingGlass" size="14"></ng-icon>
              {{ 'admin.messages.filter' | translate }}
            </button>
            <button class="btn btn--ghost btn--sm" (click)="resetFilters()">
              <ng-icon name="heroArrowPath" size="14"></ng-icon>
            </button>
          </div>
        </div>
      </div>

      <!-- Stats -->
      <div class="stats-row">
        <div class="stat-badge">
          <span class="stat-badge__value">{{ filteredMessages().length }}</span>
          <span class="stat-badge__label">{{ filteredMessages().length > 1 ? ('admin.messages.messagesPlural' | translate) : ('admin.messages.messages' | translate) }}</span>
        </div>
        @if (searchQuery) {
          <div class="stat-badge stat-badge--secondary">
            <span class="stat-badge__label">{{ 'admin.messages.total' | translate : { count: messages().length } }}</span>
          </div>
        }
      </div>

      <!-- Messages List -->
      @if (loading()) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>{{ 'common.loading' | translate }}</p>
        </div>
      } @else if (error()) {
        <div class="error-state">
          <ng-icon name="heroExclamationTriangle" size="32"></ng-icon>
          <p>{{ error() }}</p>
          <button class="btn btn--primary btn--sm" (click)="loadMessages()">{{ 'admin.messages.retry' | translate }}</button>
        </div>
      } @else {
        <div class="messages-list">
          @for (message of pagination.paginatedItems(); track message.id) {
            <div class="message-row" [class.message-row--student]="message.fromRole === 'STUDENT'">
              <div class="message-row__type">
                <span class="type-badge" [class]="'type-badge--' + message.type.toLowerCase()">
                  {{ getTypeShort(message.type) }}
                </span>
              </div>
              <div class="message-row__info">
                <div class="message-row__header">
                  <span class="message-row__from">
                    <ng-icon [name]="message.fromRole === 'TEACHER' ? 'heroAcademicCap' : 'heroUserCircle'" size="14"></ng-icon>
                    {{ message.from }}
                  </span>
                  <span class="message-row__arrow">→</span>
                  <span class="message-row__to">{{ message.to }}</span>
                  <span class="message-row__date">{{ message.date }}</span>
                </div>
                <div class="message-row__content" [innerHTML]="highlightSearch(message.content)"></div>
              </div>
              <div class="message-row__lesson">
                #{{ message.lessonId }}
              </div>
            </div>
          } @empty {
            <div class="empty-state">
              <ng-icon name="heroChatBubbleLeftRight" size="32"></ng-icon>
              <p>{{ searchQuery ? ('admin.messages.noResults' | translate) : ('admin.messages.noMessages' | translate) }}</p>
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
  `,
    styles: [`
    .messages-page {
      padding: var(--space-lg);
      max-width: 1200px;
    }

    .page-header {
      margin-bottom: var(--space-lg);

      .page-title {
        display: flex;
        align-items: center;
        gap: var(--space-sm);

        h1 {
          font-size: 1.25rem;
          font-weight: 600;
          margin: 0;
        }
      }

      .page-subtitle {
        color: var(--text-muted);
        font-size: 0.8125rem;
        margin: var(--space-xs) 0 0;
      }
    }

    .search-bar {
      display: flex;
      align-items: center;
      gap: var(--space-sm);
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      padding: var(--space-sm) var(--space-md);
      margin-bottom: var(--space-md);
      transition: border-color var(--transition-fast);

      &:focus-within {
        border-color: var(--gold-400);
      }

      &__icon {
        color: var(--text-muted);
        flex-shrink: 0;
      }

      &__input {
        flex: 1;
        background: transparent;
        border: none;
        color: var(--text-primary);
        font-size: 0.9375rem;
        outline: none;

        &::placeholder {
          color: var(--text-muted);
        }
      }

      &__clear {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        height: 24px;
        background: var(--bg-tertiary);
        border: none;
        border-radius: var(--radius-full);
        color: var(--text-muted);
        cursor: pointer;
        transition: all var(--transition-fast);

        &:hover {
          background: var(--bg-primary);
          color: var(--text-primary);
        }
      }
    }

    .filters-card {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: var(--space-sm) var(--space-md);
      margin-bottom: var(--space-md);
    }

    .filters-row {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-sm);
      align-items: flex-end;
    }

    .filter-group {
      display: flex;
      flex-direction: column;
      gap: 2px;

      label {
        font-size: 0.6875rem;
        color: var(--text-muted);
        font-weight: 500;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }
    }

    .filter-input {
      padding: 6px 10px;
      background: var(--bg-primary);
      border: 1px solid var(--border-default);
      border-radius: var(--radius-sm);
      color: var(--text-primary);
      font-size: 0.8125rem;
      min-width: 120px;

      &:focus {
        outline: none;
        border-color: var(--gold-400);
      }
    }

    .filter-actions {
      display: flex;
      gap: var(--space-xs);
      margin-left: auto;
    }

    .btn--sm {
      padding: 6px 12px;
      font-size: 0.75rem;
    }

    .stats-row {
      display: flex;
      align-items: center;
      gap: var(--space-sm);
      margin-bottom: var(--space-md);
    }

    .stat-badge {
      display: inline-flex;
      align-items: center;
      gap: var(--space-xs);
      padding: 4px 10px;
      background: rgba(212, 168, 75, 0.1);
      border-radius: var(--radius-full);

      &__value {
        font-size: 0.875rem;
        font-weight: 700;
        color: var(--gold-400);
      }

      &__label {
        font-size: 0.75rem;
        color: var(--text-muted);
      }

      &--secondary {
        background: var(--bg-tertiary);
      }
    }

    .messages-list {
      display: flex;
      flex-direction: column;
      gap: 1px;
      background: var(--border-subtle);
      border-radius: var(--radius-md);
      overflow: hidden;
    }

    .message-row {
      display: grid;
      grid-template-columns: 80px 1fr 50px;
      gap: var(--space-sm);
      padding: var(--space-sm) var(--space-md);
      background: var(--bg-secondary);
      align-items: start;
      transition: background var(--transition-fast);

      &:hover {
        background: var(--bg-tertiary);
      }

      &--student {
        .message-row__from {
          color: #3b82f6;
        }
      }

      &__type {
        padding-top: 2px;
      }

      &__info {
        min-width: 0;
      }

      &__header {
        display: flex;
        align-items: center;
        gap: var(--space-xs);
        flex-wrap: wrap;
        margin-bottom: 4px;
      }

      &__from {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 0.8125rem;
        font-weight: 600;
        color: var(--gold-400);
      }

      &__arrow {
        color: var(--text-muted);
        font-size: 0.75rem;
      }

      &__to {
        font-size: 0.8125rem;
        color: var(--text-secondary);
      }

      &__date {
        font-size: 0.6875rem;
        color: var(--text-muted);
        margin-left: auto;
      }

      &__content {
        font-size: 0.8125rem;
        color: var(--text-primary);
        line-height: 1.5;
        white-space: pre-wrap;
        word-break: break-word;

        // Limit to 3 lines with ellipsis
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;

        :host ::ng-deep .highlight {
          background: rgba(212, 168, 75, 0.3);
          padding: 1px 2px;
          border-radius: 2px;
        }
      }

      &__lesson {
        font-size: 0.6875rem;
        color: var(--text-muted);
        text-align: right;
        padding-top: 2px;
      }
    }

    .type-badge {
      display: inline-block;
      padding: 2px 6px;
      border-radius: var(--radius-sm);
      font-size: 0.625rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.02em;

      &--note_etudiant {
        background: rgba(59, 130, 246, 0.15);
        color: #3b82f6;
      }

      &--observation_coach {
        background: rgba(168, 85, 247, 0.15);
        color: #a855f7;
      }

      &--commentaire_coach {
        background: rgba(212, 168, 75, 0.15);
        color: var(--gold-400);
      }
    }

    .loading-state, .error-state, .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--space-xl);
      color: var(--text-muted);
      gap: var(--space-sm);
      background: var(--bg-secondary);
      border-radius: var(--radius-md);
    }

    .empty-state p, .error-state p {
      font-size: 0.875rem;
      margin: 0;
    }

    @media (max-width: 768px) {
      .messages-page {
        padding: var(--space-md);
      }

      .filters-row {
        flex-direction: column;
        align-items: stretch;
      }

      .filter-input {
        min-width: 100%;
      }

      .filter-actions {
        margin-left: 0;
        margin-top: var(--space-sm);
      }

      .message-row {
        grid-template-columns: 1fr;
        gap: var(--space-xs);

        &__type {
          order: 1;
        }

        &__info {
          order: 2;
        }

        &__lesson {
          order: 0;
          text-align: left;
          font-size: 0.625rem;
        }

        &__header {
          flex-direction: column;
          align-items: flex-start;
          gap: 2px;
        }

        &__date {
          margin-left: 0;
        }
      }
    }
  `]
})
export class AdminMessagesComponent implements OnInit {
  private translate = inject(TranslateService);
  messages = signal<Message[]>([]);
  teachers = signal<FilterOption[]>([]);
  students = signal<FilterOption[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  dateFrom: string = '';
  dateTo: string = '';
  selectedTeacherId: number | null = null;
  selectedStudentId: number | null = null;
  searchQuery: string = '';

  // Filtered messages based on search query
  filteredMessages = computed(() => {
    const query = this.searchQuery.toLowerCase().trim();
    if (!query) return this.messages();

    return this.messages().filter(msg =>
      msg.content.toLowerCase().includes(query) ||
      msg.from.toLowerCase().includes(query) ||
      msg.to.toLowerCase().includes(query)
    );
  });

  pagination = paginate(this.filteredMessages, 10);

  constructor(private http: HttpClient) {
    // Set default dates (last 3 months)
    const today = new Date();
    const threeMonthsAgo = new Date();
    threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);

    this.dateTo = today.toISOString().split('T')[0];
    this.dateFrom = threeMonthsAgo.toISOString().split('T')[0];

    effect(() => {
      this.filteredMessages();
      untracked(() => this.pagination.currentPage.set(0));
    });
  }

  ngOnInit(): void {
    this.loadFilters();
    this.loadMessages();
  }

  loadFilters(): void {
    this.http.get<FilterOption[]>('/api/admin/messages/teachers').subscribe({
      next: (teachers) => this.teachers.set(teachers),
      error: () => {}
    });

    this.http.get<FilterOption[]>('/api/admin/messages/students').subscribe({
      next: (students) => this.students.set(students),
      error: () => {}
    });
  }

  loadMessages(): void {
    this.loading.set(true);
    this.error.set(null);

    let url = '/api/admin/messages?';
    const params: string[] = [];

    if (this.dateFrom) params.push(`dateFrom=${this.dateFrom}`);
    if (this.dateTo) params.push(`dateTo=${this.dateTo}`);
    if (this.selectedTeacherId) params.push(`teacherId=${this.selectedTeacherId}`);
    if (this.selectedStudentId) params.push(`studentId=${this.selectedStudentId}`);

    url += params.join('&');

    this.http.get<{ messages: Message[]; total: number }>(url).subscribe({
      next: (response) => {
        this.messages.set(response.messages);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || this.translate.instant('errors.load'));
        this.loading.set(false);
      }
    });
  }

  resetFilters(): void {
    const today = new Date();
    const threeMonthsAgo = new Date();
    threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);

    this.dateTo = today.toISOString().split('T')[0];
    this.dateFrom = threeMonthsAgo.toISOString().split('T')[0];
    this.selectedTeacherId = null;
    this.selectedStudentId = null;
    this.searchQuery = '';

    this.loadMessages();
  }

  getTypeShort(type: string): string {
    switch (type) {
      case 'NOTE_ETUDIANT': return this.translate.instant('admin.messages.note');
      case 'OBSERVATION_COACH': return this.translate.instant('admin.messages.obs');
      case 'COMMENTAIRE_COACH': return this.translate.instant('admin.messages.comment');
      default: return type.substring(0, 4);
    }
  }

  highlightSearch(content: string): string {
    if (!this.searchQuery.trim()) return this.escapeHtml(content);

    const escaped = this.escapeHtml(content);
    const query = this.searchQuery.trim();
    const regex = new RegExp(`(${this.escapeRegex(query)})`, 'gi');
    return escaped.replace(regex, '<span class="highlight">$1</span>');
  }

  private escapeHtml(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  private escapeRegex(str: string): string {
    return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }
}
