import { Component, OnInit, signal, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent implements OnInit {
  form: FormGroup;
  token = '';
  loading = signal(false);
  validating = signal(true);
  tokenValid = signal(false);
  success = signal(false);
  error = signal<string | null>(null);

  private translateService = inject(TranslateService);

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';

    if (!this.token) {
      this.validating.set(false);
      this.error.set(this.translateService.instant('errors.tokenInvalid'));
      return;
    }

    // Validate token
    this.http.get<{ valid: boolean }>(`/api/auth/reset-password/validate?token=${this.token}`)
      .subscribe({
        next: (response) => {
          this.validating.set(false);
          this.tokenValid.set(response.valid);
          if (!response.valid) {
            this.error.set(this.translateService.instant('errors.resetLinkInvalid'));
          }
        },
        error: () => {
          this.validating.set(false);
          this.error.set(this.translateService.instant('errors.linkValidation'));
        }
      });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.form.value.newPassword !== this.form.value.confirmPassword) {
      this.error.set(this.translateService.instant('auth.errors.passwordMismatch'));
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.http.post('/api/auth/reset-password', {
      token: this.token,
      newPassword: this.form.value.newPassword
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.error || err.error?.message || this.translateService.instant('errors.generic'));
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
