import { ChessLevel } from './user.model';
import { GradeWithCourses } from './learning-path.model';

export interface StudentProfile {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  currentLevel: ChessLevel;
  currentLevelDisplayName: string;
  totalLessonsCompleted: number;
  progressPercentage: number;
  levelSetByCoach: boolean;
  evaluatedByTeacherId?: number;
  evaluatedByTeacherName?: string;
  evaluatedAt?: string;
  courseProgress: GradeWithCourses[];
}
