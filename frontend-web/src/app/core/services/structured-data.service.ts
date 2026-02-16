import { Injectable, inject, DOCUMENT } from '@angular/core';


export interface Teacher {
  id: number;
  uuid?: string;
  firstName: string;
  lastName: string;
  bio?: string;
  hourlyRateCents?: number;
  avatarUrl?: string;
  averageRating?: number;
  reviewCount?: number;
}

@Injectable({
  providedIn: 'root'
})
export class StructuredDataService {
  private document = inject(DOCUMENT);
  private readonly baseUrl = 'https://mychess.fr';

  setOrganizationSchema(): void {
    const schema = {
      '@context': 'https://schema.org',
      '@type': ['Organization', 'EducationalOrganization'],
      name: 'mychess',
      alternateName: 'MyChess',
      url: this.baseUrl,
      logo: `${this.baseUrl}/assets/logo.png`,
      description: 'Plateforme française de cours d\'échecs en ligne avec des coachs certifiés. Progression personnalisée du niveau Pion au niveau Roi. Premier cours gratuit.',
      foundingDate: '2024',
      areaServed: { '@type': 'Country', name: 'France' },
      availableLanguage: ['fr', 'en'],
      knowsAbout: ['Échecs', 'Chess', 'Cours d\'échecs en ligne', 'Coaching échecs'],
      contactPoint: {
        '@type': 'ContactPoint',
        contactType: 'customer service',
        email: 'support@mychess.fr',
        availableLanguage: ['French', 'English']
      }
    };

    this.insertSchema(schema, 'organization-schema');
  }

  setEducationalOrganizationSchema(): void {
    const schema = {
      '@context': 'https://schema.org',
      '@type': 'EducationalOrganization',
      name: 'mychess',
      url: this.baseUrl,
      description: 'Plateforme de cours d\'échecs en ligne avec 120 cours structurés et 546 leçons. 4 niveaux de progression : Pion, Cavalier, Reine, Roi.',
      hasCredential: {
        '@type': 'EducationalOccupationalCredential',
        credentialCategory: 'Niveau d\'échecs',
        name: 'Certification mychess (Pion à Roi)'
      }
    };

    this.insertSchema(schema, 'edu-org-schema');
  }

  setTeacherSchema(teacher: Teacher): void {
    // Use UUID for public coach URLs
    const teacherUrl = teacher.uuid
      ? `${this.baseUrl}/coaches/${teacher.uuid}`
      : `${this.baseUrl}/teachers/${teacher.id}`;

    const schema: Record<string, unknown> = {
      '@context': 'https://schema.org',
      '@type': 'Person',
      name: `${teacher.firstName} ${teacher.lastName}`,
      url: teacherUrl,
      jobTitle: 'Coach d\'échecs',
      description: teacher.bio || `Coach d'échecs disponible pour des cours particuliers en visioconférence.`,
      worksFor: {
        '@type': 'Organization',
        name: 'mychess',
        url: this.baseUrl
      }
    };

    // Add image if available
    if (teacher.avatarUrl) {
      schema['image'] = teacher.avatarUrl;
    }

    // Add aggregate rating if available
    if (teacher.averageRating && teacher.reviewCount) {
      schema['aggregateRating'] = {
        '@type': 'AggregateRating',
        ratingValue: teacher.averageRating.toFixed(1),
        reviewCount: teacher.reviewCount,
        bestRating: '5',
        worstRating: '1'
      };
    }

    this.insertSchema(schema, 'teacher-schema');
  }

  setCourseSchema(): void {
    const schema = {
      '@context': 'https://schema.org',
      '@type': 'Course',
      name: 'Cours d\'Échecs en Ligne',
      description: 'Apprenez les échecs avec des coachs certifiés. Parcours structuré de 120 cours et 546 leçons, du niveau Pion au niveau Roi.',
      provider: {
        '@type': 'EducationalOrganization',
        name: 'mychess',
        url: this.baseUrl
      },
      inLanguage: ['fr', 'en'],
      numberOfCredits: 120,
      hasCourseInstance: [
        {
          '@type': 'CourseInstance',
          name: 'Niveau Pion (Débutant)',
          description: 'Règles du jeu, mouvements des pièces, principes de base',
          courseMode: 'online',
          courseWorkload: 'PT1H'
        },
        {
          '@type': 'CourseInstance',
          name: 'Niveau Cavalier (Intermédiaire)',
          description: 'Tactiques, ouvertures classiques, milieu de partie',
          courseMode: 'online',
          courseWorkload: 'PT1H'
        },
        {
          '@type': 'CourseInstance',
          name: 'Niveau Reine (Avancé)',
          description: 'Stratégie avancée, finales, préparation de tournoi',
          courseMode: 'online',
          courseWorkload: 'PT1H'
        },
        {
          '@type': 'CourseInstance',
          name: 'Niveau Roi (Expert)',
          description: 'Maîtrise complète, analyse approfondie, compétition',
          courseMode: 'online',
          courseWorkload: 'PT1H'
        }
      ],
      offers: {
        '@type': 'Offer',
        price: '0',
        priceCurrency: 'EUR',
        description: 'Premier cours gratuit et sans engagement'
      }
    };

    this.insertSchema(schema, 'course-schema');
  }

  setBreadcrumbSchema(items: { name: string; url: string }[]): void {
    const schema = {
      '@context': 'https://schema.org',
      '@type': 'BreadcrumbList',
      itemListElement: items.map((item, index) => ({
        '@type': 'ListItem',
        position: index + 1,
        name: item.name,
        item: item.url
      }))
    };

    this.insertSchema(schema, 'breadcrumb-schema');
  }

  removeSchema(id: string): void {
    const existingScript = this.document.getElementById(id);
    if (existingScript) {
      existingScript.remove();
    }
  }

  private insertSchema(schema: object, id: string): void {
    this.removeSchema(id);

    const script = this.document.createElement('script');
    script.type = 'application/ld+json';
    script.id = id;
    script.text = JSON.stringify(schema);
    this.document.head.appendChild(script);
  }
}
