import { ChessLevel } from './user.model';

export type CourseStatus = 'LOCKED' | 'IN_PROGRESS' | 'PENDING_VALIDATION' | 'COMPLETED';

export interface Course {
  id: number;
  title: string;
  description: string;
  content: string | null;
  grade: ChessLevel;
  orderInGrade: number;
  estimatedMinutes: number;
  iconName: string;
  status: CourseStatus;
  startedAt: string | null;
  completedAt: string | null;
  validatedByTeacherId: number | null;
  validatedByTeacherName: string | null;
  validatedAt: string | null;
}

export interface GradeWithCourses {
  grade: ChessLevel;
  displayName: string;
  description: string;
  order: number;
  courses: Course[];
  totalCourses: number;
  completedCourses: number;
  progressPercentage: number;
  isUnlocked: boolean;
  isCompleted: boolean;
}

export interface LearningPath {
  grades: GradeWithCourses[];
  totalCourses: number;
  completedCourses: number;
  overallProgressPercentage: number;
}
