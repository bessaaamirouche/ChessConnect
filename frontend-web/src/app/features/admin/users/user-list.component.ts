import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, UserListResponse, Page } from '../../../core/services/admin.service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="user-list">
      <header class="page-header">
        <h1>Utilisateurs</h1>
        <div class="filters">
          <select [(ngModel)]="roleFilter" (change)="loadUsers(0)" class="input input--sm">
            <option value="">Tous les roles</option>
            <option value="STUDENT">Eleves</option>
            <option value="TEACHER">Professeurs</option>
            <option value="ADMIN">Admins</option>
          </select>
        </div>
      </header>

      @if (loading()) {
        <div class="loading">Chargement...</div>
      } @else {
        <div class="table-container">
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
              @for (user of users()?.content || []; track user.id) {
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
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--space-lg);

      h1 {
        font-size: 1.5rem;
        font-weight: 700;
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
      overflow: hidden;
    }

    .table {
      width: 100%;
      border-collapse: collapse;

      th, td {
        padding: var(--space-md);
        text-align: left;
        border-bottom: 1px solid var(--border-subtle);
      }

      th {
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--text-muted);
        background: var(--bg-tertiary);
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
  `]
})
export class UserListComponent implements OnInit {
  users = signal<Page<UserListResponse> | null>(null);
  loading = signal(true);
  actionLoading = signal(false);
  currentPage = signal(0);
  roleFilter = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers(0);
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
      STUDENT: 'Eleve',
      TEACHER: 'Professeur',
      ADMIN: 'Admin'
    };
    return labels[role] || role;
  }

  suspendUser(user: UserListResponse): void {
    const reason = prompt('Motif de suspension:');
    if (!reason) return;

    this.actionLoading.set(true);
    this.adminService.suspendUser(user.id, reason).subscribe({
      next: () => {
        this.loadUsers(this.currentPage());
        this.actionLoading.set(false);
      },
      error: () => {
        this.actionLoading.set(false);
        alert('Erreur lors de la suspension');
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
        alert('Erreur lors de la reactivation');
      }
    });
  }
}
