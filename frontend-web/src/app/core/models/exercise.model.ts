export type DifficultyLevel = 'DEBUTANT' | 'FACILE' | 'MOYEN' | 'DIFFICILE' | 'EXPERT';
export type ChessLevel = 'A' | 'B' | 'C' | 'D';

export interface Exercise {
  id: number;
  lessonId?: number;
  title: string;
  description?: string;
  startingFen: string;
  difficultyLevel: DifficultyLevel;
  stockfishSkillLevel: number;
  thinkTimeMs: number;
  chessLevel?: ChessLevel;
  playerColor: 'white' | 'black';
  timeLimitSeconds?: number;
  teacherName?: string;
  lessonDate?: string;
  courseTitle?: string;
}

export const DIFFICULTY_LABELS: Record<DifficultyLevel, { labelKey: string; color: string }> = {
  DEBUTANT: { labelKey: 'exercise.difficulty.beginner', color: '#4ade80' },
  FACILE: { labelKey: 'exercise.difficulty.easy', color: '#60a5fa' },
  MOYEN: { labelKey: 'exercise.difficulty.medium', color: '#fbbf24' },
  DIFFICILE: { labelKey: 'exercise.difficulty.hard', color: '#f97316' },
  EXPERT: { labelKey: 'exercise.difficulty.expert', color: '#ef4444' }
};
