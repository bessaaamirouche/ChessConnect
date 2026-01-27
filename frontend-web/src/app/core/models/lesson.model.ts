// Re-export types from shared contracts
export {
  LessonStatus,
  LessonResponse,
  BookLessonRequest,
  UpdateLessonStatusRequest
} from '@contracts';

// Alias for backward compatibility
export { LessonResponse as Lesson } from '@contracts';

// Import LessonStatus to use in constants
import { LessonStatus } from '@contracts';

// Frontend-specific constants
export const LESSON_STATUS_LABELS: Record<LessonStatus, { label: string; cssClass: string }> = {
  PENDING: { label: 'En attente', cssClass: 'lesson-card__status--pending' },
  CONFIRMED: { label: 'Confirmé', cssClass: 'lesson-card__status--confirmed' },
  COMPLETED: { label: 'Terminé', cssClass: 'lesson-card__status--completed' },
  CANCELLED: { label: 'Annulé', cssClass: 'lesson-card__status--cancelled' },
  NO_SHOW: { label: 'Absent', cssClass: 'lesson-card__status--cancelled' }
};
