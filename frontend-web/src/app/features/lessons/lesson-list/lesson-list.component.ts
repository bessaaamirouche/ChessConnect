import { Component, OnInit, OnDestroy, signal, ViewChild, ChangeDetectionStrategy, computed } from '@angular/core';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { LessonService } from '../../../core/services/lesson.service';
import { AuthService } from '../../../core/services/auth.service';
import { RatingService } from '../../../core/services/rating.service';
import { SeoService } from '../../../core/services/seo.service';
import { JitsiService } from '../../../core/services/jitsi.service';
import { LearningPathService } from '../../../core/services/learning-path.service';
import { WalletService } from '../../../core/services/wallet.service';
import { PaymentService } from '../../../core/services/payment.service';
import { ToastService } from '../../../core/services/toast.service';
import { NextCourse } from '../../../core/models/learning-path.model';
import { LESSON_STATUS_LABELS, Lesson } from '../../../core/models/lesson.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { StudentProfileModalComponent } from '../../../shared/student-profile-modal/student-profile-modal.component';
import { StudentEvaluationModalComponent } from '../../../shared/student-evaluation-modal/student-evaluation-modal.component';
import { RatingModalComponent } from '../../../shared/rating-modal/rating-modal.component';
import { VideoCallComponent } from '../../../shared/video-call/video-call.component';
import { ExerciseButtonComponent } from '../../../shared/components/exercise-button/exercise-button.component';
import { VideoPlayerComponent } from '../../../shared/components/video-player/video-player.component';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroCalendarDays,
  heroClock,
  heroVideoCamera,
  heroCheck,
  heroXMark,
  heroArrowLeft,
  heroChevronRight,
  heroChartBarSquare,
  heroClipboardDocumentList,
  heroUserCircle,
  heroArrowRightOnRectangle,
  heroPlus,
  heroBookOpen,
  heroTrophy,
  heroCreditCard,
  heroAcademicCap,
  heroUser,
  heroChartBar,
  heroInformationCircle,
  heroTrash,
  heroPlayCircle,
  heroFunnel,
  heroMagnifyingGlass,
  heroDocumentText,
  heroStar,
  heroBellAlert,
  heroChatBubbleLeftRight,
  heroEllipsisVertical,
  heroCpuChip,
  heroLockClosed
} from '@ng-icons/heroicons/outline';
import { CHESS_LEVELS, ChessLevel, User } from '../../../core/models/user.model';

export interface PendingValidation {
  id: number;
  lessonId: number;
  studentId: number;
  studentName: string;
  createdAt: string;
}

@Component({
  selector: 'app-lesson-list',
  standalone: true,
  imports: [RouterLink, DatePipe, FormsModule, TranslateModule, ConfirmDialogComponent, NgIconComponent, StudentProfileModalComponent, StudentEvaluationModalComponent, RatingModalComponent, VideoCallComponent, ExerciseButtonComponent, VideoPlayerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroCalendarDays,
    heroClock,
    heroVideoCamera,
    heroCheck,
    heroXMark,
    heroArrowLeft,
    heroChevronRight,
    heroChartBarSquare,
    heroClipboardDocumentList,
    heroUserCircle,
    heroArrowRightOnRectangle,
    heroPlus,
    heroBookOpen,
    heroTrophy,
    heroCreditCard,
    heroAcademicCap,
    heroUser,
    heroChartBar,
    heroInformationCircle,
    heroTrash,
    heroPlayCircle,
    heroFunnel,
    heroMagnifyingGlass,
    heroStar,
    heroBellAlert,
    heroChatBubbleLeftRight,
    heroEllipsisVertical,
    heroCpuChip,
    heroLockClosed
  })],
  templateUrl: './lesson-list.component.html',
  styleUrl: './lesson-list.component.scss'
})
export class LessonListComponent implements OnInit, OnDestroy {
  @ViewChild('confirmDialog') confirmDialog!: ConfirmDialogComponent;

  statusLabels = LESSON_STATUS_LABELS;
  selectedStudentId = signal<number | null>(null);
  visibleReasonId = signal<number | null>(null);
  visibleObservationsId = signal<number | null>(null);

  // Modale de fin de cours
  showCompleteModal = signal(false);
  completingLessonId = signal<number | null>(null);
  observationsText = signal('');

  // Rating modal
  showRatingModal = signal(false);
  ratingLessonId = signal<number | null>(null);
  ratingTeacherName = signal('');
  ratedLessons = signal<Set<number>>(new Set());

  // Video player
  showVideoPlayer = signal(false);
  videoPlayerUrl = signal('');
  videoPlayerLessonId = signal<number | null>(null);
  videoPlayerTitle = signal('');

  // Video call (iframe)
  showVideoCall = signal(false);
  videoCallRoomName = signal('');
  videoCallToken = signal('');
  videoCallTitle = signal('');
  videoCallDurationMinutes = signal(60);
  videoCallLessonId = signal<number | null>(null);

  // History filters
  historyFilterPerson = signal<string>('');
  historyFilterMonth = signal<string>('');
  historyFilterHasRecording = signal<boolean>(false);

  // Email reminders preference
  emailRemindersEnabled = signal(true);
  savingEmailReminders = signal(false);

  // Evaluation modal (shown after completing a lesson)
  showEvaluationModal = signal(false);
  evaluationStudentId = signal<number | null>(null);
  evaluationLessonId = signal<number | null>(null);

  // Teacher comment modal (for editing - teacher side)
  showCommentModal = signal(false);
  commentLessonId = signal<number | null>(null);
  commentStudentName = signal('');
  commentText = signal('');
  savingComment = signal(false);

  // View comment modal (for reading - student side)
  showViewCommentModal = signal(false);
  viewCommentText = signal('');
  viewCommentTeacherName = signal('');
  viewCommentDate = signal<string | null>(null);

  // Actions dropdown menu
  openDropdownId = signal<number | null>(null);

  // Computed filtered history
  filteredHistory = computed(() => {
    let lessons = this.lessonService.lessonHistory();
    const filterPerson = this.historyFilterPerson();
    const filterMonth = this.historyFilterMonth();
    const filterHasRecording = this.historyFilterHasRecording();

    if (filterPerson) {
      lessons = lessons.filter(l => {
        const personName = this.authService.isStudent() ? l.teacherName : l.studentName;
        return personName.toLowerCase().includes(filterPerson.toLowerCase());
      });
    }

    if (filterMonth) {
      lessons = lessons.filter(l => {
        const lessonDate = new Date(l.scheduledAt);
        const lessonMonth = `${lessonDate.getFullYear()}-${String(lessonDate.getMonth() + 1).padStart(2, '0')}`;
        return lessonMonth === filterMonth;
      });
    }

    if (filterHasRecording) {
      lessons = lessons.filter(l => l.recordingUrl);
    }

    return lessons;
  });

  // Get unique persons (coaches or students) for filter
  uniquePersons = computed(() => {
    const lessons = this.lessonService.lessonHistory();
    const persons = new Set<string>();
    lessons.forEach(l => {
      const name = this.authService.isStudent() ? l.teacherName : l.studentName;
      if (name) persons.add(name);
    });
    return Array.from(persons).sort();
  });

  // Get unique months for filter
  uniqueMonths = computed(() => {
    const lessons = this.lessonService.lessonHistory();
    const months = new Set<string>();
    lessons.forEach(l => {
      const date = new Date(l.scheduledAt);
      months.add(`${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`);
    });
    return Array.from(months).sort().reverse();
  });

  // Next course cache by student ID
  nextCourseByStudent = signal<Map<number, NextCourse | null>>(new Map());

  // Pending validations for teachers
  pendingValidations = signal<PendingValidation[]>([]);

  // Polling for lesson status check (when in video call)
  private lessonStatusCheckInterval: any = null;

  constructor(
    public lessonService: LessonService,
    public authService: AuthService,
    private ratingService: RatingService,
    private seoService: SeoService,
    private jitsiService: JitsiService,
    private learningPathService: LearningPathService,
    private walletService: WalletService,
    public paymentService: PaymentService,
    private toastService: ToastService,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.seoService.setLessonsPage();
    // Initialize email reminders from current user
    const user = this.authService.currentUser();
    if (user) {
      this.emailRemindersEnabled.set(user.emailRemindersEnabled !== false);
    }
  }

  ngOnInit(): void {
    this.lessonService.loadUpcomingLessons().subscribe({
      next: () => {
        // Check if we should auto-open a video call (from notification click)
        this.route.queryParams.subscribe(params => {
          const openCallId = params['openCall'];
          if (openCallId) {
            const lessonId = parseInt(openCallId, 10);
            const lesson = this.lessonService.upcomingLessons().find(l => l.id === lessonId);
            if (lesson && lesson.status === 'CONFIRMED' && lesson.teacherJoinedAt) {
              // Clear the query param and open the video call
              this.router.navigate([], { queryParams: {}, replaceUrl: true });
              setTimeout(() => this.openVideoCall(lesson), 500);
            }
          }
        });

        // Load next courses for pending lessons (teachers only)
        if (this.authService.isTeacher()) {
          this.loadNextCoursesForPendingLessons();
          this.loadPendingValidations();
        }
      }
    });
    this.lessonService.loadLessonHistory().subscribe();

    // Load already rated lessons to hide the "Noter" button
    if (this.authService.isStudent()) {
      this.ratingService.getMyRatedLessonIds().subscribe({
        next: (ids) => {
          this.ratedLessons.set(new Set(ids));
        }
      });

      // Load active subscription to check Premium status for exercise button
      this.paymentService.loadActiveSubscription().subscribe();
    }
  }

  ngOnDestroy(): void {
    this.stopLessonStatusCheck();
  }

  // Load next course for all students with pending lessons
  private loadNextCoursesForPendingLessons(): void {
    const pendingLessons = this.lessonService.upcomingLessons().filter(l => l.status === 'PENDING');
    const studentIds = new Set(pendingLessons.map(l => l.studentId));

    studentIds.forEach(studentId => {
      if (studentId && !this.nextCourseByStudent().has(studentId)) {
        this.learningPathService.getNextCourse(studentId).subscribe({
          next: (nextCourse) => {
            const map = new Map(this.nextCourseByStudent());
            map.set(studentId, nextCourse);
            this.nextCourseByStudent.set(map);
          }
        });
      }
    });
  }

  // Get next course for a student
  getNextCourse(studentId: number): NextCourse | null | undefined {
    return this.nextCourseByStudent().get(studentId);
  }

  // Load pending validations for teachers
  loadPendingValidations(): void {
    this.http.get<PendingValidation[]>('/api/lessons/pending-validations').subscribe({
      next: (validations) => this.pendingValidations.set(validations)
    });
  }

  // Open student profile for first pending validation
  openFirstPendingValidation(): void {
    const validations = this.pendingValidations();
    if (validations.length > 0) {
      this.openStudentProfile(validations[0].studentId);
    }
  }

  confirmLesson(lessonId: number): void {
    this.lessonService.confirmLesson(lessonId).subscribe();
  }

  async cancelLesson(lessonId: number): Promise<void> {
    const reason = await this.confirmDialog.open({
      title: 'Annuler le cours',
      message: 'Êtes-vous sûr de vouloir annuler ce cours ?',
      confirmText: 'Annuler',
      cancelText: 'Retour',
      type: 'danger',
      icon: 'warning',
      showInput: true,
      inputLabel: 'Raison (optionnel)',
      inputPlaceholder: 'Ex: Indisponibilité...'
    });

    if (reason !== null) {
      this.lessonService.cancelLesson(lessonId, reason as string || undefined).subscribe({
        next: () => {
          // Reload wallet balance after cancellation (for refund)
          if (this.authService.isStudent()) {
            this.walletService.loadBalance().subscribe();
          }
        }
      });
    }
  }

  // Vérifie si le cours peut être terminé (après l'heure de fin)
  canCompleteLesson(lesson: Lesson): boolean {
    const now = new Date();
    const lessonStart = new Date(lesson.scheduledAt);
    const lessonEnd = new Date(lessonStart.getTime() + lesson.durationMinutes * 60000);
    return now >= lessonEnd;
  }

  // Ouvre la modale pour terminer le cours
  openCompleteModal(lessonId: number): void {
    this.completingLessonId.set(lessonId);
    this.observationsText.set('');
    this.showCompleteModal.set(true);
  }

  closeCompleteModal(): void {
    this.showCompleteModal.set(false);
    this.completingLessonId.set(null);
    this.observationsText.set('');
  }

  submitCompleteLesson(): void {
    const lessonId = this.completingLessonId();
    if (lessonId) {
      // Find the lesson to get studentId before completing
      const lesson = this.lessonService.upcomingLessons().find(l => l.id === lessonId);
      const studentId = lesson?.studentId;

      const observations = this.observationsText().trim() || undefined;
      this.lessonService.completeLesson(lessonId, observations).subscribe({
        next: () => {
          this.closeCompleteModal();
          // Open evaluation modal for level evaluation or course validation
          if (studentId) {
            this.openEvaluationModal(studentId, lessonId);
          }
        },
        error: () => this.closeCompleteModal()
      });
    }
  }

  // Evaluation modal methods
  openEvaluationModal(studentId: number, lessonId: number): void {
    this.evaluationStudentId.set(studentId);
    this.evaluationLessonId.set(lessonId);
    this.showEvaluationModal.set(true);
  }

  closeEvaluationModal(): void {
    this.showEvaluationModal.set(false);
    this.evaluationStudentId.set(null);
    this.evaluationLessonId.set(null);
  }

  onLevelSet(event: { studentId: number; level: ChessLevel }): void {
    this.toastService.success(`Niveau ${event.level} défini pour cet joueur`);
    this.closeEvaluationModal();
  }

  onEvaluationCourseValidated(event: { studentId: number; courseId: number }): void {
    this.toastService.success('Cours validé avec succès');
    // Reload pending validations
    if (this.authService.isTeacher()) {
      this.loadPendingValidations();
    }
  }

  // Toggle observations visibility in history
  toggleObservations(lessonId: number): void {
    if (this.visibleObservationsId() === lessonId) {
      this.visibleObservationsId.set(null);
    } else {
      this.visibleObservationsId.set(lessonId);
    }
  }

  // Open comment view modal (for students)
  openViewCommentModal(lesson: Lesson): void {
    this.viewCommentText.set(lesson.teacherComment || '');
    this.viewCommentTeacherName.set(lesson.teacherName);
    this.viewCommentDate.set(lesson.teacherCommentAt || null);
    this.showViewCommentModal.set(true);
  }

  closeViewCommentModal(): void {
    this.showViewCommentModal.set(false);
    this.viewCommentText.set('');
    this.viewCommentTeacherName.set('');
    this.viewCommentDate.set(null);
  }

  // Open comment modal (for teachers)
  openCommentModal(lesson: Lesson): void {
    this.commentLessonId.set(lesson.id);
    this.commentStudentName.set(lesson.studentName);
    this.commentText.set(lesson.teacherComment || '');
    this.showCommentModal.set(true);
  }

  closeCommentModal(): void {
    this.showCommentModal.set(false);
    this.commentLessonId.set(null);
    this.commentStudentName.set('');
    this.commentText.set('');
  }

  getCommentForLesson(lessonId: number): string | undefined {
    const lesson = this.lessonService.lessonHistory().find(l => l.id === lessonId);
    return lesson?.teacherComment;
  }

  submitComment(): void {
    const lessonId = this.commentLessonId();
    if (!lessonId) return;

    this.savingComment.set(true);
    this.lessonService.updateTeacherComment(lessonId, this.commentText()).subscribe({
      next: () => {
        this.savingComment.set(false);
        this.closeCommentModal();
        this.toastService.success('Commentaire enregistré');
      },
      error: (err) => {
        this.savingComment.set(false);
        this.toastService.error(err.error?.message || 'Erreur lors de l\'enregistrement');
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

  openVideoCall(lesson: Lesson): void {
    // Refresh lesson data first to ensure we have the correct zoomLink
    this.lessonService.refreshLesson(lesson.id).subscribe({
      next: (refreshedLesson) => {
        this.startVideoCall(refreshedLesson);
      },
      error: () => {
        // Fallback to current lesson data if refresh fails
        this.startVideoCall(lesson);
      }
    });
  }

  private startVideoCall(lesson: Lesson): void {
    const roomName = lesson.zoomLink?.split('/').pop() || `mychess-lesson-${lesson.id}`;
    const title = this.authService.isStudent()
      ? `Cours avec ${lesson.teacherName}`
      : `Cours avec ${lesson.studentName}`;

    const durationMinutes = lesson.durationMinutes;

    // Si c'est un prof, marquer qu'il a rejoint l'appel
    if (this.authService.isTeacher()) {
      this.lessonService.markTeacherJoined(lesson.id).subscribe({
        next: () => {
          console.log('Teacher marked as joined');
        },
        error: (err) => console.warn('Could not mark teacher as joined:', err)
      });
    }

    // Obtenir le token JWT pour Jitsi (avec role moderateur pour les profs)
    this.jitsiService.getToken(roomName).subscribe({
      next: (response) => {
        this.videoCallRoomName.set(roomName);
        this.videoCallToken.set(response.token);
        this.videoCallTitle.set(title);
        this.videoCallDurationMinutes.set(durationMinutes);
        this.videoCallLessonId.set(lesson.id);
        this.showVideoCall.set(true);

        // Start polling for lesson status (students only) to auto-hang up when coach ends
        if (this.authService.isStudent()) {
          this.startLessonStatusCheck(lesson.id);
        }
      },
      error: (err) => {
        console.error('Erreur lors de la génération du token Jitsi:', err);
        // Fallback sans token
        this.videoCallRoomName.set(roomName);
        this.videoCallToken.set('');
        this.videoCallTitle.set(title);
        this.videoCallDurationMinutes.set(durationMinutes);
        this.videoCallLessonId.set(lesson.id);
        this.showVideoCall.set(true);

        // Start polling for lesson status (students only) to auto-hang up when coach ends
        if (this.authService.isStudent()) {
          this.startLessonStatusCheck(lesson.id);
        }
      }
    });
  }

  refreshLessonStatus(lessonId: number): void {
    this.lessonService.refreshLesson(lessonId).subscribe({
      next: (lesson) => {
        if (lesson.teacherJoinedAt) {
          console.log('Teacher has joined, student can now access');
        }
      },
      error: (err) => console.error('Could not refresh lesson:', err)
    });
  }

  closeVideoCall(): void {
    this.stopLessonStatusCheck();
    this.showVideoCall.set(false);
    this.videoCallRoomName.set('');
    this.videoCallToken.set('');
    this.videoCallTitle.set('');
    this.videoCallDurationMinutes.set(60);
    this.videoCallLessonId.set(null);
  }

  // Start polling to check if lesson was completed (for students to auto-hang up)
  private startLessonStatusCheck(lessonId: number): void {
    if (this.lessonStatusCheckInterval) return;

    this.lessonStatusCheckInterval = setInterval(() => {
      this.lessonService.refreshLesson(lessonId).subscribe({
        next: (lesson) => {
          if (lesson.status === 'COMPLETED') {
            // Coach has ended the lesson, close the video call
            this.stopLessonStatusCheck();
            this.closeVideoCall();
            // Reload lessons to update UI
            this.lessonService.loadUpcomingLessons().subscribe();
            this.lessonService.loadLessonHistory().subscribe();
          }
        }
      });
    }, 5000); // Check every 5 seconds
  }

  private stopLessonStatusCheck(): void {
    if (this.lessonStatusCheckInterval) {
      clearInterval(this.lessonStatusCheckInterval);
      this.lessonStatusCheckInterval = null;
    }
  }

  openRecording(lesson: Lesson): void {
    if (lesson.recordingUrl) {
      this.videoPlayerUrl.set(lesson.recordingUrl);
      this.videoPlayerLessonId.set(lesson.id);
      // Build title: course title (teacher name) or fallback
      const courseTitle = lesson.courseTitle || 'Cours';
      const teacherName = lesson.teacherName || '';
      this.videoPlayerTitle.set(teacherName ? `${courseTitle} (${teacherName})` : courseTitle);
      this.showVideoPlayer.set(true);
    }
  }

  closeVideoPlayer(): void {
    this.showVideoPlayer.set(false);
    this.videoPlayerUrl.set('');
    this.videoPlayerLessonId.set(null);
    this.videoPlayerTitle.set('');
  }

  // Check if it's time to join the lesson (15 min before until end)
  canJoinLesson(lesson: Lesson): boolean {
    const now = new Date();
    const lessonStart = new Date(lesson.scheduledAt);
    const lessonEnd = new Date(lessonStart.getTime() + lesson.durationMinutes * 60000);
    const joinableFrom = new Date(lessonStart.getTime() - 15 * 60000); // 15 min before

    return now >= joinableFrom && now <= lessonEnd;
  }

  // Get time until lesson can be joined
  getTimeUntilJoin(lesson: Lesson): string {
    const now = new Date();
    const lessonStart = new Date(lesson.scheduledAt);
    const joinableFrom = new Date(lessonStart.getTime() - 15 * 60000);
    const diffMs = joinableFrom.getTime() - now.getTime();

    if (diffMs <= 0) return '';

    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays > 0) return `dans ${diffDays}j`;
    if (diffHours > 0) return `dans ${diffHours}h`;
    return `dans ${diffMins}min`;
  }

  openStudentProfile(studentId: number): void {
    this.selectedStudentId.set(studentId);
  }

  closeStudentProfile(): void {
    this.selectedStudentId.set(null);
  }

  onCourseValidated(event: { studentId: number; courseId: number }): void {
    // Reload pending validations after course validation
    if (this.authService.isTeacher()) {
      this.loadPendingValidations();
    }
  }

  onStudentProfileClosedWithoutValidation(studentId: number): void {
    // Find student name from pending validations
    const validation = this.pendingValidations().find(v => v.studentId === studentId);
    const studentName = validation?.studentName || 'le joueur';

    // Show toast warning
    this.toastService.warning(`N'oubliez pas de valider les cours de ${studentName}`);

    // Create backend notification for later reminder
    this.http.post('/api/notifications/pending-validation', { studentId }).subscribe({
      error: (err) => console.debug('Failed to create pending validation notification:', err)
    });
  }

  getLevelLabel(level: string): string {
    const levelInfo = CHESS_LEVELS[level as ChessLevel];
    return levelInfo?.label || level;
  }

  getStatusLabel(lesson: Lesson): string {
    if (lesson.status === 'CANCELLED' && lesson.cancelledBy) {
      const isStudent = this.authService.isStudent();
      const isTeacher = this.authService.isTeacher();

      if (lesson.cancelledBy === 'STUDENT') {
        return isStudent ? 'Annulé par moi' : 'Annulé par le joueur';
      }
      if (lesson.cancelledBy === 'TEACHER') {
        return isTeacher ? 'Annulé par moi' : 'Annulé par le coach';
      }
      if (lesson.cancelledBy === 'SYSTEM') {
        return 'Annulé (auto)';
      }
    }
    return this.statusLabels[lesson.status].label;
  }

  toggleReason(lessonId: number): void {
    if (this.visibleReasonId() === lessonId) {
      this.visibleReasonId.set(null);
    } else {
      this.visibleReasonId.set(lessonId);
    }
  }

  toggleDropdown(lessonId: number, event: Event): void {
    event.stopPropagation();
    if (this.openDropdownId() === lessonId) {
      this.openDropdownId.set(null);
    } else {
      this.openDropdownId.set(lessonId);
    }
  }

  closeDropdown(): void {
    this.openDropdownId.set(null);
  }

  async deleteLesson(lessonId: number): Promise<void> {
    const confirmed = await this.confirmDialog.open({
      title: 'Supprimer le cours',
      message: 'Ce cours sera définitivement supprimé de votre historique.',
      confirmText: 'Supprimer',
      cancelText: 'Annuler',
      type: 'danger',
      icon: 'trash'
    });

    if (confirmed !== null) {
      this.lessonService.deleteLesson(lessonId).subscribe({
        error: (err) => console.error('Error deleting lesson:', err)
      });
    }
  }

  // Rating methods
  openRatingModal(lesson: Lesson): void {
    this.ratingLessonId.set(lesson.id);
    this.ratingTeacherName.set(lesson.teacherName);
    this.showRatingModal.set(true);
  }

  closeRatingModal(): void {
    this.showRatingModal.set(false);
    this.ratingLessonId.set(null);
    this.ratingTeacherName.set('');
  }

  onRatingSubmitted(): void {
    const lessonId = this.ratingLessonId();
    if (lessonId) {
      const rated = new Set(this.ratedLessons());
      rated.add(lessonId);
      this.ratedLessons.set(rated);
    }
    this.closeRatingModal();
  }

  canRateLesson(lesson: Lesson): boolean {
    return lesson.status === 'COMPLETED' &&
           this.authService.isStudent() &&
           !this.ratedLessons().has(lesson.id);
  }

  formatMonth(monthStr: string): string {
    const [year, month] = monthStr.split('-');
    const date = new Date(parseInt(year), parseInt(month) - 1);
    return date.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }

  clearHistoryFilters(): void {
    this.historyFilterPerson.set('');
    this.historyFilterMonth.set('');
    this.historyFilterHasRecording.set(false);
  }

  checkIfLessonRated(lessonId: number): void {
    this.ratingService.getLessonRating(lessonId).subscribe({
      next: (result) => {
        if (result.isRated) {
          const rated = new Set(this.ratedLessons());
          rated.add(lessonId);
          this.ratedLessons.set(rated);
        }
      }
    });
  }

  toggleEmailReminders(): void {
    if (this.savingEmailReminders()) return;

    const newValue = !this.emailRemindersEnabled();
    this.savingEmailReminders.set(true);

    this.http.patch<User>('/api/users/me', { emailRemindersEnabled: newValue }).subscribe({
      next: (updatedUser) => {
        this.emailRemindersEnabled.set(newValue);
        this.authService.updateCurrentUser(updatedUser);
        this.savingEmailReminders.set(false);
      },
      error: () => {
        this.savingEmailReminders.set(false);
      }
    });
  }
}
