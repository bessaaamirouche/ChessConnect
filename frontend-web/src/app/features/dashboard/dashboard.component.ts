import { Component, OnInit, signal, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { LessonService } from '../../core/services/lesson.service';
import { ProgressService } from '../../core/services/progress.service';
import { PaymentService } from '../../core/services/payment.service';
import { TeacherService } from '../../core/services/teacher.service';
import { SeoService } from '../../core/services/seo.service';
import { StripeConnectService } from '../../core/services/stripe-connect.service';
import { DialogService } from '../../core/services/dialog.service';
import { LESSON_STATUS_LABELS, Lesson } from '../../core/models/lesson.model';
import { CHESS_LEVELS } from '../../core/models/user.model';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ConfirmModalComponent } from '../../shared/confirm-modal/confirm-modal.component';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChartBarSquare,
  heroCalendarDays,
  heroClipboardDocumentList,
  heroTrophy,
  heroCreditCard,
  heroAcademicCap,
  heroUser,
  heroUserCircle,
  heroArrowRightOnRectangle,
  heroCheckCircle,
  heroTicket,
  heroBanknotes,
  heroArrowTrendingUp,
  heroInformationCircle,
  heroClock,
  heroXCircle,
  heroVideoCamera,
  heroCheck,
  heroXMark,
  heroPlayCircle,
  heroLockClosed,
  heroEnvelope,
  heroDocumentText,
  heroSparkles
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe, DecimalPipe, ReactiveFormsModule, ConfirmModalComponent, NgIconComponent],
  viewProviders: [provideIcons({
    heroChartBarSquare,
    heroCalendarDays,
    heroClipboardDocumentList,
    heroTrophy,
    heroCreditCard,
    heroAcademicCap,
    heroUser,
    heroUserCircle,
    heroArrowRightOnRectangle,
    heroCheckCircle,
    heroTicket,
    heroBanknotes,
    heroArrowTrendingUp,
    heroInformationCircle,
    heroClock,
    heroXCircle,
    heroVideoCamera,
    heroCheck,
    heroXMark,
    heroPlayCircle,
    heroLockClosed,
    heroEnvelope,
    heroDocumentText,
    heroSparkles
  })],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  @ViewChild('confirmModal') confirmModal!: ConfirmModalComponent;

  statusLabels = LESSON_STATUS_LABELS;
  CHESS_LEVELS = CHESS_LEVELS;
  settingsForm: FormGroup;
  passwordForm: FormGroup;
  profileForm: FormGroup;

  // Active view for teacher profile
  activeView = signal<'dashboard' | 'profile'>('dashboard');

  // Mobile menu state
  mobileMenuOpen = signal(false);

  // Profile editing
  savingProfile = signal(false);
  profileSuccess = signal(false);
  profileError = signal<string | null>(null);

  // Settings panel
  showSettings = signal(false);
  savingSettings = signal(false);
  settingsSuccess = signal(false);
  settingsError = signal<string | null>(null);
  savingPassword = signal(false);
  passwordSuccess = signal(false);
  passwordError = signal<string | null>(null);

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
    private fb: FormBuilder,
    private http: HttpClient,
    private seoService: SeoService,
    private stripeConnectService: StripeConnectService,
    private dialogService: DialogService
  ) {
    this.seoService.setDashboardPage();
    this.settingsForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      hourlyRate: [50],
      acceptsSubscription: [true],
      bio: ['']
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    });

    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      hourlyRate: [50],
      acceptsSubscription: [true],
      bio: ['']
    });
  }

  ngOnInit(): void {
    this.lessonService.loadUpcomingLessons().subscribe();
    this.lessonService.loadLessonHistory().subscribe();

    if (this.authService.isStudent()) {
      this.progressService.loadMyProgress().subscribe();
      this.paymentService.loadActiveSubscription().subscribe();
    }

    if (this.authService.isTeacher()) {
      this.teacherService.getMyBalance().subscribe({
        next: (balance) => {
          const maxAmount = Math.floor(balance.availableBalanceCents / 100);
          this.withdrawAmount.set(Math.max(100, maxAmount));
        }
      });
      this.loadProfileForm();
      this.loadStripeConnectStatus();
    }
  }

  loadProfileForm(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.profileForm.patchValue({
        firstName: user.firstName,
        lastName: user.lastName,
        hourlyRate: user.hourlyRateCents ? user.hourlyRateCents / 100 : 50,
        acceptsSubscription: user.acceptsSubscription ?? true,
        bio: user.bio || ''
      });
    }
  }

  showProfile(): void {
    this.activeView.set('profile');
    this.loadProfileForm();
    this.profileSuccess.set(false);
    this.profileError.set(null);
  }

  showDashboard(): void {
    this.activeView.set('dashboard');
  }

  saveProfile(): void {
    if (this.profileForm.invalid || this.savingProfile()) return;

    this.savingProfile.set(true);
    this.profileSuccess.set(false);
    this.profileError.set(null);

    const formValue = this.profileForm.value;
    const payload: any = {
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      hourlyRateCents: formValue.hourlyRate * 100,
      acceptsSubscription: formValue.acceptsSubscription,
      bio: formValue.bio
    };

    this.http.patch<any>('/api/users/me', payload).subscribe({
      next: (updatedUser) => {
        this.authService.updateCurrentUser(updatedUser);
        this.savingProfile.set(false);
        this.profileSuccess.set(true);
        setTimeout(() => this.profileSuccess.set(false), 3000);
      },
      error: (err) => {
        this.savingProfile.set(false);
        this.profileError.set(err.error?.message || 'Erreur lors de la sauvegarde');
      }
    });
  }

  formatPrice(cents: number): string {
    return (cents / 100).toFixed(0) + '€';
  }

  getNextRenewalDate(): Date {
    const subscription = this.paymentService.activeSubscription();
    if (!subscription?.startDate) {
      return new Date();
    }

    // Calculate next month from start date
    const startDate = new Date(subscription.startDate);
    const today = new Date();

    // Find the next renewal date (same day of month as start)
    const nextRenewal = new Date(today.getFullYear(), today.getMonth() + 1, startDate.getDate());

    // If we're past the renewal day this month, it's next month
    if (today.getDate() >= startDate.getDate()) {
      return nextRenewal;
    }

    // Otherwise it's this month
    return new Date(today.getFullYear(), today.getMonth(), startDate.getDate());
  }

  // Settings Panel Methods
  openSettings(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.settingsForm.patchValue({
        firstName: user.firstName,
        lastName: user.lastName,
        hourlyRate: user.hourlyRateCents ? user.hourlyRateCents / 100 : 50,
        acceptsSubscription: user.acceptsSubscription ?? true,
        bio: user.bio || ''
      });
    }
    this.showSettings.set(true);
    this.settingsSuccess.set(false);
    this.settingsError.set(null);
    this.passwordSuccess.set(false);
    this.passwordError.set(null);
    this.passwordForm.reset();
  }

  closeSettings(): void {
    this.showSettings.set(false);
  }

  saveSettings(): void {
    if (this.settingsForm.invalid || this.savingSettings()) return;

    this.savingSettings.set(true);
    this.settingsSuccess.set(false);
    this.settingsError.set(null);

    const formValue = this.settingsForm.value;
    const payload: any = {
      firstName: formValue.firstName,
      lastName: formValue.lastName
    };

    if (this.authService.isTeacher()) {
      payload.hourlyRateCents = formValue.hourlyRate * 100;
      payload.acceptsSubscription = formValue.acceptsSubscription;
      payload.bio = formValue.bio;
    }

    this.http.patch<any>('/api/users/me', payload).subscribe({
      next: (updatedUser) => {
        this.authService.updateCurrentUser(updatedUser);
        this.savingSettings.set(false);
        this.settingsSuccess.set(true);
        setTimeout(() => this.settingsSuccess.set(false), 3000);
      },
      error: (err) => {
        this.savingSettings.set(false);
        this.settingsError.set(err.error?.message || 'Erreur lors de la sauvegarde');
      }
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid || this.savingPassword()) return;

    const { currentPassword, newPassword, confirmPassword } = this.passwordForm.value;

    if (newPassword !== confirmPassword) {
      this.passwordError.set('Les mots de passe ne correspondent pas');
      return;
    }

    this.savingPassword.set(true);
    this.passwordSuccess.set(false);
    this.passwordError.set(null);

    this.http.post('/api/users/me/password', {
      currentPassword,
      newPassword
    }).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.passwordSuccess.set(true);
        this.passwordForm.reset();
        setTimeout(() => this.passwordSuccess.set(false), 3000);
      },
      error: (err) => {
        this.savingPassword.set(false);
        this.passwordError.set(err.error?.message || 'Mot de passe actuel incorrect');
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

  // Mobile menu methods
  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  // Lesson actions for teachers
  confirmLesson(lessonId: number): void {
    this.lessonService.confirmLesson(lessonId).subscribe();
  }

  async cancelLesson(lessonId: number): Promise<void> {
    const reason = await this.confirmModal.open({
      title: 'Annuler le cours',
      message: 'Êtes-vous sûr de vouloir annuler ce cours ?',
      confirmText: 'Annuler le cours',
      cancelText: 'Retour',
      type: 'danger',
      showInput: true,
      inputLabel: 'Raison de l\'annulation (optionnel)',
      inputPlaceholder: 'Ex: Indisponibilité...'
    });

    if (reason !== null) {
      this.lessonService.cancelLesson(lessonId, reason as string || undefined).subscribe();
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

  // Generate Google Meet link as Zoom alternative
  generateMeetLink(lesson: Lesson): string {
    const date = new Date(lesson.scheduledAt);
    const title = encodeURIComponent(`Cours d'échecs - mychess`);
    return `https://meet.google.com/new?hs=122&authuser=0`;
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
      this.withdrawError.set('Le montant depasse votre solde disponible');
      return;
    }

    const confirmed = await this.dialogService.confirm(
      `Voulez-vous retirer ${amount} EUR vers votre compte bancaire ?`,
      'Retirer mes gains',
      { confirmText: 'Retirer', cancelText: 'Annuler', variant: 'info' }
    );
    if (!confirmed) return;

    this.withdrawing.set(true);
    this.withdrawError.set(null);

    this.stripeConnectService.withdraw(amountCents).subscribe({
      next: (response) => {
        this.withdrawing.set(false);
        if (response.success) {
          this.withdrawSuccess.set(`Retrait de ${this.formatCents(response.amountCents || 0)} effectue !`);
          this.teacherService.getMyBalance().subscribe({
            next: (balance) => {
              const maxAmount = Math.floor(balance.availableBalanceCents / 100);
              this.withdrawAmount.set(Math.max(100, maxAmount));
            }
          });
          setTimeout(() => this.withdrawSuccess.set(null), 5000);
        } else {
          this.withdrawError.set(response.message || 'Erreur lors du retrait');
        }
      },
      error: (err) => {
        this.withdrawing.set(false);
        this.withdrawError.set(err.error?.message || 'Erreur lors du retrait');
      }
    });
  }
}
