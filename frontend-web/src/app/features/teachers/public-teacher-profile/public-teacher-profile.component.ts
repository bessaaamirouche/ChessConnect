import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { TeacherService } from '../../../core/services/teacher.service';
import { SeoService } from '../../../core/services/seo.service';
import { StructuredDataService } from '../../../core/services/structured-data.service';
import { LanguageSelectorComponent } from '../../../shared/components/language-selector/language-selector.component';

@Component({
  selector: 'app-public-teacher-profile',
  standalone: true,
  imports: [RouterLink, TranslateModule, LanguageSelectorComponent],
  templateUrl: './public-teacher-profile.component.html',
  styleUrl: './public-teacher-profile.component.scss'
})
export class PublicTeacherProfileComponent implements OnInit {
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);

  constructor(
    private route: ActivatedRoute,
    public teacherService: TeacherService
  ) {}

  ngOnInit(): void {
    const uuidParam = this.route.snapshot.paramMap.get('uuid');
    if (uuidParam) {
      this.teacherService.getTeacherByUuid(uuidParam).subscribe(teacher => {
        this.seoService.setTeacherProfilePage(teacher);
        this.structuredDataService.setTeacherSchema(teacher);
        this.structuredDataService.setBreadcrumbSchema([
          { name: 'Accueil', url: 'https://mychess.fr/' },
          { name: 'Coachs', url: 'https://mychess.fr/coaches' },
          { name: `${teacher.firstName} ${teacher.lastName}`, url: `https://mychess.fr/coaches/${teacher.uuid}` }
        ]);
      });
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
