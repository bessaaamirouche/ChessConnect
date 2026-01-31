import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError } from 'rxjs';
import { QuizQuestion, QuizSubmitRequest, QuizResult, QuizAnswer } from '../models/quiz.model';
import { ChessLevel } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class QuizService {
  private readonly apiUrl = '/api/quiz';

  private questionsSignal = signal<QuizQuestion[]>([]);
  private currentIndexSignal = signal<number>(0);
  private answersSignal = signal<Map<number, QuizAnswer>>(new Map());
  private resultSignal = signal<QuizResult | null>(null);
  private loadingSignal = signal<boolean>(false);
  private submittingSignal = signal<boolean>(false);
  private errorSignal = signal<string | null>(null);

  readonly questions = this.questionsSignal.asReadonly();
  readonly currentIndex = this.currentIndexSignal.asReadonly();
  readonly answers = this.answersSignal.asReadonly();
  readonly result = this.resultSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly submitting = this.submittingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly currentQuestion = computed(() => {
    const questions = this.questionsSignal();
    const index = this.currentIndexSignal();
    return questions[index] ?? null;
  });

  readonly totalQuestions = computed(() => this.questionsSignal().length);

  readonly currentAnswer = computed(() => {
    const question = this.currentQuestion();
    if (!question) return null;
    return this.answersSignal().get(question.id)?.answer ?? null;
  });

  readonly progressPercentage = computed(() => {
    const total = this.totalQuestions();
    if (total === 0) return 0;
    return Math.round(((this.currentIndexSignal() + 1) / total) * 100);
  });

  readonly isFirstQuestion = computed(() => this.currentIndexSignal() === 0);

  readonly isLastQuestion = computed(() => {
    return this.currentIndexSignal() === this.totalQuestions() - 1;
  });

  readonly canSubmit = computed(() => {
    const questions = this.questionsSignal();
    const answers = this.answersSignal();
    return questions.length > 0 && questions.every(q => answers.has(q.id));
  });

  readonly answeredCount = computed(() => this.answersSignal().size);

  readonly questionsByLevel = computed(() => {
    const questions = this.questionsSignal();
    const grouped = new Map<ChessLevel, QuizQuestion[]>();

    for (const q of questions) {
      if (!grouped.has(q.level)) {
        grouped.set(q.level, []);
      }
      grouped.get(q.level)!.push(q);
    }

    return grouped;
  });

  constructor(private http: HttpClient) {}

  loadQuestions(): Observable<QuizQuestion[]> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.resetQuiz();

    return this.http.get<QuizQuestion[]>(`${this.apiUrl}/questions`).pipe(
      tap(questions => {
        this.questionsSignal.set(questions);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.errorSignal.set('Impossible de charger les questions');
        this.loadingSignal.set(false);
        throw error;
      })
    );
  }

  setAnswer(questionId: number, answer: 'A' | 'B' | 'C' | 'D'): void {
    const newAnswers = new Map(this.answersSignal());
    newAnswers.set(questionId, { questionId, answer });
    this.answersSignal.set(newAnswers);
  }

  goToNext(): void {
    const index = this.currentIndexSignal();
    if (index < this.totalQuestions() - 1) {
      this.currentIndexSignal.set(index + 1);
    }
  }

  goToPrevious(): void {
    const index = this.currentIndexSignal();
    if (index > 0) {
      this.currentIndexSignal.set(index - 1);
    }
  }

  goToQuestion(index: number): void {
    if (index >= 0 && index < this.totalQuestions()) {
      this.currentIndexSignal.set(index);
    }
  }

  submitQuiz(): Observable<QuizResult> {
    this.submittingSignal.set(true);
    this.errorSignal.set(null);

    const request: QuizSubmitRequest = {
      answers: Array.from(this.answersSignal().values())
    };

    return this.http.post<QuizResult>(`${this.apiUrl}/submit`, request).pipe(
      tap(result => {
        this.resultSignal.set(result);
        this.submittingSignal.set(false);
      }),
      catchError(error => {
        this.errorSignal.set('Erreur lors de la soumission du quiz');
        this.submittingSignal.set(false);
        throw error;
      })
    );
  }

  getLastResult(): Observable<QuizResult | null> {
    return this.http.get<QuizResult>(`${this.apiUrl}/result`).pipe(
      tap(result => this.resultSignal.set(result)),
      catchError(() => {
        // No result found is not an error
        return [];
      })
    );
  }

  resetQuiz(): void {
    this.currentIndexSignal.set(0);
    this.answersSignal.set(new Map());
    this.resultSignal.set(null);
    this.errorSignal.set(null);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  getLevelIcon(level: ChessLevel): string {
    const icons: Record<ChessLevel, string> = {
      A: '♟',  // Pion
      B: '♞',  // Cavalier
      C: '♛',  // Reine
      D: '♚'   // Roi
    };
    return icons[level];
  }

  getLevelLabel(level: ChessLevel): string {
    const labels: Record<ChessLevel, string> = {
      A: 'Pion',
      B: 'Cavalier',
      C: 'Reine',
      D: 'Roi'
    };
    return labels[level];
  }
}
