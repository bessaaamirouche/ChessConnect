import { Component, OnInit, signal, ChangeDetectionStrategy, viewChild, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { LessonService } from '../../core/services/lesson.service';
import { ProgressService } from '../../core/services/progress.service';
import { PaymentService } from '../../core/services/payment.service';
import { TeacherService } from '../../core/services/teacher.service';
import { SeoService } from '../../core/services/seo.service';
import { StripeConnectService } from '../../core/services/stripe-connect.service';
import { DialogService } from '../../core/services/dialog.service';
import { WalletService } from '../../core/services/wallet.service';
import { LESSON_STATUS_LABELS, Lesson } from '../../core/models/lesson.model';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
  heroCalendarDays,
  heroTrophy,
  heroCreditCard,
  heroCheckCircle,
  heroBanknotes,
  heroArrowTrendingUp,
  heroCheck,
  heroSparkles,
  heroExclamationTriangle
} from '@ng-icons/heroicons/outline';

@Component({
    selector: 'app-dashboard',
    imports: [RouterLink, DatePipe, DecimalPipe, ConfirmDialogComponent, NgIconComponent, TranslateModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [provideIcons({
            heroCalendarDays,
            heroTrophy,
            heroCreditCard,
            heroCheckCircle,
            heroBanknotes,
            heroArrowTrendingUp,
            heroCheck,
            heroSparkles,
            heroExclamationTriangle
        })],
    templateUrl: './dashboard.component.html',
    styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  readonly confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private translate = inject(TranslateService);

  statusLabels = LESSON_STATUS_LABELS;

  // Teacher earnings withdrawal
  withdrawing = signal(false);
  withdrawAmount = signal(100);
  withdrawError = signal<string | null>(null);
  withdrawSuccess = signal<string | null>(null);
  stripeConnectReady = signal(false);

  constructor(
    public authService: AuthService,
    public lessonService: LessonService,
    public progressService: ProgressService,
    public paymentService: PaymentService,
    public teacherService: TeacherService,
    private http: HttpClient,
    private seoService: SeoService,
    private stripeConnectService: StripeConnectService,
    private dialogService: DialogService,
    public walletService: WalletService
  ) {
    this.seoService.setDashboardPage();
  }

  ngOnInit(): void {
    this.lessonService.loadUpcomingLessons().subscribe();
    this.lessonService.loadLessonHistory().subscribe();

    if (this.authService.isStudent()) {
      this.progressService.loadMyProgress().subscribe();
      this.paymentService.loadActiveSubscription().subscribe();
      this.walletService.loadBalance().subscribe();
    }

    if (this.authService.isTeacher()) {
      this.teacherService.getMyBalance().subscribe({
        next: (balance) => {
          const maxAmount = Math.floor(balance.availableBalanceCents / 100);
          this.withdrawAmount.set(Math.max(100, maxAmount));
        }
      });
      this.loadStripeConnectStatus();
    }
  }

  formatPrice(cents: number): string {
    return (cents / 100).toFixed(0) + '€';
  }

  // Lesson actions for teachers
  confirmLesson(lessonId: number): void {
    this.lessonService.confirmLesson(lessonId).subscribe();
  }

  async cancelLesson(lessonId: number): Promise<void> {
    const reason = await this.confirmDialog().open({
      title: this.translate.instant('dashboard.cancelDialog.title'),
      message: this.translate.instant('dashboard.cancelDialog.message'),
      confirmText: this.translate.instant('dashboard.cancelDialog.confirmText'),
      cancelText: this.translate.instant('dashboard.cancelDialog.cancelText'),
      type: 'danger',
      icon: 'warning',
      showInput: true,
      inputLabel: this.translate.instant('dashboard.cancelDialog.inputLabel'),
      inputPlaceholder: this.translate.instant('dashboard.cancelDialog.inputPlaceholder')
    });

    if (reason !== null) {
      this.lessonService.cancelLesson(lessonId, reason as string || undefined).subscribe({
        next: () => {
          // Reload wallet balance after cancellation (for refund)
          if (this.authService.isStudent()) {
            this.walletService.loadBalance().subscribe();
          }
        }
      });
    }
  }

  completeLesson(lessonId: number): void {
    this.lessonService.completeLesson(lessonId).subscribe({
      next: () => {
        // Reload balance after completing a lesson
        this.teacherService.getMyBalance().subscribe();
      }
    });
  }

  // Check if it's time to join the lesson (15 min before until end)
  canJoinLesson(lesson: Lesson): boolean {
    const now = new Date();
    const lessonStart = new Date(lesson.scheduledAt);
    const lessonEnd = new Date(lessonStart.getTime() + lesson.durationMinutes * 60000);
    const joinableFrom = new Date(lessonStart.getTime() - 15 * 60000);

    return now >= joinableFrom && now <= lessonEnd;
  }

  // Get time until lesson can be joined
  getTimeUntilJoin(lesson: Lesson): string {
    const now = new Date();
    const lessonStart = new Date(lesson.scheduledAt);
    const joinableFrom = new Date(lessonStart.getTime() - 15 * 60000);
    const diffMs = joinableFrom.getTime() - now.getTime();

    if (diffMs <= 0) return '';

    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays > 0) return `dans ${diffDays}j`;
    if (diffHours > 0) return `dans ${diffHours}h`;
    return `dans ${diffMins}min`;
  }

  // Teacher earnings withdrawal methods
  loadStripeConnectStatus(): void {
    this.stripeConnectService.getStatus().subscribe({
      next: (status) => {
        this.stripeConnectReady.set(status.isReady);
      },
      error: () => this.stripeConnectReady.set(false)
    });
  }

  formatCents(cents: number): string {
    return (cents / 100).toFixed(2) + ' EUR';
  }

  setWithdrawAmount(event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = parseInt(input.value) || 100;
    this.withdrawAmount.set(value);
  }

  async withdrawEarnings(): Promise<void> {
    const balance = this.teacherService.balance();
    const amount = this.withdrawAmount();

    if (!balance || amount < 100) return;

    const amountCents = amount * 100;
    if (amountCents > balance.availableBalanceCents) {
      this.withdrawError.set(this.translate.instant('errors.insufficientBalance'));
      return;
    }

    const confirmed = await this.dialogService.confirm(
      this.translate.instant('settings.stripe.withdrawConfirmMessage', { amount }),
      this.translate.instant('settings.stripe.withdrawTitle'),
      { confirmText: this.translate.instant('common.withdraw'), cancelText: this.translate.instant('common.cancel'), variant: 'info' }
    );
    if (!confirmed) return;

    this.withdrawing.set(true);
    this.withdrawError.set(null);

    this.stripeConnectService.withdraw(amountCents).subscribe({
      next: (response) => {
        this.withdrawing.set(false);
        if (response.success) {
          this.withdrawSuccess.set(this.translate.instant('success.withdrawalDone', { amount: this.formatCents(response.amountCents || 0) }));
          this.teacherService.getMyBalance().subscribe({
            next: (balance) => {
              const maxAmount = Math.floor(balance.availableBalanceCents / 100);
              this.withdrawAmount.set(Math.max(100, maxAmount));
            }
          });
          setTimeout(() => this.withdrawSuccess.set(null), 5000);
        } else {
          this.withdrawError.set(response.message || this.translate.instant('errors.withdrawal'));
        }
      },
      error: (err) => {
        this.withdrawing.set(false);
        this.withdrawError.set(err.error?.message || this.translate.instant('errors.withdrawal'));
      }
    });
  }
}
