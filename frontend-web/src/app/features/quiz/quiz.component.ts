import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { QuizService } from '../../core/services/quiz.service';
import { SeoService } from '../../core/services/seo.service';
import { AuthService } from '../../core/services/auth.service';
import { ProgressService } from '../../core/services/progress.service';
import { CHESS_LEVELS, ChessLevel } from '../../core/models/user.model';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChartBarSquare,
  heroCalendarDays,
  heroTrophy,
  heroAcademicCap,
  heroUserCircle,
  heroArrowRightOnRectangle,
  heroArrowLeft,
  heroArrowRight,
  heroCheckCircle,
  heroXCircle,
  heroPlayCircle,
  heroCreditCard
} from '@ng-icons/heroicons/outline';

type QuizStep = 'intro' | 'questions' | 'result';

@Component({
  selector: 'app-quiz',
  standalone: true,
  imports: [RouterLink, NgIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroChartBarSquare,
    heroCalendarDays,
    heroTrophy,
    heroAcademicCap,
    heroUserCircle,
    heroArrowRightOnRectangle,
    heroArrowLeft,
    heroArrowRight,
    heroCheckCircle,
    heroXCircle,
    heroPlayCircle,
    heroCreditCard
  })],
  templateUrl: './quiz.component.html',
  styleUrl: './quiz.component.scss'
})
export class QuizComponent implements OnInit {
  step = signal<QuizStep>('intro');
  CHESS_LEVELS = CHESS_LEVELS;
  levels: ChessLevel[] = ['PION', 'CAVALIER', 'FOU', 'TOUR', 'DAME'];

  constructor(
    public quizService: QuizService,
    public authService: AuthService,
    public progressService: ProgressService,
    private router: Router,
    private seoService: SeoService
  ) {
    this.seoService.setQuizPage();
  }

  ngOnInit(): void {
    // Check if user already has a result
    this.quizService.getLastResult().subscribe();
  }

  startQuiz(): void {
    this.quizService.loadQuestions().subscribe({
      next: () => {
        this.step.set('questions');
      }
    });
  }

  selectAnswer(answer: 'A' | 'B' | 'C' | 'D'): void {
    const question = this.quizService.currentQuestion();
    if (question) {
      this.quizService.setAnswer(question.id, answer);
    }
  }

  isAnswerSelected(answer: 'A' | 'B' | 'C' | 'D'): boolean {
    return this.quizService.currentAnswer() === answer;
  }

  goNext(): void {
    if (this.quizService.isLastQuestion()) {
      this.submitQuiz();
    } else {
      this.quizService.goToNext();
    }
  }

  goPrevious(): void {
    this.quizService.goToPrevious();
  }

  submitQuiz(): void {
    this.quizService.submitQuiz().subscribe({
      next: () => {
        this.step.set('result');
        // Reload progress to reflect new level
        this.progressService.loadMyProgress().subscribe();
      }
    });
  }

  retakeQuiz(): void {
    this.quizService.resetQuiz();
    this.step.set('intro');
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  goToProgress(): void {
    this.router.navigate(['/progress']);
  }

  getLevelIcon(level: ChessLevel): string {
    return this.quizService.getLevelIcon(level);
  }

  getScorePercentage(level: ChessLevel): number {
    const result = this.quizService.result();
    if (!result) return 0;

    const score = result.scoresByLevel[level] ?? 0;
    const total = result.totalByLevel[level] ?? 1;
    return Math.round((score / total) * 100);
  }

  isLevelPassed(level: ChessLevel): boolean {
    return this.getScorePercentage(level) >= 70;
  }

  isLevelEvaluated(level: ChessLevel): boolean {
    const result = this.quizService.result();
    if (!result) return false;

    const determinedLevelOrder = CHESS_LEVELS[result.determinedLevel].order;
    const levelOrder = CHESS_LEVELS[level].order;

    // A level is evaluated if it's at or below the determined level + 1
    // (we evaluate until first failure)
    return levelOrder <= determinedLevelOrder + 1;
  }

  logout(): void {
    this.authService.logout();
  }
}
