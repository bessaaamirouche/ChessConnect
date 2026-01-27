import { Component, OnInit, signal, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { DialogService } from '../../core/services/dialog.service';
import { HttpClient } from '@angular/common/http';
import { StripeConnectService, StripeConnectStatus } from '../../core/services/stripe-connect.service';
import { AVAILABLE_LANGUAGES, User, UpdateUserRequest } from '../../core/models/user.model';
import { DateInputComponent } from '../../shared/components/date-input/date-input.component';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChartBarSquare,
  heroCalendarDays,
  heroClipboardDocumentList,
  heroTrophy,
  heroCreditCard,
  heroAcademicCap,
  heroUserCircle,
  heroArrowRightOnRectangle
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NgIconComponent, DateInputComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroChartBarSquare,
    heroCalendarDays,
    heroClipboardDocumentList,
    heroTrophy,
    heroCreditCard,
    heroAcademicCap,
    heroUserCircle,
    heroArrowRightOnRectangle
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

  private dialogService = inject(DialogService);

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
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      // Teacher fields
      hourlyRate: [50],
      acceptsFreeTrial: [true],
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
        this.stripeConnectSuccess.set('Configuration terminee !');
        this.loadStripeConnectStatus();
        window.history.replaceState({}, '', '/settings');
        setTimeout(() => this.stripeConnectSuccess.set(null), 3000);
      }

      // Handle Stripe Connect refresh (incomplete onboarding)
      if (params['stripe_connect'] === 'refresh') {
        this.stripeConnectError.set('Configuration incomplete. Veuillez reprendre le processus.');
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
        acceptsFreeTrial: user.acceptsFreeTrial ?? true,
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
      this.avatarError.set('Le fichier doit etre une image');
      return;
    }

    // Validate file size (5MB max)
    if (file.size > 5 * 1024 * 1024) {
      this.avatarError.set('L\'image ne doit pas depasser 5 Mo');
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
        this.avatarError.set(err.error?.message || 'Erreur lors de l\'upload');
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
        this.avatarError.set(err.error?.message || 'Erreur lors de la suppression');
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
      payload.acceptsFreeTrial = formValue.acceptsFreeTrial;
      payload.bio = formValue.bio;
      payload.languages = this.selectedLanguages();
      // Professional fields
      payload.siret = formValue.siret;
      payload.companyName = formValue.companyName;
    }

    if (this.isStudent()) {
      if (formValue.birthDate) {
        payload.birthDate = formValue.birthDate;
      }
      if (formValue.knowsElo && formValue.eloRating) {
        payload.eloRating = formValue.eloRating;
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
        this.profileError.set(err.error?.message || 'Erreur lors de la sauvegarde');
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
        this.preferencesError.set(err.error?.message || 'Erreur lors de la sauvegarde');
      }
    });
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
          this.stripeConnectError.set(response.message || 'Erreur lors de la configuration');
        }
        // If success, the service will redirect to Stripe
      },
      error: (err) => {
        this.stripeConnectLoading.set(false);
        this.stripeConnectError.set(err.error?.message || 'Erreur de connexion');
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
          this.stripeConnectError.set(response.message || 'Erreur lors de la reprise');
        }
      },
      error: (err) => {
        this.stripeConnectLoading.set(false);
        this.stripeConnectError.set(err.error?.message || 'Erreur de connexion');
      }
    });
  }

  async disconnectStripeConnect(): Promise<void> {
    const confirmed = await this.dialogService.confirm(
      'Etes-vous sur de vouloir deconnecter votre compte ? Vous ne pourrez plus recevoir de paiements.',
      'Deconnexion Stripe',
      { confirmText: 'Deconnecter', cancelText: 'Annuler', variant: 'danger' }
    );
    if (!confirmed) return;

    this.stripeConnectLoading.set(true);
    this.stripeConnectError.set(null);

    this.stripeConnectService.disconnect().subscribe({
      next: () => {
        this.stripeConnectLoading.set(false);
        this.stripeConnectSuccess.set('Compte deconnecte');
        this.loadStripeConnectStatus();
        setTimeout(() => this.stripeConnectSuccess.set(null), 3000);
      },
      error: (err) => {
        this.stripeConnectLoading.set(false);
        this.stripeConnectError.set(err.error?.message || 'Erreur lors de la deconnexion');
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
      this.stripeConnectError.set('Le montant depasse votre solde disponible');
      return;
    }

    const confirmed = await this.dialogService.confirm(
      `Voulez-vous retirer ${amount} EUR vers votre compte bancaire ?`,
      'Retirer mes gains',
      { confirmText: 'Retirer', cancelText: 'Annuler', variant: 'info' }
    );
    if (!confirmed) return;

    this.withdrawing.set(true);
    this.stripeConnectError.set(null);

    this.stripeConnectService.withdraw(amountCents).subscribe({
      next: (response) => {
        this.withdrawing.set(false);
        if (response.success) {
          this.stripeConnectSuccess.set(`Retrait de ${this.formatCents(response.amountCents || 0)} effectue !`);
          this.loadTeacherBalance();
          setTimeout(() => this.stripeConnectSuccess.set(null), 5000);
        } else {
          this.stripeConnectError.set(response.message || 'Erreur lors du retrait');
        }
      },
      error: (err) => {
        this.withdrawing.set(false);
        this.stripeConnectError.set(err.error?.message || 'Erreur lors du retrait');
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
          this.stripeConnectSuccess.set('Balance recalculee avec succes !');
          this.loadTeacherBalance();
          setTimeout(() => this.stripeConnectSuccess.set(null), 3000);
        } else {
          this.stripeConnectError.set(response.message || 'Erreur lors du recalcul');
        }
      },
      error: (err) => {
        this.recalculatingBalance.set(false);
        this.stripeConnectError.set(err.error?.message || 'Erreur lors du recalcul');
      }
    });
  }
}
