import { Injectable, inject } from '@angular/core';
import { Title, Meta } from '@angular/platform-browser';
import { DOCUMENT } from '@angular/common';

export interface Teacher {
  id: number;
  uuid?: string;
  firstName: string;
  lastName: string;
  bio?: string;
  hourlyRateCents?: number;
}

@Injectable({
  providedIn: 'root'
})
export class SeoService {
  private title = inject(Title);
  private meta = inject(Meta);
  private document = inject(DOCUMENT);

  private readonly baseUrl = 'https://mychess.fr';
  private readonly siteName = 'mychess';

  setHomePage(): void {
    const pageTitle = 'mychess - Cours d\'Échecs en Ligne avec les Meilleurs Coachs';
    const description = 'Apprenez les échecs avec des coachs qualifiés. Cours particuliers en visioconférence, progression du niveau Pion au niveau Dame. Réservez votre première leçon.';

    this.updateTags({
      title: pageTitle,
      description,
      url: this.baseUrl,
      type: 'website'
    });
  }

  setTeachersListPage(): void {
    const pageTitle = 'Nos Coachs d\'Échecs - mychess';
    const description = 'Découvrez nos coachs d\'échecs qualifiés. Trouvez le coach idéal pour progresser aux échecs avec des cours particuliers en ligne.';

    this.updateTags({
      title: pageTitle,
      description,
      url: `${this.baseUrl}/coaches`,
      type: 'website'
    });
  }

  setTeacherProfilePage(teacher: Teacher): void {
    const fullName = `${teacher.firstName} ${teacher.lastName}`;
    const pageTitle = `${fullName} - Coach d'Échecs | mychess`;
    const description = teacher.bio
      ? teacher.bio.substring(0, 155) + (teacher.bio.length > 155 ? '...' : '')
      : `Réservez un cours d'échecs avec ${fullName}. Coach qualifié disponible pour des cours particuliers en visioconférence.`;

    // Use UUID for public coach URLs
    const url = teacher.uuid
      ? `${this.baseUrl}/coaches/${teacher.uuid}`
      : `${this.baseUrl}/teachers/${teacher.id}`;

    this.updateTags({
      title: pageTitle,
      description,
      url,
      type: 'profile'
    });
  }

  setRegisterPage(): void {
    const pageTitle = 'Inscription - mychess';
    const description = 'Créez votre compte mychess. Inscrivez-vous comme joueur ou coach d\'échecs et commencez votre parcours d\'apprentissage.';

    this.updateTags({
      title: pageTitle,
      description,
      url: `${this.baseUrl}/register`,
      type: 'website'
    });
  }

  setLoginPage(): void {
    const pageTitle = 'Connexion - mychess';
    const description = 'Connectez-vous à votre compte mychess pour accéder à vos cours d\'échecs.';

    this.updateTags({
      title: pageTitle,
      description,
      url: `${this.baseUrl}/login`,
      type: 'website',
      noIndex: true
    });
  }

  private updateTags(config: {
    title: string;
    description: string;
    url: string;
    type: string;
    noIndex?: boolean;
  }): void {
    // Update title
    this.title.setTitle(config.title);

    // Update meta description
    this.meta.updateTag({ name: 'description', content: config.description });

    // Update robots
    if (config.noIndex) {
      this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
    } else {
      this.meta.updateTag({ name: 'robots', content: 'index, follow' });
    }

    // Update canonical link
    this.updateCanonicalUrl(config.url);

    // Update Open Graph tags
    this.meta.updateTag({ property: 'og:title', content: config.title });
    this.meta.updateTag({ property: 'og:description', content: config.description });
    this.meta.updateTag({ property: 'og:url', content: config.url });
    this.meta.updateTag({ property: 'og:type', content: config.type });
    this.meta.updateTag({ property: 'og:site_name', content: this.siteName });
    this.meta.updateTag({ property: 'og:locale', content: 'fr_FR' });

    // Update Twitter Card tags
    this.meta.updateTag({ name: 'twitter:title', content: config.title });
    this.meta.updateTag({ name: 'twitter:description', content: config.description });
    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
  }

  private updateCanonicalUrl(url: string): void {
    let link: HTMLLinkElement | null = this.document.querySelector('link[rel="canonical"]');

    if (link) {
      link.setAttribute('href', url);
    } else {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      link.setAttribute('href', url);
      this.document.head.appendChild(link);
    }
  }

  /**
   * Update meta tags for articles (used by blog components)
   */
  updateMetaTags(config: {
    title: string;
    description: string;
    keywords?: string;
    image?: string;
    url?: string;
    type?: string;
    publishedTime?: string;
    author?: string;
  }): void {
    // Update title
    this.title.setTitle(config.title);

    // Update meta description
    this.meta.updateTag({ name: 'description', content: config.description });

    // Update keywords if provided
    if (config.keywords) {
      this.meta.updateTag({ name: 'keywords', content: config.keywords });
    }

    // Update robots
    this.meta.updateTag({ name: 'robots', content: 'index, follow' });

    // Update canonical link
    const url = config.url || `${this.baseUrl}${typeof window !== 'undefined' ? window.location.pathname : ''}`;
    this.updateCanonicalUrl(url);

    // Update Open Graph tags
    this.meta.updateTag({ property: 'og:title', content: config.title });
    this.meta.updateTag({ property: 'og:description', content: config.description });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:type', content: config.type || 'article' });
    this.meta.updateTag({ property: 'og:site_name', content: this.siteName });
    this.meta.updateTag({ property: 'og:locale', content: 'fr_FR' });

    if (config.image) {
      this.meta.updateTag({ property: 'og:image', content: config.image });
    }

    if (config.publishedTime) {
      this.meta.updateTag({ property: 'article:published_time', content: config.publishedTime });
    }

    if (config.author) {
      this.meta.updateTag({ property: 'article:author', content: config.author });
    }

    // Update Twitter Card tags
    this.meta.updateTag({ name: 'twitter:card', content: config.image ? 'summary_large_image' : 'summary' });
    this.meta.updateTag({ name: 'twitter:title', content: config.title });
    this.meta.updateTag({ name: 'twitter:description', content: config.description });

    if (config.image) {
      this.meta.updateTag({ name: 'twitter:image', content: config.image });
    }
  }

  /**
   * Set blog list page SEO
   */
  setBlogListPage(): void {
    const pageTitle = 'Blog Échecs - Conseils et Stratégies | mychess';
    const description = 'Découvrez nos articles sur les échecs : stratégies, ouvertures, conseils pour progresser. Apprenez des meilleurs coachs d\'échecs en ligne.';

    this.updateTags({
      title: pageTitle,
      description,
      url: `${this.baseUrl}/blog`,
      type: 'website'
    });
  }

  // === Internal pages (noIndex) ===

  setDashboardPage(): void {
    this.title.setTitle('Tableau de bord - mychess');
    this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
  }

  setLessonsPage(): void {
    this.title.setTitle('Mes cours - mychess');
    this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
  }

  setProgressPage(): void {
    this.title.setTitle('Ma progression - mychess');
    this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
  }

  setQuizPage(): void {
    this.title.setTitle('Quiz d\'évaluation - mychess');
    this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
  }

  setSubscriptionPage(): void {
    this.title.setTitle('Mon abonnement - mychess');
    this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
  }

  setSettingsPage(): void {
    this.title.setTitle('Paramètres - mychess');
    this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
  }

  setAvailabilityPage(): void {
    this.title.setTitle('Mes disponibilités - mychess');
    this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
  }

  setBookLessonPage(teacherName?: string): void {
    const title = teacherName
      ? `Réserver avec ${teacherName} - mychess`
      : 'Réserver un cours - mychess';
    this.title.setTitle(title);
    this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
  }

  /**
   * Add JSON-LD structured data for articles
   */
  setArticleStructuredData(article: {
    title: string;
    description: string;
    author: string;
    publishedAt: string;
    image?: string;
    slug: string;
  }): void {
    // Remove existing script if any
    const existingScript = this.document.querySelector('script[type="application/ld+json"]#article-schema');
    if (existingScript) {
      existingScript.remove();
    }

    const structuredData = {
      '@context': 'https://schema.org',
      '@type': 'Article',
      'headline': article.title,
      'description': article.description,
      'author': {
        '@type': 'Person',
        'name': article.author
      },
      'publisher': {
        '@type': 'Organization',
        'name': 'mychess',
        'logo': {
          '@type': 'ImageObject',
          'url': `${this.baseUrl}/assets/logo.png`
        }
      },
      'datePublished': article.publishedAt,
      'mainEntityOfPage': {
        '@type': 'WebPage',
        '@id': `${this.baseUrl}/blog/${article.slug}`
      }
    };

    if (article.image) {
      (structuredData as any)['image'] = article.image;
    }

    const script = this.document.createElement('script');
    script.type = 'application/ld+json';
    script.id = 'article-schema';
    script.text = JSON.stringify(structuredData);
    this.document.head.appendChild(script);
  }

  /**
   * Set organization structured data (for homepage)
   */
  setOrganizationStructuredData(): void {
    // Remove existing script if any
    const existingScript = this.document.querySelector('script[type="application/ld+json"]#org-schema');
    if (existingScript) {
      existingScript.remove();
    }

    const structuredData = {
      '@context': 'https://schema.org',
      '@type': 'Organization',
      'name': 'mychess',
      'url': this.baseUrl,
      'logo': `${this.baseUrl}/assets/logo.png`,
      'description': 'Plateforme de cours d\'échecs en ligne avec des coachs qualifiés',
      'sameAs': [],
      'contactPoint': {
        '@type': 'ContactPoint',
        'contactType': 'customer service',
        'availableLanguage': 'French'
      }
    };

    const script = this.document.createElement('script');
    script.type = 'application/ld+json';
    script.id = 'org-schema';
    script.text = JSON.stringify(structuredData);
    this.document.head.appendChild(script);
  }
}
