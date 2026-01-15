import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { HttpClient } from '@angular/common/http';
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
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NgIconComponent],
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
  profileSuccess = signal(false);
  passwordSuccess = signal(false);
  profileError = signal<string | null>(null);
  passwordError = signal<string | null>(null);

  constructor(
    private fb: FormBuilder,
    public authService: AuthService,
    private http: HttpClient
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      // Teacher fields
      hourlyRate: [50],
      acceptsSubscription: [true],
      bio: [''],
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
        birthDate: user.birthDate || '',
        eloRating: user.eloRating || null,
        knowsElo: !!user.eloRating
      });
    }
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

  logout(): void {
    this.authService.logout();
  }
}
