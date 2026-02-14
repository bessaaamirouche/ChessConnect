/**
 * Shared contracts between frontend and backend
 * These interfaces must match the Java DTOs exactly
 *
 * Source of truth: backend-api/src/main/java/com/chessconnect/dto/
 */

// ============================================================================
// ENUMS
// ============================================================================

export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN';

export type ChessLevel = 'A' | 'B' | 'C' | 'D';

export type LessonStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';

export type PaymentType = 'LESSON' | 'SUBSCRIPTION' | 'WALLET_CREDIT';

export type SubscriptionPlan = 'PREMIUM';

export type InvoiceStatus = 'DRAFT' | 'PAID' | 'VOID' | 'REFUNDED';

export type InvoiceType = 'LESSON_INVOICE' | 'COMMISSION_INVOICE' | 'PAYOUT_INVOICE' | 'CREDIT_NOTE' | 'WALLET_CREDIT_INVOICE';

export type CreditTransactionType = 'CREDIT' | 'DEBIT' | 'REFUND';

export type CourseStatus = 'LOCKED' | 'IN_PROGRESS' | 'PENDING_VALIDATION' | 'COMPLETED';

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export type LessonType = 'INDIVIDUAL' | 'GROUP';

export type PromoCodeType = 'PROMO' | 'REFERRAL';

export type DiscountType = 'COMMISSION_REDUCTION' | 'STUDENT_DISCOUNT';

export type GroupLessonStatus = 'OPEN' | 'FULL' | 'DEADLINE_PASSED';

export type ParticipantRole = 'CREATOR' | 'PARTICIPANT';

export type ParticipantStatus = 'ACTIVE' | 'CANCELLED';

// ============================================================================
// AUTH
// ============================================================================

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
  hourlyRateCents?: number;
  bio?: string;
  languages?: string[];
  birthDate?: string;
  eloRating?: number;
  referralCode?: string;
}

export interface AuthResponse {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
}

export interface RegisterResponse {
  email: string;
  firstName: string;
  message: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  password: string;
}

// ============================================================================
// USER
// ============================================================================

export interface User {
  id: number;
  uuid?: string;
  email: string;
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
  lessonsCompleted?: number;
  totalStudents?: number;
  isOnline?: boolean;
  // Teacher banking fields
  iban?: string;
  bic?: string;
  accountHolderName?: string;
  siret?: string;
  companyName?: string;
  // Student fields
  birthDate?: string;
  eloRating?: number;
  // Settings
  emailRemindersEnabled?: boolean;
  pushNotificationsEnabled?: boolean;
  // Admin
  isSuspended?: boolean;
  createdAt?: string;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  hourlyRateCents?: number;
  bio?: string;
  languages?: string[];
  siret?: string;
  companyName?: string;
  birthDate?: string;
  eloRating?: number;
  clearEloRating?: boolean;
  emailRemindersEnabled?: boolean;
  pushNotificationsEnabled?: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface TeacherProfileResponse {
  id: number;
  uuid?: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  hourlyRateCents?: number;
  acceptsSubscription?: boolean;
  bio?: string;
  avatarUrl?: string;
  languages?: string[];
  iban?: string;
  bic?: string;
  accountHolderName?: string;
  siret?: string;
  companyName?: string;
  birthDate?: string;
  eloRating?: number;
  emailRemindersEnabled?: boolean;
}

// ============================================================================
// LESSON
// ============================================================================

export interface LessonResponse {
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
  priceCents?: number;
  commissionCents?: number;
  teacherEarningsCents?: number;
  isFromSubscription?: boolean;
  notes?: string;
  cancellationReason?: string;
  cancelledBy?: string;
  refundPercentage?: number;
  refundedAmountCents?: number;
  teacherObservations?: string;
  teacherComment?: string;
  teacherCommentAt?: string;
  recordingUrl?: string;
  teacherJoinedAt?: string;
  createdAt: string;
  courseId?: number;
  courseTitle?: string;
  courseGrade?: string;
  // Group lesson fields
  isGroupLesson?: boolean;
  maxParticipants?: number;
  groupStatus?: GroupLessonStatus;
  currentParticipantCount?: number;
  invitationToken?: string;
  participants?: ParticipantSummary[];
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

// ============================================================================
// AVAILABILITY
// ============================================================================

export interface AvailabilityResponse {
  id: number;
  teacherId: number;
  teacherName: string;
  dayOfWeek: DayOfWeek;
  dayOfWeekLabel: string;
  startTime: string;
  endTime: string;
  isRecurring: boolean;
  specificDate?: string;
  isActive: boolean;
  durationMinutes: number;
  lessonType: LessonType;
}

export interface AvailabilityRequest {
  dayOfWeek?: DayOfWeek;
  startTime: string;
  endTime: string;
  isRecurring: boolean;
  specificDate?: string;
  lessonType?: LessonType;
}

export interface TimeSlotResponse {
  date: string;
  startTime: string;
  endTime: string;
  dateTime: string;
  isAvailable: boolean;
  dayOfWeekLabel: string;
  lessonType: LessonType;
}

// ============================================================================
// PAYMENT
// ============================================================================

export interface PaymentResponse {
  id: number;
  payerId: number;
  payerName?: string;
  payerEmail?: string;
  teacherId?: number;
  teacherName?: string;
  lessonId?: number;
  subscriptionId?: number;
  paymentType: PaymentType;
  amountCents: number;
  commissionCents: number;
  teacherPayoutCents?: number;
  status: PaymentStatus;
  stripePaymentIntentId?: string;
  stripeChargeId?: string;
  stripeTransferId?: string;
  failureReason?: string;
  refundReason?: string;
  processedAt?: string;
  createdAt: string;
}

export interface CheckoutSessionResponse {
  sessionId: string;
  clientSecret: string;
}

export interface CreateCheckoutSessionRequest {
  planId: string;
  successUrl: string;
  cancelUrl: string;
}

export interface CreateLessonCheckoutRequest {
  lessonId: number;
  successUrl: string;
  cancelUrl: string;
  promoCode?: string;
}

// ============================================================================
// SUBSCRIPTION
// ============================================================================

export interface SubscriptionResponse {
  id: number;
  studentId: number;
  studentName?: string;
  studentEmail?: string;
  planType: SubscriptionPlan;
  priceCents: number;
  startDate: string;
  endDate?: string;
  cancelledAt?: string;
  isActive: boolean;
  stripeSubscriptionId?: string;
  createdAt: string;
}

export interface SubscriptionPlanResponse {
  id: string;
  name: string;
  priceCents: number;
  interval: string;
  features: string[];
}

// ============================================================================
// WALLET
// ============================================================================

export interface WalletResponse {
  balanceCents: number;
  pendingCents: number;
}

export interface TopUpRequest {
  amountCents: number;
  successUrl: string;
  cancelUrl: string;
}

export interface CreditTransactionResponse {
  id: number;
  type: CreditTransactionType;
  amountCents: number;
  description: string;
  lessonId?: number;
  createdAt: string;
}

export interface BookWithCreditRequest {
  teacherId: number;
  scheduledAt: string;
  durationMinutes?: number;
  notes?: string;
  courseId?: number;
  promoCode?: string;
}

// ============================================================================
// INVOICE
// ============================================================================

export interface InvoiceResponse {
  id: number;
  invoiceNumber: string;
  invoiceType: InvoiceType;
  status: InvoiceStatus;
  issuerId: number;
  issuerName: string;
  customerId: number;
  customerName: string;
  description: string;
  totalCents: number;
  issuedAt: string;
  isReceived?: boolean;
}

// ============================================================================
// PROGRESS & LEARNING PATH
// ============================================================================

export interface ProgressResponse {
  id: number;
  studentId: number;
  currentLevel: ChessLevel;
  totalLessonsCompleted: number;
  lessonsAtCurrentLevel: number;
  lessonsRequiredForNextLevel: number;
  progressPercentage: number;
}

export interface CourseResponse {
  id: number;
  grade: ChessLevel;
  orderInGrade: number;
  title: string;
  description: string;
  objectives: string[];
  status: CourseStatus;
  completedAt?: string;
  validatedByTeacherId?: number;
  validatedByTeacherName?: string;
}

export interface GradeWithCoursesResponse {
  grade: ChessLevel;
  gradeLabel: string;
  gradeDescription: string;
  courses: CourseResponse[];
  completedCount: number;
  totalCount: number;
  isCurrentGrade: boolean;
  isUnlocked: boolean;
}

export interface LearningPathResponse {
  currentLevel: ChessLevel;
  grades: GradeWithCoursesResponse[];
}

export interface NextCourseResponse {
  course: CourseResponse;
  gradeLabel: string;
}

export interface SetLevelRequest {
  level: ChessLevel;
}

// ============================================================================
// QUIZ
// ============================================================================

export interface QuizQuestionResponse {
  id: number;
  questionText: string;
  imageUrl?: string;
  options: string[];
  level: ChessLevel;
  orderInLevel: number;
}

export interface QuizAnswerRequest {
  questionId: number;
  selectedOption: number;
}

export interface QuizSubmitRequest {
  answers: QuizAnswerRequest[];
}

export interface QuizResultResponse {
  determinedLevel: ChessLevel;
  correctAnswers: number;
  totalQuestions: number;
  scorePercentage: number;
  levelScores: Record<ChessLevel, number>;
}

// ============================================================================
// RATING
// ============================================================================

export interface CreateRatingRequest {
  lessonId: number;
  rating: number;
  comment?: string;
}

export interface RatingResponse {
  id: number;
  lessonId: number;
  studentId: number;
  studentName: string;
  teacherId: number;
  rating: number;
  comment?: string;
  createdAt: string;
}

// ============================================================================
// FAVORITE
// ============================================================================

export interface FavoriteTeacherResponse {
  teacherId: number;
  firstName: string;
  lastName: string;
  avatarUrl?: string;
  hourlyRateCents: number;
  averageRating?: number;
  notifyOnNewSlot: boolean;
  favoritedAt: string;
}

export interface UpdateNotifyRequest {
  notify: boolean;
}

// ============================================================================
// TEACHER
// ============================================================================

export interface TeacherBalanceResponse {
  teacherId: number;
  availableBalanceCents: number;
  pendingBalanceCents: number;
  totalEarnedCents: number;
  totalWithdrawnCents: number;
  lessonsCompleted: number;
}

// ============================================================================
// ADMIN
// ============================================================================

export interface AdminStatsResponse {
  totalUsers: number;
  totalStudents: number;
  totalTeachers: number;
  activeSubscriptions: number;
  totalLessons: number;
  lessonsThisMonth: number;
  totalRevenueCents: number;
  revenueThisMonthCents: number;
}

export interface AccountingResponse {
  totalRevenueCents: number;
  totalCommissionsCents: number;
  totalTeacherEarningsCents: number;
  totalRefundedCents: number;
  totalLessons: number;
  completedLessons: number;
  cancelledLessons: number;
}

export interface UserListResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  isSuspended: boolean;
  createdAt: string;
  lastLoginAt?: string;
  hourlyRateCents?: number;
  languages?: string;
  averageRating?: number;
  reviewCount?: number;
  lessonsCount: number;
}

export interface TeacherBalanceListResponse {
  teacherId: number;
  firstName: string;
  lastName: string;
  email: string;
  availableBalanceCents: number;
  pendingBalanceCents: number;
  totalEarnedCents: number;
  totalWithdrawnCents: number;
  lessonsCompleted: number;
  iban?: string;
  bic?: string;
  accountHolderName?: string;
  siret?: string;
  companyName?: string;
  currentMonthPaid: boolean;
  currentMonthEarningsCents: number;
  currentMonthLessonsCount: number;
  stripeConnectEnabled?: boolean;
  stripeConnectReady?: boolean;
}

export interface DataPoint {
  date: string;
  value: number;
}

export interface HourlyDataPoint {
  hour: number;
  value: number;
}

export interface AnalyticsResponse {
  studentRegistrations: DataPoint[];
  teacherRegistrations: DataPoint[];
  newSubscriptions: DataPoint[];
  renewals: DataPoint[];
  cancellations: DataPoint[];
  dailyVisits: DataPoint[];
  hourlyVisits: HourlyDataPoint[];
}

export interface AdminActionResponse {
  message: string;
}

export interface RefundResponse {
  message: string;
  refundId?: string;
  amountCents?: number;
}

export interface MarkTeacherPaidResponse {
  success: boolean;
  message: string;
  amountCents?: number;
  stripeTransferId?: string;
  lessonsCount?: number;
}

// ============================================================================
// ARTICLE / BLOG
// ============================================================================

export interface ArticleListDTO {
  id: number;
  slug: string;
  title: string;
  excerpt: string;
  category: string;
  imageUrl?: string;
  author: string;
  publishedAt: string;
  readTimeMinutes: number;
}

export interface ArticleDetailDTO {
  id: number;
  slug: string;
  title: string;
  content: string;
  excerpt: string;
  category: string;
  imageUrl?: string;
  author: string;
  publishedAt: string;
  readTimeMinutes: number;
  metaTitle?: string;
  metaDescription?: string;
}

// ============================================================================
// EXERCISE
// ============================================================================

export interface ExerciseResponse {
  id: number;
  lessonId: number;
  fen: string;
  solution: string;
  difficulty: string;
  description?: string;
}

// ============================================================================
// JITSI
// ============================================================================

export interface JitsiTokenResponse {
  token: string;
  roomName: string;
}

// ============================================================================
// CONTACT
// ============================================================================

export interface ContactAdminRequest {
  subject: string;
  message: string;
}

// ============================================================================
// TRACKING
// ============================================================================

export interface PageViewRequest {
  path: string;
  referrer?: string;
}

// ============================================================================
// PAGINATION
// ============================================================================

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ============================================================================
// GROUP LESSONS
// ============================================================================

export interface ParticipantSummary {
  displayName: string;
  role: ParticipantRole;
}

export interface BookGroupLessonRequest {
  teacherId: number;
  scheduledAt: string;
  durationMinutes?: number;
  notes?: string;
  targetGroupSize: number;
  courseId?: number;
}

export interface JoinGroupLessonRequest {
  token: string;
}

export interface ResolveDeadlineRequest {
  choice: 'PAY_FULL' | 'CANCEL';
}

export interface GroupInvitationResponse {
  token: string;
  lessonId: number;
  teacherFirstName: string;
  teacherLastInitial: string;
  teacherAvatarUrl?: string;
  scheduledAt: string;
  durationMinutes: number;
  targetGroupSize: number;
  currentParticipantCount: number;
  spotsRemaining: number;
  pricePerPersonCents: number;
  deadline: string;
  isExpired: boolean;
  isFull: boolean;
  participants: ParticipantSummary[];
}

export interface GroupLessonResponse {
  lesson: LessonResponse;
  isGroupLesson: boolean;
  maxParticipants: number;
  groupStatus: GroupLessonStatus;
  currentParticipantCount: number;
  pricePerPersonCents: number;
  invitationToken: string;
  deadline: string;
  participants: ParticipantSummary[];
}

// ============================================================================
// PROMO CODES
// ============================================================================

export interface ValidatePromoCodeResponse {
  valid: boolean;
  message: string;
  discountType?: DiscountType;
  discountPercent?: number;
  finalPriceCents?: number;
  discountAmountCents?: number;
}

export interface PromoCodeResponse {
  id: number;
  code: string;
  codeType: PromoCodeType;
  discountType?: DiscountType;
  discountPercent?: number;
  referrerName?: string;
  referrerEmail?: string;
  premiumDays: number;
  revenueSharePercent: number;
  maxUses?: number;
  currentUses: number;
  firstLessonOnly: boolean;
  minAmountCents?: number;
  isActive: boolean;
  expiresAt?: string;
  createdAt: string;
  totalDiscountCents: number;
  totalEarningsCents: number;
  unpaidEarningsCents: number;
}
