import { ChessLevel } from './user.model';

export interface QuizQuestion {
  id: number;
  level: ChessLevel;
  question: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD?: string;
  orderInLevel: number;
}

export interface QuizAnswer {
  questionId: number;
  answer: 'A' | 'B' | 'C' | 'D';
}

export interface QuizSubmitRequest {
  answers: QuizAnswer[];
}

export interface QuizResult {
  determinedLevel: ChessLevel;
  levelDisplayName: string;
  levelDescription: string;
  scoresByLevel: Record<ChessLevel, number>;
  totalByLevel: Record<ChessLevel, number>;
  message: string;
}
