import { Component, forwardRef, Input, signal, computed, OnInit, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';

@Component({
  selector: 'app-date-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DateInputComponent),
      multi: true
    }
  ],
  template: `
    <div class="date-input-wrapper">
      <div class="date-input-group" [class.date-input-group--small]="size === 'sm'">
        <input
          type="text"
          inputmode="numeric"
          maxlength="2"
          [placeholder]="'JJ'"
          [value]="day()"
          (input)="onDayInput($event)"
          (blur)="onBlur()"
          (keydown)="onKeyDown($event, 'day')"
          class="date-input date-input--day"
          #dayInput
        >
        <span class="date-separator">/</span>
        <input
          type="text"
          inputmode="numeric"
          maxlength="2"
          [placeholder]="'MM'"
          [value]="month()"
          (input)="onMonthInput($event)"
          (blur)="onBlur()"
          (keydown)="onKeyDown($event, 'month')"
          class="date-input date-input--month"
          #monthInput
        >
        <span class="date-separator">/</span>
        <input
          type="text"
          inputmode="numeric"
          maxlength="4"
          [placeholder]="'AAAA'"
          [value]="year()"
          (input)="onYearInput($event)"
          (blur)="onBlur()"
          (keydown)="onKeyDown($event, 'year')"
          class="date-input date-input--year"
          #yearInput
        >
        <button type="button" class="calendar-btn" (click)="toggleCalendar($event)" title="Ouvrir le calendrier">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
        </button>
      </div>

      @if (showCalendar()) {
        <div class="calendar-backdrop" (click)="showCalendar.set(false)"></div>
        <div class="calendar-dropdown" [style.top.px]="calendarTop()" [style.left.px]="calendarLeft()">
          <div class="calendar-header">
            <button type="button" class="calendar-nav" (click)="prevMonth($event)">&lt;</button>
            <span class="calendar-title">{{ monthNames[calendarMonth()] }} {{ calendarYear() }}</span>
            <button type="button" class="calendar-nav" (click)="nextMonth($event)">&gt;</button>
          </div>
          <div class="calendar-weekdays">
            @for (day of weekDays; track day) {
              <span class="weekday">{{ day }}</span>
            }
          </div>
          <div class="calendar-days">
            @for (day of calendarDays(); track $index) {
              @if (day === 0) {
                <span class="calendar-day calendar-day--empty"></span>
              } @else {
                <button
                  type="button"
                  class="calendar-day"
                  [class.calendar-day--today]="isToday(day)"
                  [class.calendar-day--selected]="isSelected(day)"
                  [class.calendar-day--disabled]="isDisabled(day)"
                  [disabled]="isDisabled(day)"
                  (click)="selectDate(day, $event)"
                >
                  {{ day }}
                </button>
              }
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .date-input-wrapper {
      position: relative;
      display: inline-block;
      z-index: 10;
    }

    .date-input-group {
      display: inline-flex;
      align-items: center;
      background: var(--bg-tertiary, #1a1a1a);
      border: 1px solid var(--border-subtle, #333);
      border-radius: var(--radius-md, 8px);
      padding: 0 8px 0 12px;
      gap: 4px;
      transition: border-color 0.2s;

      &:focus-within {
        border-color: var(--gold-500, #d4a84b);
      }

      &--small {
        padding: 0 6px 0 8px;

        .date-input {
          padding: 8px 0;
          font-size: 0.875rem;
        }

        .calendar-btn {
          padding: 6px;
        }
      }
    }

    .date-input {
      background: transparent;
      border: none;
      color: var(--text-primary, #fff);
      font-size: 1rem;
      text-align: center;
      padding: 12px 0;

      &:focus {
        outline: none;
      }

      &::placeholder {
        color: var(--text-muted, #666);
      }

      &--day,
      &--month {
        width: 28px;
      }

      &--year {
        width: 48px;
      }
    }

    .date-separator {
      color: var(--text-muted, #666);
      font-size: 1rem;
      user-select: none;
    }

    .calendar-btn {
      background: transparent;
      border: none;
      color: var(--text-muted, #888);
      cursor: pointer;
      padding: 8px;
      border-radius: var(--radius-sm, 4px);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;
      margin-left: 4px;

      &:hover {
        color: var(--gold-500, #d4a84b);
        background: rgba(212, 168, 75, 0.1);
      }
    }

    .calendar-backdrop {
      display: none;

      @media (max-width: 768px) {
        display: block;
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.5);
        z-index: 99998;
      }
    }

    .calendar-dropdown {
      position: fixed;
      z-index: 99999;
      background: var(--bg-secondary, #1a1a1a);
      border: 1px solid var(--border-subtle, #333);
      border-radius: var(--radius-lg, 12px);
      padding: 10px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
      min-width: 240px;

      @media (max-width: 768px) {
        left: 50% !important;
        right: auto;
        transform: translateX(-50%);
        top: auto;
        bottom: 20px;
        min-width: auto;
        width: calc(100vw - 40px);
        max-width: 280px;
      }
    }

    .calendar-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;
    }

    .calendar-nav {
      background: transparent;
      border: none;
      color: var(--text-secondary, #aaa);
      cursor: pointer;
      padding: 6px 10px;
      border-radius: var(--radius-sm, 4px);
      font-size: 0.875rem;
      transition: all 0.2s;
      min-height: 32px;
      min-width: 32px;
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        background: var(--bg-tertiary, #222);
        color: var(--text-primary, #fff);
      }
    }

    .calendar-title {
      font-weight: 600;
      color: var(--text-primary, #fff);
      font-size: 0.8125rem;
    }

    .calendar-weekdays {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 1px;
      margin-bottom: 4px;
    }

    .weekday {
      text-align: center;
      font-size: 0.6875rem;
      font-weight: 600;
      color: var(--text-muted, #666);
      padding: 2px;
      text-transform: uppercase;
      min-height: 28px;
      min-width: 28px;
      display: flex;
      align-items: center;
      justify-content: center;

      @media (max-width: 768px) {
        min-height: 32px;
        min-width: 32px;
      }
    }

    .calendar-days {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 1px;
    }

    .calendar-day {
      aspect-ratio: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.8125rem;
      color: var(--text-primary, #fff);
      background: transparent;
      border: none;
      border-radius: var(--radius-sm, 4px);
      cursor: pointer;
      transition: all 0.15s;
      min-height: 28px;
      min-width: 28px;

      @media (max-width: 768px) {
        min-height: 32px;
        min-width: 32px;
        font-size: 0.875rem;
      }

      &:hover:not(:disabled) {
        background: var(--bg-tertiary, #222);
      }

      &--empty {
        cursor: default;
      }

      &--today {
        border: 1px solid var(--gold-500, #d4a84b);
      }

      &--selected {
        background: var(--gold-500, #d4a84b) !important;
        color: #000 !important;
        font-weight: 600;
      }

      &--disabled {
        color: var(--text-muted, #444);
        cursor: not-allowed;
        opacity: 0.4;
      }
    }
  `]
})
export class DateInputComponent implements ControlValueAccessor, OnInit {
  @Input() size: 'md' | 'sm' = 'md';
  @Input() min?: string;
  @Input() max?: string;

  day = signal('');
  month = signal('');
  year = signal('');
  showCalendar = signal(false);
  calendarMonth = signal(new Date().getMonth());
  calendarYear = signal(new Date().getFullYear());
  calendarTop = signal(0);
  calendarLeft = signal(0);

  weekDays = ['Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa', 'Di'];
  monthNames = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(private elementRef: ElementRef) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.showCalendar.set(false);
    }
  }

  calendarDays = computed(() => {
    const year = this.calendarYear();
    const month = this.calendarMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();

    // Get day of week (0 = Sunday, adjust for Monday start)
    let startDay = firstDay.getDay() - 1;
    if (startDay < 0) startDay = 6;

    const days: number[] = [];

    // Add empty slots for days before the 1st
    for (let i = 0; i < startDay; i++) {
      days.push(0);
    }

    // Add days of the month
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }

    return days;
  });

  ngOnInit() {
    // Initialize calendar to current date or selected date
    const now = new Date();
    this.calendarMonth.set(now.getMonth());
    this.calendarYear.set(now.getFullYear());
  }

  writeValue(value: string): void {
    if (value) {
      // Expected format: yyyy-mm-dd
      const parts = value.split('-');
      if (parts.length === 3) {
        this.year.set(parts[0]);
        this.month.set(parts[1]);
        this.day.set(parts[2]);
        // Update calendar view
        this.calendarYear.set(parseInt(parts[0], 10));
        this.calendarMonth.set(parseInt(parts[1], 10) - 1);
      }
    } else {
      this.day.set('');
      this.month.set('');
      this.year.set('');
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  toggleCalendar(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.showCalendar.set(!this.showCalendar());

    // If opening, set calendar to current selection or today
    if (this.showCalendar()) {
      if (this.year() && this.month()) {
        this.calendarYear.set(parseInt(this.year(), 10));
        this.calendarMonth.set(parseInt(this.month(), 10) - 1);
      } else {
        const now = new Date();
        this.calendarMonth.set(now.getMonth());
        this.calendarYear.set(now.getFullYear());
      }

      // Calculate position for fixed positioning
      const rect = this.elementRef.nativeElement.getBoundingClientRect();
      const calendarHeight = 280; // Approximate height
      const calendarWidth = 240;
      const viewportHeight = window.innerHeight;
      const viewportWidth = window.innerWidth;

      // Check if mobile
      if (viewportWidth <= 768) {
        // Mobile: center horizontally, position at bottom
        this.calendarTop.set(viewportHeight - calendarHeight - 20);
        this.calendarLeft.set((viewportWidth - calendarWidth) / 2);
      } else {
        // Desktop: position below input, check if fits
        let top = rect.bottom + 8;
        let left = rect.left;

        // If calendar would go below viewport, show above input
        if (top + calendarHeight > viewportHeight) {
          top = rect.top - calendarHeight - 8;
        }

        // If calendar would go off right edge, align to right of input
        if (left + calendarWidth > viewportWidth) {
          left = rect.right - calendarWidth;
        }

        this.calendarTop.set(Math.max(8, top));
        this.calendarLeft.set(Math.max(8, left));
      }
    }
  }

  prevMonth(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.calendarMonth() === 0) {
      this.calendarMonth.set(11);
      this.calendarYear.set(this.calendarYear() - 1);
    } else {
      this.calendarMonth.set(this.calendarMonth() - 1);
    }
  }

  nextMonth(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.calendarMonth() === 11) {
      this.calendarMonth.set(0);
      this.calendarYear.set(this.calendarYear() + 1);
    } else {
      this.calendarMonth.set(this.calendarMonth() + 1);
    }
  }

  selectDate(dayNum: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    const selectedYear = this.calendarYear();
    const selectedMonth = this.calendarMonth() + 1;

    this.day.set(dayNum.toString().padStart(2, '0'));
    this.month.set(selectedMonth.toString().padStart(2, '0'));
    this.year.set(selectedYear.toString());

    this.showCalendar.set(false);
    this.emitValue();
  }

  isToday(dayNum: number): boolean {
    const today = new Date();
    return dayNum === today.getDate() &&
           this.calendarMonth() === today.getMonth() &&
           this.calendarYear() === today.getFullYear();
  }

  isSelected(dayNum: number): boolean {
    return dayNum === parseInt(this.day(), 10) &&
           this.calendarMonth() === parseInt(this.month(), 10) - 1 &&
           this.calendarYear() === parseInt(this.year(), 10);
  }

  isDisabled(dayNum: number): boolean {
    const date = new Date(this.calendarYear(), this.calendarMonth(), dayNum);

    if (this.min) {
      const minDate = new Date(this.min);
      minDate.setHours(0, 0, 0, 0);
      if (date < minDate) return true;
    }

    if (this.max) {
      const maxDate = new Date(this.max);
      maxDate.setHours(23, 59, 59, 999);
      if (date > maxDate) return true;
    }

    return false;
  }

  onDayInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');

    if (value.length > 2) {
      value = value.substring(0, 2);
    }

    const num = parseInt(value, 10);
    if (num > 31) {
      value = '31';
    }

    this.day.set(value);
    input.value = value;

    // Auto-advance to month if 2 digits entered
    if (value.length === 2) {
      const monthInput = input.parentElement?.querySelector('.date-input--month') as HTMLInputElement;
      monthInput?.focus();
    }

    this.emitValue();
  }

  onMonthInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');

    if (value.length > 2) {
      value = value.substring(0, 2);
    }

    const num = parseInt(value, 10);
    if (num > 12) {
      value = '12';
    }

    this.month.set(value);
    input.value = value;

    // Auto-advance to year if 2 digits entered
    if (value.length === 2) {
      const yearInput = input.parentElement?.querySelector('.date-input--year') as HTMLInputElement;
      yearInput?.focus();
    }

    this.emitValue();
  }

  onYearInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');

    if (value.length > 4) {
      value = value.substring(0, 4);
    }

    this.year.set(value);
    input.value = value;
    this.emitValue();
  }

  onKeyDown(event: KeyboardEvent, field: 'day' | 'month' | 'year'): void {
    const input = event.target as HTMLInputElement;

    // Handle backspace to go to previous field
    if (event.key === 'Backspace' && input.value === '') {
      if (field === 'year') {
        const monthInput = input.parentElement?.querySelector('.date-input--month') as HTMLInputElement;
        monthInput?.focus();
      } else if (field === 'month') {
        const dayInput = input.parentElement?.querySelector('.date-input--day') as HTMLInputElement;
        dayInput?.focus();
      }
    }

    // Handle arrow keys
    if (event.key === 'ArrowRight' && input.selectionStart === input.value.length) {
      if (field === 'day') {
        const monthInput = input.parentElement?.querySelector('.date-input--month') as HTMLInputElement;
        monthInput?.focus();
      } else if (field === 'month') {
        const yearInput = input.parentElement?.querySelector('.date-input--year') as HTMLInputElement;
        yearInput?.focus();
      }
    }

    if (event.key === 'ArrowLeft' && input.selectionStart === 0) {
      if (field === 'year') {
        const monthInput = input.parentElement?.querySelector('.date-input--month') as HTMLInputElement;
        monthInput?.focus();
      } else if (field === 'month') {
        const dayInput = input.parentElement?.querySelector('.date-input--day') as HTMLInputElement;
        dayInput?.focus();
      }
    }

    // Close calendar on Escape
    if (event.key === 'Escape') {
      this.showCalendar.set(false);
    }
  }

  onBlur(): void {
    // Pad values on blur
    if (this.day() && this.day().length === 1) {
      this.day.set(this.day().padStart(2, '0'));
    }
    if (this.month() && this.month().length === 1) {
      this.month.set(this.month().padStart(2, '0'));
    }

    this.onTouched();
    this.emitValue();
  }

  private emitValue(): void {
    const d = this.day();
    const m = this.month();
    const y = this.year();

    if (d && m && y && y.length === 4) {
      const paddedDay = d.padStart(2, '0');
      const paddedMonth = m.padStart(2, '0');
      const dateStr = `${y}-${paddedMonth}-${paddedDay}`;
      this.onChange(dateStr);
    } else if (!d && !m && !y) {
      this.onChange('');
    }
  }
}
