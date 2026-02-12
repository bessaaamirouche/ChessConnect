import { Component, signal, ChangeDetectionStrategy, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { SeoService } from '../../../core/services/seo.service';
import { ContactService } from '../../../core/services/contact.service';

@Component({
    selector: 'app-login',
    imports: [ReactiveFormsModule, RouterLink, TranslateModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './login.component.html',
    styleUrl: './login.component.scss'
})
export class LoginComponent {
  private seoService = inject(SeoService);
  private contactService = inject(ContactService);
  private translateService = inject(TranslateService);

  loginForm: FormGroup;
  contactForm: FormGroup;
  loading = signal(false);
  error = signal<string | null>(null);
  isSuspended = signal(false);
  showContactForm = signal(false);
  contactLoading = signal(false);
  contactSuccess = signal(false);
  contactError = signal<string | null>(null);
  showPassword = signal(false);

  returnUrl: string | null = null;

  // Email verification
  emailNotVerified = signal(false);
  unverifiedEmail = signal<string | null>(null);
  resendingEmail = signal(false);
  resendSuccess = signal(false);
  resendError = signal<string | null>(null);

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    this.seoService.setLoginPage();
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
    this.contactForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      subject: ['Contestation de suspension de compte', [Validators.required, Validators.minLength(5)]],
      message: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.loading.set(true);
    this.error.set(null);
    this.isSuspended.set(false);
    this.emailNotVerified.set(false);
    this.resendSuccess.set(false);
    this.resendError.set(null);

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        // Bloquer l'accès admin depuis la page de connexion publique
        if (this.authService.isAdmin()) {
          this.authService.logout();
          this.error.set(this.translateService.instant('errors.forbidden'));
          this.loading.set(false);
          return;
        }
        this.router.navigateByUrl(this.returnUrl || this.authService.getRedirectRoute());
      },
      error: (err) => {
        if (err.error?.code === 'ACCOUNT_SUSPENDED') {
          this.isSuspended.set(true);
          this.error.set(null);
          // Pre-fill the email in the contact form
          this.contactForm.patchValue({ email: this.loginForm.value.email });
        } else if (err.error?.code === 'EMAIL_NOT_VERIFIED') {
          this.emailNotVerified.set(true);
          this.unverifiedEmail.set(err.error?.email || this.loginForm.value.email);
          this.error.set(null);
        } else {
          this.error.set(err.error?.error || err.error?.message || this.translateService.instant('auth.errors.invalidCredentials'));
        }
        this.loading.set(false);
      }
    });
  }

  resendVerificationEmail(): void {
    const email = this.unverifiedEmail();
    if (!email) return;

    this.resendingEmail.set(true);
    this.resendSuccess.set(false);
    this.resendError.set(null);

    this.authService.resendVerificationEmail(email).subscribe({
      next: (response) => {
        this.resendingEmail.set(false);
        if (response.success) {
          this.resendSuccess.set(true);
        } else {
          this.resendError.set(response.message);
        }
      },
      error: (err) => {
        this.resendingEmail.set(false);
        this.resendError.set(err.error?.message || this.translateService.instant('errors.sendEmail'));
      }
    });
  }

  openContactForm(): void {
    this.showContactForm.set(true);
    this.contactSuccess.set(false);
    this.contactError.set(null);
  }

  closeContactForm(): void {
    this.showContactForm.set(false);
  }

  submitContact(): void {
    if (this.contactForm.invalid) return;

    this.contactLoading.set(true);
    this.contactError.set(null);

    this.contactService.contactAdmin(this.contactForm.value).subscribe({
      next: () => {
        this.contactSuccess.set(true);
        this.contactLoading.set(false);
      },
      error: (err) => {
        this.contactError.set(err.error?.error || this.translateService.instant('errors.sendMessage'));
        this.contactLoading.set(false);
      }
    });
  }
}
