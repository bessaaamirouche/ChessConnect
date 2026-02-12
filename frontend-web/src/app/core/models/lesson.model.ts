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
export const LESSON_STATUS_LABELS: Record<LessonStatus, { labelKey: string; cssClass: string }> = {
  PENDING: { labelKey: 'status.pending', cssClass: 'lesson-card__status--pending' },
  CONFIRMED: { labelKey: 'status.confirmed', cssClass: 'lesson-card__status--confirmed' },
  COMPLETED: { labelKey: 'status.completed', cssClass: 'lesson-card__status--completed' },
  CANCELLED: { labelKey: 'status.cancelled', cssClass: 'lesson-card__status--cancelled' },
  NO_SHOW: { labelKey: 'lessons.status.noShow', cssClass: 'lesson-card__status--cancelled' }
};
