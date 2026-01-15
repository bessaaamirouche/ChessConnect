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
  heroArrowLeft
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-book-lesson',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, NgIconComponent],
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
    heroArrowLeft
  })],
  templateUrl: './book-lesson.component.html',
  styleUrl: './book-lesson.component.scss'
})
export class BookLessonComponent implements OnInit {
  bookingForm: FormGroup;
  loading = signal(false);
  success = signal(false);
  selectedSlot = signal<TimeSlot | null>(null);
  useSubscriptionSignal = signal(true);

  teacherSlots = this.availabilityService.teacherSlots;
  slotsLoading = this.availabilityService.loading;

  slotsByDate = computed(() => {
    return this.availabilityService.getSlotsByDate();
  });

  sortedDates = computed(() => {
    return Array.from(this.slotsByDate().keys()).sort();
  });

  // Check if teacher accepts subscription
  teacherAcceptsSubscription = computed(() => {
    return this.teacherService.selectedTeacher()?.acceptsSubscription ?? false;
  });

  // Check if student can use subscription for this teacher
  canUseSubscription = computed(() => {
    const teacher = this.teacherService.selectedTeacher();
    const subscription = this.paymentService.activeSubscription();

    if (!teacher?.acceptsSubscription) return false;
    if (!subscription) return false;
    return subscription.remainingLessons > 0;
  });

  // Check if one-time payment is required
  requiresPayment = computed(() => {
    const teacher = this.teacherService.selectedTeacher();
    if (!teacher) return false;

    // If teacher doesn't accept subscription, payment is required
    if (!teacher.acceptsSubscription) return true;

    // If user has no subscription or no remaining lessons, payment required
    const subscription = this.paymentService.activeSubscription();
    if (!subscription) return true;
    if (subscription.remainingLessons <= 0) return true;

    // If user chose not to use subscription
    if (!this.useSubscriptionSignal()) return true;

    return false;
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
      notes: [''],
      useSubscription: [true]
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

    // Load active subscription
    this.paymentService.loadActiveSubscription().subscribe();

    // Sync checkbox with signal
    this.bookingForm.get('useSubscription')?.valueChanges.subscribe(value => {
      this.useSubscriptionSignal.set(value);
    });
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
    const { notes, useSubscription } = this.bookingForm.value;

    this.loading.set(true);

    // If payment is required, redirect to Stripe checkout
    if (this.requiresPayment()) {
      this.paymentService.createLessonCheckout({
        teacherId: teacher.id,
        scheduledAt: scheduledAt,
        durationMinutes: 60,
        notes: notes || ''
      }).subscribe({
        next: (response) => {
          // Redirect to Stripe checkout
          window.location.href = response.url;
        },
        error: () => {
          this.loading.set(false);
        }
      });
    } else {
      // Book using subscription
      this.lessonService.bookLesson({
        teacherId: teacher.id,
        scheduledAt,
        notes,
        useSubscription: true
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
    }
  }

  ngOnDestroy(): void {
    this.availabilityService.clearSlots();
  }
}
