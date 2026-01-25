import {
  Component, Input, Output, EventEmitter, OnDestroy,
  signal, ElementRef, ViewChild, AfterViewInit, PLATFORM_ID, Inject,
  ChangeDetectionStrategy
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroArrowPath, heroFlag } from '@ng-icons/heroicons/outline';

// Types for chess.js and chessground
interface ChessMove {
  from: string;
  to: string;
  promotion?: string;
  san?: string;
  color?: 'w' | 'b';
  piece?: string;
  captured?: string;
  flags?: string;
}

@Component({
  selector: 'app-chess-board',
  standalone: true,
  imports: [NgIconComponent],
  viewProviders: [provideIcons({ heroArrowPath, heroFlag })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="chess-board-wrapper">
      <div #boardContainer class="chess-board-container"></div>
      @if (showControls) {
        <div class="chess-board-controls">
          <button class="btn btn--ghost btn--sm" (click)="onReset()">
            <ng-icon name="heroArrowPath" size="16"></ng-icon>
            Recommencer
          </button>
          <button class="btn btn--ghost btn--sm btn--danger" (click)="onResign()">
            <ng-icon name="heroFlag" size="16"></ng-icon>
            Abandonner
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .chess-board-wrapper {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .chess-board-container {
      width: 100%;
      max-width: min(70vh, 600px);
      aspect-ratio: 1;
      margin: 0 auto;
    }

    @media (min-width: 1200px) {
      .chess-board-container {
        max-width: min(75vh, 700px);
      }
    }

    .chess-board-controls {
      display: flex;
      justify-content: center;
      gap: 1rem;
    }

    .btn--danger {
      color: #ef4444;
      &:hover {
        background: rgba(239, 68, 68, 0.1);
      }
    }

    :host ::ng-deep {
      .cg-wrap {
        width: 100%;
        height: 100%;
      }

      cg-board {
        background-color: #312e2b;
      }

      .cg-wrap piece {
        will-change: transform;
      }

      .cg-wrap piece.dragging {
        cursor: grabbing !important;
      }
    }
  `]
})
export class ChessBoardComponent implements AfterViewInit, OnDestroy {
  @ViewChild('boardContainer', { static: false }) boardContainer!: ElementRef;

  @Input() fen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
  @Input() orientation: 'white' | 'black' = 'white';
  @Input() movable = true;
  @Input() showControls = true;
  @Input() disabled = false;

  @Output() move = new EventEmitter<{ from: string; to: string; promotion?: string }>();
  @Output() reset = new EventEmitter<void>();
  @Output() resign = new EventEmitter<void>();

  private ground: any = null;
  private isBrowser: boolean;
  private chess: any = null;
  private Chessground: any = null;
  private Chess: any = null;

  constructor(@Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngAfterViewInit(): void {
    if (this.isBrowser) {
      this.loadDependencies().then(() => {
        this.initializeBoard();
      }).catch(err => {
        console.error('Failed to load chess dependencies:', err);
      });
    }
  }

  ngOnDestroy(): void {
    if (this.ground) {
      this.ground.destroy();
      this.ground = null;
    }
  }

  private async loadDependencies(): Promise<void> {
    // Load Chessground CSS dynamically
    if (!document.querySelector('link[href*="chessground"]')) {
      const cssFiles = [
        'https://unpkg.com/chessground@9.1.1/assets/chessground.base.css',
        'https://unpkg.com/chessground@9.1.1/assets/chessground.brown.css',
        'https://unpkg.com/chessground@9.1.1/assets/chessground.cburnett.css'
      ];

      for (const href of cssFiles) {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = href;
        document.head.appendChild(link);
      }

      // Wait a bit for CSS to load
      await new Promise(resolve => setTimeout(resolve, 100));
    }

    // Dynamic imports
    const [chessgroundModule, chessModule] = await Promise.all([
      import('chessground'),
      import('chess.js')
    ]);

    this.Chessground = chessgroundModule.Chessground;
    this.Chess = chessModule.Chess;
  }

  private initializeBoard(): void {
    if (!this.Chessground || !this.Chess || !this.boardContainer?.nativeElement) {
      return;
    }

    this.chess = new this.Chess(this.fen);

    this.ground = this.Chessground(this.boardContainer.nativeElement, {
      fen: this.fen,
      orientation: this.orientation,
      turnColor: this.chess.turn() === 'w' ? 'white' : 'black',
      movable: {
        free: false,
        color: this.movable && !this.disabled ? this.orientation : undefined,
        dests: this.movable && !this.disabled ? this.getValidMoves() : new Map(),
        events: {
          after: (orig: string, dest: string) => {
            this.onMoveInternal(orig, dest);
          }
        }
      },
      draggable: {
        enabled: this.movable && !this.disabled
      },
      highlight: {
        lastMove: true,
        check: true
      },
      animation: {
        enabled: true,
        duration: 200
      },
      premovable: {
        enabled: false
      }
    });
  }

  private getValidMoves(): Map<string, string[]> {
    const dests = new Map<string, string[]>();
    if (!this.chess) return dests;

    const moves: ChessMove[] = this.chess.moves({ verbose: true });

    for (const move of moves) {
      if (!dests.has(move.from)) {
        dests.set(move.from, []);
      }
      dests.get(move.from)!.push(move.to);
    }

    return dests;
  }

  private onMoveInternal(from: string, to: string): void {
    if (!this.chess) return;

    // Check for promotion
    const piece = this.chess.get(from);
    const isPromotion = piece?.type === 'p' &&
      ((piece.color === 'w' && to[1] === '8') ||
       (piece.color === 'b' && to[1] === '1'));

    const promotion = isPromotion ? 'q' : undefined;

    // Make move in chess.js
    const moveResult = this.chess.move({ from, to, promotion });

    if (moveResult) {
      // Update board state
      this.updateBoardState();
      // Emit move event
      this.move.emit({ from, to, promotion });
    }
  }

  private updateBoardState(): void {
    if (!this.ground || !this.chess) return;

    const turnColor = this.chess.turn() === 'w' ? 'white' : 'black';
    const isPlayerTurn = turnColor === this.orientation;

    this.ground.set({
      fen: this.chess.fen(),
      turnColor,
      check: this.chess.inCheck(),
      movable: {
        color: isPlayerTurn && this.movable && !this.disabled ? this.orientation : undefined,
        dests: isPlayerTurn && this.movable && !this.disabled ? this.getValidMoves() : new Map()
      }
    });
  }

  // Public methods for external control

  makeMove(from: string, to: string, promotion?: string): boolean {
    if (!this.chess || !this.ground) return false;

    const moveResult = this.chess.move({ from, to, promotion });
    if (moveResult) {
      // Update board with new position and highlight last move
      const turnColor = this.chess.turn() === 'w' ? 'white' : 'black';
      const isPlayerTurn = turnColor === this.orientation;

      this.ground.set({
        fen: this.chess.fen(),
        turnColor,
        lastMove: [from, to],
        check: this.chess.inCheck(),
        movable: {
          color: isPlayerTurn && this.movable && !this.disabled ? this.orientation : undefined,
          dests: isPlayerTurn && this.movable && !this.disabled ? this.getValidMoves() : new Map()
        }
      });
      return true;
    }
    return false;
  }

  setPosition(fen: string): void {
    if (!this.chess || !this.ground) return;

    this.chess.load(fen);
    this.ground.set({
      fen,
      turnColor: this.chess.turn() === 'w' ? 'white' : 'black',
      lastMove: undefined,
      check: this.chess.inCheck()
    });
    this.updateBoardState();
  }

  setMovable(enabled: boolean): void {
    if (!this.ground) return;

    const turnColor = this.chess?.turn() === 'w' ? 'white' : 'black';
    const isPlayerTurn = turnColor === this.orientation;

    this.ground.set({
      movable: {
        color: enabled && isPlayerTurn ? this.orientation : undefined,
        dests: enabled && isPlayerTurn ? this.getValidMoves() : new Map()
      },
      draggable: {
        enabled
      }
    });
  }

  isGameOver(): boolean {
    return this.chess?.isGameOver() ?? false;
  }

  isCheckmate(): boolean {
    return this.chess?.isCheckmate() ?? false;
  }

  isStalemate(): boolean {
    return this.chess?.isStalemate() ?? false;
  }

  isDraw(): boolean {
    return this.chess?.isDraw() ?? false;
  }

  isInCheck(): boolean {
    return this.chess?.inCheck() ?? false;
  }

  getCurrentFen(): string {
    return this.chess?.fen() ?? this.fen;
  }

  getTurn(): 'white' | 'black' {
    return this.chess?.turn() === 'w' ? 'white' : 'black';
  }

  getMoveHistory(): string[] {
    return this.chess?.history() ?? [];
  }

  onReset(): void {
    this.reset.emit();
  }

  onResign(): void {
    this.resign.emit();
  }
}
