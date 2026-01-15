export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface Availability {
  id: number;
  teacherId: number;
  teacherName: string;
  dayOfWeek: DayOfWeek;
  dayOfWeekLabel: string;
  startTime: string;
  endTime: string;
  isRecurring: boolean;
  specificDate?: string;
  isActive: boolean;
  durationMinutes: number;
}

export interface AvailabilityRequest {
  dayOfWeek?: DayOfWeek;
  startTime: string;
  endTime: string;
  isRecurring: boolean;
  specificDate?: string;
}

export interface TimeSlot {
  date: string;
  startTime: string;
  endTime: string;
  dateTime: string;
  isAvailable: boolean;
  dayOfWeekLabel: string;
}

export const DAYS_OF_WEEK: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Lundi' },
  { value: 'TUESDAY', label: 'Mardi' },
  { value: 'WEDNESDAY', label: 'Mercredi' },
  { value: 'THURSDAY', label: 'Jeudi' },
  { value: 'FRIDAY', label: 'Vendredi' },
  { value: 'SATURDAY', label: 'Samedi' },
  { value: 'SUNDAY', label: 'Dimanche' }
];

// Hours list (00 to 23) - 24h
export const HOURS: string[] = Array.from({ length: 24 }, (_, i) => i.toString().padStart(2, '0'));

// Minutes list (00, 05, 10, ... 55)
export const MINUTES: string[] = Array.from({ length: 12 }, (_, i) => (i * 5).toString().padStart(2, '0'));

// Keep for backwards compatibility
export const TIME_SLOTS: string[] = HOURS.flatMap(h => MINUTES.map(m => `${h}:${m}`));
