import { Component, signal, ChangeDetectionStrategy, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { SeoService } from '../../../core/services/seo.service';
import { ContactService } from '../../../core/services/contact.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private seoService = inject(SeoService);
  private contactService = inject(ContactService);

  loginForm: FormGroup;
  contactForm: FormGroup;
  loading = signal(false);
  error = signal<string | null>(null);
  isSuspended = signal(false);
  showContactForm = signal(false);
  contactLoading = signal(false);
  contactSuccess = signal(false);
  contactError = signal<string | null>(null);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
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

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.router.navigate([this.authService.getRedirectRoute()]);
      },
      error: (err) => {
        if (err.error?.code === 'ACCOUNT_SUSPENDED') {
          this.isSuspended.set(true);
          this.error.set(null);
          // Pre-fill the email in the contact form
          this.contactForm.patchValue({ email: this.loginForm.value.email });
        } else {
          this.error.set(err.error?.error || err.error?.message || 'Email ou mot de passe incorrect');
        }
        this.loading.set(false);
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
        this.contactError.set(err.error?.error || 'Erreur lors de l\'envoi du message. Veuillez reessayer.');
        this.contactLoading.set(false);
      }
    });
  }
}
