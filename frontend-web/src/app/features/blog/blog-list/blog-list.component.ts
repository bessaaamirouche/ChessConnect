import { Component, OnInit, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ArticleService } from '../../../core/services/article.service';
import { SeoService } from '../../../core/services/seo.service';
import { StructuredDataService } from '../../../core/services/structured-data.service';
import { ArticleList, ArticlePage, ARTICLE_CATEGORIES } from '../../../core/models/article.model';
import { ScrollRevealDirective, StaggerRevealDirective } from '../../../shared/directives/scroll-reveal.directive';
import { PublicNavbarComponent, NavLink } from '../../../shared/components/public-navbar/public-navbar.component';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroClock, heroArrowRight, heroArrowLeft } from '@ng-icons/heroicons/outline';

@Component({
    selector: 'app-blog-list',
    imports: [RouterLink, NgIconComponent, ScrollRevealDirective, StaggerRevealDirective, TranslateModule, PublicNavbarComponent, FooterComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [provideIcons({ heroClock, heroArrowRight, heroArrowLeft })],
    templateUrl: './blog-list.component.html',
    styleUrl: './blog-list.component.scss'
})
export class BlogListComponent implements OnInit {
  private articleService = inject(ArticleService);
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);
  private translate = inject(TranslateService);

  articles = signal<ArticleList[]>([]);
  categories = signal<string[]>([]);
  selectedCategory = signal<string | null>(null);
  currentPage = signal(0);
  totalPages = signal(0);
  loading = signal(false);

  navLinks: NavLink[] = [
    { route: '/', labelKey: 'nav.home' },
    { route: '/teachers', labelKey: 'teachers.title' },
    { route: '/blog', labelKey: 'blog.title', active: true }
  ];

  categoryLabels = ARTICLE_CATEGORIES;

  ngOnInit(): void {
    this.seoService.updateMetaTags({
      title: 'Blog - mychess | Conseils et stratégies aux échecs',
      description: 'Découvrez nos articles sur les échecs : conseils pour débutants, stratégies avancées, ouvertures, finales et bien plus encore.',
      keywords: 'blog échecs, conseils échecs, stratégie échecs, apprendre échecs'
    });
    this.structuredDataService.setBreadcrumbSchema([
      { name: 'Accueil', url: 'https://mychess.fr/' },
      { name: 'Blog', url: 'https://mychess.fr/blog' }
    ]);

    this.loadArticles();
    this.loadCategories();
  }

  loadArticles(): void {
    this.loading.set(true);

    const category = this.selectedCategory();
    const page = this.currentPage();

    const request = category
      ? this.articleService.getArticlesByCategory(category, page)
      : this.articleService.getArticles(page);

    request.subscribe({
      next: (response: ArticlePage) => {
        this.articles.set(response.content);
        this.totalPages.set(response.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  loadCategories(): void {
    this.articleService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories)
    });
  }

  selectCategory(category: string | null): void {
    this.selectedCategory.set(category);
    this.currentPage.set(0);
    this.loadArticles();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadArticles();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  getCategoryLabel(category: string): string {
    const key = this.categoryLabels[category]?.labelKey;
    return key ? this.translate.instant(key) : category;
  }

  getCategoryColor(category: string): string {
    return this.categoryLabels[category]?.color || '#6b6965';
  }

  formatDate(dateString: string): string {
    return this.articleService.formatDate(dateString);
  }

}
