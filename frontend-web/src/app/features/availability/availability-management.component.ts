import { Component, OnInit, inject, ChangeDetectionStrategy, signal, computed, viewChild } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AvailabilityService } from '../../core/services/availability.service';
import { SeoService } from '../../core/services/seo.service';
import { AuthService } from '../../core/services/auth.service';
import { DAYS_OF_WEEK, HOURS, MINUTES, DayOfWeek, AvailabilityRequest } from '../../core/models/availability.model';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { DateInputComponent } from '../../shared/components/date-input/date-input.component';
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
    imports: [FormsModule, RouterLink, ConfirmDialogComponent, NgIconComponent, DateInputComponent, TranslateModule],
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
  readonly confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  private availabilityService = inject(AvailabilityService);
  private seoService = inject(SeoService);
  private translateService = inject(TranslateService);
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
    this.seoService.setAvailabilityPage();
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
    const confirmed = await this.confirmDialog().open({
      title: this.translateService.instant('availability.confirmDelete.title'),
      message: this.translateService.instant('availability.confirmDelete.message'),
      confirmText: this.translateService.instant('availability.confirmDelete.confirm'),
      cancelText: this.translateService.instant('availability.confirmDelete.cancel'),
      type: 'danger',
      icon: 'trash'
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

  getMaxDate(): string {
    const maxDate = new Date();
    maxDate.setFullYear(maxDate.getFullYear() + 1); // Max 1 year in the future
    return maxDate.toISOString().split('T')[0];
  }

  validateAvailabilityDate(event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = input.value;
    if (!value) return;

    const selectedDate = new Date(value);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const maxDate = new Date();
    maxDate.setFullYear(maxDate.getFullYear() + 1);

    if (selectedDate < today || selectedDate > maxDate) {
      input.value = '';
      this.selectedDate = '';
    }
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
