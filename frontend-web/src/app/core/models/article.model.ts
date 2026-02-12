export interface ArticleList {
  id: number;
  title: string;
  slug: string;
  excerpt: string;
  coverImage: string;
  author: string;
  publishedAt: string;
  category: string;
  readingTimeMinutes: number;
  published?: boolean;
}

export interface ArticleCreateRequest {
  title: string;
  metaDescription: string;
  metaKeywords: string;
  excerpt: string;
  content: string;
  coverImage: string;
  author: string;
  category: string;
  published: boolean;
}

export interface ArticleDetail {
  id: number;
  title: string;
  slug: string;
  metaDescription: string;
  metaKeywords: string;
  excerpt: string;
  content: string;
  coverImage: string;
  author: string;
  createdAt: string;
  publishedAt: string;
  updatedAt: string;
  published: boolean;
  category: string;
  readingTimeMinutes: number;
  relatedArticles: ArticleList[];
}

export interface ArticlePage {
  content: ArticleList[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export const ARTICLE_CATEGORIES: Record<string, { labelKey: string; color: string }> = {
  debutant: { labelKey: 'blog.categories.beginner', color: '#78716c' },
  strategie: { labelKey: 'blog.categories.strategy', color: '#3b82f6' },
  progression: { labelKey: 'blog.categories.progression', color: '#22c55e' },
  conseils: { labelKey: 'blog.categories.advice', color: '#a855f7' },
  enfants: { labelKey: 'blog.categories.children', color: '#f59e0b' },
  coach: { labelKey: 'blog.categories.coach', color: '#ec4899' }
};
