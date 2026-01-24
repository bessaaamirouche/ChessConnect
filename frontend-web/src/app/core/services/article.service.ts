import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { ArticleList, ArticleDetail, ArticlePage, ArticleCreateRequest } from '../models/article.model';

@Injectable({
  providedIn: 'root'
})
export class ArticleService {
  private http = inject(HttpClient);

  // Signals for caching
  private articlesCache = signal<ArticleList[]>([]);
  private categoriesCache = signal<string[]>([]);

  readonly articles = this.articlesCache.asReadonly();
  readonly categories = this.categoriesCache.asReadonly();

  /**
   * Get paginated list of published articles
   */
  getArticles(page = 0, size = 10): Observable<ArticlePage> {
    return this.http.get<ArticlePage>(`/api/articles?page=${page}&size=${size}`);
  }

  /**
   * Get articles by category
   */
  getArticlesByCategory(category: string, page = 0, size = 10): Observable<ArticlePage> {
    return this.http.get<ArticlePage>(`/api/articles/category/${category}?page=${page}&size=${size}`);
  }

  /**
   * Get latest articles (for homepage)
   */
  getLatestArticles(): Observable<ArticleList[]> {
    return this.http.get<ArticleList[]>('/api/articles/latest').pipe(
      tap(articles => this.articlesCache.set(articles)),
      catchError(() => of([]))
    );
  }

  /**
   * Get article by slug
   */
  getArticleBySlug(slug: string): Observable<ArticleDetail> {
    return this.http.get<ArticleDetail>(`/api/articles/${slug}`);
  }

  /**
   * Get all categories
   */
  getCategories(): Observable<string[]> {
    return this.http.get<string[]>('/api/articles/categories').pipe(
      tap(categories => this.categoriesCache.set(categories)),
      catchError(() => of([]))
    );
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  }

  // ===== Admin Methods =====

  /**
   * Get all articles including unpublished (admin only)
   */
  getAllArticles(page = 0, size = 20): Observable<ArticlePage> {
    return this.http.get<ArticlePage>(`/api/articles/admin?page=${page}&size=${size}`);
  }

  /**
   * Get article by ID (admin only)
   */
  getArticleById(id: number): Observable<ArticleDetail> {
    return this.http.get<ArticleDetail>(`/api/articles/id/${id}`);
  }

  /**
   * Create a new article (admin only)
   */
  createArticle(article: ArticleCreateRequest): Observable<ArticleDetail> {
    return this.http.post<ArticleDetail>('/api/articles', article);
  }

  /**
   * Update an article (admin only)
   */
  updateArticle(id: number, article: ArticleCreateRequest): Observable<ArticleDetail> {
    return this.http.put<ArticleDetail>(`/api/articles/${id}`, article);
  }

  /**
   * Delete an article (admin only)
   */
  deleteArticle(id: number): Observable<void> {
    return this.http.delete<void>(`/api/articles/${id}`);
  }

  /**
   * Publish an article (admin only)
   */
  publishArticle(id: number): Observable<ArticleDetail> {
    return this.http.patch<ArticleDetail>(`/api/articles/${id}/publish`, {});
  }

  /**
   * Unpublish an article (admin only)
   */
  unpublishArticle(id: number): Observable<ArticleDetail> {
    return this.http.patch<ArticleDetail>(`/api/articles/${id}/unpublish`, {});
  }
}
