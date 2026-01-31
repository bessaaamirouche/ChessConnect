// Re-export types from shared contracts
export {
  UserRole,
  ChessLevel,
  User,
  AuthResponse,
  RegisterResponse,
  LoginRequest,
  RegisterRequest,
  UpdateUserRequest,
  ChangePasswordRequest,
  TeacherProfileResponse,
  TeacherBalanceResponse,
  ProgressResponse
} from '@contracts';

// Alias for backward compatibility
export { ProgressResponse as Progress } from '@contracts';
export { TeacherBalanceResponse as TeacherBalance } from '@contracts';

// Frontend-specific interfaces (not in backend DTOs)
export interface UpdateTeacherProfileRequest {
  hourlyRateCents?: number;
  acceptsFreeTrial?: boolean;
  bio?: string;
}

// Frontend-specific constants
export const AVAILABLE_LANGUAGES: { code: string; name: string }[] = [
  { code: 'FR', name: 'Francais' },
  { code: 'EN', name: 'English' },
  { code: 'ES', name: 'Espanol' },
  { code: 'DE', name: 'Deutsch' },
  { code: 'IT', name: 'Italiano' },
  { code: 'PT', name: 'Portugues' },
  { code: 'RU', name: 'Russkiy' },
  { code: 'ZH', name: 'Zhongwen' },
  { code: 'AR', name: 'Al-Arabiya' }
];

// Import ChessLevel to use in CHESS_LEVELS constant
import { ChessLevel } from '@contracts';

// 4 levels mapped to chess pieces: A→Pion, B→Cavalier, C→Reine, D→Roi
export const CHESS_LEVELS: Record<ChessLevel, { order: number; label: string; description: string; icon: string; pieceName: string }> = {
  A: { order: 1, label: 'Pion', description: 'Débutant', icon: '♟', pieceName: 'PION' },
  B: { order: 2, label: 'Cavalier', description: 'Intermédiaire', icon: '♞', pieceName: 'CAVALIER' },
  C: { order: 3, label: 'Reine', description: 'Avancé', icon: '♛', pieceName: 'REINE' },
  D: { order: 4, label: 'Roi', description: 'Expert', icon: '♚', pieceName: 'ROI' }
};

// Helper function to get level info
export function getLevelInfo(level: ChessLevel) {
  return CHESS_LEVELS[level] || CHESS_LEVELS.A;
}

// Helper function to get level icon
export function getLevelIcon(level: ChessLevel): string {
  return CHESS_LEVELS[level]?.icon || '🎓';
}

// Helper function to get level label
export function getLevelLabel(level: ChessLevel): string {
  return CHESS_LEVELS[level]?.label || level;
}
