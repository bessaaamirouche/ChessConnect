import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ExerciseService } from './exercise.service';
import { Exercise } from '../models/exercise.model';

describe('ExerciseService', () => {
  let service: ExerciseService;
  let httpMock: HttpTestingController;

  const mockExercise: Exercise = {
    id: 1,
    lessonId: 1,
    title: 'Test Exercise',
    description: 'Practice with myChessBot',
    startingFen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    playerColor: 'white',
    difficultyLevel: 'DEBUTANT',
    chessLevel: 'PION'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ExerciseService]
    });

    service = TestBed.inject(ExerciseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Initial State', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should have null currentExercise initially', () => {
      expect(service.currentExercise()).toBeNull();
    });

    it('should have loading as false initially', () => {
      expect(service.loading()).toBeFalse();
    });

    it('should have error as null initially', () => {
      expect(service.error()).toBeNull();
    });
  });

  describe('loadExerciseForLesson', () => {
    it('should load exercise for a lesson', fakeAsync(() => {
      service.loadExerciseForLesson(1);

      const req = httpMock.expectOne('/api/exercises/lesson/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockExercise);

      tick();

      expect(service.currentExercise()).toEqual(mockExercise);
      expect(service.loading()).toBeFalse();
      expect(service.error()).toBeNull();
    }));

    it('should set loading to true while fetching', () => {
      service.loadExerciseForLesson(1);

      expect(service.loading()).toBeTrue();

      httpMock.expectOne('/api/exercises/lesson/1').flush(mockExercise);
    });

    it('should handle error response', fakeAsync(() => {
      service.loadExerciseForLesson(1);

      const req = httpMock.expectOne('/api/exercises/lesson/1');
      req.flush('Error', { status: 500, statusText: 'Server Error' });

      tick();

      expect(service.currentExercise()).toBeNull();
      expect(service.loading()).toBeFalse();
      expect(service.error()).toBeTruthy();
    }));

    it('should handle premium required error', fakeAsync(() => {
      service.loadExerciseForLesson(1);

      const req = httpMock.expectOne('/api/exercises/lesson/1');
      req.flush({ message: 'Premium subscription required' }, { status: 403, statusText: 'Forbidden' });

      tick();

      expect(service.error()).toContain('Premium');
    }));
  });

  describe('loadExerciseById', () => {
    it('should load exercise by ID', fakeAsync(() => {
      service.loadExerciseById(1);

      const req = httpMock.expectOne('/api/exercises/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockExercise);

      tick();

      expect(service.currentExercise()).toEqual(mockExercise);
    }));

    it('should handle not found error', fakeAsync(() => {
      service.loadExerciseById(999);

      const req = httpMock.expectOne('/api/exercises/999');
      req.flush({ message: 'Exercise not found' }, { status: 404, statusText: 'Not Found' });

      tick();

      expect(service.currentExercise()).toBeNull();
      expect(service.error()).toBeTruthy();
    }));
  });

  describe('loadAllExercises', () => {
    const mockExercises: Exercise[] = [
      mockExercise,
      {
        id: 2,
        lessonId: 2,
        title: 'Second Exercise',
        description: 'Another exercise',
        startingFen: 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1',
        playerColor: 'black',
        difficultyLevel: 'FACILE',
        chessLevel: 'CAVALIER'
      }
    ];

    it('should load all exercises', fakeAsync(() => {
      let exercises: Exercise[] = [];
      service.loadAllExercises().subscribe(result => exercises = result);

      const req = httpMock.expectOne('/api/exercises');
      expect(req.request.method).toBe('GET');
      req.flush(mockExercises);

      tick();

      expect(exercises).toHaveLength(2);
      expect(exercises[0].id).toBe(1);
      expect(exercises[1].id).toBe(2);
    }));

    it('should return empty array when no exercises', fakeAsync(() => {
      let exercises: Exercise[] = [];
      service.loadAllExercises().subscribe(result => exercises = result);

      const req = httpMock.expectOne('/api/exercises');
      req.flush([]);

      tick();

      expect(exercises).toEqual([]);
    }));
  });

  describe('clearExercise', () => {
    it('should clear current exercise', fakeAsync(() => {
      // First load an exercise
      service.loadExerciseForLesson(1);
      httpMock.expectOne('/api/exercises/lesson/1').flush(mockExercise);
      tick();

      expect(service.currentExercise()).toBeTruthy();

      // Then clear it
      service.clearExercise();

      expect(service.currentExercise()).toBeNull();
      expect(service.error()).toBeNull();
    }));
  });

  describe('Exercise Data Validation', () => {
    it('should have valid FEN in response', fakeAsync(() => {
      service.loadExerciseForLesson(1);
      httpMock.expectOne('/api/exercises/lesson/1').flush(mockExercise);
      tick();

      const exercise = service.currentExercise();
      expect(exercise?.startingFen).toMatch(/^[rnbqkpRNBQKP1-8\/]+ [wb] [KQkq-]+ [a-h1-8-]+ \d+ \d+$/);
    }));

    it('should have valid player color', fakeAsync(() => {
      service.loadExerciseForLesson(1);
      httpMock.expectOne('/api/exercises/lesson/1').flush(mockExercise);
      tick();

      const exercise = service.currentExercise();
      expect(['white', 'black']).toContain(exercise?.playerColor);
    }));

    it('should have valid difficulty level', fakeAsync(() => {
      service.loadExerciseForLesson(1);
      httpMock.expectOne('/api/exercises/lesson/1').flush(mockExercise);
      tick();

      const exercise = service.currentExercise();
      expect(['DEBUTANT', 'FACILE', 'MOYEN', 'DIFFICILE', 'EXPERT']).toContain(exercise?.difficultyLevel);
    }));

    it('description should contain myChessBot, not l\'IA', fakeAsync(() => {
      service.loadExerciseForLesson(1);
      httpMock.expectOne('/api/exercises/lesson/1').flush(mockExercise);
      tick();

      const exercise = service.currentExercise();
      expect(exercise?.description).toContain('myChessBot');
      expect(exercise?.description).not.toContain('l\'IA');
    }));
  });

  describe('Error Handling', () => {
    it('should handle network error', fakeAsync(() => {
      service.loadExerciseForLesson(1);

      const req = httpMock.expectOne('/api/exercises/lesson/1');
      req.error(new ErrorEvent('Network error'));

      tick();

      expect(service.error()).toBeTruthy();
      expect(service.loading()).toBeFalse();
    }));

    it('should handle 401 unauthorized', fakeAsync(() => {
      service.loadExerciseForLesson(1);

      const req = httpMock.expectOne('/api/exercises/lesson/1');
      req.flush({}, { status: 401, statusText: 'Unauthorized' });

      tick();

      expect(service.error()).toBeTruthy();
    }));

    it('should handle 403 forbidden', fakeAsync(() => {
      service.loadExerciseForLesson(1);

      const req = httpMock.expectOne('/api/exercises/lesson/1');
      req.flush({}, { status: 403, statusText: 'Forbidden' });

      tick();

      expect(service.error()).toBeTruthy();
    }));
  });

  describe('API Endpoint URLs', () => {
    it('should use correct URL for lesson exercise', () => {
      service.loadExerciseForLesson(42);
      const req = httpMock.expectOne('/api/exercises/lesson/42');
      expect(req.request.url).toBe('/api/exercises/lesson/42');
      req.flush(mockExercise);
    });

    it('should use correct URL for exercise by ID', () => {
      service.loadExerciseById(123);
      const req = httpMock.expectOne('/api/exercises/123');
      expect(req.request.url).toBe('/api/exercises/123');
      req.flush(mockExercise);
    });

    it('should use correct URL for all exercises', () => {
      service.loadAllExercises().subscribe();
      const req = httpMock.expectOne('/api/exercises');
      expect(req.request.url).toBe('/api/exercises');
      req.flush([]);
    });
  });
});
