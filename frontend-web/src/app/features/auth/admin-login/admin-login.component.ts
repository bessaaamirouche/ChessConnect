import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-login.component.html',
  styleUrl: './admin-login.component.scss'
})
export class AdminLoginComponent {
  loginForm: FormGroup;
  loading = signal(false);
  error = signal<string | null>(null);

  // Identifiants admin codés en dur
  private readonly ADMIN_USERNAME = '503412850';
  private readonly ADMIN_PASSWORD = '94D723044158a!';
  private readonly ADMIN_EMAIL = 'admin@mychess.fr';

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

    const { username, password } = this.loginForm.value;

    // Vérification locale des identifiants
    if (username !== this.ADMIN_USERNAME || password !== this.ADMIN_PASSWORD) {
      this.error.set('Identifiants incorrects');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    // Connexion avec l'email admin réel
    this.authService.login({
      email: this.ADMIN_EMAIL,
      password: this.ADMIN_PASSWORD
    }).subscribe({
      next: () => {
        this.router.navigate(['/admin']);
      },
      error: (err) => {
        this.error.set(err.error?.error || err.error?.message || 'Erreur de connexion');
        this.loading.set(false);
      }
    });
  }
}
