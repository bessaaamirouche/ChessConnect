import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ArticleService } from '../../../core/services/article.service';
import { ArticleList, ArticleDetail, ArticlePage, ArticleCreateRequest, ARTICLE_CATEGORIES } from '../../../core/models/article.model';
import { DialogService } from '../../../core/services/dialog.service';

@Component({
  selector: 'app-admin-articles',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="articles-page">
      <header class="page-header">
        <h1>Blog</h1>
        <button class="btn btn--primary" (click)="openCreateModal()">
          + Nouvel article
        </button>
      </header>

      @if (loading()) {
        <div class="loading">Chargement...</div>
      } @else {
        <div class="table-container">
          <div class="table-header">
            <span class="results-count">{{ articles()?.totalElements || 0 }} article(s)</span>
          </div>
          <table class="table">
            <thead>
              <tr>
                <th>Titre</th>
                <th>Categorie</th>
                <th>Auteur</th>
                <th>Date</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (article of articles()?.content || []; track article.id) {
                <tr>
                  <td>
                    <div class="article-info">
                      <strong>{{ article.title }}</strong>
                      <span class="text-muted">{{ article.slug }}</span>
                    </div>
                  </td>
                  <td>
                    <span class="category-badge" [style.background]="getCategoryColor(article.category)">
                      {{ getCategoryLabel(article.category) }}
                    </span>
                  </td>
                  <td>{{ article.author }}</td>
                  <td>
                    @if (article.publishedAt) {
                      {{ article.publishedAt | date:'dd/MM/yyyy' }}
                    } @else {
                      <span class="text-muted">-</span>
                    }
                  </td>
                  <td>
                    @if (article.published) {
                      <span class="badge badge--published">Publie</span>
                    } @else {
                      <span class="badge badge--draft">Brouillon</span>
                    }
                  </td>
                  <td>
                    <div class="actions-cell">
                      <button
                        class="action-btn action-btn--edit"
                        (click)="openEditModal(article)"
                        [disabled]="actionLoading()"
                      >
                        Modifier
                      </button>
                      @if (article.published) {
                        <button
                          class="action-btn action-btn--unpublish"
                          (click)="unpublishArticle(article)"
                          [disabled]="actionLoading()"
                        >
                          Depublier
                        </button>
                      } @else {
                        <button
                          class="action-btn action-btn--publish"
                          (click)="publishArticle(article)"
                          [disabled]="actionLoading()"
                        >
                          Publier
                        </button>
                      }
                      <button
                        class="action-btn action-btn--delete"
                        (click)="confirmDelete(article)"
                        [disabled]="actionLoading()"
                      >
                        Supprimer
                      </button>
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="6" class="empty-state">Aucun article</td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        @if (articles() && articles()!.totalPages > 1) {
          <div class="pagination">
            <button
              class="btn btn--sm btn--ghost"
              [disabled]="currentPage() === 0"
              (click)="loadArticles(currentPage() - 1)"
            >
              Precedent
            </button>
            <span>Page {{ currentPage() + 1 }} / {{ articles()!.totalPages }}</span>
            <button
              class="btn btn--sm btn--ghost"
              [disabled]="currentPage() >= articles()!.totalPages - 1"
              (click)="loadArticles(currentPage() + 1)"
            >
              Suivant
            </button>
          </div>
        }
      }

      <!-- Create/Edit Modal -->
      @if (showModal()) {
        <div class="modal-overlay" (click)="closeModal()">
          <div class="modal modal--large" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>{{ editingArticle() ? 'Modifier l\\'article' : 'Nouvel article' }}</h3>
              <button class="modal-close" (click)="closeModal()">&times;</button>
            </div>
            <div class="modal-body">
              <div class="form-row">
                <div class="form-group">
                  <label>Titre *</label>
                  <input type="text" [(ngModel)]="formData.title" class="input" placeholder="Titre de l'article">
                </div>
                <div class="form-group">
                  <label>Categorie *</label>
                  <select [(ngModel)]="formData.category" class="input">
                    @for (cat of categoryKeys; track cat) {
                      <option [value]="cat">{{ categories[cat].label }}</option>
                    }
                  </select>
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>Auteur *</label>
                  <input type="text" [(ngModel)]="formData.author" class="input" placeholder="mychess">
                </div>
                <div class="form-group">
                  <label>Image de couverture (URL)</label>
                  <input type="text" [(ngModel)]="formData.coverImage" class="input" placeholder="https://...">
                </div>
              </div>

              <div class="form-group">
                <label>Meta description (SEO)</label>
                <input type="text" [(ngModel)]="formData.metaDescription" class="input" placeholder="Description pour les moteurs de recherche">
              </div>

              <div class="form-group">
                <label>Meta keywords (SEO)</label>
                <input type="text" [(ngModel)]="formData.metaKeywords" class="input" placeholder="mot-cle1, mot-cle2, mot-cle3">
              </div>

              <div class="form-group">
                <label>Extrait</label>
                <textarea [(ngModel)]="formData.excerpt" class="input textarea" rows="2" placeholder="Resume court de l'article"></textarea>
              </div>

              <div class="form-group">
                <label>Contenu (Markdown) *</label>
                <textarea [(ngModel)]="formData.content" class="input textarea textarea--large" rows="15" placeholder="## Titre de section

Votre contenu en Markdown..."></textarea>
              </div>

              <div class="form-group">
                <label class="checkbox-label">
                  <input type="checkbox" [(ngModel)]="formData.published">
                  <span>Publier immediatement</span>
                </label>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn--ghost" (click)="closeModal()">Annuler</button>
              <button
                class="btn btn--primary"
                (click)="saveArticle()"
                [disabled]="saving() || !isFormValid()"
              >
                @if (saving()) {
                  Enregistrement...
                } @else {
                  {{ editingArticle() ? 'Enregistrer' : 'Creer' }}
                }
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Delete Modal -->
      @if (showDeleteModal()) {
        <div class="modal-overlay" (click)="cancelDelete()">
          <div class="modal" (click)="$event.stopPropagation()">
            <h3>Confirmer la suppression</h3>
            <p>Voulez-vous vraiment supprimer l'article <strong>{{ articleToDelete()?.title }}</strong> ?</p>
            <p class="warning">Cette action est irreversible.</p>
            <div class="modal-actions">
              <button class="btn btn--ghost" (click)="cancelDelete()">Non, annuler</button>
              <button class="btn btn--danger" (click)="deleteArticle()" [disabled]="actionLoading()">
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
    }

    .table-container {
      background: var(--bg-secondary);
      border-radius: 0.75rem;
      overflow: hidden;
      box-shadow: 0 0.0625rem 0.1875rem rgba(0, 0, 0, 0.1);
      overflow-x: auto;
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

    .table {
      width: 100%;
      border-collapse: collapse;
      min-width: 50rem;

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

      tbody tr:hover {
        background: rgba(128, 128, 128, 0.04);
      }
    }

    .article-info {
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
    }

    .category-badge {
      display: inline-flex;
      padding: 0.25rem 0.625rem;
      font-size: 0.6875rem;
      font-weight: 500;
      border-radius: 0.375rem;
      color: white;
    }

    .badge {
      display: inline-flex;
      padding: 0.25rem 0.625rem;
      font-size: 0.6875rem;
      font-weight: 500;
      border-radius: 0.375rem;

      &--published {
        background: rgba(34, 197, 94, 0.1);
        color: #22c55e;
      }

      &--draft {
        background: rgba(128, 128, 128, 0.1);
        color: var(--text-muted);
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

      &--edit {
        background: rgba(59, 130, 246, 0.1);
        color: #3b82f6;

        &:hover:not(:disabled) {
          background: rgba(59, 130, 246, 0.2);
        }
      }

      &--publish {
        background: rgba(34, 197, 94, 0.1);
        color: #22c55e;

        &:hover:not(:disabled) {
          background: rgba(34, 197, 94, 0.2);
        }
      }

      &--unpublish {
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

    .empty-state {
      text-align: center;
      padding: var(--space-2xl);
      color: var(--text-muted);
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
      max-height: 90vh;
      overflow-y: auto;

      &--large {
        max-width: 50rem;
      }

      h3 {
        margin-bottom: var(--space-md);
        color: var(--text-primary);
        font-weight: 600;
        font-size: 1.125rem;
      }

      p {
        color: var(--text-secondary);
        margin-bottom: var(--space-sm);
      }

      .warning {
        color: #ef4444;
        font-weight: 500;
        font-size: 0.875rem;
      }
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--space-lg);
      padding-bottom: var(--space-md);
      border-bottom: 1px solid var(--border-subtle);
    }

    .modal-close {
      background: none;
      border: none;
      font-size: 1.5rem;
      color: var(--text-muted);
      cursor: pointer;

      &:hover {
        color: var(--text-primary);
      }
    }

    .modal-body {
      margin-bottom: var(--space-lg);
    }

    .modal-footer {
      display: flex;
      gap: var(--space-md);
      justify-content: flex-end;
      padding-top: var(--space-md);
      border-top: 1px solid var(--border-subtle);
    }

    .modal-actions {
      display: flex;
      gap: var(--space-md);
      justify-content: flex-end;
      margin-top: var(--space-lg);
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: var(--space-md);

      @media (max-width: 40rem) {
        grid-template-columns: 1fr;
      }
    }

    .form-group {
      margin-bottom: var(--space-md);

      label {
        display: block;
        font-size: 0.875rem;
        font-weight: 500;
        color: var(--text-secondary);
        margin-bottom: 0.375rem;
      }
    }

    .input {
      width: 100%;
      padding: 0.625rem 0.875rem;
      font-size: 0.875rem;
      border: 1px solid var(--border-subtle);
      border-radius: 0.5rem;
      background: var(--bg-tertiary);
      color: var(--text-primary);
      transition: border-color 0.15s ease;

      &:focus {
        outline: none;
        border-color: var(--gold-500);
      }

      &::placeholder {
        color: var(--text-muted);
      }
    }

    .textarea {
      resize: vertical;
      min-height: 5rem;
      font-family: inherit;

      &--large {
        min-height: 20rem;
        font-family: monospace;
        font-size: 0.8125rem;
        line-height: 1.5;
      }
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      cursor: pointer;

      input[type="checkbox"] {
        width: 1rem;
        height: 1rem;
        cursor: pointer;
      }

      span {
        font-size: 0.875rem;
        color: var(--text-secondary);
      }
    }

    .btn {
      padding: 0.5rem 1rem;
      font-size: 0.875rem;
      font-weight: 500;
      border-radius: 0.5rem;
      cursor: pointer;
      transition: all 0.15s ease;
      border: none;

      &--primary {
        background: var(--gold-500);
        color: var(--bg-primary);

        &:hover:not(:disabled) {
          background: var(--gold-400);
        }

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      }

      &--ghost {
        background: transparent;
        color: var(--text-secondary);
        border: 1px solid var(--border-subtle);

        &:hover {
          background: var(--bg-tertiary);
        }
      }

      &--danger {
        background: #ef4444;
        color: white;

        &:hover:not(:disabled) {
          background: #dc2626;
        }

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      }

      &--sm {
        padding: 0.375rem 0.75rem;
        font-size: 0.8125rem;
      }
    }
  `]
})
export class AdminArticlesComponent implements OnInit {
  private articleService = inject(ArticleService);
  private dialogService = inject(DialogService);

  articles = signal<ArticlePage | null>(null);
  loading = signal(true);
  actionLoading = signal(false);
  currentPage = signal(0);
  showModal = signal(false);
  showDeleteModal = signal(false);
  editingArticle = signal<ArticleList | null>(null);
  articleToDelete = signal<ArticleList | null>(null);
  saving = signal(false);

  categories = ARTICLE_CATEGORIES;
  categoryKeys = Object.keys(ARTICLE_CATEGORIES);

  formData: ArticleCreateRequest = {
    title: '',
    metaDescription: '',
    metaKeywords: '',
    excerpt: '',
    content: '',
    coverImage: '',
    author: 'mychess',
    category: 'conseils',
    published: false
  };

  ngOnInit(): void {
    this.loadArticles(0);
  }

  loadArticles(page: number): void {
    this.loading.set(true);
    this.currentPage.set(page);

    this.articleService.getAllArticles(page, 20).subscribe({
      next: (articles) => {
        this.articles.set(articles);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  getCategoryLabel(category: string): string {
    return this.categories[category]?.label || category;
  }

  getCategoryColor(category: string): string {
    return this.categories[category]?.color || '#6b7280';
  }

  openCreateModal(): void {
    this.editingArticle.set(null);
    this.formData = {
      title: '',
      metaDescription: '',
      metaKeywords: '',
      excerpt: '',
      content: '',
      coverImage: '',
      author: 'mychess',
      category: 'conseils',
      published: false
    };
    this.showModal.set(true);
  }

  openEditModal(article: ArticleList): void {
    this.editingArticle.set(article);
    this.articleService.getArticleById(article.id).subscribe({
      next: (detail) => {
        this.formData = {
          title: detail.title,
          metaDescription: detail.metaDescription || '',
          metaKeywords: detail.metaKeywords || '',
          excerpt: detail.excerpt || '',
          content: detail.content,
          coverImage: detail.coverImage || '',
          author: detail.author,
          category: detail.category,
          published: detail.published
        };
        this.showModal.set(true);
      }
    });
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingArticle.set(null);
  }

  isFormValid(): boolean {
    return !!(this.formData.title && this.formData.content && this.formData.author && this.formData.category);
  }

  saveArticle(): void {
    if (!this.isFormValid()) return;

    this.saving.set(true);
    const editing = this.editingArticle();

    const obs = editing
      ? this.articleService.updateArticle(editing.id, this.formData)
      : this.articleService.createArticle(this.formData);

    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeModal();
        this.loadArticles(this.currentPage());
      },
      error: () => {
        this.saving.set(false);
        this.dialogService.alert('Erreur lors de l\'enregistrement', 'Erreur', { variant: 'danger' });
      }
    });
  }

  publishArticle(article: ArticleList): void {
    this.actionLoading.set(true);
    this.articleService.publishArticle(article.id).subscribe({
      next: () => {
        this.loadArticles(this.currentPage());
        this.actionLoading.set(false);
      },
      error: () => {
        this.actionLoading.set(false);
      }
    });
  }

  unpublishArticle(article: ArticleList): void {
    this.actionLoading.set(true);
    this.articleService.unpublishArticle(article.id).subscribe({
      next: () => {
        this.loadArticles(this.currentPage());
        this.actionLoading.set(false);
      },
      error: () => {
        this.actionLoading.set(false);
      }
    });
  }

  confirmDelete(article: ArticleList): void {
    this.articleToDelete.set(article);
    this.showDeleteModal.set(true);
  }

  cancelDelete(): void {
    this.showDeleteModal.set(false);
    this.articleToDelete.set(null);
  }

  deleteArticle(): void {
    const article = this.articleToDelete();
    if (!article) return;

    this.actionLoading.set(true);
    this.articleService.deleteArticle(article.id).subscribe({
      next: () => {
        this.showDeleteModal.set(false);
        this.articleToDelete.set(null);
        this.loadArticles(this.currentPage());
        this.actionLoading.set(false);
      },
      error: () => {
        this.actionLoading.set(false);
        this.dialogService.alert('Erreur lors de la suppression', 'Erreur', { variant: 'danger' });
      }
    });
  }
}
