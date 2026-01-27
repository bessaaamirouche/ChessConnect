// Re-export types from shared contracts
export {
  DayOfWeek,
  AvailabilityResponse,
  AvailabilityRequest,
  TimeSlotResponse
} from '@contracts';

// Alias for backward compatibility
export { AvailabilityResponse as Availability } from '@contracts';
export { TimeSlotResponse as TimeSlot } from '@contracts';

// Import DayOfWeek to use in constants
import { DayOfWeek } from '@contracts';

// Frontend-specific constants
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
