import {
  Component, OnInit, OnDestroy, signal, computed,
  ChangeDetectionStrategy,
  viewChild
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroArrowLeft, heroCpuChip, heroTrophy, heroClock,
  heroExclamationTriangle, heroCheckCircle, heroXCircle
} from '@ng-icons/heroicons/outline';
import { ChessBoardComponent } from '../../shared/components/chess-board/chess-board.component';
import { ExerciseService } from '../../core/services/exercise.service';
import { ChessEngineService } from '../../core/services/chess-engine.service';
import { Exercise, DIFFICULTY_LABELS } from '../../core/models/exercise.model';

type GameStatus = 'loading' | 'playing' | 'player_won' | 'ai_won' | 'draw';

@Component({
    selector: 'app-exercise',
    imports: [RouterLink, NgIconComponent, ChessBoardComponent, TranslateModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [provideIcons({
            heroArrowLeft, heroCpuChip, heroTrophy, heroClock,
            heroExclamationTriangle, heroCheckCircle, heroXCircle
        })],
    templateUrl: './exercise.component.html',
    styleUrl: './exercise.component.scss'
})
export class ExerciseComponent implements OnInit, OnDestroy {
  readonly chessBoard = viewChild.required<ChessBoardComponent>('chessBoard');

  exercise = signal<Exercise | null>(null);
  gameStatus = signal<GameStatus>('loading');
  moveHistory = signal<string[]>([]);
  isPlayerTurn = signal<boolean>(true);
  engineInitialized = signal<boolean>(false);

  difficultyLabels = DIFFICULTY_LABELS;

  // Computed signals
  moveCount = computed(() => Math.ceil(this.moveHistory().length / 2));

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public exerciseService: ExerciseService,
    public engineService: ChessEngineService
  ) {}

  async ngOnInit(): Promise<void> {
    const lessonId = this.route.snapshot.params['lessonId'];

    if (!lessonId) {
      this.router.navigate(['/lessons']);
      return;
    }

    // Initialize Stockfish engine
    try {
      await this.engineService.initialize();
      this.engineInitialized.set(true);
    } catch (error) {
      console.error('Failed to initialize chess engine:', error);
    }

    // Load exercise
    this.exerciseService.loadExerciseForLesson(parseInt(lessonId, 10)).subscribe({
      next: (exercise) => {
        this.exercise.set(exercise);
        this.setupGame(exercise);
      },
      error: (err) => {
        console.error('Failed to load exercise:', err);
        this.router.navigate(['/lessons']);
      }
    });
  }

  ngOnDestroy(): void {
    this.engineService.dispose();
    this.exerciseService.clearExercise();
  }

  private setupGame(exercise: Exercise): void {
    // Configure engine difficulty
    this.engineService.setDifficulty(exercise.difficultyLevel);
    this.engineService.newGame();
    this.engineService.setPosition(exercise.startingFen);

    this.gameStatus.set('playing');

    // If player is black, AI moves first
    if (exercise.playerColor === 'black') {
      this.isPlayerTurn.set(false);
      // Small delay to let the board render
      setTimeout(() => this.makeAIMove(), 500);
    } else {
      this.isPlayerTurn.set(true);
    }
  }

  onPlayerMove(move: { from: string; to: string; promotion?: string }): void {
    const moveUci = `${move.from}${move.to}${move.promotion || ''}`;
    this.moveHistory.update(history => [...history, moveUci]);

    // Update engine position
    const exercise = this.exercise();
    if (exercise) {
      this.engineService.setPositionWithMoves(
        exercise.startingFen,
        this.moveHistory()
      );
    }

    // Check game status
    if (this.checkGameOver()) {
      return;
    }

    // AI's turn
    this.isPlayerTurn.set(false);
    this.chessBoard()?.setMovable(false);

    // Small delay before AI move for better UX
    setTimeout(() => this.makeAIMove(), 300);
  }

  private async makeAIMove(): Promise<void> {
    const exercise = this.exercise();
    if (!exercise || this.gameStatus() !== 'playing') return;

    try {
      const result = await this.engineService.getBestMove(exercise.thinkTimeMs);

      if (result.bestMove && result.bestMove !== '(none)') {
        // Parse UCI move (e.g., "e2e4" or "e7e8q")
        const from = result.bestMove.substring(0, 2);
        const to = result.bestMove.substring(2, 4);
        const promotion = result.bestMove.length > 4
          ? result.bestMove.substring(4, 5)
          : undefined;

        // Make move on board
        const chessBoard = this.chessBoard();
        if (chessBoard) {
          chessBoard.makeMove(from, to, promotion);
          this.moveHistory.update(history => [...history, result.bestMove]);

          // Update engine position
          this.engineService.setPositionWithMoves(
            exercise.startingFen,
            this.moveHistory()
          );

          // Check game status after AI move
          if (this.checkGameOver()) {
            return;
          }

          // Player's turn
          this.isPlayerTurn.set(true);
          chessBoard.setMovable(true);
        }
      }
    } catch (error) {
      console.error('AI move error:', error);
      // Recover by enabling player moves
      this.isPlayerTurn.set(true);
      this.chessBoard()?.setMovable(true);
    }
  }

  private checkGameOver(): boolean {
    const chessBoard = this.chessBoard();
    if (!chessBoard) return false;

    if (chessBoard.isCheckmate()) {
      const playerColor = this.exercise()?.playerColor || 'white';
      const currentTurn = chessBoard.getTurn();

      // If it's the losing side's turn during checkmate
      if (currentTurn !== playerColor) {
        // Player delivered checkmate
        this.gameStatus.set('player_won');
      } else {
        // AI delivered checkmate
        this.gameStatus.set('ai_won');
      }
      chessBoard.setMovable(false);
      return true;
    }

    if (chessBoard.isDraw() || chessBoard.isStalemate()) {
      this.gameStatus.set('draw');
      chessBoard.setMovable(false);
      return true;
    }

    return false;
  }

  onReset(): void {
    const exercise = this.exercise();
    if (!exercise) return;

    this.gameStatus.set('playing');
    this.moveHistory.set([]);

    const chessBoard = this.chessBoard();
    if (chessBoard) {
      chessBoard.setPosition(exercise.startingFen);
    }

    this.engineService.newGame();
    this.engineService.setPosition(exercise.startingFen);

    if (exercise.playerColor === 'white') {
      this.isPlayerTurn.set(true);
      chessBoard?.setMovable(true);
    } else {
      this.isPlayerTurn.set(false);
      setTimeout(() => this.makeAIMove(), 500);
    }
  }

  onResign(): void {
    this.gameStatus.set('ai_won');
    this.chessBoard()?.setMovable(false);
  }

  goBack(): void {
    this.router.navigate(['/lessons']);
  }

  formatMoveHistory(): { moveNumber: number; white: string; black?: string }[] {
    const history = this.moveHistory();
    const formatted: { moveNumber: number; white: string; black?: string }[] = [];

    for (let i = 0; i < history.length; i += 2) {
      formatted.push({
        moveNumber: Math.floor(i / 2) + 1,
        white: history[i],
        black: history[i + 1]
      });
    }

    return formatted;
  }

  getEvalBarWidth(): number {
    const evaluation = this.engineService.evaluation();
    if (evaluation === null) return 50;

    // Clamp evaluation between -10 and 10 for display
    const clampedEval = Math.max(-10, Math.min(10, evaluation));
    // Convert to percentage (0-100)
    return 50 + (clampedEval * 5);
  }

  formatEvaluation(): string {
    const evaluation = this.engineService.evaluation();
    if (evaluation === null) return '0.0';

    // Handle mate scores
    if (Math.abs(evaluation) >= 100) {
      return evaluation > 0 ? 'M+' : 'M-';
    }

    const sign = evaluation > 0 ? '+' : '';
    return `${sign}${evaluation.toFixed(1)}`;
  }
}
