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
      </header>

      @if (loading()) {
        <div class="loading">Chargement...</div>
      } @else {
        <div class="table-container">
          <div class="table-header">
            <span class="results-count">{{ filteredUsers().length }} resultat(s)</span>
          </div>
          <table class="table">
            <thead>
              <tr>
                <th>Utilisateur</th>
                <th>Role</th>
                <th>Cours</th>
                <th>Inscription</th>
                <th>Derniere connexion</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (user of filteredUsers(); track user.id) {
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
                    @if (user.isSuspended) {
                      <span class="badge badge--suspended">Suspendu</span>
                    } @else {
                      <span class="badge badge--active">Actif</span>
                    }
                  </td>
                  <td>
                    <div class="actions-cell">
                      @if (user.isSuspended) {
                        <button
                          class="action-btn action-btn--activate"
                          (click)="activateUser(user)"
                          [disabled]="actionLoading()"
                        >
                          Reactiver
                        </button>
                      } @else {
                        <button
                          class="action-btn action-btn--suspend"
                          (click)="suspendUser(user)"
                          [disabled]="actionLoading()"
                        >
                          Suspendre
                        </button>
                      }
                      <button
                        class="action-btn action-btn--delete"
                        (click)="confirmDeleteUser(user)"
                        [disabled]="actionLoading()"
                        title="Supprimer definitivement"
                      >
                        Supprimer
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

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

      @if (showDeleteModal()) {
        <div class="modal-overlay" (click)="cancelDelete()">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>Confirmer la suppression</h3>
            <p>Voulez-vous vraiment supprimer le compte de <strong>{{ userToDelete()?.firstName }} {{ userToDelete()?.lastName }}</strong> ?</p>
            <p class="warning">Cette action est irreversible.</p>
            <div class="modal-actions">
              <button class="btn btn--ghost" (click)="cancelDelete()">Non, annuler</button>
              <button class="btn btn--danger" (click)="deleteUser()" [disabled]="actionLoading()">
                @if (actionLoading()) {
                  Suppression...
                } @else {
                  Oui, supprimer
                }
              </button>
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

      @media (max-width: 26rem) {
        h1 {
          font-size: 1.125rem;
        }
      }
    }

    .filters {
      display: flex;
      gap: var(--space-md);
      flex-wrap: wrap;
      width: 100%;

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
    }

    .table {
      width: 100%;
      border-collapse: collapse;
      min-width: 53rem;

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

      &--active {
        background: rgba(34, 197, 94, 0.1);
        color: #22c55e;
      }

      &--suspended {
        background: rgba(239, 68, 68, 0.1);
        color: #ef4444;
      }
    }

    .actions-cell {
      display: flex;
      gap: 0.5rem;
      align-items: center;
    }

    .action-btn {
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
        background: rgba(128, 128, 128, 0.1);
        color: var(--text-secondary);

        &:hover:not(:disabled) {
          background: rgba(128, 128, 128, 0.2);
        }
      }

      &--delete {
        background: rgba(128, 128, 128, 0.08);
        color: var(--text-muted);

        &:hover:not(:disabled) {
          background: rgba(239, 68, 68, 0.1);
          color: #ef4444;
        }
      }
    }

    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      backdrop-filter: blur(0.25rem);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 1rem;
    }

    .modal {
      background: var(--bg-secondary);
      border-radius: 1rem;
      padding: var(--space-xl);
      max-width: 25rem;
      width: 100%;
      box-shadow: 0 1.25rem 1.5625rem -0.3125rem rgba(0, 0, 0, 0.1), 0 0.625rem 0.625rem -0.3125rem rgba(0, 0, 0, 0.04);

      @media (max-width: 26rem) {
        padding: 1rem;
        border-radius: 0.75rem;
      }

      h3 {
        margin-bottom: var(--space-md);
        color: var(--text-primary);
        font-weight: 600;
        font-size: 1.125rem;

        @media (max-width: 26rem) {
          font-size: 1rem;
        }
      }

      p {
        color: var(--text-secondary);
        margin-bottom: var(--space-sm);
        font-size: 0.9375rem;
        line-height: 1.5;

        @media (max-width: 26rem) {
          font-size: 0.875rem;
        }
      }

      .warning {
        color: #ef4444;
        font-weight: 500;
        font-size: 0.875rem;
      }
    }

    .modal-actions {
      display: flex;
      gap: var(--space-md);
      justify-content: flex-end;
      margin-top: var(--space-lg);

      @media (max-width: 26rem) {
        flex-direction: column-reverse;
        gap: 0.5rem;
      }
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

      @media (max-width: 26rem) {
        width: 100%;
        padding: 0.75rem 1rem;
      }

      &:hover {
        background: var(--bg-tertiary);
      }
    }

    .btn--danger {
      background: #ef4444;
      color: white;
      border: none;
      padding: 0.5rem 1rem;
      font-size: 0.875rem;
      font-weight: 500;
      border-radius: 0.5rem;
      cursor: pointer;
      transition: all 0.15s ease;

      @media (max-width: 26rem) {
        width: 100%;
        padding: 0.75rem 1rem;
      }

      &:hover:not(:disabled) {
        background: #dc2626;
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
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

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem 1rem;
      background: var(--bg-tertiary);
      border-bottom: 0.0625rem solid var(--border-subtle);
    }

    .results-count {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
  `]
})
export class UserListComponent implements OnInit {
  users = signal<Page<UserListResponse> | null>(null);
  loading = signal(true);
  actionLoading = signal(false);
  currentPage = signal(0);
  roleFilter = '';
  searchQuery = '';
  showDeleteModal = signal(false);
  userToDelete = signal<UserListResponse | null>(null);

  private dialogService = inject(DialogService);
  private adminStateService = inject(AdminStateService);

  // Filtered users based on search query
  filteredUsers = computed(() => {
    const allUsers = this.users()?.content || [];
    const query = this.searchQuery.toLowerCase().trim();

    if (!query) return allUsers;

    return allUsers.filter(user => {
      const fullName = `${user.firstName} ${user.lastName}`.toLowerCase();
      const email = user.email.toLowerCase();
      return fullName.includes(query) ||
             user.firstName.toLowerCase().includes(query) ||
             user.lastName.toLowerCase().includes(query) ||
             email.includes(query);
    });
  });

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers(0);
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

    this.adminService.getUsers(page, 20, this.roleFilter || undefined).subscribe({
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
      `Pourquoi suspendre ${user.firstName} ${user.lastName} ?`,
      'Suspension utilisateur',
      { inputLabel: 'Motif de suspension', inputPlaceholder: 'Ex: Comportement inapproprie...', variant: 'warning' }
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

  confirmDeleteUser(user: UserListResponse): void {
    this.userToDelete.set(user);
    this.showDeleteModal.set(true);
  }

  cancelDelete(): void {
    this.showDeleteModal.set(false);
    this.userToDelete.set(null);
  }

  deleteUser(): void {
    const user = this.userToDelete();
    if (!user) return;

    this.actionLoading.set(true);
    this.adminService.deleteUser(user.id).subscribe({
      next: () => {
        this.showDeleteModal.set(false);
        this.userToDelete.set(null);
        this.loadUsers(this.currentPage());
        this.actionLoading.set(false);
        // Notify other admin components to refresh their data
        this.adminStateService.notifyUserChange('delete', user.id);
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.dialogService.alert(err.error?.message || 'Erreur lors de la suppression', 'Erreur', { variant: 'danger' });
      }
    });
  }
}
