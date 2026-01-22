import { Component, OnInit, signal, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { DialogService } from '../../core/services/dialog.service';
import { HttpClient } from '@angular/common/http';
import { CalendarService, CalendarStatus } from '../../core/services/calendar.service';
import { StripeConnectService, StripeConnectStatus } from '../../core/services/stripe-connect.service';
import { AVAILABLE_LANGUAGES } from '../../core/models/user.model';
import { AppSidebarComponent, SidebarSection } from '../../shared/components/app-sidebar/app-sidebar.component';
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
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NgIconComponent, AppSidebarComponent],
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
  passwordForm: FormGroup;

  savingProfile = signal(false);
  savingPassword = signal(false);
  savingPreferences = signal(false);
  profileSuccess = signal(false);
  passwordSuccess = signal(false);
  preferencesSuccess = signal(false);
  profileError = signal<string | null>(null);
  passwordError = signal<string | null>(null);
  preferencesError = signal<string | null>(null);

  emailRemindersEnabled = signal(true);

  // Languages
  availableLanguages = AVAILABLE_LANGUAGES;
  selectedLanguages = signal<string[]>(['FR']);

  // Google Calendar
  calendarStatus = signal<CalendarStatus>({ configured: false, connected: false, enabled: false });
  calendarLoading = signal(false);
  calendarSuccess = signal<string | null>(null);
  calendarError = signal<string | null>(null);

  // Stripe Connect (teachers only)
  stripeConnectStatus = signal<StripeConnectStatus>({ connected: false, accountExists: false, isReady: false });
  stripeConnectLoading = signal(false);
  stripeConnectSuccess = signal<string | null>(null);
  stripeConnectError = signal<string | null>(null);

  // Teacher balance and withdrawal
  teacherBalance = signal<{ availableBalanceCents: number; totalEarnedCents: number; totalWithdrawnCents: number } | null>(null);
  withdrawing = signal(false);
  withdrawAmount = signal(100); // Default 100€ minimum

  private dialogService = inject(DialogService);

  // Sidebar sections
  sidebarSections = computed<SidebarSection[]>(() => {
    const sections: SidebarSection[] = [];
    const menuItems: any[] = [
      { label: 'Mon Espace', icon: 'heroChartBarSquare', route: '/dashboard' },
      { label: 'Mes Cours', icon: 'heroCalendarDays', route: '/lessons' }
    ];
    if (this.authService.isTeacher()) {
      menuItems.push({ label: 'Mes Disponibilites', icon: 'heroClipboardDocumentList', route: '/availability' });
    }
    if (this.authService.isStudent()) {
      menuItems.push({ label: 'Ma Progression', icon: 'heroTrophy', route: '/progress' });
      menuItems.push({ label: 'Abonnement', icon: 'heroCreditCard', route: '/subscription' });
      menuItems.push({ label: 'Trouver un Coach', icon: 'heroAcademicCap', route: '/teachers' });
    }
    sections.push({ title: 'Menu', items: menuItems });
    sections.push({
      title: 'Compte',
      items: [
        { label: 'Mon Profil', icon: 'heroUserCircle', route: '/settings', active: true },
        { label: 'Mes Factures', icon: 'heroDocumentText', route: '/invoices' },
        { label: 'Deconnexion', icon: 'heroArrowRightOnRectangle', action: () => this.logout() }
      ]
    });
    return sections;
  });

  constructor(
    private fb: FormBuilder,
    public authService: AuthService,
    private http: HttpClient,
    private calendarService: CalendarService,
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
      acceptsSubscription: [true],
      bio: [''],
      // Teacher banking fields
      iban: [''],
      bic: [''],
      accountHolderName: [''],
      siret: [''],
      companyName: [''],
      // Student fields
      birthDate: [''],
      eloRating: [null],
      knowsElo: [false]
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadProfile();
    this.loadCalendarStatus();

    // Load Stripe Connect status and balance for teachers
    if (this.authService.isTeacher()) {
      this.loadStripeConnectStatus();
      this.loadTeacherBalance();
    }

    // Handle OAuth callbacks and Stripe Connect returns
    this.route.queryParams.subscribe(params => {
      if (params['calendar'] === 'callback' && params['code']) {
        this.handleCalendarCallback(params['code']);
      }

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
        acceptsSubscription: user.acceptsSubscription ?? true,
        bio: user.bio || '',
        // Banking fields
        iban: user.iban || '',
        bic: user.bic || '',
        accountHolderName: user.accountHolderName || '',
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

  saveProfile(): void {
    if (this.profileForm.invalid || this.savingProfile()) return;

    this.savingProfile.set(true);
    this.profileSuccess.set(false);
    this.profileError.set(null);

    const formValue = this.profileForm.value;
    const payload: any = {
      firstName: formValue.firstName,
      lastName: formValue.lastName
    };

    if (this.isTeacher()) {
      payload.hourlyRateCents = formValue.hourlyRate * 100;
      payload.acceptsSubscription = formValue.acceptsSubscription;
      payload.bio = formValue.bio;
      payload.languages = this.selectedLanguages();
      // Banking fields
      payload.iban = formValue.iban;
      payload.bic = formValue.bic;
      payload.accountHolderName = formValue.accountHolderName;
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

  toggleEmailReminders(): void {
    if (this.savingPreferences()) return;

    const newValue = !this.emailRemindersEnabled();
    this.savingPreferences.set(true);
    this.preferencesSuccess.set(false);
    this.preferencesError.set(null);

    this.http.patch<any>('/api/users/me', { emailRemindersEnabled: newValue }).subscribe({
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

  // Google Calendar methods
  loadCalendarStatus(): void {
    this.calendarService.getStatus().subscribe({
      next: (status) => this.calendarStatus.set(status),
      error: () => this.calendarStatus.set({ configured: false, connected: false, enabled: false })
    });
  }

  connectGoogleCalendar(): void {
    this.calendarLoading.set(true);
    this.calendarError.set(null);

    this.calendarService.getAuthUrl().subscribe({
      next: (response) => {
        this.calendarLoading.set(false);
        if (response.authUrl) {
          window.location.href = response.authUrl;
        } else {
          this.calendarError.set(response.message || 'Google Calendar non disponible');
        }
      },
      error: (err) => {
        this.calendarLoading.set(false);
        this.calendarError.set(err.error?.message || 'Erreur de connexion');
      }
    });
  }

  handleCalendarCallback(code: string): void {
    this.calendarLoading.set(true);
    this.calendarError.set(null);

    this.calendarService.handleCallback(code).subscribe({
      next: () => {
        this.calendarLoading.set(false);
        this.calendarSuccess.set('Google Calendar connecte avec succes !');
        this.loadCalendarStatus();
        // Clear URL params
        window.history.replaceState({}, '', '/settings');
        setTimeout(() => this.calendarSuccess.set(null), 3000);
      },
      error: (err) => {
        this.calendarLoading.set(false);
        this.calendarError.set(err.error?.message || 'Erreur lors de la connexion');
        window.history.replaceState({}, '', '/settings');
      }
    });
  }

  disconnectGoogleCalendar(): void {
    this.calendarLoading.set(true);
    this.calendarError.set(null);

    this.calendarService.disconnect().subscribe({
      next: () => {
        this.calendarLoading.set(false);
        this.calendarSuccess.set('Google Calendar deconnecte');
        this.loadCalendarStatus();
        setTimeout(() => this.calendarSuccess.set(null), 3000);
      },
      error: (err) => {
        this.calendarLoading.set(false);
        this.calendarError.set(err.error?.message || 'Erreur lors de la deconnexion');
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
}
