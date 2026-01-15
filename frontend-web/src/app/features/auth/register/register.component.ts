import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole } from '../../../core/models/user.model';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroAcademicCap, heroUserGroup, heroPlayCircle, heroArrowRight } from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NgIconComponent],
  viewProviders: [provideIcons({ heroAcademicCap, heroUserGroup, heroPlayCircle, heroArrowRight })],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  registerForm: FormGroup;
  loading = signal(false);
  error = signal<string | null>(null);
  selectedRole = signal<UserRole>('STUDENT');
  success = signal(false);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      role: ['STUDENT', Validators.required],
      // Teacher fields
      hourlyRate: [50],
      acceptsSubscription: [true],
      bio: [''],
      // Student fields
      birthDate: [''],
      eloRating: [null],
      knowsElo: [false]
    });
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
      delete formValue.acceptsSubscription;
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
    }

    this.authService.register(formValue).subscribe({
      next: () => {
        // For students, show success screen with quiz option
        if (this.selectedRole() === 'STUDENT') {
          this.success.set(true);
          this.loading.set(false);
        } else {
          // Teachers go directly to dashboard
          this.router.navigate([this.authService.getRedirectRoute()]);
        }
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Une erreur est survenue');
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
}
