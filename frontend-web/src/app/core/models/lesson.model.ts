export type LessonStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface Lesson {
  id: number;
  studentId: number;
  studentName: string;
  studentLevel?: string;
  studentAge?: number;
  studentElo?: number;
  teacherId: number;
  teacherName: string;
  scheduledAt: string;
  durationMinutes: number;
  zoomLink?: string;
  status: LessonStatus;
  priceCents: number;
  commissionCents: number;
  teacherEarningsCents: number;
  isFromSubscription: boolean;
  isFreeTrial?: boolean;
  notes?: string;
  cancellationReason?: string;
  cancelledBy?: string;
  refundPercentage?: number;
  refundedAmountCents?: number;
  teacherObservations?: string;
  recordingUrl?: string;
  teacherJoinedAt?: string;
  createdAt: string;
  // Course information
  courseId?: number;
  courseTitle?: string;
  courseGrade?: string;
}

export interface BookLessonRequest {
  teacherId: number;
  scheduledAt: string;
  durationMinutes?: number;
  notes?: string;
  useSubscription?: boolean;
  courseId?: number;
}

export interface UpdateLessonStatusRequest {
  status: LessonStatus;
  cancellationReason?: string;
  teacherObservations?: string;
}

export const LESSON_STATUS_LABELS: Record<LessonStatus, { label: string; cssClass: string }> = {
  PENDING: { label: 'En attente', cssClass: 'lesson-card__status--pending' },
  CONFIRMED: { label: 'Confirmé', cssClass: 'lesson-card__status--confirmed' },
  COMPLETED: { label: 'Terminé', cssClass: 'lesson-card__status--completed' },
  CANCELLED: { label: 'Annulé', cssClass: 'lesson-card__status--cancelled' },
  NO_SHOW: { label: 'Absent', cssClass: 'lesson-card__status--cancelled' }
};
