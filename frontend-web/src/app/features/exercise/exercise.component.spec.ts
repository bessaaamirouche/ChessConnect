import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ExerciseComponent } from './exercise.component';
import { ExerciseService } from '../../core/services/exercise.service';
import { ChessEngineService, EngineEvaluation } from '../../core/services/chess-engine.service';
import { Exercise } from '../../core/models/exercise.model';

describe('ExerciseComponent', () => {
  let component: ExerciseComponent;
  let fixture: ComponentFixture<ExerciseComponent>;
  let exerciseServiceSpy: jasmine.SpyObj<ExerciseService>;
  let engineServiceSpy: jasmine.SpyObj<ChessEngineService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let activatedRouteMock: any;

  const mockExercise: Exercise = {
    id: 1,
    lessonId: 10,
    title: 'Test Exercise',
    description: 'Practice against myChessBot',
    startingFen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    playerColor: 'white',
    difficultyLevel: 'DEBUTANT',
    chessLevel: 'PION',
    thinkTimeMs: 1000
  };

  const mockBestMoveResult: EngineEvaluation = {
    bestMove: 'e7e5',
    evaluation: 0.3
  };

  beforeEach(async () => {
    exerciseServiceSpy = jasmine.createSpyObj('ExerciseService', [
      'loadExerciseForLesson',
      'clearExercise',
      'loading',
      'error',
      'currentExercise'
    ]);

    engineServiceSpy = jasmine.createSpyObj('ChessEngineService', [
      'initialize',
      'dispose',
      'setDifficulty',
      'newGame',
      'setPosition',
      'setPositionWithMoves',
      'getBestMove',
      'stopThinking',
      'engineReady',
      'thinking',
      'evaluation'
    ]);

    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    activatedRouteMock = {
      snapshot: {
        params: { lessonId: '10' }
      }
    };

    // Setup default mock returns
    exerciseServiceSpy.loadExerciseForLesson.and.returnValue(of(mockExercise));
    exerciseServiceSpy.loading.and.returnValue(false);
    exerciseServiceSpy.error.and.returnValue(null);
    exerciseServiceSpy.currentExercise.and.returnValue(mockExercise);

    engineServiceSpy.initialize.and.returnValue(Promise.resolve());
    engineServiceSpy.getBestMove.and.returnValue(Promise.resolve(mockBestMoveResult));
    engineServiceSpy.engineReady.and.returnValue(true);
    engineServiceSpy.thinking.and.returnValue(false);
    engineServiceSpy.evaluation.and.returnValue(0.5);

    await TestBed.configureTestingModule({
      imports: [ExerciseComponent],
      providers: [
        { provide: ExerciseService, useValue: exerciseServiceSpy },
        { provide: ChessEngineService, useValue: engineServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ExerciseComponent);
    component = fixture.componentInstance;
  });

  describe('Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have loading game status initially', () => {
      expect(component.gameStatus()).toBe('loading');
    });

    it('should have empty move history initially', () => {
      expect(component.moveHistory()).toEqual([]);
    });

    it('should have player turn as true initially', () => {
      expect(component.isPlayerTurn()).toBeTrue();
    });
  });

  describe('ngOnInit', () => {
    it('should navigate to lessons if no lessonId', fakeAsync(() => {
      activatedRouteMock.snapshot.params = {};

      component.ngOnInit();
      tick();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/lessons']);
    }));

    it('should initialize chess engine', fakeAsync(() => {
      component.ngOnInit();
      tick();

      expect(engineServiceSpy.initialize).toHaveBeenCalled();
    }));

    it('should load exercise for lesson', fakeAsync(() => {
      component.ngOnInit();
      tick();

      expect(exerciseServiceSpy.loadExerciseForLesson).toHaveBeenCalledWith(10);
    }));

    it('should set engine initialized signal after initialization', fakeAsync(() => {
      component.ngOnInit();
      tick();

      expect(component.engineInitialized()).toBeTrue();
    }));

    it('should navigate to lessons on error', fakeAsync(() => {
      exerciseServiceSpy.loadExerciseForLesson.and.returnValue(
        throwError(() => new Error('Exercise not found'))
      );

      component.ngOnInit();
      tick();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/lessons']);
    }));

    it('should handle engine initialization error gracefully', fakeAsync(() => {
      engineServiceSpy.initialize.and.returnValue(Promise.reject(new Error('Engine failed')));

      component.ngOnInit();
      tick();

      expect(component.engineInitialized()).toBeFalse();
    }));
  });

  describe('ngOnDestroy', () => {
    it('should dispose engine', () => {
      component.ngOnDestroy();

      expect(engineServiceSpy.dispose).toHaveBeenCalled();
    });

    it('should clear exercise', () => {
      component.ngOnDestroy();

      expect(exerciseServiceSpy.clearExercise).toHaveBeenCalled();
    });
  });

  describe('Game Setup', () => {
    it('should set game status to playing after setup', fakeAsync(() => {
      component.ngOnInit();
      tick();

      expect(component.gameStatus()).toBe('playing');
    }));

    it('should set difficulty from exercise', fakeAsync(() => {
      component.ngOnInit();
      tick();

      expect(engineServiceSpy.setDifficulty).toHaveBeenCalledWith('DEBUTANT');
    }));

    it('should call newGame on engine', fakeAsync(() => {
      component.ngOnInit();
      tick();

      expect(engineServiceSpy.newGame).toHaveBeenCalled();
    }));

    it('should set position from exercise FEN', fakeAsync(() => {
      component.ngOnInit();
      tick();

      expect(engineServiceSpy.setPosition).toHaveBeenCalledWith(mockExercise.startingFen);
    }));

    it('should set player turn for white', fakeAsync(() => {
      component.ngOnInit();
      tick();

      expect(component.isPlayerTurn()).toBeTrue();
    }));

    it('should make AI move first when player is black', fakeAsync(() => {
      const blackExercise = { ...mockExercise, playerColor: 'black' };
      exerciseServiceSpy.loadExerciseForLesson.and.returnValue(of(blackExercise));

      component.ngOnInit();
      tick(600); // Wait for AI move delay

      expect(component.isPlayerTurn()).toBeFalse();
      expect(engineServiceSpy.getBestMove).toHaveBeenCalled();
    }));
  });

  describe('Player Move', () => {
    beforeEach(fakeAsync(() => {
      component.ngOnInit();
      tick();
    }));

    it('should add move to history', fakeAsync(() => {
      component.onPlayerMove({ from: 'e2', to: 'e4' });
      tick(400);

      expect(component.moveHistory()).toContain('e2e4');
    }));

    it('should include promotion in move', fakeAsync(() => {
      component.onPlayerMove({ from: 'e7', to: 'e8', promotion: 'q' });
      tick(400);

      expect(component.moveHistory()).toContain('e7e8q');
    }));

    it('should update engine position with moves', fakeAsync(() => {
      component.onPlayerMove({ from: 'e2', to: 'e4' });
      tick(400);

      expect(engineServiceSpy.setPositionWithMoves).toHaveBeenCalledWith(
        mockExercise.startingFen,
        jasmine.arrayContaining(['e2e4'])
      );
    }));

    it('should set player turn to false after move', fakeAsync(() => {
      component.onPlayerMove({ from: 'e2', to: 'e4' });
      tick();

      expect(component.isPlayerTurn()).toBeFalse();
    }));
  });

  describe('Reset Game', () => {
    beforeEach(fakeAsync(() => {
      component.ngOnInit();
      tick();
      component.onPlayerMove({ from: 'e2', to: 'e4' });
      tick(400);
    }));

    it('should reset game status to playing', () => {
      component.gameStatus.set('ai_won');
      component.onReset();

      expect(component.gameStatus()).toBe('playing');
    });

    it('should clear move history', () => {
      component.onReset();

      expect(component.moveHistory()).toEqual([]);
    });

    it('should call newGame on engine', () => {
      component.onReset();

      expect(engineServiceSpy.newGame).toHaveBeenCalled();
    });

    it('should reset position on engine', () => {
      component.onReset();

      expect(engineServiceSpy.setPosition).toHaveBeenCalledWith(mockExercise.startingFen);
    });

    it('should set player turn for white exercise', () => {
      component.onReset();

      expect(component.isPlayerTurn()).toBeTrue();
    });
  });

  describe('Resign', () => {
    beforeEach(fakeAsync(() => {
      component.ngOnInit();
      tick();
    }));

    it('should set game status to ai_won', () => {
      component.onResign();

      expect(component.gameStatus()).toBe('ai_won');
    });
  });

  describe('Go Back', () => {
    it('should navigate to lessons', () => {
      component.goBack();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/lessons']);
    });
  });

  describe('Move History Formatting', () => {
    it('should format empty history', () => {
      const formatted = component.formatMoveHistory();

      expect(formatted).toEqual([]);
    });

    it('should format single move', () => {
      component.moveHistory.set(['e2e4']);

      const formatted = component.formatMoveHistory();

      expect(formatted).toEqual([{ moveNumber: 1, white: 'e2e4', black: undefined }]);
    });

    it('should format complete move pair', () => {
      component.moveHistory.set(['e2e4', 'e7e5']);

      const formatted = component.formatMoveHistory();

      expect(formatted).toEqual([{ moveNumber: 1, white: 'e2e4', black: 'e7e5' }]);
    });

    it('should format multiple moves', () => {
      component.moveHistory.set(['e2e4', 'e7e5', 'g1f3', 'b8c6']);

      const formatted = component.formatMoveHistory();

      expect(formatted).toHaveLength(2);
      expect(formatted[0]).toEqual({ moveNumber: 1, white: 'e2e4', black: 'e7e5' });
      expect(formatted[1]).toEqual({ moveNumber: 2, white: 'g1f3', black: 'b8c6' });
    });
  });

  describe('Evaluation Bar', () => {
    it('should return 50% for null evaluation', () => {
      engineServiceSpy.evaluation.and.returnValue(null);

      expect(component.getEvalBarWidth()).toBe(50);
    });

    it('should return 50% for 0 evaluation', () => {
      engineServiceSpy.evaluation.and.returnValue(0);

      expect(component.getEvalBarWidth()).toBe(50);
    });

    it('should return higher percentage for positive evaluation', () => {
      engineServiceSpy.evaluation.and.returnValue(2);

      expect(component.getEvalBarWidth()).toBe(60);
    });

    it('should return lower percentage for negative evaluation', () => {
      engineServiceSpy.evaluation.and.returnValue(-2);

      expect(component.getEvalBarWidth()).toBe(40);
    });

    it('should clamp at 100% for very high evaluation', () => {
      engineServiceSpy.evaluation.and.returnValue(20);

      expect(component.getEvalBarWidth()).toBe(100);
    });

    it('should clamp at 0% for very low evaluation', () => {
      engineServiceSpy.evaluation.and.returnValue(-20);

      expect(component.getEvalBarWidth()).toBe(0);
    });
  });

  describe('Format Evaluation', () => {
    it('should format null evaluation as 0.0', () => {
      engineServiceSpy.evaluation.and.returnValue(null);

      expect(component.formatEvaluation()).toBe('0.0');
    });

    it('should format positive evaluation with +', () => {
      engineServiceSpy.evaluation.and.returnValue(1.5);

      expect(component.formatEvaluation()).toBe('+1.5');
    });

    it('should format negative evaluation without +', () => {
      engineServiceSpy.evaluation.and.returnValue(-2.3);

      expect(component.formatEvaluation()).toBe('-2.3');
    });

    it('should show M+ for large positive (mate)', () => {
      engineServiceSpy.evaluation.and.returnValue(100);

      expect(component.formatEvaluation()).toBe('M+');
    });

    it('should show M- for large negative (getting mated)', () => {
      engineServiceSpy.evaluation.and.returnValue(-100);

      expect(component.formatEvaluation()).toBe('M-');
    });
  });

  describe('Move Count', () => {
    it('should return 0 for empty history', () => {
      expect(component.moveCount()).toBe(0);
    });

    it('should return 1 for first move', () => {
      component.moveHistory.set(['e2e4']);

      expect(component.moveCount()).toBe(1);
    });

    it('should return 1 for complete first move pair', () => {
      component.moveHistory.set(['e2e4', 'e7e5']);

      expect(component.moveCount()).toBe(1);
    });

    it('should return 2 for second move started', () => {
      component.moveHistory.set(['e2e4', 'e7e5', 'g1f3']);

      expect(component.moveCount()).toBe(2);
    });
  });

  describe('Difficulty Labels', () => {
    it('should have difficulty labels available', () => {
      expect(component.difficultyLabels).toBeDefined();
      expect(component.difficultyLabels['DEBUTANT']).toBeDefined();
    });
  });
});
