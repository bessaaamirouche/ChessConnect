import { Injectable, signal, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export interface EngineEvaluation {
  bestMove: string;
  ponder?: string;
  evaluation?: number;
  depth: number;
}

@Injectable({
  providedIn: 'root'
})
export class ChessEngineService {
  private worker: Worker | null = null;
  private isBrowser: boolean;
  private isReady = false;
  private pendingCallback: ((result: EngineEvaluation) => void) | null = null;
  private currentDepth = 0;

  private engineReadySignal = signal<boolean>(false);
  private thinkingSignal = signal<boolean>(false);
  private evaluationSignal = signal<number | null>(null);

  readonly engineReady = this.engineReadySignal.asReadonly();
  readonly thinking = this.thinkingSignal.asReadonly();
  readonly evaluation = this.evaluationSignal.asReadonly();

  // Difficulty to Stockfish skill level mapping
  private readonly SKILL_LEVELS: Record<string, number> = {
    'DEBUTANT': 0,
    'FACILE': 5,
    'MOYEN': 10,
    'DIFFICILE': 15,
    'EXPERT': 20
  };

  constructor(@Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  async initialize(): Promise<void> {
    if (!this.isBrowser || this.worker) return;

    return new Promise((resolve, reject) => {
      try {
        // Load Stockfish from local assets
        this.worker = new Worker('/assets/stockfish/stockfish.js');

        this.worker.onmessage = (event: MessageEvent) => {
          this.handleEngineMessage(event.data);
        };

        this.worker.onerror = (error) => {
          console.error('[Stockfish] Worker error:', error);
          reject(error);
        };

        // Initialize UCI protocol
        this.sendCommand('uci');

        // Wait for UCI OK with timeout
        const timeout = setTimeout(() => {
          reject(new Error('Stockfish initialization timeout'));
        }, 15000);

        const checkReady = setInterval(() => {
          if (this.isReady) {
            clearInterval(checkReady);
            clearTimeout(timeout);
            this.engineReadySignal.set(true);
            console.log('[Stockfish] Engine ready');
            resolve();
          }
        }, 100);

      } catch (error) {
        reject(error);
      }
    });
  }

  private handleEngineMessage(message: string): void {
    if (message === 'uciok') {
      // Set default options for lower memory usage
      this.sendCommand('setoption name Hash value 16');
      this.sendCommand('setoption name Threads value 1');
      this.sendCommand('isready');
    }

    if (message === 'readyok') {
      this.isReady = true;
    }

    // Parse evaluation info
    if (message.startsWith('info') && message.includes('score cp')) {
      const cpMatch = message.match(/score cp (-?\d+)/);
      const depthMatch = message.match(/depth (\d+)/);

      if (cpMatch) {
        const centipawns = parseInt(cpMatch[1], 10);
        this.evaluationSignal.set(centipawns / 100);
      }
      if (depthMatch) {
        this.currentDepth = parseInt(depthMatch[1], 10);
      }
    }

    // Parse mate score
    if (message.startsWith('info') && message.includes('score mate')) {
      const mateMatch = message.match(/score mate (-?\d+)/);
      if (mateMatch) {
        const mateIn = parseInt(mateMatch[1], 10);
        // Show large eval for mate
        this.evaluationSignal.set(mateIn > 0 ? 100 : -100);
      }
    }

    // Parse best move
    if (message.startsWith('bestmove')) {
      this.thinkingSignal.set(false);
      const parts = message.split(' ');
      const bestMove = parts[1];
      const ponder = parts.length > 3 ? parts[3] : undefined;

      if (this.pendingCallback) {
        this.pendingCallback({
          bestMove,
          ponder,
          evaluation: this.evaluationSignal() ?? undefined,
          depth: this.currentDepth
        });
        this.pendingCallback = null;
      }
    }
  }

  private sendCommand(command: string): void {
    if (this.worker) {
      this.worker.postMessage(command);
    }
  }

  setDifficulty(level: string): void {
    const skillLevel = this.SKILL_LEVELS[level] ?? 10;
    this.sendCommand(`setoption name Skill Level value ${skillLevel}`);

    // Enable strength limiting for lower levels
    const limitStrength = skillLevel < 20;
    this.sendCommand(`setoption name UCI_LimitStrength value ${limitStrength}`);

    if (limitStrength) {
      // Approximate ELO based on skill level
      const elo = 1350 + (skillLevel * 60);
      this.sendCommand(`setoption name UCI_Elo value ${elo}`);
    }
  }

  setPosition(fen: string): void {
    this.sendCommand(`position fen ${fen}`);
  }

  setPositionWithMoves(fen: string, moves: string[]): void {
    if (moves.length === 0) {
      this.setPosition(fen);
    } else {
      this.sendCommand(`position fen ${fen} moves ${moves.join(' ')}`);
    }
  }

  getBestMove(timeMs: number = 2000): Promise<EngineEvaluation> {
    // Ensure minimum thinking time for Stockfish to respond
    const actualTime = Math.max(timeMs, 300);

    return new Promise((resolve) => {
      this.thinkingSignal.set(true);
      this.currentDepth = 0;
      this.pendingCallback = resolve;

      // Safety timeout in case engine doesn't respond
      const timeout = setTimeout(() => {
        if (this.pendingCallback) {
          console.warn('[Stockfish] Timeout - forcing stop');
          this.sendCommand('stop');
        }
      }, actualTime + 2000);

      // Clear timeout when we get a response
      const originalCallback = this.pendingCallback;
      this.pendingCallback = (result) => {
        clearTimeout(timeout);
        originalCallback(result);
      };

      this.sendCommand(`go movetime ${actualTime}`);
    });
  }

  stopThinking(): void {
    this.sendCommand('stop');
  }

  newGame(): void {
    this.sendCommand('ucinewgame');
    this.sendCommand('isready');
    this.evaluationSignal.set(null);
    this.currentDepth = 0;
  }

  dispose(): void {
    if (this.worker) {
      this.sendCommand('quit');
      this.worker.terminate();
      this.worker = null;
    }
    this.isReady = false;
    this.engineReadySignal.set(false);
    this.thinkingSignal.set(false);
    this.pendingCallback = null;
  }
}
