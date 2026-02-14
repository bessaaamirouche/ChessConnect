import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroCalendarDays, heroBell } from '@ng-icons/heroicons/outline';
import { heroStarSolid, heroBellAlertSolid } from '@ng-icons/heroicons/solid';
import { TeacherService } from '../../../core/services/teacher.service';
import { AuthService } from '../../../core/services/auth.service';
import { FavoriteService } from '../../../core/services/favorite.service';
import { PaymentService } from '../../../core/services/payment.service';
import { SeoService } from '../../../core/services/seo.service';
import { StructuredDataService } from '../../../core/services/structured-data.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
    selector: 'app-teacher-profile',
    imports: [RouterLink, TranslateModule, NgIconComponent],
    viewProviders: [provideIcons({ heroCalendarDays, heroStarSolid, heroBell, heroBellAlertSolid })],
    templateUrl: './teacher-profile.component.html',
    styleUrl: './teacher-profile.component.scss'
})
export class TeacherProfileComponent implements OnInit {
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);
  private favoriteService = inject(FavoriteService);
  private toastService = inject(ToastService);
  private translate = inject(TranslateService);
  paymentService = inject(PaymentService);

  isFavorite = signal(false);
  notifyEnabled = signal(false);
  private teacherId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    public teacherService: TeacherService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    const teacherIdParam = this.route.snapshot.paramMap.get('id');
    if (teacherIdParam) {
      this.teacherId = +teacherIdParam;
      this.teacherService.getTeacher(this.teacherId).subscribe(teacher => {
        this.seoService.setTeacherProfilePage(teacher);
        this.structuredDataService.setTeacherSchema(teacher);
        this.structuredDataService.setBreadcrumbSchema([
          { name: this.translate.instant('legal.home'), url: 'https://mychess.fr/' },
          { name: this.translate.instant('breadcrumbs.coaches'), url: 'https://mychess.fr/coaches' },
          { name: `${teacher.firstName} ${teacher.lastName}`, url: `https://mychess.fr/coaches/${teacher.uuid}` }
        ]);
      });

      // Load favorite status and subscription for students
      if (this.authService.isStudent()) {
        this.favoriteService.loadFavorites().subscribe(favorites => {
          const fav = favorites.find(f => f.teacherId === this.teacherId);
          if (fav) {
            this.isFavorite.set(true);
            this.notifyEnabled.set(fav.notifyNewSlots);
          }
        });
        this.paymentService.loadActiveSubscription().subscribe();
      }
    }
  }

  toggleFavorite(): void {
    if (!this.teacherId) return;

    if (this.isFavorite()) {
      this.favoriteService.removeFavorite(this.teacherId).subscribe({
        next: () => {
          this.isFavorite.set(false);
          this.notifyEnabled.set(false);
          this.toastService.info(this.translate.instant('success.removedFromFavorites'));
        },
        error: () => {
          this.toastService.error(this.translate.instant('errors.generic'));
        }
      });
    } else {
      this.favoriteService.addFavorite(this.teacherId).subscribe({
        next: () => {
          this.isFavorite.set(true);
          this.toastService.success(this.translate.instant('success.addedToFavorites'));
        },
        error: () => {
          this.toastService.error(this.translate.instant('errors.generic'));
        }
      });
    }
  }

  toggleNotifications(): void {
    if (!this.teacherId || !this.isFavorite()) return;

    const newValue = !this.notifyEnabled();
    this.favoriteService.updateNotifications(this.teacherId, newValue).subscribe(() => {
      this.notifyEnabled.set(newValue);
      if (newValue) {
        this.toastService.success(this.translate.instant('success.notificationsEnabled'));
      } else {
        this.toastService.info(this.translate.instant('success.notificationsDisabled'));
      }
    });
  }

  formatPrice(cents: number | undefined): string {
    if (!cents) return '-';
    return (cents / 100).toFixed(0) + '€';
  }

  getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }

  formatCoachName(firstName: string, lastName: string): string {
    return `${firstName} ${lastName.charAt(0)}.`;
  }
}
