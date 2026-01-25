import { TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { ChessEngineService, EngineEvaluation } from './chess-engine.service';

describe('ChessEngineService', () => {
  let service: ChessEngineService;
  let mockWorker: any;

  beforeEach(() => {
    // Mock Worker
    mockWorker = {
      postMessage: jasmine.createSpy('postMessage'),
      terminate: jasmine.createSpy('terminate'),
      onmessage: null as any,
      onerror: null as any
    };

    // Mock Worker constructor
    (window as any).Worker = jasmine.createSpy('Worker').and.callFake(() => mockWorker);

    TestBed.configureTestingModule({
      providers: [
        ChessEngineService,
        { provide: PLATFORM_ID, useValue: 'browser' }
      ]
    });

    service = TestBed.inject(ChessEngineService);
  });

  afterEach(() => {
    service.dispose();
  });

  describe('Initialization', () => {
    it('should create the service', () => {
      expect(service).toBeTruthy();
    });

    it('should initialize engine in browser environment', fakeAsync(() => {
      const initPromise = service.initialize();

      // Simulate UCI response
      tick(100);
      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'id name myChessBot' });
        mockWorker.onmessage({ data: 'id author mychess.fr' });
        mockWorker.onmessage({ data: 'uciok' });
      }

      tick(100);
      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'readyok' });
      }

      tick(100);

      initPromise.then(() => {
        expect(service.engineReady()).toBeTrue();
      });

      flush();
    }));

    it('should send uci command on initialization', fakeAsync(() => {
      service.initialize();
      tick(100);

      expect(mockWorker.postMessage).toHaveBeenCalledWith('uci');
      flush();
    }));

    it('should not initialize when not in browser', () => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          ChessEngineService,
          { provide: PLATFORM_ID, useValue: 'server' }
        ]
      });

      const serverService = TestBed.inject(ChessEngineService);
      const promise = serverService.initialize();

      promise.then(() => {
        expect(serverService.engineReady()).toBeFalse();
      });
    });
  });

  describe('setDifficulty', () => {
    beforeEach(fakeAsync(() => {
      service.initialize();
      tick(100);
      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'uciok' });
        mockWorker.onmessage({ data: 'readyok' });
      }
      tick(100);
      flush();
    }));

    it('should set DEBUTANT skill level to 0', () => {
      service.setDifficulty('DEBUTANT');
      expect(mockWorker.postMessage).toHaveBeenCalledWith('setoption name Skill Level value 0');
    });

    it('should set FACILE skill level to 5', () => {
      service.setDifficulty('FACILE');
      expect(mockWorker.postMessage).toHaveBeenCalledWith('setoption name Skill Level value 5');
    });

    it('should set MOYEN skill level to 10', () => {
      service.setDifficulty('MOYEN');
      expect(mockWorker.postMessage).toHaveBeenCalledWith('setoption name Skill Level value 10');
    });

    it('should set DIFFICILE skill level to 15', () => {
      service.setDifficulty('DIFFICILE');
      expect(mockWorker.postMessage).toHaveBeenCalledWith('setoption name Skill Level value 15');
    });

    it('should set EXPERT skill level to 20', () => {
      service.setDifficulty('EXPERT');
      expect(mockWorker.postMessage).toHaveBeenCalledWith('setoption name Skill Level value 20');
    });

    it('should default to 10 for unknown level', () => {
      service.setDifficulty('UNKNOWN');
      expect(mockWorker.postMessage).toHaveBeenCalledWith('setoption name Skill Level value 10');
    });
  });

  describe('setPosition', () => {
    beforeEach(fakeAsync(() => {
      service.initialize();
      tick(100);
      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'uciok' });
        mockWorker.onmessage({ data: 'readyok' });
      }
      tick(100);
      flush();
    }));

    it('should send position command with FEN', () => {
      const fen = 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1';
      service.setPosition(fen);
      expect(mockWorker.postMessage).toHaveBeenCalledWith(`position fen ${fen}`);
    });

    it('should handle starting position FEN', () => {
      const startingFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
      service.setPosition(startingFen);
      expect(mockWorker.postMessage).toHaveBeenCalledWith(`position fen ${startingFen}`);
    });
  });

  describe('setPositionWithMoves', () => {
    beforeEach(fakeAsync(() => {
      service.initialize();
      tick(100);
      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'uciok' });
        mockWorker.onmessage({ data: 'readyok' });
      }
      tick(100);
      flush();
    }));

    it('should send position with moves', () => {
      const fen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
      const moves = ['e2e4', 'e7e5', 'g1f3'];
      service.setPositionWithMoves(fen, moves);
      expect(mockWorker.postMessage).toHaveBeenCalledWith(`position fen ${fen} moves e2e4 e7e5 g1f3`);
    });

    it('should send position without moves when moves array is empty', () => {
      const fen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
      service.setPositionWithMoves(fen, []);
      expect(mockWorker.postMessage).toHaveBeenCalledWith(`position fen ${fen}`);
    });
  });

  describe('getBestMove', () => {
    beforeEach(fakeAsync(() => {
      service.initialize();
      tick(100);
      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'uciok' });
        mockWorker.onmessage({ data: 'readyok' });
      }
      tick(100);
      flush();
    }));

    it('should send go command', fakeAsync(() => {
      service.getBestMove();
      tick(100);
      expect(mockWorker.postMessage).toHaveBeenCalledWith('go');
      flush();
    }));

    it('should set thinking signal to true while thinking', fakeAsync(() => {
      service.getBestMove();
      tick(100);
      expect(service.thinking()).toBeTrue();
      flush();
    }));

    it('should return best move from engine response', fakeAsync(() => {
      let result: EngineEvaluation | null = null;

      service.getBestMove().then(r => result = r);
      tick(100);

      // Simulate engine response
      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'info depth 3 score cp 50' });
        mockWorker.onmessage({ data: 'bestmove e2e4' });
      }

      tick(100);
      flush();

      expect(result).toBeTruthy();
      expect(result!.bestMove).toBe('e2e4');
    }));

    it('should set thinking signal to false after receiving best move', fakeAsync(() => {
      service.getBestMove();
      tick(100);

      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'bestmove e2e4' });
      }

      tick(100);
      flush();

      expect(service.thinking()).toBeFalse();
    }));

    it('should parse evaluation from info message', fakeAsync(() => {
      let result: EngineEvaluation | null = null;

      service.getBestMove().then(r => result = r);
      tick(100);

      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'info depth 3 score cp 150' });
        mockWorker.onmessage({ data: 'bestmove e2e4' });
      }

      tick(100);
      flush();

      expect(service.evaluation()).toBe(1.5); // 150 centipawns = 1.5 pawns
    }));

    it('should handle mate score', fakeAsync(() => {
      service.getBestMove();
      tick(100);

      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'info depth 3 score mate 2' });
        mockWorker.onmessage({ data: 'bestmove d1h5' });
      }

      tick(100);
      flush();

      expect(service.evaluation()).toBe(100); // Large positive for mate
    }));

    it('should handle negative mate score', fakeAsync(() => {
      service.getBestMove();
      tick(100);

      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'info depth 3 score mate -1' });
        mockWorker.onmessage({ data: 'bestmove e7e6' });
      }

      tick(100);
      flush();

      expect(service.evaluation()).toBe(-100); // Large negative for getting mated
    }));
  });

  describe('newGame', () => {
    beforeEach(fakeAsync(() => {
      service.initialize();
      tick(100);
      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'uciok' });
        mockWorker.onmessage({ data: 'readyok' });
      }
      tick(100);
      flush();
    }));

    it('should send ucinewgame command', () => {
      service.newGame();
      expect(mockWorker.postMessage).toHaveBeenCalledWith('ucinewgame');
    });

    it('should send isready command after ucinewgame', () => {
      service.newGame();
      expect(mockWorker.postMessage).toHaveBeenCalledWith('isready');
    });

    it('should reset evaluation signal', () => {
      service.newGame();
      expect(service.evaluation()).toBeNull();
    });
  });

  describe('stopThinking', () => {
    beforeEach(fakeAsync(() => {
      service.initialize();
      tick(100);
      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'uciok' });
        mockWorker.onmessage({ data: 'readyok' });
      }
      tick(100);
      flush();
    }));

    it('should send stop command', () => {
      service.stopThinking();
      expect(mockWorker.postMessage).toHaveBeenCalledWith('stop');
    });
  });

  describe('dispose', () => {
    beforeEach(fakeAsync(() => {
      service.initialize();
      tick(100);
      if (mockWorker.onmessage) {
        mockWorker.onmessage({ data: 'uciok' });
        mockWorker.onmessage({ data: 'readyok' });
      }
      tick(100);
      flush();
    }));

    it('should send quit command', () => {
      service.dispose();
      expect(mockWorker.postMessage).toHaveBeenCalledWith('quit');
    });

    it('should terminate worker', () => {
      service.dispose();
      expect(mockWorker.terminate).toHaveBeenCalled();
    });

    it('should set engineReady to false', () => {
      service.dispose();
      expect(service.engineReady()).toBeFalse();
    });

    it('should set thinking to false', () => {
      service.dispose();
      expect(service.thinking()).toBeFalse();
    });
  });

  describe('Skill Level Mapping', () => {
    it('should have correct mapping for all difficulty levels', () => {
      const expectedMappings: Record<string, number> = {
        'DEBUTANT': 0,
        'FACILE': 5,
        'MOYEN': 10,
        'DIFFICILE': 15,
        'EXPERT': 20
      };

      // Verify service has these mappings
      expect(service['SKILL_LEVELS']).toEqual(expectedMappings);
    });
  });
});
