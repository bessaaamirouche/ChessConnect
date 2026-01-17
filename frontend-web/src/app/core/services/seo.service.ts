import { Injectable, inject } from '@angular/core';
import { Title, Meta } from '@angular/platform-browser';
import { DOCUMENT } from '@angular/common';

export interface Teacher {
  id: number;
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
  private readonly siteName = 'ChessConnect';

  setHomePage(): void {
    const pageTitle = 'ChessConnect - Cours d\'Échecs en Ligne avec les Meilleurs Professeurs';
    const description = 'Apprenez les échecs avec des professeurs qualifiés. Cours particuliers en visioconférence, progression du niveau Pion au niveau Dame. Réservez votre première leçon.';

    this.updateTags({
      title: pageTitle,
      description,
      url: this.baseUrl,
      type: 'website'
    });
  }

  setTeachersListPage(): void {
    const pageTitle = 'Nos Professeurs d\'Échecs - ChessConnect';
    const description = 'Découvrez nos professeurs d\'échecs qualifiés. Trouvez le professeur idéal pour progresser aux échecs avec des cours particuliers en ligne.';

    this.updateTags({
      title: pageTitle,
      description,
      url: `${this.baseUrl}/teachers`,
      type: 'website'
    });
  }

  setTeacherProfilePage(teacher: Teacher): void {
    const fullName = `${teacher.firstName} ${teacher.lastName}`;
    const pageTitle = `${fullName} - Professeur d'Échecs | ChessConnect`;
    const description = teacher.bio
      ? teacher.bio.substring(0, 155) + (teacher.bio.length > 155 ? '...' : '')
      : `Réservez un cours d'échecs avec ${fullName}. Professeur qualifié disponible pour des cours particuliers en visioconférence.`;

    this.updateTags({
      title: pageTitle,
      description,
      url: `${this.baseUrl}/teachers/${teacher.id}`,
      type: 'profile'
    });
  }

  setRegisterPage(): void {
    const pageTitle = 'Inscription - ChessConnect';
    const description = 'Créez votre compte ChessConnect. Inscrivez-vous comme élève ou professeur d\'échecs et commencez votre parcours d\'apprentissage.';

    this.updateTags({
      title: pageTitle,
      description,
      url: `${this.baseUrl}/register`,
      type: 'website'
    });
  }

  setLoginPage(): void {
    const pageTitle = 'Connexion - ChessConnect';
    const description = 'Connectez-vous à votre compte ChessConnect pour accéder à vos cours d\'échecs.';

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
}
