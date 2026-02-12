import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TeacherService } from '../../../core/services/teacher.service';
import { SeoService } from '../../../core/services/seo.service';
import { StructuredDataService } from '../../../core/services/structured-data.service';
import { LanguageSelectorComponent } from '../../../shared/components/language-selector/language-selector.component';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { User } from '../../../core/models/user.model';

@Component({
    selector: 'app-public-teacher-profile',
    imports: [RouterLink, TranslateModule, LanguageSelectorComponent, FooterComponent],
    templateUrl: './public-teacher-profile.component.html',
    styleUrl: './public-teacher-profile.component.scss'
})
export class PublicTeacherProfileComponent implements OnInit {
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);
  private translate = inject(TranslateService);

  constructor(
    private route: ActivatedRoute,
    public teacherService: TeacherService
  ) {}

  ngOnInit(): void {
    // Get resolved teacher data (loaded by resolver for SSR support)
    const teacher = this.route.snapshot.data['teacher'] as User | null;

    if (teacher) {
      // Set the teacher in the service for template access
      this.teacherService.setSelectedTeacher(teacher);

      // Set SEO meta tags immediately (works with SSR)
      this.seoService.setTeacherProfilePage(teacher);
      this.structuredDataService.setTeacherSchema(teacher);
      this.structuredDataService.setBreadcrumbSchema([
        { name: this.translate.instant('legal.home'), url: 'https://mychess.fr/' },
        { name: this.translate.instant('breadcrumbs.coaches'), url: 'https://mychess.fr/coaches' },
        { name: `${teacher.firstName} ${teacher.lastName}`, url: `https://mychess.fr/coaches/${teacher.uuid}` }
      ]);
    } else {
      // Fallback: if no resolved data, load via API (client-side navigation)
      const uuidParam = this.route.snapshot.paramMap.get('uuid');
      if (uuidParam) {
        this.teacherService.getTeacherByUuid(uuidParam).subscribe(loadedTeacher => {
          this.seoService.setTeacherProfilePage(loadedTeacher);
          this.structuredDataService.setTeacherSchema(loadedTeacher);
          this.structuredDataService.setBreadcrumbSchema([
            { name: 'Accueil', url: 'https://mychess.fr/' },
            { name: 'Coachs', url: 'https://mychess.fr/coaches' },
            { name: `${loadedTeacher.firstName} ${loadedTeacher.lastName}`, url: `https://mychess.fr/coaches/${loadedTeacher.uuid}` }
          ]);
        });
      }
    }
  }

  formatPrice(cents: number | undefined): string {
    if (!cents) return '-';
    return (cents / 100).toFixed(0) + '€';
  }

  getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }
}
