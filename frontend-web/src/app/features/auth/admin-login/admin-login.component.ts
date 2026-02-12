import { Component, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
    selector: 'app-admin-login',
    imports: [ReactiveFormsModule, TranslateModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './admin-login.component.html',
    styleUrl: './admin-login.component.scss'
})
export class AdminLoginComponent {
  loginForm: FormGroup;
  loading = signal(false);
  error = signal<string | null>(null);

  private translate = inject(TranslateService);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    // Appel direct à l'endpoint admin-login
    this.authService.adminLogin(this.loginForm.value).subscribe({
      next: () => {
        this.router.navigate(['/mint/dashboard']);
      },
      error: (err) => {
        this.error.set(err.error?.error || err.error?.message || this.translate.instant('auth.adminLogin.invalidCredentials'));
        this.loading.set(false);
      }
    });
  }
}
