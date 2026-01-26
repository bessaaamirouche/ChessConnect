import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TeacherService } from '../../../core/services/teacher.service';
import { LessonService } from '../../../core/services/lesson.service';
import { AvailabilityService } from '../../../core/services/availability.service';
import { PaymentService } from '../../../core/services/payment.service';
import { WalletService } from '../../../core/services/wallet.service';
import { AuthService } from '../../../core/services/auth.service';
import { LearningPathService } from '../../../core/services/learning-path.service';
import { TimeSlot } from '../../../core/models/availability.model';
import { Course, GradeWithCourses } from '../../../core/models/learning-path.model';
import { ChessLevel, CHESS_LEVELS } from '../../../core/models/user.model';
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
  heroSparkles,
  heroWallet,
  heroDocumentText,
  heroBookOpen,
  heroCheckCircle,
  heroArrowPath
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
    heroSparkles,
    heroWallet,
    heroDocumentText,
    heroBookOpen,
    heroCheckCircle,
    heroArrowPath
  })],
  templateUrl: './book-lesson.component.html',
  styleUrl: './book-lesson.component.scss'
})
export class BookLessonComponent implements OnInit {
  bookingForm: FormGroup;
  sidebarCollapsed = signal(false);

  sidebarSections = computed<SidebarSection[]>(() => {
    const menuItems: any[] = [
      { label: 'Mon Espace', icon: 'heroChartBarSquare', route: '/dashboard' },
      { label: 'Mes Cours', icon: 'heroCalendarDays', route: '/lessons' },
      { label: 'Ma Progression', icon: 'heroTrophy', route: '/progress' },
      { label: 'Trouver un Coach', icon: 'heroAcademicCap', route: '/teachers', active: true }
    ];

    const compteItems: any[] = [
      { label: 'Mon Profil', icon: 'heroUserCircle', route: '/settings' },
      { label: 'Mon Solde', icon: 'heroWallet', route: '/wallet' },
      { label: 'Abonnement', icon: 'heroCreditCard', route: '/subscription' },
      { label: 'Mes Factures', icon: 'heroDocumentText', route: '/invoices' },
      { label: 'Deconnexion', icon: 'heroArrowRightOnRectangle', action: () => this.logout() }
    ];

    return [
      { title: 'Menu', items: menuItems },
      { title: 'Compte', items: compteItems }
    ];
  });

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }
  loading = signal(false);
  success = signal(false);
  selectedSlot = signal<TimeSlot | null>(null);
  selectedCourse = signal<Course | null>(null);
  useFreeTrialSignal = signal(true);
  payWithCreditSignal = signal(false);

  // Embedded checkout
  showCheckout = signal(false);
  checkoutClientSecret = signal<string | null>(null);
  checkoutSessionId = signal<string | null>(null);

  // Course selection
  learningPathLoading = signal(false);
  selectedLevel = signal<ChessLevel | null>(null);

  // Get unlocked grades for the level dropdown
  unlockedGrades = computed(() => {
    const path = this.learningPathService.learningPath();
    if (!path) return [];
    return path.grades.filter(g => g.isUnlocked);
  });

  // Get courses for the selected level
  coursesForSelectedLevel = computed(() => {
    const level = this.selectedLevel();
    if (!level) return [];

    const path = this.learningPathService.learningPath();
    if (!path) return [];

    const grade = path.grades.find(g => g.grade === level);
    if (!grade || !grade.isUnlocked) return [];

    return grade.courses;
  });

  // Free trial eligibility (student eligible AND teacher accepts free trial)
  freeTrialEligible = this.lessonService.freeTrialEligible;

  // Can use free trial: student eligible AND selected teacher accepts free trials
  canUseFreeTrial = computed(() => {
    const teacher = this.teacherService.selectedTeacher();
    return this.freeTrialEligible() && teacher?.acceptsFreeTrial === true;
  });

  // Check if user has enough credit for the lesson
  canPayWithCredit = computed(() => {
    const teacher = this.teacherService.selectedTeacher();
    if (!teacher?.hourlyRateCents) return false;
    return this.walletService.hasEnoughCredit(teacher.hourlyRateCents);
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
    public walletService: WalletService,
    public authService: AuthService,
    public learningPathService: LearningPathService
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

    // Load wallet balance
    this.walletService.loadBalance().subscribe();

    // Load subscription status (for premium priority banner)
    this.paymentService.loadActiveSubscription().subscribe();

    // Load learning path for course selection
    this.learningPathLoading.set(true);
    this.learningPathService.loadLearningPath().subscribe({
      next: () => {
        this.learningPathLoading.set(false);
        // Auto-select first unlocked level
        const unlocked = this.unlockedGrades();
        if (unlocked.length > 0) {
          this.selectedLevel.set(unlocked[0].grade as ChessLevel);
        }
      },
      error: () => this.learningPathLoading.set(false)
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

  formatCoachName(firstName: string, lastName: string): string {
    return `${firstName} ${lastName.charAt(0)}.`;
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

  selectLevel(level: ChessLevel | null): void {
    this.selectedLevel.set(level);
    // Reset selected course when level changes
    this.selectedCourse.set(null);
  }

  selectCourse(course: Course): void {
    this.selectedCourse.set(course);
  }

  getLevelDisplayName(level: ChessLevel): string {
    return CHESS_LEVELS[level]?.label || level;
  }

  isCourseSelected(course: Course): boolean {
    const selected = this.selectedCourse();
    return selected !== null && selected.id === course.id;
  }

  getGradeIcon(grade: string): string {
    return this.learningPathService.getGradeIcon(grade as any);
  }

  getGradeColor(grade: string): string {
    return this.learningPathService.getGradeColor(grade as any);
  }

  toggleFreeTrial(value: boolean): void {
    this.useFreeTrialSignal.set(value);
  }

  togglePayWithCredit(value: boolean): void {
    this.payWithCreditSignal.set(value);
  }

  isSlotSelected(slot: TimeSlot): boolean {
    const selected = this.selectedSlot();
    return selected !== null &&
           selected.date === slot.date &&
           selected.startTime === slot.startTime;
  }

  onSubmit(): void {
    const slot = this.selectedSlot();
    const course = this.selectedCourse();
    if (!slot || !course || !this.teacherService.selectedTeacher()) return;

    const teacher = this.teacherService.selectedTeacher()!;
    const scheduledAt = slot.dateTime;
    const { notes } = this.bookingForm.value;
    const courseId = course.id;

    this.loading.set(true);

    // If can use free trial (eligible + teacher accepts) and user wants it, book free trial
    if (this.canUseFreeTrial() && this.useFreeTrialSignal()) {
      this.lessonService.bookFreeTrialLesson({
        teacherId: teacher.id,
        scheduledAt,
        notes,
        useSubscription: false,
        courseId
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

    // Pay with credit
    if (this.payWithCreditSignal() && this.canPayWithCredit()) {
      this.walletService.bookWithCredit({
        teacherId: teacher.id,
        scheduledAt: scheduledAt,
        durationMinutes: 60,
        notes: notes || '',
        courseId
      }).subscribe({
        next: (response) => {
          if (response.success) {
            this.success.set(true);
            this.loading.set(false);
            setTimeout(() => {
              this.router.navigate(['/dashboard']);
            }, 2000);
          } else {
            this.loading.set(false);
          }
        },
        error: () => {
          this.loading.set(false);
        }
      });
      return;
    }

    // Pay with card via Stripe
    this.paymentService.createLessonCheckout({
      teacherId: teacher.id,
      scheduledAt: scheduledAt,
      durationMinutes: 60,
      notes: notes || '',
      courseId
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
