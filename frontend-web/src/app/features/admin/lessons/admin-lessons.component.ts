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
              À venir ({{ upcomingLessons().length }})
            </button>
            <button
              class="tab"
              [class.tab--active]="activeTab() === 'completed'"
              (click)="setTab('completed')"
            >
              Terminés ({{ completedLessons().length }})
            </button>
            <button
              class="tab"
              [class.tab--active]="activeTab() === 'cancelled'"
              (click)="setTab('cancelled')"
            >
              Annulés ({{ cancelledLessons().length }})
            </button>
          </div>
        </div>
      </header>

      @if (loading()) {
        <div class="loading">Chargement...</div>
      } @else {
        <div class="table-container">
          <div class="table-header">
            <span class="results-count">{{ filteredLessons().length }} résultat(s)</span>
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
                    <div class="actions-cell">
                      <button class="btn-action btn-action--details" (click)="openDetails(lesson)" title="Voir les détails">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <circle cx="12" cy="12" r="10"></circle>
                          <path d="M12 16v-4"></path>
                          <path d="M12 8h.01"></path>
                        </svg>
                      </button>
                      @if (lesson.status === 'COMPLETED' && lesson.recordingUrl) {
                        <button class="btn-action btn-action--recording" (click)="openRecording(lesson)" title="Voir l'enregistrement">
                          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <polygon points="23 7 16 12 23 17 23 7"></polygon>
                            <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                          </svg>
                        </button>
                      }
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7" class="empty-state">
                    Aucun cours {{ activeTab() === 'upcoming' ? 'à venir' : (activeTab() === 'completed' ? 'terminé' : 'annulé') }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        @if (activeTab() === 'completed' && completedLessons().length > 0) {
          <div class="summary-card">
            <h3>Résumé des cours effectués</h3>
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

      <!-- Detail Modal -->
      @if (showDetailModal() && selectedLesson()) {
        <div class="detail-overlay" (click)="closeDetails()">
          <div class="detail-modal" (click)="$event.stopPropagation()">
            <div class="detail-header">
              <h3>Details du cours #{{ selectedLesson()!.id }}</h3>
              <button class="detail-close" (click)="closeDetails()">✕</button>
            </div>
            <div class="detail-content">
              <div class="detail-section">
                <h4>Informations générales</h4>
                <div class="detail-grid">
                  <div class="detail-item">
                    <span class="detail-label">Date</span>
                    <span class="detail-value">{{ selectedLesson()!.scheduledAt | date:'dd/MM/yyyy HH:mm' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Durée</span>
                    <span class="detail-value">{{ selectedLesson()!.durationMinutes }} min</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Statut</span>
                    <span class="badge" [class]="getStatusClass(selectedLesson()!.status)">
                      {{ getStatusLabel(selectedLesson()!.status) }}
                    </span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Prix</span>
                    <span class="detail-value">{{ selectedLesson()!.priceCents ? formatPrice(selectedLesson()!.priceCents!) : 'Gratuit' }}</span>
                  </div>
                </div>
              </div>

              <div class="detail-section">
                <h4>Participants</h4>
                <div class="detail-grid">
                  <div class="detail-item">
                    <span class="detail-label">Coach</span>
                    <span class="detail-value">{{ selectedLesson()!.teacherName }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Joueur</span>
                    <span class="detail-value">
                      {{ selectedLesson()!.studentName }}
                      @if (selectedLesson()!.studentLevel) {
                        <span class="badge badge--level">{{ selectedLesson()!.studentLevel }}</span>
                      }
                    </span>
                  </div>
                  @if (selectedLesson()!.studentAge) {
                    <div class="detail-item">
                      <span class="detail-label">Age</span>
                      <span class="detail-value">{{ selectedLesson()!.studentAge }} ans</span>
                    </div>
                  }
                  @if (selectedLesson()!.studentElo) {
                    <div class="detail-item">
                      <span class="detail-label">ELO</span>
                      <span class="detail-value">{{ selectedLesson()!.studentElo }}</span>
                    </div>
                  }
                </div>
              </div>

              @if (selectedLesson()!.status === 'CANCELLED') {
                <div class="detail-section detail-section--warning">
                  <h4>Annulation</h4>
                  <div class="detail-grid">
                    <div class="detail-item">
                      <span class="detail-label">Annulé par</span>
                      <span class="detail-value">{{ getCancelledByLabel(selectedLesson()!.cancelledBy) }}</span>
                    </div>
                    @if (selectedLesson()!.cancellationReason) {
                      <div class="detail-item detail-item--full">
                        <span class="detail-label">Raison</span>
                        <span class="detail-value">{{ selectedLesson()!.cancellationReason }}</span>
                      </div>
                    }
                    @if (selectedLesson()!.refundedAmountCents) {
                      <div class="detail-item">
                        <span class="detail-label">Remboursement</span>
                        <span class="detail-value">{{ formatPrice(selectedLesson()!.refundedAmountCents!) }} ({{ selectedLesson()!.refundPercentage }}%)</span>
                      </div>
                    }
                  </div>
                </div>
              }

              @if (selectedLesson()!.teacherObservations) {
                <div class="detail-section">
                  <h4>Observations du coach</h4>
                  <p class="detail-text">{{ selectedLesson()!.teacherObservations }}</p>
                </div>
              }

              @if (selectedLesson()!.zoomLink) {
                <div class="detail-section">
                  <h4>Lien visioconference</h4>
                  <div class="detail-link-box">
                    <code>{{ selectedLesson()!.zoomLink }}</code>
                    <button class="btn-copy" (click)="copyToClipboard(selectedLesson()!.zoomLink!)" title="Copier">
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect width="14" height="14" x="8" y="8" rx="2" ry="2"></rect>
                        <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"></path>
                      </svg>
                    </button>
                  </div>
                </div>
              }

              <div class="detail-section">
                <h4>Finances</h4>
                <div class="detail-grid">
                  <div class="detail-item">
                    <span class="detail-label">Prix total</span>
                    <span class="detail-value">{{ selectedLesson()!.priceCents ? formatPrice(selectedLesson()!.priceCents!) : '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Commission</span>
                    <span class="detail-value">{{ selectedLesson()!.commissionCents ? formatPrice(selectedLesson()!.commissionCents!) : '-' }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Gain coach</span>
                    <span class="detail-value">{{ selectedLesson()!.teacherEarningsCents ? formatPrice(selectedLesson()!.teacherEarningsCents!) : '-' }}</span>
                  </div>
                </div>
              </div>
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

    .actions-cell {
      display: flex;
      gap: var(--space-xs);
    }

    .btn-action {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      border: none;
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all var(--transition-fast);

      &--details {
        background: rgba(59, 130, 246, 0.1);
        color: #3b82f6;

        &:hover {
          background: rgba(59, 130, 246, 0.2);
        }
      }

      &--recording {
        background: rgba(34, 197, 94, 0.1);
        color: #22c55e;

        &:hover {
          background: rgba(34, 197, 94, 0.2);
        }
      }
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

    .detail-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.8);
      z-index: 1100;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;

      @media (max-width: 767px) {
        padding: 0.5rem;
        align-items: flex-end;
      }
    }

    .detail-modal {
      width: 100%;
      max-width: 600px;
      max-height: 90vh;
      background: var(--bg-secondary);
      border-radius: var(--radius-xl);
      border: 1px solid var(--border-subtle);
      overflow: hidden;
      display: flex;
      flex-direction: column;

      @media (max-width: 767px) {
        max-height: 85vh;
        border-radius: var(--radius-lg) var(--radius-lg) 0 0;
      }
    }

    .detail-header {
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

    .detail-close {
      width: 36px;
      height: 36px;
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

    .detail-content {
      padding: 1.5rem;
      overflow-y: auto;

      @media (max-width: 767px) {
        padding: 1rem;
      }
    }

    .detail-section {
      margin-bottom: 1.5rem;

      &:last-child {
        margin-bottom: 0;
      }

      h4 {
        font-size: 0.875rem;
        font-weight: 600;
        color: var(--text-muted);
        text-transform: uppercase;
        letter-spacing: 0.05em;
        margin-bottom: 0.75rem;
      }

      &--warning {
        background: rgba(239, 68, 68, 0.1);
        border: 1px solid rgba(239, 68, 68, 0.2);
        border-radius: var(--radius-md);
        padding: 1rem;

        h4 {
          color: var(--error);
        }
      }
    }

    .detail-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1rem;

      @media (max-width: 480px) {
        grid-template-columns: 1fr;
      }
    }

    .detail-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      &--full {
        grid-column: 1 / -1;
      }
    }

    .detail-label {
      font-size: 0.75rem;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .detail-value {
      font-size: 0.9375rem;
      color: var(--text-primary);
    }

    .detail-text {
      font-size: 0.9375rem;
      color: var(--text-primary);
      line-height: 1.6;
      margin: 0;
    }

    .detail-link-box {
      display: flex;
      align-items: center;
      gap: var(--space-sm);
      background: var(--bg-tertiary);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      padding: 0.75rem 1rem;

      code {
        flex: 1;
        font-size: 0.8125rem;
        color: var(--text-secondary);
        word-break: break-all;
      }
    }

    .btn-copy {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: transparent;
      border: none;
      border-radius: var(--radius-md);
      color: var(--text-muted);
      cursor: pointer;
      transition: all var(--transition-fast);

      &:hover {
        background: var(--bg-secondary);
        color: var(--gold-400);
      }
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
  cancelledLessons = signal<AdminLessonResponse[]>([]);
  loading = signal(true);
  activeTab = signal<'upcoming' | 'completed' | 'cancelled'>('upcoming');
  searchQuery = '';

  // Video player
  showVideoPlayer = signal(false);
  videoPlayerUrl = signal('');

  // Detail modal
  showDetailModal = signal(false);
  selectedLesson = signal<AdminLessonResponse | null>(null);

  // Filtered lessons based on search query and active tab
  filteredLessons = computed(() => {
    let lessons: AdminLessonResponse[];
    switch (this.activeTab()) {
      case 'upcoming':
        lessons = this.upcomingLessons();
        break;
      case 'completed':
        lessons = this.completedLessons();
        break;
      case 'cancelled':
        lessons = this.cancelledLessons();
        break;
      default:
        lessons = [];
    }

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

    // Load upcoming lessons
    this.adminService.getUpcomingLessons().subscribe({
      next: (lessons) => this.upcomingLessons.set(lessons),
      error: () => this.upcomingLessons.set([])
    });

    // Load all past lessons and separate them
    this.adminService.getPastLessons().subscribe({
      next: (lessons) => {
        this.completedLessons.set(lessons.filter(l => l.status === 'COMPLETED'));
        this.cancelledLessons.set(lessons.filter(l => l.status === 'CANCELLED'));
        this.loading.set(false);
      },
      error: () => {
        this.completedLessons.set([]);
        this.cancelledLessons.set([]);
        this.loading.set(false);
      }
    });
  }

  setTab(tab: 'upcoming' | 'completed' | 'cancelled'): void {
    this.activeTab.set(tab);
  }

  currentLessons(): AdminLessonResponse[] {
    switch (this.activeTab()) {
      case 'upcoming': return this.upcomingLessons();
      case 'completed': return this.completedLessons();
      case 'cancelled': return this.cancelledLessons();
      default: return [];
    }
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
      CONFIRMED: 'Confirmé',
      COMPLETED: 'Terminé',
      CANCELLED: 'Annulé'
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

  openDetails(lesson: AdminLessonResponse): void {
    this.selectedLesson.set(lesson);
    this.showDetailModal.set(true);
  }

  closeDetails(): void {
    this.showDetailModal.set(false);
    this.selectedLesson.set(null);
  }

  getCancelledByLabel(cancelledBy?: string): string {
    if (!cancelledBy) return 'Inconnu';
    const labels: Record<string, string> = {
      STUDENT: 'Le joueur',
      TEACHER: 'Le coach',
      SYSTEM: 'Système (auto)',
      ADMIN: 'Administrateur'
    };
    return labels[cancelledBy] || cancelledBy;
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text).then(() => {
      // Could add a toast notification here
    });
  }
}
