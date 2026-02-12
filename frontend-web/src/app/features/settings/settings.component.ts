import { Component, OnInit, signal, ChangeDetectionStrategy, inject, computed, HostListener, ElementRef } from '@angular/core';

import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';

// Custom validator for names - allows only letters, spaces, hyphens, and apostrophes
function nameValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) return null;
  const namePattern = /^[a-zA-ZÀ-ÿ\u00C0-\u024F\u1E00-\u1EFF\s\-']+$/;
  if (!namePattern.test(value)) {
    return { invalidName: true };
  }
  return null;
}
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { DialogService } from '../../core/services/dialog.service';
import { ToastService } from '../../core/services/toast.service';
import { HttpClient } from '@angular/common/http';
import { StripeConnectService, StripeConnectStatus } from '../../core/services/stripe-connect.service';
import { PushNotificationService } from '../../core/services/push-notification.service';
import { AVAILABLE_LANGUAGES, User, UpdateUserRequest } from '../../core/models/user.model';
import { DateInputComponent } from '../../shared/components/date-input/date-input.component';
import { LanguageSelectorComponent } from '../../shared/components/language-selector/language-selector.component';
import { LanguageService } from '../../core/services/language.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChartBarSquare,
  heroCalendarDays,
  heroClipboardDocumentList,
  heroTrophy,
  heroCreditCard,
  heroAcademicCap,
  heroUserCircle,
  heroArrowRightOnRectangle,
  heroTrash,
  heroExclamationTriangle,
  heroArrowUpOnSquare,
  heroLink
} from '@ng-icons/heroicons/outline';

@Component({
    selector: 'app-settings',
    imports: [ReactiveFormsModule, RouterLink, NgIconComponent, DateInputComponent, LanguageSelectorComponent, TranslateModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [provideIcons({
            heroChartBarSquare,
            heroCalendarDays,
            heroClipboardDocumentList,
            heroTrophy,
            heroCreditCard,
            heroAcademicCap,
            heroUserCircle,
            heroArrowRightOnRectangle,
            heroTrash,
            heroExclamationTriangle,
            heroArrowUpOnSquare,
            heroLink
        })],
    templateUrl: './settings.component.html',
    styleUrl: './settings.component.scss'
})
export class SettingsComponent implements OnInit {
  profileForm: FormGroup;

  savingProfile = signal(false);
  savingPreferences = signal(false);
  profileSuccess = signal(false);
  preferencesSuccess = signal(false);
  profileError = signal<string | null>(null);
  preferencesError = signal<string | null>(null);

  emailRemindersEnabled = signal(true);

  // Push notifications
  pushNotificationService = inject(PushNotificationService);
  pushNotificationsEnabled = signal(true);
  savingPushPreferences = signal(false);

  // Languages
  availableLanguages = AVAILABLE_LANGUAGES;
  selectedLanguages = signal<string[]>(['FR']);
  originalLanguages = signal<string[]>(['FR']);

  // Date constraints
  maxBirthDate = new Date().toISOString().split('T')[0];
  minBirthYear = 1920;
  maxBirthYear = new Date().getFullYear();

  // Track if languages have changed
  languagesChanged = computed(() => {
    const current = this.selectedLanguages().slice().sort();
    const original = this.originalLanguages().slice().sort();
    if (current.length !== original.length) return true;
    return current.some((lang, i) => lang !== original[i]);
  });

  // Avatar upload (teachers only)
  avatarUrl = signal<string | null>(null);
  uploadingAvatar = signal(false);
  avatarError = signal<string | null>(null);

  // Stripe Connect (teachers only)
  stripeConnectStatus = signal<StripeConnectStatus>({ connected: false, accountExists: false, isReady: false });
  stripeConnectLoading = signal(false);
  stripeConnectSuccess = signal<string | null>(null);
  stripeConnectError = signal<string | null>(null);

  // Teacher balance and withdrawal
  teacherBalance = signal<{ availableBalanceCents: number; totalEarnedCents: number; totalWithdrawnCents: number } | null>(null);
  withdrawing = signal(false);
  withdrawAmount = signal(100); // Default 100€ minimum
  recalculatingBalance = signal(false);

  // Account deletion
  deletingAccount = signal(false);
  deleteAccountError = signal<string | null>(null);

  private dialogService = inject(DialogService);
  private toastService = inject(ToastService);
  private router = inject(Router);
  private translateService = inject(TranslateService);
  private elementRef = inject(ElementRef);
  languageService = inject(LanguageService);

  // Close share menu when clicking outside
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.showShareMenu()) return;

    const target = event.target as HTMLElement;
    const shareContainer = this.elementRef.nativeElement.querySelector('.share-container');
    if (shareContainer && !shareContainer.contains(target)) {
      this.closeShareMenu();
    }
  }

  constructor(
    private fb: FormBuilder,
    public authService: AuthService,
    private http: HttpClient,
    private stripeConnectService: StripeConnectService,
    private route: ActivatedRoute,
    private seoService: SeoService
  ) {
    this.seoService.setSettingsPage();
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, nameValidator]],
      lastName: ['', [Validators.required, nameValidator]],
      email: ['', [Validators.required, Validators.email]],
      // Teacher fields
      hourlyRate: [50],
      bio: [''],
      // Teacher professional fields
      siret: [''],
      companyName: [''],
      // Student fields
      birthDate: [''],
      eloRating: [null],
      knowsElo: [false]
    });

  }

  ngOnInit(): void {
    // Load full profile from server (includes avatarUrl)
    this.authService.loadCurrentUserProfile().subscribe({
      next: () => this.loadProfile(),
      error: () => this.loadProfile() // Fallback to local data
    });

    // Load Stripe Connect status and balance for teachers
    if (this.authService.isTeacher()) {
      this.loadStripeConnectStatus();
      this.loadTeacherBalance();
    }

    // Handle Stripe Connect returns
    this.route.queryParams.subscribe(params => {
      // Handle Stripe Connect return
      if (params['stripe_connect'] === 'return') {
        this.stripeConnectSuccess.set(this.translateService.instant('settings.stripe.configurationComplete'));
        this.loadStripeConnectStatus();
        window.history.replaceState({}, '', '/settings');
        setTimeout(() => this.stripeConnectSuccess.set(null), 3000);
      }

      // Handle Stripe Connect refresh (incomplete onboarding)
      if (params['stripe_connect'] === 'refresh') {
        this.stripeConnectError.set(this.translateService.instant('settings.stripe.configurationIncomplete'));
        window.history.replaceState({}, '', '/settings');
      }
    });
  }

  loadProfile(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.profileForm.patchValue({
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        hourlyRate: user.hourlyRateCents ? user.hourlyRateCents / 100 : 50,
        bio: user.bio || '',
        // Professional fields
        siret: user.siret || '',
        companyName: user.companyName || '',
        // Student fields
        birthDate: user.birthDate || '',
        eloRating: user.eloRating || null,
        knowsElo: !!user.eloRating
      });
      // Load email reminder preference
      this.emailRemindersEnabled.set(user.emailRemindersEnabled !== false);
      // Load push notification preference
      this.pushNotificationsEnabled.set(user.pushNotificationsEnabled !== false);
      // Load languages for teachers
      if (user.languages && user.languages.length > 0) {
        this.selectedLanguages.set(user.languages);
        this.originalLanguages.set([...user.languages]);
      }
      // Load avatar for teachers
      if (user.avatarUrl) {
        this.avatarUrl.set(user.avatarUrl);
      }
    }
  }

  toggleLanguage(langCode: string): void {
    const current = this.selectedLanguages();
    if (current.includes(langCode)) {
      // Don't allow removing all languages
      if (current.length > 1) {
        this.selectedLanguages.set(current.filter(l => l !== langCode));
      }
    } else {
      this.selectedLanguages.set([...current, langCode]);
    }
  }

  isLanguageSelected(langCode: string): boolean {
    return this.selectedLanguages().includes(langCode);
  }

  isStudent(): boolean {
    return this.authService.isStudent();
  }

  isTeacher(): boolean {
    return this.authService.isTeacher();
  }

  validateBirthDate(event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = input.value;
    if (!value) return;

    const year = parseInt(value.split('-')[0], 10);
    if (isNaN(year) || year < this.minBirthYear || year > this.maxBirthYear) {
      input.value = '';
      this.profileForm.patchValue({ birthDate: '' });
    }
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];

    // Validate file type
    if (!file.type.startsWith('image/')) {
      this.avatarError.set(this.translateService.instant('errors.fileMustBeImage'));
      return;
    }

    // Validate file size (5MB max)
    if (file.size > 5 * 1024 * 1024) {
      this.avatarError.set(this.translateService.instant('errors.imageTooLarge'));
      return;
    }

    this.uploadAvatar(file);
  }

  uploadAvatar(file: File): void {
    this.uploadingAvatar.set(true);
    this.avatarError.set(null);

    const formData = new FormData();
    formData.append('file', file);

    this.http.post<{ success: boolean; avatarUrl: string }>('/api/upload/avatar', formData).subscribe({
      next: (response) => {
        this.uploadingAvatar.set(false);
        if (response.success) {
          this.avatarUrl.set(response.avatarUrl);
          this.authService.updateCurrentUser({ ...this.authService.currentUser()!, avatarUrl: response.avatarUrl });
        }
      },
      error: (err) => {
        this.uploadingAvatar.set(false);
        this.avatarError.set(err.error?.message || this.translateService.instant('errors.upload'));
      }
    });
  }

  deleteAvatar(): void {
    this.uploadingAvatar.set(true);
    this.avatarError.set(null);

    this.http.delete<{ success: boolean }>('/api/upload/avatar').subscribe({
      next: () => {
        this.uploadingAvatar.set(false);
        this.avatarUrl.set(null);
        this.authService.updateCurrentUser({ ...this.authService.currentUser()!, avatarUrl: undefined });
      },
      error: (err) => {
        this.uploadingAvatar.set(false);
        this.avatarError.set(err.error?.message || this.translateService.instant('errors.delete'));
      }
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid || this.savingProfile()) return;

    this.savingProfile.set(true);
    this.profileSuccess.set(false);
    this.profileError.set(null);

    const formValue = this.profileForm.value;
    const payload: UpdateUserRequest = {
      firstName: formValue.firstName,
      lastName: formValue.lastName
    };

    if (this.isTeacher()) {
      payload.hourlyRateCents = formValue.hourlyRate * 100;
      payload.bio = formValue.bio;
      payload.languages = this.selectedLanguages();
      // Professional fields
      payload.siret = formValue.siret;
      payload.companyName = formValue.companyName;
      // Optional ELO rating
      if (formValue.knowsElo && formValue.eloRating) {
        payload.eloRating = formValue.eloRating;
      } else {
        payload.clearEloRating = true;
      }
    }

    if (this.isStudent()) {
      if (formValue.birthDate) {
        payload.birthDate = formValue.birthDate;
      }
      // Optional ELO rating
      if (formValue.knowsElo && formValue.eloRating) {
        payload.eloRating = formValue.eloRating;
      } else {
        payload.clearEloRating = true;
      }
    }

    this.http.patch<User>('/api/users/me', payload).subscribe({
      next: (updatedUser) => {
        this.authService.updateCurrentUser(updatedUser);
        this.savingProfile.set(false);
        this.profileSuccess.set(true);
        // Reset form state and original languages
        this.profileForm.markAsPristine();
        this.originalLanguages.set([...this.selectedLanguages()]);
        setTimeout(() => this.profileSuccess.set(false), 3000);
      },
      error: (err) => {
        this.savingProfile.set(false);
        this.profileError.set(err.error?.message || this.translateService.instant('errors.save'));
      }
    });
  }

  toggleEmailReminders(): void {
    if (this.savingPreferences()) return;

    const newValue = !this.emailRemindersEnabled();
    this.savingPreferences.set(true);
    this.preferencesSuccess.set(false);
    this.preferencesError.set(null);

    this.http.patch<User>('/api/users/me', { emailRemindersEnabled: newValue } as UpdateUserRequest).subscribe({
      next: (updatedUser) => {
        this.emailRemindersEnabled.set(newValue);
        this.authService.updateCurrentUser(updatedUser);
        this.savingPreferences.set(false);
        this.preferencesSuccess.set(true);
        setTimeout(() => this.preferencesSuccess.set(false), 3000);
      },
      error: (err) => {
        this.savingPreferences.set(false);
        this.preferencesError.set(err.error?.message || this.translateService.instant('errors.save'));
      }
    });
  }

  async togglePushNotifications(): Promise<void> {
    if (this.savingPushPreferences()) return;

    const newValue = !this.pushNotificationsEnabled();
    this.savingPushPreferences.set(true);
    this.preferencesError.set(null);

    try {
      // If enabling, subscribe to push notifications
      if (newValue) {
        const subscribed = await this.pushNotificationService.subscribe();
        if (!subscribed) {
          // Permission denied or subscription failed
          this.savingPushPreferences.set(false);
          this.preferencesError.set(this.pushNotificationService.error() || this.translateService.instant('errors.pushPermissionDenied'));
          return;
        }
      } else {
        // Unsubscribe from push notifications
        await this.pushNotificationService.unsubscribe();
      }

      // Update server preference
      await this.pushNotificationService.updatePreference(newValue);
      this.pushNotificationsEnabled.set(newValue);
      this.preferencesSuccess.set(true);
      setTimeout(() => this.preferencesSuccess.set(false), 3000);
    } catch (err) {
      this.preferencesError.set(this.translateService.instant('errors.save'));
    } finally {
      this.savingPushPreferences.set(false);
    }
  }

  async enablePushNotifications(): Promise<void> {
    if (this.savingPushPreferences()) return;

    this.savingPushPreferences.set(true);
    this.preferencesError.set(null);

    try {
      const subscribed = await this.pushNotificationService.subscribe();
      if (subscribed) {
        await this.pushNotificationService.updatePreference(true);
        this.pushNotificationsEnabled.set(true);
        this.preferencesSuccess.set(true);
        setTimeout(() => this.preferencesSuccess.set(false), 3000);
      } else {
        this.preferencesError.set(this.pushNotificationService.error() || this.translateService.instant('errors.pushPermissionDenied'));
      }
    } catch (err) {
      this.preferencesError.set(this.translateService.instant('errors.save'));
    } finally {
      this.savingPushPreferences.set(false);
    }
  }

  logout(): void {
    this.authService.logout();
  }

  // Stripe Connect methods (teachers only)
  loadStripeConnectStatus(): void {
    this.stripeConnectService.getStatus().subscribe({
      next: (status) => this.stripeConnectStatus.set(status),
      error: () => this.stripeConnectStatus.set({ connected: false, accountExists: false, isReady: false })
    });
  }

  startStripeConnectOnboarding(): void {
    this.stripeConnectLoading.set(true);
    this.stripeConnectError.set(null);

    this.stripeConnectService.startOnboarding().subscribe({
      next: (response) => {
        this.stripeConnectLoading.set(false);
        if (!response.success) {
          this.stripeConnectError.set(response.message || this.translateService.instant('errors.stripeConfig'));
        }
        // If success, the service will redirect to Stripe
      },
      error: (err) => {
        this.stripeConnectLoading.set(false);
        this.stripeConnectError.set(err.error?.message || this.translateService.instant('errors.connection'));
      }
    });
  }

  continueStripeConnectOnboarding(): void {
    this.stripeConnectLoading.set(true);
    this.stripeConnectError.set(null);

    this.stripeConnectService.refreshOnboardingLink().subscribe({
      next: (response) => {
        this.stripeConnectLoading.set(false);
        if (!response.success) {
          this.stripeConnectError.set(response.message || this.translateService.instant('errors.stripeResume'));
        }
      },
      error: (err) => {
        this.stripeConnectLoading.set(false);
        this.stripeConnectError.set(err.error?.message || this.translateService.instant('errors.connection'));
      }
    });
  }

  async disconnectStripeConnect(): Promise<void> {
    const confirmed = await this.dialogService.confirm(
      this.translateService.instant('settings.stripe.disconnectConfirmMessage'),
      this.translateService.instant('settings.stripe.disconnectTitle'),
      { confirmText: this.translateService.instant('common.disconnect'), cancelText: this.translateService.instant('common.cancel'), variant: 'danger' }
    );
    if (!confirmed) return;

    this.stripeConnectLoading.set(true);
    this.stripeConnectError.set(null);

    this.stripeConnectService.disconnect().subscribe({
      next: () => {
        this.stripeConnectLoading.set(false);
        this.stripeConnectSuccess.set(this.translateService.instant('settings.stripe.accountDisconnected'));
        this.loadStripeConnectStatus();
        setTimeout(() => this.stripeConnectSuccess.set(null), 3000);
      },
      error: (err) => {
        this.stripeConnectLoading.set(false);
        this.stripeConnectError.set(err.error?.message || this.translateService.instant('errors.stripeDisconnect'));
      }
    });
  }

  // Teacher balance methods
  loadTeacherBalance(): void {
    this.stripeConnectService.getBalance().subscribe({
      next: (balance) => {
        this.teacherBalance.set(balance);
        // Set default withdraw amount to available balance (capped at available, min 100)
        const maxAmount = Math.floor(balance.availableBalanceCents / 100);
        this.withdrawAmount.set(Math.max(100, maxAmount));
      },
      error: () => this.teacherBalance.set(null)
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
    const balance = this.teacherBalance();
    const amount = this.withdrawAmount();

    if (!balance || amount < 100) return;

    const amountCents = amount * 100;
    if (amountCents > balance.availableBalanceCents) {
      this.stripeConnectError.set(this.translateService.instant('errors.amountExceedsBalance'));
      return;
    }

    const confirmed = await this.dialogService.confirm(
      this.translateService.instant('settings.stripe.withdrawConfirmMessage', { amount }),
      this.translateService.instant('settings.stripe.withdrawTitle'),
      { confirmText: this.translateService.instant('common.withdraw'), cancelText: this.translateService.instant('common.cancel'), variant: 'info' }
    );
    if (!confirmed) return;

    this.withdrawing.set(true);
    this.stripeConnectError.set(null);

    this.stripeConnectService.withdraw(amountCents).subscribe({
      next: (response) => {
        this.withdrawing.set(false);
        if (response.success) {
          this.stripeConnectSuccess.set(this.translateService.instant('settings.stripe.withdrawSuccess', { amount: this.formatCents(response.amountCents || 0) }));
          this.loadTeacherBalance();
          setTimeout(() => this.stripeConnectSuccess.set(null), 5000);
        } else {
          this.stripeConnectError.set(response.message || this.translateService.instant('errors.withdrawal'));
        }
      },
      error: (err) => {
        this.withdrawing.set(false);
        this.stripeConnectError.set(err.error?.message || this.translateService.instant('errors.withdrawal'));
      }
    });
  }

  recalculateBalance(): void {
    this.recalculatingBalance.set(true);
    this.stripeConnectError.set(null);

    this.stripeConnectService.recalculateBalance().subscribe({
      next: (response) => {
        this.recalculatingBalance.set(false);
        if (response.success) {
          this.stripeConnectSuccess.set(this.translateService.instant('settings.stripe.balanceRecalculatedSuccess'));
          this.loadTeacherBalance();
          setTimeout(() => this.stripeConnectSuccess.set(null), 3000);
        } else {
          this.stripeConnectError.set(response.message || this.translateService.instant('errors.recalculate'));
        }
      },
      error: (err) => {
        this.recalculatingBalance.set(false);
        this.stripeConnectError.set(err.error?.message || this.translateService.instant('errors.recalculate'));
      }
    });
  }

  // Share profile (teachers only)
  showShareMenu = signal(false);

  getProfileUrl(): string {
    const uuid = this.authService.currentUser()?.uuid;
    return `https://mychess.fr/coaches/${uuid}`;
  }

  toggleShareMenu(event?: Event): void {
    event?.stopPropagation();
    this.showShareMenu.update(v => !v);
  }

  closeShareMenu(): void {
    this.showShareMenu.set(false);
  }

  copyProfileLink(): void {
    const url = this.getProfileUrl();
    navigator.clipboard.writeText(url).then(() => {
      this.toastService.success(this.translateService.instant('settings.share.linkCopied'));
      this.closeShareMenu();
    });
  }

  shareOnTwitter(): void {
    const url = this.getProfileUrl();
    const text = this.translateService.instant('settings.share.twitterText');
    window.open(`https://twitter.com/intent/tweet?url=${encodeURIComponent(url)}&text=${encodeURIComponent(text)}`, '_blank');
    this.closeShareMenu();
  }

  shareOnFacebook(): void {
    const url = this.getProfileUrl();
    window.open(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`, '_blank');
    this.closeShareMenu();
  }

  shareOnLinkedIn(): void {
    const url = this.getProfileUrl();
    window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(url)}`, '_blank');
    this.closeShareMenu();
  }

  shareOnWhatsApp(): void {
    const url = this.getProfileUrl();
    const text = this.translateService.instant('settings.share.whatsappText');
    window.open(`https://wa.me/?text=${encodeURIComponent(text + ' ' + url)}`, '_blank');
    this.closeShareMenu();
  }

  // Account deletion (RGPD compliance)
  async deleteAccount(): Promise<void> {
    // First confirmation
    const firstConfirm = await this.dialogService.confirm(
      this.translateService.instant('settings.deleteAccount.confirmMessage'),
      this.translateService.instant('settings.deleteAccount.title'),
      { confirmText: this.translateService.instant('common.continue'), cancelText: this.translateService.instant('common.cancel'), variant: 'danger' }
    );
    if (!firstConfirm) return;

    // Ask for password
    const password = await this.dialogService.prompt(
      this.translateService.instant('settings.deleteAccount.passwordPrompt'),
      this.translateService.instant('settings.deleteAccount.confirmationRequired'),
      { confirmText: this.translateService.instant('settings.deleteAccount.deletePermanently'), cancelText: this.translateService.instant('common.cancel'), inputLabel: this.translateService.instant('common.password'), inputType: 'password', variant: 'danger' }
    );
    if (!password) return;

    this.deletingAccount.set(true);
    this.deleteAccountError.set(null);

    this.http.delete<{ message: string }>('/api/users/me', {
      body: { password }
    }).subscribe({
      next: () => {
        this.deletingAccount.set(false);
        this.toastService.success(this.translateService.instant('settings.deleteAccount.success'));
        this.authService.logout();
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.deletingAccount.set(false);
        this.deleteAccountError.set(err.error?.message || this.translateService.instant('errors.deleteAccount'));
      }
    });
  }
}
