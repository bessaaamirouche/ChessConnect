export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN';

export type ChessLevel = 'PION' | 'CAVALIER' | 'FOU' | 'TOUR' | 'DAME';

export interface User {
  id: number;
  email?: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  // Teacher fields
  hourlyRateCents?: number;
  acceptsSubscription?: boolean;
  bio?: string;
  avatarUrl?: string;
  languages?: string[];
  averageRating?: number;
  reviewCount?: number;
  // Student fields
  birthDate?: string;
  eloRating?: number;
  // Settings
  emailRemindersEnabled?: boolean;
  googleCalendarEnabled?: boolean;
  // Admin
  isSuspended?: boolean;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  // Teacher fields
  hourlyRateCents?: number;
  acceptsSubscription?: boolean;
  bio?: string;
  languages?: string[];
  // Student fields
  birthDate?: string;
  eloRating?: number;
}

// Available languages for teachers
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

export interface UpdateTeacherProfileRequest {
  hourlyRateCents?: number;
  acceptsSubscription?: boolean;
  bio?: string;
}

export interface TeacherBalance {
  teacherId: number;
  availableBalanceCents: number;
  pendingBalanceCents: number;
  totalEarnedCents: number;
  totalWithdrawnCents: number;
  lessonsCompleted: number;
}

export interface Progress {
  id: number;
  studentId: number;
  currentLevel: ChessLevel;
  totalLessonsCompleted: number;
  lessonsAtCurrentLevel: number;
  lessonsRequiredForNextLevel: number;
  progressPercentage: number;
}

export const CHESS_LEVELS: Record<ChessLevel, { order: number; label: string; description: string }> = {
  PION: { order: 1, label: 'Pion', description: 'Débutant' },
  CAVALIER: { order: 2, label: 'Cavalier', description: 'Intermédiaire' },
  FOU: { order: 3, label: 'Fou', description: 'Confirmé' },
  TOUR: { order: 4, label: 'Tour', description: 'Avancé' },
  DAME: { order: 5, label: 'Dame', description: 'Expert' }
};
