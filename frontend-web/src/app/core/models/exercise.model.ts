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

export const DIFFICULTY_LABELS: Record<DifficultyLevel, { label: string; color: string }> = {
  DEBUTANT: { label: 'Débutant', color: '#4ade80' },
  FACILE: { label: 'Facile', color: '#60a5fa' },
  MOYEN: { label: 'Moyen', color: '#fbbf24' },
  DIFFICILE: { label: 'Difficile', color: '#f97316' },
  EXPERT: { label: 'Expert', color: '#ef4444' }
};
