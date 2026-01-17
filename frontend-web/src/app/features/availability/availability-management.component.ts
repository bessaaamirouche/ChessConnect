import { Component, OnInit, inject, ViewChild, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AvailabilityService } from '../../core/services/availability.service';
import { AuthService } from '../../core/services/auth.service';
import { DAYS_OF_WEEK, HOURS, MINUTES, DayOfWeek, AvailabilityRequest } from '../../core/models/availability.model';
import { ConfirmModalComponent } from '../../shared/confirm-modal/confirm-modal.component';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChartBarSquare,
  heroCalendarDays,
  heroClipboardDocumentList,
  heroUserCircle,
  heroArrowRightOnRectangle,
  heroPlus,
  heroTrash,
  heroXMark,
  heroClock,
  heroBars3
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-availability-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ConfirmModalComponent, NgIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroChartBarSquare,
    heroCalendarDays,
    heroClipboardDocumentList,
    heroUserCircle,
    heroArrowRightOnRectangle,
    heroPlus,
    heroTrash,
    heroXMark,
    heroClock,
    heroBars3
  })],
  templateUrl: './availability-management.component.html',
  styleUrl: './availability-management.component.scss'
})
export class AvailabilityManagementComponent implements OnInit {
  @ViewChild('confirmModal') confirmModal!: ConfirmModalComponent;

  private availabilityService = inject(AvailabilityService);
  authService = inject(AuthService);

  readonly daysOfWeek = DAYS_OF_WEEK;
  readonly hours = HOURS;
  readonly minutes = MINUTES;
  // Start hours limited to 00-23 (last slot starts at 23:00, ends at 00:00)
  readonly availableStartHours = HOURS;

  myAvailabilities = this.availabilityService.myAvailabilities;
  loading = this.availabilityService.loading;
  error = this.availabilityService.error;

  sidebarOpen = signal(false);
  showAddModal = false;
  isRecurring = true;
  selectedDay: DayOfWeek = 'MONDAY';
  selectedDate = '';
  startHour = '09';
  startMinute = '00';
  submitting = false;

  ngOnInit(): void {
    this.loadAvailabilities();
  }

  loadAvailabilities(): void {
    this.availabilityService.loadMyAvailabilities().subscribe();
  }

  openAddModal(): void {
    this.resetForm();
    this.showAddModal = true;
  }

  closeModal(): void {
    this.showAddModal = false;
    this.resetForm();
  }

  resetForm(): void {
    this.isRecurring = true;
    this.selectedDay = 'MONDAY';
    this.selectedDate = '';
    this.startHour = '09';
    this.startMinute = '00';
  }

  get startTime(): string {
    return `${this.startHour}:${this.startMinute}`;
  }

  get endTime(): string {
    // Calculate end time as start time + 1 hour
    const startH = parseInt(this.startHour, 10);
    const endH = (startH + 1) % 24;
    return `${endH.toString().padStart(2, '0')}:${this.startMinute}`;
  }

  onRecurringChange(): void {
    if (this.isRecurring) {
      this.selectedDate = '';
    } else {
      this.selectedDay = 'MONDAY';
    }
  }

  isFormValid(): boolean {
    if (!this.startTime) return false;
    if (!this.isRecurring && !this.selectedDate) return false;
    return true;
  }

  createAvailability(): void {
    if (!this.isFormValid() || this.submitting) return;

    this.submitting = true;

    const request: AvailabilityRequest = {
      startTime: this.startTime,
      endTime: this.endTime,
      isRecurring: this.isRecurring
    };

    if (this.isRecurring) {
      request.dayOfWeek = this.selectedDay;
    } else {
      request.specificDate = this.selectedDate;
    }

    this.availabilityService.createAvailability(request).subscribe({
      next: () => {
        this.closeModal();
        this.submitting = false;
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  async deleteAvailability(id: number): Promise<void> {
    const confirmed = await this.confirmModal.open({
      title: 'Supprimer la disponibilité',
      message: 'Êtes-vous sûr de vouloir supprimer cette disponibilité ?',
      confirmText: 'Supprimer',
      cancelText: 'Annuler',
      type: 'danger'
    });

    if (confirmed) {
      this.availabilityService.deleteAvailability(id).subscribe();
    }
  }

  getAvailabilitiesByDay(day: DayOfWeek) {
    return this.myAvailabilities().filter(a => a.isRecurring && a.dayOfWeek === day);
  }

  getSpecificDateAvailabilities() {
    return this.myAvailabilities().filter(a => !a.isRecurring);
  }

  hasAnyAvailability(): boolean {
    return this.myAvailabilities().length > 0;
  }

  getTodayDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  clearError(): void {
    this.availabilityService.clearError();
  }

  logout(): void {
    this.authService.logout();
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }
}
