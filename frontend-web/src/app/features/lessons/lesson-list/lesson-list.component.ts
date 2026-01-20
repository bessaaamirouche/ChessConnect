import { Component, OnInit, signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LessonService } from '../../../core/services/lesson.service';
import { AuthService } from '../../../core/services/auth.service';
import { RatingService } from '../../../core/services/rating.service';
import { LESSON_STATUS_LABELS, Lesson } from '../../../core/models/lesson.model';
import { ConfirmModalComponent } from '../../../shared/confirm-modal/confirm-modal.component';
import { StudentProfileModalComponent } from '../../../shared/student-profile-modal/student-profile-modal.component';
import { RatingModalComponent } from '../../../shared/rating-modal/rating-modal.component';
import { VideoCallComponent } from '../../../shared/video-call/video-call.component';
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
  heroTrash
} from '@ng-icons/heroicons/outline';
import { CHESS_LEVELS } from '../../../core/models/user.model';

@Component({
  selector: 'app-lesson-list',
  standalone: true,
  imports: [RouterLink, DatePipe, FormsModule, ConfirmModalComponent, NgIconComponent, StudentProfileModalComponent, RatingModalComponent, VideoCallComponent],
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
    heroTrash
  })],
  templateUrl: './lesson-list.component.html',
  styleUrl: './lesson-list.component.scss'
})
export class LessonListComponent implements OnInit {
  @ViewChild('confirmModal') confirmModal!: ConfirmModalComponent;

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

  // Video call
  showVideoCall = signal(false);
  videoCallRoomName = signal('');
  videoCallUserName = signal('');
  videoCallTitle = signal('');

  constructor(
    public lessonService: LessonService,
    public authService: AuthService,
    private ratingService: RatingService
  ) {}

  ngOnInit(): void {
    this.lessonService.loadUpcomingLessons().subscribe();
    this.lessonService.loadLessonHistory().subscribe();
  }

  confirmLesson(lessonId: number): void {
    this.lessonService.confirmLesson(lessonId).subscribe();
  }

  async cancelLesson(lessonId: number): Promise<void> {
    const reason = await this.confirmModal.open({
      title: 'Annuler le cours',
      message: 'Êtes-vous sûr de vouloir annuler ce cours ?',
      confirmText: 'Annuler le cours',
      cancelText: 'Retour',
      type: 'danger',
      showInput: true,
      inputLabel: 'Raison de l\'annulation (optionnel)',
      inputPlaceholder: 'Ex: Indisponibilité...'
    });

    if (reason !== null) {
      this.lessonService.cancelLesson(lessonId, reason as string || undefined).subscribe();
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
      const observations = this.observationsText().trim() || undefined;
      this.lessonService.completeLesson(lessonId, observations).subscribe({
        next: () => this.closeCompleteModal(),
        error: () => this.closeCompleteModal()
      });
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

  logout(): void {
    this.authService.logout();
  }

  openVideoCall(lesson: Lesson): void {
    const roomName = lesson.zoomLink?.split('/').pop() || `Lesson-${lesson.id}`;
    const userName = this.authService.currentUser()?.firstName + ' ' + this.authService.currentUser()?.lastName;
    const isTeacher = this.authService.isTeacher();
    const otherPerson = isTeacher ? lesson.studentName : lesson.teacherName;

    this.videoCallRoomName.set(roomName);
    this.videoCallUserName.set(userName || 'Participant');
    this.videoCallTitle.set(`Cours avec ${otherPerson}`);
    this.showVideoCall.set(true);
  }

  closeVideoCall(): void {
    this.showVideoCall.set(false);
    this.videoCallRoomName.set('');
    this.videoCallUserName.set('');
    this.videoCallTitle.set('');
  }

  openRecording(lesson: Lesson): void {
    if (lesson.recordingUrl) {
      window.open(lesson.recordingUrl, '_blank');
    }
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
    // Optionally refresh lessons or show notification
    console.log('Course validated:', event);
  }

  getLevelLabel(level: string): string {
    const levelInfo = CHESS_LEVELS[level as keyof typeof CHESS_LEVELS];
    return levelInfo?.label || level;
  }

  getStatusLabel(lesson: Lesson): string {
    if (lesson.status === 'CANCELLED' && lesson.cancelledBy) {
      const isStudent = this.authService.isStudent();
      const isTeacher = this.authService.isTeacher();

      if (lesson.cancelledBy === 'STUDENT') {
        return isStudent ? 'Annulé par moi' : 'Annulé par l\'élève';
      }
      if (lesson.cancelledBy === 'TEACHER') {
        return isTeacher ? 'Annulé par moi' : 'Annulé par le prof';
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

  async deleteLesson(lessonId: number): Promise<void> {
    const confirmed = await this.confirmModal.open({
      title: 'Supprimer le cours',
      message: 'Êtes-vous sûr de vouloir supprimer ce cours de votre historique ?',
      confirmText: 'Supprimer',
      cancelText: 'Annuler',
      type: 'danger'
    });

    if (confirmed !== null) {
      this.lessonService.deleteLesson(lessonId).subscribe();
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
}
