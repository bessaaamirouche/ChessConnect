import { Component, OnInit, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ArticleService } from '../../../core/services/article.service';
import { SeoService } from '../../../core/services/seo.service';
import { ArticleDetail, ARTICLE_CATEGORIES } from '../../../core/models/article.model';
import { ScrollRevealDirective } from '../../../shared/directives/scroll-reveal.directive';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroClock, heroArrowLeft, heroArrowRight } from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-blog-article',
  standalone: true,
  imports: [RouterLink, DatePipe, NgIconComponent, ScrollRevealDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({ heroClock, heroArrowLeft, heroArrowRight })],
  templateUrl: './blog-article.component.html',
  styleUrl: './blog-article.component.scss'
})
export class BlogArticleComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private articleService = inject(ArticleService);
  private seoService = inject(SeoService);

  article = signal<ArticleDetail | null>(null);
  loading = signal(true);
  error = signal(false);

  categoryLabels = ARTICLE_CATEGORIES;

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (slug) {
      this.loadArticle(slug);
    }
  }

  loadArticle(slug: string): void {
    this.loading.set(true);
    this.error.set(false);

    this.articleService.getArticleBySlug(slug).subscribe({
      next: (article) => {
        this.article.set(article);
        this.loading.set(false);

        // Update SEO meta tags
        this.seoService.updateMetaTags({
          title: `${article.title} - mychess Blog`,
          description: article.metaDescription || article.excerpt,
          keywords: article.metaKeywords,
          image: article.coverImage,
          publishedTime: article.publishedAt,
          author: article.author
        });

        // Add structured data for SEO
        this.seoService.setArticleStructuredData({
          title: article.title,
          description: article.metaDescription || article.excerpt,
          author: article.author,
          publishedAt: article.publishedAt,
          image: article.coverImage,
          slug: article.slug
        });
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      }
    });
  }

  getCategoryLabel(category: string): string {
    return this.categoryLabels[category]?.label || category;
  }

  getCategoryColor(category: string): string {
    return this.categoryLabels[category]?.color || '#6b6965';
  }

  formatDate(dateString: string): string {
    return this.articleService.formatDate(dateString);
  }

  shareOnTwitter(): void {
    const article = this.article();
    if (article) {
      const url = encodeURIComponent(window.location.href);
      const text = encodeURIComponent(article.title);
      window.open(`https://twitter.com/intent/tweet?text=${text}&url=${url}`, '_blank');
    }
  }

  shareOnLinkedIn(): void {
    const url = encodeURIComponent(window.location.href);
    window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${url}`, '_blank');
  }

  shareOnFacebook(): void {
    const url = encodeURIComponent(window.location.href);
    window.open(`https://www.facebook.com/sharer/sharer.php?u=${url}`, '_blank');
  }
}
