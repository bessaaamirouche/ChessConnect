import { Injectable, inject } from '@angular/core';
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
export class StructuredDataService {
  private document = inject(DOCUMENT);
  private readonly baseUrl = 'https://mychess.fr';

  setOrganizationSchema(): void {
    const schema = {
      '@context': 'https://schema.org',
      '@type': 'Organization',
      name: 'mychess',
      alternateName: 'MyChess',
      url: this.baseUrl,
      description: 'Plateforme de cours d\'échecs en ligne avec les meilleurs professeurs. Progressez du niveau Pion au niveau Dame.',
      foundingDate: '2024',
      areaServed: 'FR',
      serviceType: 'Cours d\'échecs en ligne'
    };

    this.insertSchema(schema, 'organization-schema');
  }

  setTeacherSchema(teacher: Teacher): void {
    const schema = {
      '@context': 'https://schema.org',
      '@type': 'Person',
      name: `${teacher.firstName} ${teacher.lastName}`,
      url: `${this.baseUrl}/teachers/${teacher.id}`,
      jobTitle: 'Professeur d\'échecs',
      description: teacher.bio || `Professeur d'échecs disponible pour des cours particuliers en visioconférence.`,
      worksFor: {
        '@type': 'Organization',
        name: 'mychess',
        url: this.baseUrl
      }
    };

    this.insertSchema(schema, 'teacher-schema');
  }

  setCourseSchema(): void {
    const schema = {
      '@context': 'https://schema.org',
      '@type': 'Course',
      name: 'Cours d\'Échecs en Ligne',
      description: 'Apprenez les échecs avec des professeurs qualifiés. Progression structurée du niveau Pion au niveau Dame.',
      provider: {
        '@type': 'Organization',
        name: 'mychess',
        url: this.baseUrl
      },
      hasCourseInstance: {
        '@type': 'CourseInstance',
        courseMode: 'online',
        instructor: {
          '@type': 'Person',
          name: 'Professeurs qualifiés'
        }
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
