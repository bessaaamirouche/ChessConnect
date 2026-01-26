import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TeacherService } from '../../../core/services/teacher.service';
import { AuthService } from '../../../core/services/auth.service';
import { FavoriteService } from '../../../core/services/favorite.service';
import { PaymentService } from '../../../core/services/payment.service';
import { SeoService } from '../../../core/services/seo.service';
import { StructuredDataService } from '../../../core/services/structured-data.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-teacher-profile',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './teacher-profile.component.html',
  styleUrl: './teacher-profile.component.scss'
})
export class TeacherProfileComponent implements OnInit {
  private seoService = inject(SeoService);
  private structuredDataService = inject(StructuredDataService);
  private favoriteService = inject(FavoriteService);
  private toastService = inject(ToastService);
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
          { name: 'Accueil', url: 'https://mychess.fr/' },
          { name: 'Coachs', url: 'https://mychess.fr/teachers' },
          { name: `${teacher.firstName} ${teacher.lastName}`, url: `https://mychess.fr/teachers/${teacher.id}` }
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
      this.favoriteService.removeFavorite(this.teacherId).subscribe(() => {
        this.isFavorite.set(false);
        this.notifyEnabled.set(false);
        this.toastService.info('Retiré des favoris');
      });
    } else {
      this.favoriteService.addFavorite(this.teacherId).subscribe(() => {
        this.isFavorite.set(true);
        this.toastService.success('Ajouté aux favoris');
      });
    }
  }

  toggleNotifications(): void {
    if (!this.teacherId || !this.isFavorite()) return;

    const newValue = !this.notifyEnabled();
    this.favoriteService.updateNotifications(this.teacherId, newValue).subscribe(() => {
      this.notifyEnabled.set(newValue);
      if (newValue) {
        this.toastService.success('Vous serez notifié des nouveaux créneaux');
      } else {
        this.toastService.info('Notifications désactivées');
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
