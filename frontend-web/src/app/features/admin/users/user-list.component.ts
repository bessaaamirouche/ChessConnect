import { Component, OnInit, signal, inject, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, UserListResponse, Page } from '../../../core/services/admin.service';
import { AdminStateService } from '../../../core/services/admin-state.service';
import { DialogService } from '../../../core/services/dialog.service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="user-list">
      <header class="page-header">
        <h1>Utilisateurs</h1>
      </header>

      <!-- Tabs -->
      <div class="tabs">
        <button
          class="tab"
          [class.active]="activeTab() === 'users'"
          (click)="switchTab('users')"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M22 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
          Utilisateurs
          @if (activeUsersCount() > 0) {
            <span class="tab-count">{{ activeUsersCount() }}</span>
          }
        </button>
        <button
          class="tab tab--blacklist"
          [class.active]="activeTab() === 'blacklist'"
          (click)="switchTab('blacklist')"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
          </svg>
          Blacklist
          @if (blacklistCount() > 0) {
            <span class="tab-count tab-count--danger">{{ blacklistCount() }}</span>
          }
        </button>
      </div>

      <!-- Filters -->
      <div class="filters">
        <div class="search-box">
          <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <path d="m21 21-4.3-4.3"></path>
          </svg>
          <input
            type="text"
            [(ngModel)]="searchQuery"
            (input)="onSearchChange()"
            placeholder="Rechercher par nom, prenom, email..."
            class="search-input"
          >
          @if (searchQuery) {
            <button class="search-clear" (click)="clearSearch()">×</button>
          }
        </div>
        <select [(ngModel)]="roleFilter" (change)="loadUsers(0)" class="input input--sm">
          <option value="">Tous les roles</option>
          <option value="STUDENT">Joueurs</option>
          <option value="TEACHER">Coachs</option>
        </select>
      </div>

      @if (loading()) {
        <div class="loading">Chargement...</div>
      } @else {
        <!-- Active Users Tab -->
        @if (activeTab() === 'users') {
          <div class="table-container">
            <div class="table-header">
              <span class="results-count">{{ filteredActiveUsers().length }} utilisateur(s) actif(s)</span>
            </div>
            @if (filteredActiveUsers().length === 0) {
              <div class="empty-state">
                <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                  <path d="M22 21v-2a4 4 0 0 0-3-3.87"></path>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
                <p>Aucun utilisateur actif trouve</p>
              </div>
            } @else {
              <table class="table">
                <thead>
                  <tr>
                    <th>Utilisateur</th>
                    <th>Role</th>
                    <th>Cours</th>
                    <th>Inscription</th>
                    <th>Derniere connexion</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (user of filteredActiveUsers(); track user.id) {
                    <tr>
                      <td>
                        <div class="user-info">
                          <strong>{{ user.firstName }} {{ user.lastName }}</strong>
                          <span class="text-muted">{{ user.email }}</span>
                        </div>
                      </td>
                      <td>
                        <span class="badge" [class]="'badge--' + user.role.toLowerCase()">
                          {{ getRoleLabel(user.role) }}
                        </span>
                      </td>
                      <td>{{ user.lessonsCount }}</td>
                      <td>{{ user.createdAt | date:'dd/MM/yyyy' }}</td>
                      <td>
                        @if (user.lastLoginAt) {
                          {{ user.lastLoginAt | date:'dd/MM/yyyy HH:mm' }}
                        } @else {
                          <span class="text-muted">Jamais</span>
                        }
                      </td>
                      <td>
                        <div class="action-buttons">
                          <button
                            class="action-btn action-btn--suspend"
                            (click)="suspendUser(user)"
                            [disabled]="actionLoading()"
                            title="Ajouter a la blacklist"
                          >
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                              <circle cx="12" cy="12" r="10"></circle>
                              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                            </svg>
                            Blacklister
                          </button>
                          <button
                            class="action-btn action-btn--delete"
                            (click)="deleteUser(user)"
                            [disabled]="actionLoading()"
                            title="Supprimer definitivement"
                          >
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                              <polyline points="3 6 5 6 21 6"></polyline>
                              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                              <line x1="10" y1="11" x2="10" y2="17"></line>
                              <line x1="14" y1="11" x2="14" y2="17"></line>
                            </svg>
                            Supprimer
                          </button>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </div>
        }

        <!-- Blacklist Tab -->
        @if (activeTab() === 'blacklist') {
          <div class="table-container table-container--blacklist">
            <div class="table-header table-header--blacklist">
              <span class="results-count">{{ filteredBlacklistedUsers().length }} utilisateur(s) blackliste(s)</span>
            </div>
            @if (filteredBlacklistedUsers().length === 0) {
              <div class="empty-state">
                <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                  <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
                <p>Aucun utilisateur blackliste</p>
              </div>
            } @else {
              <table class="table">
                <thead>
                  <tr>
                    <th>Utilisateur</th>
                    <th>Role</th>
                    <th>Cours</th>
                    <th>Inscription</th>
                    <th>Derniere connexion</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (user of filteredBlacklistedUsers(); track user.id) {
                    <tr>
                      <td>
                        <div class="user-info">
                          <strong>{{ user.firstName }} {{ user.lastName }}</strong>
                          <span class="text-muted">{{ user.email }}</span>
                        </div>
                      </td>
                      <td>
                        <span class="badge" [class]="'badge--' + user.role.toLowerCase()">
                          {{ getRoleLabel(user.role) }}
                        </span>
                      </td>
                      <td>{{ user.lessonsCount }}</td>
                      <td>{{ user.createdAt | date:'dd/MM/yyyy' }}</td>
                      <td>
                        @if (user.lastLoginAt) {
                          {{ user.lastLoginAt | date:'dd/MM/yyyy HH:mm' }}
                        } @else {
                          <span class="text-muted">Jamais</span>
                        }
                      </td>
                      <td>
                        <div class="action-buttons">
                          <button
                            class="action-btn action-btn--activate"
                            (click)="activateUser(user)"
                            [disabled]="actionLoading()"
                            title="Retirer de la blacklist"
                          >
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                              <polyline points="22 4 12 14.01 9 11.01"></polyline>
                            </svg>
                            Reactiver
                          </button>
                          <button
                            class="action-btn action-btn--delete"
                            (click)="deleteUser(user)"
                            [disabled]="actionLoading()"
                            title="Supprimer definitivement"
                          >
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                              <polyline points="3 6 5 6 21 6"></polyline>
                              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                              <line x1="10" y1="11" x2="10" y2="17"></line>
                              <line x1="14" y1="11" x2="14" y2="17"></line>
                            </svg>
                            Supprimer
                          </button>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </div>
        }

        @if (users() && users()!.totalPages > 1) {
          <div class="pagination">
            <button
              class="btn btn--sm btn--ghost"
              [disabled]="currentPage() === 0"
              (click)="loadUsers(currentPage() - 1)"
            >
              Precedent
            </button>
            <span>Page {{ currentPage() + 1 }} / {{ users()!.totalPages }}</span>
            <button
              class="btn btn--sm btn--ghost"
              [disabled]="currentPage() >= users()!.totalPages - 1"
              (click)="loadUsers(currentPage() + 1)"
            >
              Suivant
            </button>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--space-md);
      flex-wrap: wrap;
      gap: var(--space-md);

      h1 {
        font-size: 1.5rem;
        font-weight: 600;
        color: var(--text-primary);
      }

      @media (max-width: 48rem) {
        flex-direction: column;
        align-items: flex-start;

        h1 {
          font-size: 1.25rem;
        }
      }
    }

    .tabs {
      display: flex;
      gap: 0.5rem;
      margin-bottom: var(--space-lg);
      border-bottom: 1px solid var(--border-subtle);
      padding-bottom: 0;
    }

    .tab {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem 1rem;
      font-size: 0.875rem;
      font-weight: 500;
      color: var(--text-muted);
      background: transparent;
      border: none;
      border-bottom: 2px solid transparent;
      cursor: pointer;
      transition: all 0.2s ease;
      margin-bottom: -1px;

      svg {
        opacity: 0.7;
      }

      &:hover {
        color: var(--text-primary);
        background: rgba(128, 128, 128, 0.05);
      }

      &.active {
        color: var(--gold-500);
        border-bottom-color: var(--gold-500);

        svg {
          opacity: 1;
        }
      }

      &--blacklist.active {
        color: #ef4444;
        border-bottom-color: #ef4444;
      }
    }

    .tab-count {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 1.25rem;
      height: 1.25rem;
      padding: 0 0.375rem;
      font-size: 0.6875rem;
      font-weight: 600;
      background: rgba(128, 128, 128, 0.15);
      border-radius: 0.625rem;

      &--danger {
        background: rgba(239, 68, 68, 0.15);
        color: #ef4444;
      }
    }

    .filters {
      display: flex;
      gap: var(--space-md);
      flex-wrap: wrap;
      width: 100%;
      margin-bottom: var(--space-lg);

      @media (max-width: 48rem) {
        flex-direction: column;
        gap: 0.5rem;
      }
    }

    .table-container {
      background: var(--bg-secondary);
      border-radius: 0.75rem;
      overflow: hidden;
      box-shadow: 0 0.0625rem 0.1875rem rgba(0, 0, 0, 0.1), 0 0.0625rem 0.125rem rgba(0, 0, 0, 0.06);
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;

      &--blacklist {
        border: 1px solid rgba(239, 68, 68, 0.2);
      }
    }

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem 1rem;
      background: var(--bg-tertiary);
      border-bottom: 0.0625rem solid var(--border-subtle);

      &--blacklist {
        background: rgba(239, 68, 68, 0.05);
        border-bottom-color: rgba(239, 68, 68, 0.15);
      }
    }

    .table {
      width: 100%;
      border-collapse: collapse;
      min-width: 45rem;

      th, td {
        padding: 0.875rem 1rem;
        text-align: left;
        vertical-align: middle;
      }

      th {
        font-size: 0.6875rem;
        font-weight: 500;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: var(--text-muted);
        background: var(--bg-tertiary);
        border-bottom: 0.0625rem solid var(--border-subtle);
      }

      td {
        font-size: 0.875rem;
        color: var(--text-primary);
        border-bottom: 0.0625rem solid rgba(128, 128, 128, 0.1);
      }

      tbody tr {
        transition: background-color 0.15s ease;
      }

      tbody tr:hover {
        background: rgba(128, 128, 128, 0.04);
      }

      tbody tr:last-child td {
        border-bottom: none;
      }
    }

    .user-info {
      display: flex;
      flex-direction: column;
      gap: 0.125rem;

      strong {
        font-weight: 500;
        color: var(--text-primary);
      }

      .text-muted {
        font-size: 0.75rem;
        color: var(--text-muted);
      }
    }

    .text-muted {
      color: var(--text-muted);
      font-size: 0.8125rem;
    }

    .badge {
      display: inline-flex;
      align-items: center;
      padding: 0.25rem 0.625rem;
      font-size: 0.6875rem;
      font-weight: 500;
      border-radius: 0.375rem;
      white-space: nowrap;

      &--student {
        background: rgba(59, 130, 246, 0.1);
        color: #3b82f6;
      }

      &--teacher {
        background: rgba(139, 92, 246, 0.1);
        color: #8b5cf6;
      }

      &--admin {
        background: rgba(212, 168, 75, 0.1);
        color: var(--gold-500);
      }
    }

    .action-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.375rem;
      padding: 0.375rem 0.75rem;
      font-size: 0.75rem;
      font-weight: 500;
      border-radius: 0.375rem;
      cursor: pointer;
      transition: all 0.15s ease;
      border: none;
      white-space: nowrap;

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      &--activate {
        background: rgba(34, 197, 94, 0.1);
        color: #22c55e;

        &:hover:not(:disabled) {
          background: rgba(34, 197, 94, 0.2);
        }
      }

      &--suspend {
        background: rgba(239, 68, 68, 0.08);
        color: #ef4444;

        &:hover:not(:disabled) {
          background: rgba(239, 68, 68, 0.15);
        }
      }

      &--delete {
        background: rgba(220, 38, 38, 0.08);
        color: #dc2626;

        &:hover:not(:disabled) {
          background: rgba(220, 38, 38, 0.15);
        }
      }
    }

    .action-buttons {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 3rem 1rem;
      color: var(--text-muted);

      svg {
        margin-bottom: 1rem;
        opacity: 0.5;
      }

      p {
        font-size: 0.9375rem;
      }
    }

    .pagination {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: var(--space-md);
      padding: var(--space-lg);
      color: var(--text-secondary);
      font-size: 0.875rem;
      flex-wrap: wrap;

      @media (max-width: 26rem) {
        font-size: 0.8125rem;
        gap: 0.5rem;
      }
    }

    .loading {
      text-align: center;
      padding: var(--space-2xl);
      color: var(--text-muted);
    }

    .input--sm {
      padding: 0.5rem 0.75rem;
      font-size: 0.875rem;
      border: 0.0625rem solid var(--border-subtle);
      border-radius: 0.5rem;
      background: var(--bg-tertiary);
      color: var(--text-primary);
      transition: all 0.15s ease;
      width: 100%;
      min-width: 0;

      @media (min-width: 48rem) {
        width: auto;
      }

      &:focus {
        outline: none;
        border-color: var(--gold-500);
      }
    }

    .search-box {
      position: relative;
      display: flex;
      align-items: center;
      width: 100%;

      @media (min-width: 48rem) {
        width: auto;
      }
    }

    .search-icon {
      position: absolute;
      left: 0.75rem;
      color: var(--text-muted);
      pointer-events: none;
    }

    .search-input {
      padding: 0.5rem 2.25rem;
      font-size: 0.875rem;
      border: 0.0625rem solid var(--border-subtle);
      border-radius: 0.5rem;
      background: var(--bg-tertiary);
      color: var(--text-primary);
      width: 100%;
      min-width: 0;
      transition: all 0.15s ease;

      @media (min-width: 48rem) {
        min-width: 17.5rem;
        width: auto;
      }

      &::placeholder {
        color: var(--text-muted);
      }

      &:focus {
        outline: none;
        border-color: var(--gold-500);
        background: var(--bg-secondary);
      }
    }

    .search-clear {
      position: absolute;
      right: 0.5rem;
      width: 1.25rem;
      height: 1.25rem;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(128, 128, 128, 0.15);
      border: none;
      border-radius: 50%;
      color: var(--text-muted);
      cursor: pointer;
      font-size: 0.875rem;
      line-height: 1;
      transition: all 0.15s ease;

      &:hover {
        background: rgba(128, 128, 128, 0.25);
        color: var(--text-primary);
      }
    }

    .results-count {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .btn--ghost {
      background: transparent;
      color: var(--text-secondary);
      border: 0.0625rem solid var(--border-subtle);
      padding: 0.5rem 1rem;
      font-size: 0.875rem;
      font-weight: 500;
      border-radius: 0.5rem;
      cursor: pointer;
      transition: all 0.15s ease;

      &:hover:not(:disabled) {
        background: var(--bg-tertiary);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  `]
})
export class UserListComponent implements OnInit {
  users = signal<Page<UserListResponse> | null>(null);
  loading = signal(true);
  actionLoading = signal(false);
  currentPage = signal(0);
  activeTab = signal<'users' | 'blacklist'>('users');
  roleFilter = '';
  searchQuery = '';

  private dialogService = inject(DialogService);
  private adminStateService = inject(AdminStateService);

  // Count of active users
  activeUsersCount = computed(() => {
    const allUsers = this.users()?.content || [];
    return allUsers.filter(u => !u.isSuspended).length;
  });

  // Count of blacklisted users
  blacklistCount = computed(() => {
    const allUsers = this.users()?.content || [];
    return allUsers.filter(u => u.isSuspended).length;
  });

  // Filtered active users based on search query
  filteredActiveUsers = computed(() => {
    const allUsers = this.users()?.content || [];
    const activeUsers = allUsers.filter(u => !u.isSuspended);
    return this.filterBySearch(activeUsers);
  });

  // Filtered blacklisted users based on search query
  filteredBlacklistedUsers = computed(() => {
    const allUsers = this.users()?.content || [];
    const blacklistedUsers = allUsers.filter(u => u.isSuspended);
    return this.filterBySearch(blacklistedUsers);
  });

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers(0);
  }

  private filterBySearch(users: UserListResponse[]): UserListResponse[] {
    const query = this.searchQuery.toLowerCase().trim();
    if (!query) return users;

    return users.filter(user => {
      const fullName = `${user.firstName} ${user.lastName}`.toLowerCase();
      const email = user.email.toLowerCase();
      return fullName.includes(query) ||
             user.firstName.toLowerCase().includes(query) ||
             user.lastName.toLowerCase().includes(query) ||
             email.includes(query);
    });
  }

  switchTab(tab: 'users' | 'blacklist'): void {
    this.activeTab.set(tab);
  }

  onSearchChange(): void {
    // Search is computed, no action needed
  }

  clearSearch(): void {
    this.searchQuery = '';
  }

  loadUsers(page: number): void {
    this.loading.set(true);
    this.currentPage.set(page);

    this.adminService.getUsers(page, 100, this.roleFilter || undefined).subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      STUDENT: 'Joueur',
      TEACHER: 'Coach',
      ADMIN: 'Admin'
    };
    return labels[role] || role;
  }

  async suspendUser(user: UserListResponse): Promise<void> {
    const reason = await this.dialogService.prompt(
      `Pourquoi blacklister ${user.firstName} ${user.lastName} ?`,
      'Ajouter a la blacklist',
      { inputLabel: 'Motif', inputPlaceholder: 'Ex: Comportement inapproprie...', variant: 'warning' }
    );
    if (!reason) return;

    this.actionLoading.set(true);
    this.adminService.suspendUser(user.id, reason).subscribe({
      next: () => {
        this.loadUsers(this.currentPage());
        this.actionLoading.set(false);
        this.adminStateService.notifyUserChange('update', user.id);
      },
      error: () => {
        this.actionLoading.set(false);
        this.dialogService.alert('Erreur lors de la suspension', 'Erreur', { variant: 'danger' });
      }
    });
  }

  activateUser(user: UserListResponse): void {
    this.actionLoading.set(true);
    this.adminService.activateUser(user.id).subscribe({
      next: () => {
        this.loadUsers(this.currentPage());
        this.actionLoading.set(false);
        this.adminStateService.notifyUserChange('update', user.id);
      },
      error: () => {
        this.actionLoading.set(false);
        this.dialogService.alert('Erreur lors de la reactivation', 'Erreur', { variant: 'danger' });
      }
    });
  }

  async deleteUser(user: UserListResponse): Promise<void> {
    // First check wallet balance for students
    if (user.role === 'STUDENT') {
      this.adminService.getUserWallet(user.id).subscribe({
        next: async (wallet) => {
          if (wallet.balanceCents > 0) {
            const confirmWithWallet = await this.dialogService.confirm(
              `${user.firstName} ${user.lastName} a ${wallet.balanceFormatted} dans son portefeuille.\n\nLe solde sera automatiquement enregistre pour remboursement manuel (virement bancaire).\n\nVoulez-vous continuer ?`,
              'Supprimer avec solde',
              { confirmText: 'Supprimer et enregistrer remboursement', cancelText: 'Annuler', variant: 'danger' }
            );
            if (confirmWithWallet) {
              this.performDelete(user);
            }
          } else {
            this.confirmAndDelete(user);
          }
        },
        error: () => {
          // If wallet check fails, proceed with normal deletion
          this.confirmAndDelete(user);
        }
      });
    } else {
      this.confirmAndDelete(user);
    }
  }

  private async confirmAndDelete(user: UserListResponse): Promise<void> {
    const confirmed = await this.dialogService.confirm(
      `Etes-vous sur de vouloir supprimer definitivement ${user.firstName} ${user.lastName} ?\n\nCette action est irreversible et supprimera toutes les donnees associees.`,
      'Supprimer l\'utilisateur',
      { confirmText: 'Supprimer definitivement', cancelText: 'Annuler', variant: 'danger' }
    );
    if (confirmed) {
      this.performDelete(user);
    }
  }

  private performDelete(user: UserListResponse): void {
    this.actionLoading.set(true);
    this.adminService.deleteUser(user.id).subscribe({
      next: (response) => {
        this.loadUsers(this.currentPage());
        this.actionLoading.set(false);
        this.adminStateService.notifyUserChange('delete', user.id);

        if (response.requiresManualRefund) {
          const amount = (response.refundedAmountCents / 100).toFixed(2);
          this.dialogService.alert(
            `Utilisateur supprime.\n\n${amount} EUR a rembourser manuellement par virement bancaire.`,
            'Remboursement requis',
            { variant: 'warning' }
          );
        } else {
          this.dialogService.alert('Utilisateur supprime avec succes.', 'Succes', { variant: 'success' });
        }
      },
      error: (err) => {
        this.actionLoading.set(false);
        const message = err.error?.message || 'Erreur lors de la suppression';
        this.dialogService.alert(message, 'Erreur', { variant: 'danger' });
      }
    });
  }
}
