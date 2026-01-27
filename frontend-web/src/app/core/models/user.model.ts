// Re-export types from shared contracts
export {
  UserRole,
  ChessLevel,
  User,
  AuthResponse,
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

export const CHESS_LEVELS: Record<ChessLevel, { order: number; label: string; description: string; icon?: string }> = {
  PION: { order: 1, label: 'Pion', description: 'Débutant', icon: '♟' },
  CAVALIER: { order: 2, label: 'Cavalier', description: 'Intermédiaire', icon: '♞' },
  FOU: { order: 3, label: 'Fou', description: 'Confirmé', icon: '♝' },
  TOUR: { order: 4, label: 'Tour', description: 'Avancé', icon: '♜' },
  DAME: { order: 5, label: 'Dame', description: 'Expert', icon: '♛' },
  ROI: { order: 6, label: 'Roi', description: 'Maître', icon: '♚' }
};
