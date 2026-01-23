import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TeacherService } from '../../../core/services/teacher.service';
import { LessonService } from '../../../core/services/lesson.service';
import { AvailabilityService } from '../../../core/services/availability.service';
import { PaymentService } from '../../../core/services/payment.service';
import { AuthService } from '../../../core/services/auth.service';
import { TimeSlot } from '../../../core/models/availability.model';
import { EmbeddedCheckoutComponent } from '../../../shared/embedded-checkout/embedded-checkout.component';
import { AppSidebarComponent, SidebarSection } from '../../../shared/components/app-sidebar/app-sidebar.component';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroTicket,
  heroCreditCard,
  heroBanknotes,
  heroChartBarSquare,
  heroCalendarDays,
  heroTrophy,
  heroAcademicCap,
  heroUserCircle,
  heroArrowRightOnRectangle,
  heroArrowLeft,
  heroGift,
  heroSparkles
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-book-lesson',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, NgIconComponent, EmbeddedCheckoutComponent, AppSidebarComponent],
  viewProviders: [provideIcons({
    heroTicket,
    heroCreditCard,
    heroBanknotes,
    heroChartBarSquare,
    heroCalendarDays,
    heroTrophy,
    heroAcademicCap,
    heroUserCircle,
    heroArrowRightOnRectangle,
    heroArrowLeft,
    heroGift,
    heroSparkles
  })],
  templateUrl: './book-lesson.component.html',
  styleUrl: './book-lesson.component.scss'
})
export class BookLessonComponent implements OnInit {
  bookingForm: FormGroup;
  sidebarCollapsed = signal(false);

  sidebarSections: SidebarSection[] = [
    {
      title: 'Menu',
      items: [
        { label: 'Mon Espace', icon: 'heroChartBarSquare', route: '/dashboard' },
        { label: 'Mes Cours', icon: 'heroCalendarDays', route: '/lessons' },
        { label: 'Ma Progression', icon: 'heroTrophy', route: '/progress' },
        { label: 'Abonnement', icon: 'heroCreditCard', route: '/subscription' },
        { label: 'Trouver un Coach', icon: 'heroAcademicCap', route: '/teachers', active: true }
      ]
    },
    {
      title: 'Compte',
      items: [
        { label: 'Mon Profil', icon: 'heroUserCircle', route: '/settings' },
        { label: 'Mes Factures', icon: 'heroDocumentText', route: '/invoices' },
        { label: 'Deconnexion', icon: 'heroArrowRightOnRectangle', action: () => this.logout() }
      ]
    }
  ];

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }
  loading = signal(false);
  success = signal(false);
  selectedSlot = signal<TimeSlot | null>(null);
  useFreeTrialSignal = signal(true);

  // Embedded checkout
  showCheckout = signal(false);
  checkoutClientSecret = signal<string | null>(null);
  checkoutSessionId = signal<string | null>(null);

  // Free trial eligibility (student eligible AND teacher accepts free trial)
  freeTrialEligible = this.lessonService.freeTrialEligible;

  // Can use free trial: student eligible AND selected teacher accepts free trials
  canUseFreeTrial = computed(() => {
    const teacher = this.teacherService.selectedTeacher();
    return this.freeTrialEligible() && teacher?.acceptsFreeTrial === true;
  });

  teacherSlots = this.availabilityService.teacherSlots;
  slotsLoading = this.availabilityService.loading;

  slotsByDate = computed(() => {
    return this.availabilityService.getSlotsByDate();
  });

  sortedDates = computed(() => {
    return Array.from(this.slotsByDate().keys()).sort();
  });

  // Check if one-time payment is required (always true unless free trial)
  requiresPayment = computed(() => {
    const teacher = this.teacherService.selectedTeacher();
    if (!teacher) return false;

    // If can use free trial (eligible + teacher accepts) and user wants to use it, no payment required
    if (this.canUseFreeTrial() && this.useFreeTrialSignal()) {
      return false;
    }

    // All lessons require payment at coach's rate
    return true;
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    public teacherService: TeacherService,
    public lessonService: LessonService,
    private availabilityService: AvailabilityService,
    public paymentService: PaymentService,
    public authService: AuthService
  ) {
    this.bookingForm = this.fb.group({
      notes: ['']
    });
  }

  logout(): void {
    this.authService.logout();
  }

  ngOnInit(): void {
    const teacherId = this.route.snapshot.paramMap.get('teacherId');
    if (teacherId) {
      this.teacherService.getTeacher(+teacherId).subscribe(() => {
        this.loadAvailableSlots(+teacherId);
      });
    }

    // Check free trial eligibility
    this.lessonService.checkFreeTrialEligibility().subscribe();
  }

  loadAvailableSlots(teacherId: number): void {
    const startDate = new Date(); // Inclut aujourd'hui

    const endDate = new Date();
    endDate.setDate(endDate.getDate() + 14);

    this.availabilityService.loadAvailableSlots(
      teacherId,
      startDate.toISOString().split('T')[0],
      endDate.toISOString().split('T')[0]
    ).subscribe();
  }

  formatPrice(cents: number | undefined): string {
    if (!cents) return '-';
    return (cents / 100).toFixed(0) + '€';
  }

  getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long'
    });
  }

  formatDateShort(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', {
      weekday: 'short',
      day: 'numeric',
      month: 'short'
    }).replace(/\./g, '');
  }

  formatTime(time: string): string {
    // Remove seconds if present (15:45:00 -> 15:45)
    return time.substring(0, 5);
  }

  selectSlot(slot: TimeSlot): void {
    if (!slot.isAvailable) return;
    this.selectedSlot.set(slot);
  }

  toggleFreeTrial(value: boolean): void {
    this.useFreeTrialSignal.set(value);
  }

  isSlotSelected(slot: TimeSlot): boolean {
    const selected = this.selectedSlot();
    return selected !== null &&
           selected.date === slot.date &&
           selected.startTime === slot.startTime;
  }

  onSubmit(): void {
    const slot = this.selectedSlot();
    if (!slot || !this.teacherService.selectedTeacher()) return;

    const teacher = this.teacherService.selectedTeacher()!;
    const scheduledAt = slot.dateTime;
    const { notes } = this.bookingForm.value;

    this.loading.set(true);

    // If can use free trial (eligible + teacher accepts) and user wants it, book free trial
    if (this.canUseFreeTrial() && this.useFreeTrialSignal()) {
      this.lessonService.bookFreeTrialLesson({
        teacherId: teacher.id,
        scheduledAt,
        notes,
        useSubscription: false
      }).subscribe({
        next: () => {
          this.success.set(true);
          this.loading.set(false);
          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 2000);
        },
        error: () => {
          this.loading.set(false);
        }
      });
      return;
    }

    // All lessons require payment at coach's rate
    this.paymentService.createLessonCheckout({
      teacherId: teacher.id,
      scheduledAt: scheduledAt,
      durationMinutes: 60,
      notes: notes || ''
    }, true).subscribe({
      next: (response) => {
        if (response.clientSecret) {
          // Use embedded checkout
          this.checkoutClientSecret.set(response.clientSecret);
          this.checkoutSessionId.set(response.sessionId);
          this.showCheckout.set(true);
          this.loading.set(false);
        } else if (response.url) {
          // Fallback to redirect
          window.location.href = response.url;
        }
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  closeCheckout(): void {
    this.showCheckout.set(false);
    this.checkoutClientSecret.set(null);
    this.checkoutSessionId.set(null);
  }

  onCheckoutCompleted(): void {
    const sessionId = this.checkoutSessionId();
    this.closeCheckout();

    if (sessionId) {
      // Navigate to success page to confirm payment
      this.router.navigate(['/lessons/payment/success'], {
        queryParams: { session_id: sessionId }
      });
    }
  }

  ngOnDestroy(): void {
    this.availabilityService.clearSlots();
  }
}
