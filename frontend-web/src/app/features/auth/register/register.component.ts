import { Component, signal, ChangeDetectionStrategy, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { SeoService } from '../../../core/services/seo.service';
import { NotificationCenterService } from '../../../core/services/notification-center.service';
import { ToastService } from '../../../core/services/toast.service';
import { UserRole, AVAILABLE_LANGUAGES } from '../../../core/models/user.model';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroAcademicCap, heroUserGroup, heroPlayCircle, heroArrowRight } from '@ng-icons/heroicons/outline';
import { DateInputComponent } from '../../../shared/components/date-input/date-input.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NgIconComponent, DateInputComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({ heroAcademicCap, heroUserGroup, heroPlayCircle, heroArrowRight })],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private seoService = inject(SeoService);
  private notificationCenter = inject(NotificationCenterService);
  private toastService = inject(ToastService);

  registerForm: FormGroup;
  loading = signal(false);
  error = signal<string | null>(null);
  selectedRole = signal<UserRole>('STUDENT');
  success = signal(false);
  registeredEmail = signal<string | null>(null);
  resendLoading = signal(false);
  resendSuccess = signal(false);
  availableLanguages = AVAILABLE_LANGUAGES;
  selectedLanguages = signal<string[]>(['FR']);
  maxBirthDate = new Date().toISOString().split('T')[0]; // Today's date in YYYY-MM-DD format
  minBirthYear = 1920;
  maxBirthYear = new Date().getFullYear();
  showPassword = signal(false);

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.seoService.setRegisterPage();
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      role: ['STUDENT', Validators.required],
      // Teacher fields
      hourlyRate: [50],
      acceptsFreeTrial: [true],
      bio: [''],
      // Student fields
      birthDate: [''],
      eloRating: [null],
      knowsElo: [false]
    });
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

  validateBirthDate(event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = input.value;
    if (!value) return;

    const year = parseInt(value.split('-')[0], 10);
    if (isNaN(year) || year < this.minBirthYear || year > this.maxBirthYear) {
      input.value = '';
      this.registerForm.patchValue({ birthDate: '' });
    }
  }

  selectRole(role: UserRole): void {
    this.selectedRole.set(role);
    this.registerForm.patchValue({ role });
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const formValue = { ...this.registerForm.value };
    if (formValue.role === 'STUDENT') {
      delete formValue.hourlyRate;
      delete formValue.acceptsFreeTrial;
      delete formValue.bio;
      // Handle ELO - only include if user knows it
      if (!formValue.knowsElo || !formValue.eloRating) {
        delete formValue.eloRating;
      }
      delete formValue.knowsElo;
      // birthDate is already in correct format (YYYY-MM-DD)
      if (!formValue.birthDate) {
        delete formValue.birthDate;
      }
    } else {
      // Convertir euros en centimes pour le backend
      formValue.hourlyRateCents = formValue.hourlyRate * 100;
      delete formValue.hourlyRate;
      delete formValue.birthDate;
      delete formValue.eloRating;
      delete formValue.knowsElo;
      // Add selected languages
      formValue.languages = this.selectedLanguages();
    }

    this.authService.register(formValue).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.success.set(true);
        this.registeredEmail.set(response.email);
        // User needs to verify email before logging in
      },
      error: (err) => {
        this.error.set(err.error?.error || err.error?.message || 'Une erreur est survenue');
        this.loading.set(false);
      }
    });
  }

  goToQuiz(): void {
    this.router.navigate(['/quiz']);
  }

  skipQuiz(): void {
    this.router.navigate(['/dashboard']);
  }

  resendVerificationEmail(): void {
    const email = this.registeredEmail();
    if (!email || this.resendLoading()) return;

    this.resendLoading.set(true);
    this.authService.resendVerificationEmail(email).subscribe({
      next: () => {
        this.resendLoading.set(false);
        this.resendSuccess.set(true);
        this.toastService.success('Email de verification renvoye !');
      },
      error: (err) => {
        this.resendLoading.set(false);
        const errorMsg = err.error?.error || 'Erreur lors de l\'envoi';
        this.toastService.error(errorMsg);
      }
    });
  }
}
