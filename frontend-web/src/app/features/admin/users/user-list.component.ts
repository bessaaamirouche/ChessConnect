import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, UserListResponse, Page } from '../../../core/services/admin.service';
import { DialogService } from '../../../core/services/dialog.service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
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
                    @if (user.isSuspended) {
                      <span class="badge badge--error">Suspendu</span>
                    } @else {
                      <span class="badge badge--success">Actif</span>
                    }
                  </td>
                  <td>
                    <div class="actions-cell">
                      @if (user.isSuspended) {
                        <button
                          class="btn btn--sm btn--success"
                          (click)="activateUser(user)"
                          [disabled]="actionLoading()"
                        >
                          Reactiver
                        </button>
                      } @else {
                        <button
                          class="btn btn--sm btn--error"
                          (click)="suspendUser(user)"
                          [disabled]="actionLoading()"
                        >
                          Suspendre
                        </button>
                      }
                      <button
                        class="btn btn--sm btn--danger"
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
        font-weight: 700;
      }

      @media (max-width: 767px) {
        h1 {
          font-size: 1.25rem;
        }
      }
    }

    .filters {
      display: flex;
      gap: var(--space-md);
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
      min-width: 700px;

      th, td {
        padding: var(--space-md);
        text-align: left;
        border-bottom: 1px solid var(--border-subtle);
        vertical-align: middle;
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

    .user-info {
      display: flex;
      flex-direction: column;
      gap: 2px;

      strong {
        color: var(--text-primary);
      }

      .text-muted {
        font-size: 0.8125rem;
        color: var(--text-muted);
      }
    }

    .badge {
      display: inline-block;
      padding: 4px 8px;
      font-size: 0.6875rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      border-radius: var(--radius-sm);

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

      &--success {
        background: var(--success-muted);
        color: var(--success);
      }

      &--error {
        background: var(--error-muted);
        color: var(--error);
      }
    }

    .btn--success {
      background: var(--success);
      color: white;
      border: none;

      &:hover:not(:disabled) {
        opacity: 0.9;
      }
    }

    .btn--error {
      background: var(--error);
      color: white;
      border: none;

      &:hover:not(:disabled) {
        opacity: 0.9;
      }
    }

    .btn--sm {
      padding: 6px 12px;
      font-size: 0.75rem;
    }

    .btn--danger {
      background: #dc2626;
      color: white;
      border: none;

      &:hover:not(:disabled) {
        background: #b91c1c;
      }
    }

    .actions-cell {
      display: flex;
      gap: var(--space-sm);
      align-items: center;
      flex-wrap: nowrap;
    }

    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .modal {
      background: var(--bg-secondary);
      border-radius: var(--radius-lg);
      padding: var(--space-xl);
      max-width: 400px;
      width: 90%;

      h3 {
        margin-bottom: var(--space-md);
        color: var(--text-primary);
      }

      p {
        color: var(--text-secondary);
        margin-bottom: var(--space-sm);
      }

      .warning {
        color: var(--error);
        font-weight: 500;
      }
    }

    .modal-actions {
      display: flex;
      gap: var(--space-md);
      justify-content: flex-end;
      margin-top: var(--space-lg);
    }

    .pagination {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: var(--space-md);
      padding: var(--space-lg);
      color: var(--text-secondary);
      font-size: 0.875rem;
    }

    .loading {
      text-align: center;
      padding: var(--space-2xl);
      color: var(--text-muted);
    }

    .input--sm {
      padding: 8px 12px;
      font-size: 0.875rem;
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
      min-width: 280px;
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
        min-width: 200px;
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
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.dialogService.alert(err.error?.message || 'Erreur lors de la suppression', 'Erreur', { variant: 'danger' });
      }
    });
  }
}
