import { Component, OnInit, signal, computed, inject } from '@angular/core';

import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TeacherService } from '../../../core/services/teacher.service';
import { LessonService } from '../../../core/services/lesson.service';
import { AvailabilityService } from '../../../core/services/availability.service';
import { PaymentService } from '../../../core/services/payment.service';
import { WalletService } from '../../../core/services/wallet.service';
import { AuthService } from '../../../core/services/auth.service';
import { LearningPathService } from '../../../core/services/learning-path.service';
import { ProgrammeService, ProgrammeCourse } from '../../../core/services/programme.service';
import { UrlValidatorService } from '../../../core/services/url-validator.service';
import { GroupLessonService } from '../../../core/services/group-lesson.service';
import { DialogService } from '../../../core/services/dialog.service';
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
    imports: [RouterLink, ReactiveFormsModule, TranslateModule, NgIconComponent, EmbeddedCheckoutComponent, AppSidebarComponent],
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
  private translate = inject(TranslateService);
  bookingForm: FormGroup;
  sidebarCollapsed = signal(false);

  sidebarSections = computed<SidebarSection[]>(() => {
    const menuItems: any[] = [
      { label: this.translate.instant('sidebar.dashboard'), icon: 'heroChartBarSquare', route: '/dashboard' },
      { label: this.translate.instant('sidebar.myLessons'), icon: 'heroCalendarDays', route: '/lessons' },
      { label: this.translate.instant('sidebar.myProgress'), icon: 'heroBookOpen', route: '/programme' },
      { label: this.translate.instant('sidebar.findCoach'), icon: 'heroAcademicCap', route: '/teachers', active: true }
    ];

    const compteItems: any[] = [
      { label: this.translate.instant('sidebar.settings'), icon: 'heroUserCircle', route: '/settings' },
      { label: this.translate.instant('sidebar.wallet'), icon: 'heroWallet', route: '/wallet' },
      { label: this.translate.instant('sidebar.subscription'), icon: 'heroCreditCard', route: '/subscription' },
      { label: this.translate.instant('sidebar.invoices'), icon: 'heroDocumentText', route: '/invoices' },
      { label: this.translate.instant('sidebar.logout'), icon: 'heroArrowRightOnRectangle', action: () => this.logout() }
    ];

    return [
      { title: this.translate.instant('sidebar.menu'), items: menuItems },
      { title: this.translate.instant('sidebar.account'), items: compteItems }
    ];
  });

  onSidebarCollapsedChange(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }
  loading = signal(false);
  success = signal(false);
  selectedSlot = signal<TimeSlot | null>(null);
  selectedCourse = signal<Course | null>(null);
  payWithCreditSignal = signal(false);

  // Group lesson
  isGroupMode = signal(false);
  groupSize = signal<2 | 3>(2);
  groupCreatedToken = signal<string | null>(null);
  groupStripeCheckout = signal(false);
  linkCopied = signal(false);

  // Group price calculation (matches backend GroupPricingCalculator)
  groupPricePerPerson = computed(() => {
    const teacher = this.teacherService.selectedTeacher();
    if (!teacher?.hourlyRateCents) return 0;
    const size = this.groupSize();
    const rate = size === 2 ? 0.60 : 0.45;
    return Math.round(teacher.hourlyRateCents * rate);
  });

  groupSavingsPercent = computed(() => {
    return this.groupSize() === 2 ? 40 : 55;
  });

  // Effective price (private or group)
  effectivePrice = computed(() => {
    if (this.isGroupMode()) return this.groupPricePerPerson();
    const teacher = this.teacherService.selectedTeacher();
    return teacher?.hourlyRateCents || 0;
  });

  canPayGroupWithCredit = computed(() => {
    return this.walletService.hasEnoughCredit(this.groupPricePerPerson());
  });

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

  // Check if one-time payment is required
  requiresPayment = computed(() => {
    const teacher = this.teacherService.selectedTeacher();
    return !!teacher;
  });

  private urlValidator = inject(UrlValidatorService);

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
    public learningPathService: LearningPathService,
    public programmeService: ProgrammeService,
    private groupLessonService: GroupLessonService,
    private dialogService: DialogService
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

    // Load wallet balance
    this.walletService.loadBalance().subscribe();

    // Load subscription status (for premium priority banner)
    this.paymentService.loadActiveSubscription().subscribe();

    // Load current course from programme
    this.learningPathLoading.set(true);
    this.programmeService.loadCourses().subscribe({
      next: () => {
        this.learningPathLoading.set(false);
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

  togglePayWithCredit(value: boolean): void {
    this.payWithCreditSignal.set(value);
  }

  toggleGroupMode(value: boolean): void {
    this.isGroupMode.set(value);
    this.groupCreatedToken.set(null);
    // Auto-enable wallet if sufficient balance
    if (value) {
      this.payWithCreditSignal.set(this.canPayGroupWithCredit());
    }
  }

  setGroupSize(size: 2 | 3): void {
    this.groupSize.set(size);
    // Re-check wallet toggle when group size changes
    if (this.isGroupMode()) {
      this.payWithCreditSignal.set(this.canPayGroupWithCredit());
    }
  }

  copyInvitationLink(): void {
    const token = this.groupCreatedToken();
    if (!token) return;
    const url = `${window.location.origin}/join/${token}`;
    navigator.clipboard.writeText(url).then(() => {
      this.linkCopied.set(true);
      setTimeout(() => this.linkCopied.set(false), 2000);
    });
  }

  isSlotSelected(slot: TimeSlot): boolean {
    const selected = this.selectedSlot();
    return selected !== null &&
           selected.date === slot.date &&
           selected.startTime === slot.startTime;
  }

  onSubmit(): void {
    const slot = this.selectedSlot();
    const currentCourse = this.programmeService.currentCourse();
    if (!slot || !currentCourse || !this.teacherService.selectedTeacher()) return;

    const teacher = this.teacherService.selectedTeacher()!;
    const scheduledAt = slot.dateTime;
    const { notes } = this.bookingForm.value;
    const courseId = currentCourse.id;

    this.loading.set(true);

    // Group lesson mode
    if (this.isGroupMode()) {
      this.handleGroupSubmit(teacher, scheduledAt, notes, courseId);
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
          // Fallback to redirect - validate URL before redirecting
          if (this.urlValidator.isValidStripeUrl(response.url)) {
            window.location.href = response.url;
          } else {
            console.error('Invalid checkout URL received');
            this.loading.set(false);
          }
        }
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  private async handleGroupSubmit(teacher: any, scheduledAt: string, notes: string, courseId: number): Promise<void> {
    const priceFormatted = this.formatPrice(this.groupPricePerPerson());
    const useWallet = this.payWithCreditSignal() && this.canPayGroupWithCredit();
    const confirmMessage = useWallet
      ? this.translate.instant('group_lesson.confirmWallet', { amount: priceFormatted })
      : this.translate.instant('group_lesson.confirmCard', { amount: priceFormatted });

    const confirmed = await this.dialogService.confirm(
      confirmMessage,
      this.translate.instant('group_lesson.confirmTitle'),
      { confirmText: this.translate.instant('common.confirm'), variant: 'info' }
    );

    if (!confirmed) {
      this.loading.set(false);
      return;
    }

    const request = {
      teacherId: teacher.id,
      scheduledAt,
      durationMinutes: 60,
      notes: notes || '',
      targetGroupSize: this.groupSize(),
      courseId
    };

    // Wallet flow
    if (useWallet) {
      this.groupLessonService.createGroupLesson(request).subscribe({
        next: (response: any) => {
          if (response.success) {
            this.groupCreatedToken.set(response.invitationToken);
            this.success.set(true);
            this.loading.set(false);
            this.walletService.loadBalance().subscribe();
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

    // Stripe flow
    this.groupStripeCheckout.set(true);
    this.groupLessonService.createGroupCheckout(request).subscribe({
      next: (response) => {
        if (response.clientSecret) {
          this.checkoutClientSecret.set(response.clientSecret);
          this.checkoutSessionId.set(response.sessionId);
          this.showCheckout.set(true);
          this.loading.set(false);
        }
      },
      error: () => {
        this.groupStripeCheckout.set(false);
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

    if (sessionId && this.groupStripeCheckout()) {
      // Group create: confirm payment and show success with invitation link
      this.groupStripeCheckout.set(false);
      this.groupLessonService.confirmCreatePayment(sessionId).subscribe({
        next: (response: any) => {
          if (response.success) {
            this.groupCreatedToken.set(response.invitationToken);
            this.success.set(true);
          }
        },
        error: () => {
          // Fallback: navigate to payment success page
          this.router.navigate(['/lessons/payment/success'], {
            queryParams: { session_id: sessionId }
          });
        }
      });
    } else if (sessionId) {
      // Private lesson: redirect to success page
      this.router.navigate(['/lessons/payment/success'], {
        queryParams: { session_id: sessionId }
      });
    }
  }

  ngOnDestroy(): void {
    this.availabilityService.clearSlots();
  }
}
