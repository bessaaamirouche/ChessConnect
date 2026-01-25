import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WalletService, WalletInfo, CreditTransaction, BookWithCreditResponse } from './wallet.service';
import { CheckoutSessionResponse } from './payment.service';

describe('WalletService', () => {
  let service: WalletService;
  let httpMock: HttpTestingController;

  const mockWallet: WalletInfo = {
    id: 1,
    balanceCents: 5000,
    totalTopUpsCents: 10000,
    totalUsedCents: 5000,
    totalRefundedCents: 0
  };

  const mockTransactions: CreditTransaction[] = [
    {
      id: 1,
      transactionType: 'TOPUP',
      amountCents: 5000,
      description: 'Recharge du portefeuille',
      createdAt: '2024-01-01T00:00:00'
    },
    {
      id: 2,
      transactionType: 'LESSON_PAYMENT',
      amountCents: 3000,
      description: 'Cours avec Coach Smith',
      lessonId: 5,
      createdAt: '2024-01-15T00:00:00'
    },
    {
      id: 3,
      transactionType: 'REFUND',
      amountCents: 3000,
      description: 'Remboursement cours annulé',
      lessonId: 5,
      createdAt: '2024-01-16T00:00:00'
    }
  ];

  const mockCheckoutSession: CheckoutSessionResponse = {
    sessionId: 'sess_test123',
    publishableKey: 'pk_test_123',
    clientSecret: 'cs_test_secret'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [WalletService]
    });

    service = TestBed.inject(WalletService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Initial State', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should have null wallet initially', () => {
      expect(service.wallet()).toBeNull();
    });

    it('should have empty transactions initially', () => {
      expect(service.transactions()).toEqual([]);
    });

    it('should have balance as 0 initially', () => {
      expect(service.balance()).toBe(0);
    });

    it('should have hasCredit as false initially', () => {
      expect(service.hasCredit()).toBeFalse();
    });

    it('should have loading as false initially', () => {
      expect(service.loading()).toBeFalse();
    });

    it('should have error as null initially', () => {
      expect(service.error()).toBeNull();
    });
  });

  describe('loadWallet', () => {
    it('should load wallet info', fakeAsync(() => {
      let result: WalletInfo | null = null;
      service.loadWallet().subscribe(w => result = w);

      expect(service.loading()).toBeTrue();

      const req = httpMock.expectOne('/api/wallet');
      expect(req.request.method).toBe('GET');
      req.flush(mockWallet);

      tick();

      expect(result).toEqual(mockWallet);
      expect(service.wallet()).toEqual(mockWallet);
      expect(service.balance()).toBe(5000);
      expect(service.hasCredit()).toBeTrue();
      expect(service.loading()).toBeFalse();
    }));

    it('should return empty wallet on error', fakeAsync(() => {
      let result: WalletInfo | null = null;
      service.loadWallet().subscribe(w => result = w);

      const req = httpMock.expectOne('/api/wallet');
      req.flush({}, { status: 404, statusText: 'Not Found' });

      tick();

      expect(result?.balanceCents).toBe(0);
      expect(service.balance()).toBe(0);
      expect(service.hasCredit()).toBeFalse();
    }));
  });

  describe('loadBalance', () => {
    it('should load balance and update wallet', fakeAsync(() => {
      // First set a wallet
      service.loadWallet().subscribe();
      httpMock.expectOne('/api/wallet').flush(mockWallet);
      tick();

      // Then load balance
      service.loadBalance().subscribe();

      const req = httpMock.expectOne('/api/wallet/balance');
      expect(req.request.method).toBe('GET');
      req.flush({ balanceCents: 7500 });

      tick();

      expect(service.balance()).toBe(7500);
    }));

    it('should create wallet if not exists when loading balance', fakeAsync(() => {
      service.loadBalance().subscribe();

      const req = httpMock.expectOne('/api/wallet/balance');
      req.flush({ balanceCents: 3000 });

      tick();

      expect(service.balance()).toBe(3000);
      expect(service.wallet()?.id).toBeNull();
    }));

    it('should return 0 on error', fakeAsync(() => {
      let result: any;
      service.loadBalance().subscribe(r => result = r);

      const req = httpMock.expectOne('/api/wallet/balance');
      req.flush({}, { status: 500, statusText: 'Error' });

      tick();

      expect(result.balanceCents).toBe(0);
    }));
  });

  describe('loadTransactions', () => {
    it('should load transactions', fakeAsync(() => {
      let result: CreditTransaction[] = [];
      service.loadTransactions().subscribe(t => result = t);

      const req = httpMock.expectOne('/api/wallet/transactions');
      expect(req.request.method).toBe('GET');
      req.flush(mockTransactions);

      tick();

      expect(result).toHaveLength(3);
      expect(service.transactions()).toEqual(mockTransactions);
    }));

    it('should return empty array when no transactions', fakeAsync(() => {
      let result: CreditTransaction[] = [];
      service.loadTransactions().subscribe(t => result = t);

      httpMock.expectOne('/api/wallet/transactions').flush([]);

      tick();

      expect(result).toEqual([]);
    }));
  });

  describe('createTopUpSession', () => {
    it('should create top-up checkout session', fakeAsync(() => {
      let result: CheckoutSessionResponse | null = null;
      service.createTopUpSession(2000).subscribe(r => result = r);

      expect(service.loading()).toBeTrue();

      const req = httpMock.expectOne('/api/wallet/topup');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ amountCents: 2000, embedded: true });
      req.flush(mockCheckoutSession);

      tick();

      expect(result).toEqual(mockCheckoutSession);
      expect(service.loading()).toBeFalse();
    }));

    it('should handle error when creating top-up session', fakeAsync(() => {
      let error: any;
      service.createTopUpSession(1000).subscribe({
        error: e => error = e
      });

      const req = httpMock.expectOne('/api/wallet/topup');
      req.flush({}, { status: 500, statusText: 'Server Error' });

      tick();

      expect(error).toBeTruthy();
      expect(service.loading()).toBeFalse();
      expect(service.error()).toBe('Erreur lors de la création de la session de paiement');
    }));
  });

  describe('confirmTopUp', () => {
    it('should confirm top-up and update balance', fakeAsync(() => {
      // First set a wallet
      service.loadWallet().subscribe();
      httpMock.expectOne('/api/wallet').flush(mockWallet);
      tick();

      let result: any;
      service.confirmTopUp('sess_test123').subscribe(r => result = r);

      const req = httpMock.expectOne('/api/wallet/topup/confirm?sessionId=sess_test123');
      expect(req.request.method).toBe('POST');
      req.flush({ success: true, balanceCents: 7000, message: 'Recharge effectuée' });

      tick();

      expect(result.success).toBeTrue();
      expect(service.balance()).toBe(7000);
    }));

    it('should handle failed confirmation', fakeAsync(() => {
      let result: any;
      service.confirmTopUp('sess_invalid').subscribe(r => result = r);

      const req = httpMock.expectOne('/api/wallet/topup/confirm?sessionId=sess_invalid');
      req.flush({ success: false, error: 'Session invalide' });

      tick();

      expect(result.success).toBeFalse();
    }));
  });

  describe('bookWithCredit', () => {
    const bookRequest = {
      teacherId: 1,
      scheduledAt: '2024-02-01T14:00:00',
      durationMinutes: 60,
      notes: 'First lesson'
    };

    it('should book lesson with credit and update balance', fakeAsync(() => {
      // First set a wallet
      service.loadWallet().subscribe();
      httpMock.expectOne('/api/wallet').flush(mockWallet);
      tick();

      let result: BookWithCreditResponse | null = null;
      service.bookWithCredit(bookRequest).subscribe(r => result = r);

      expect(service.loading()).toBeTrue();

      const req = httpMock.expectOne('/api/wallet/book-with-credit');
      expect(req.request.method).toBe('POST');
      req.flush({
        success: true,
        lessonId: 10,
        remainingBalanceCents: 2000,
        message: 'Cours réservé'
      });

      tick();

      expect(result?.success).toBeTrue();
      expect(result?.lessonId).toBe(10);
      expect(service.balance()).toBe(2000);
      expect(service.loading()).toBeFalse();
    }));

    it('should handle insufficient balance error', fakeAsync(() => {
      service.loadWallet().subscribe();
      httpMock.expectOne('/api/wallet').flush({ ...mockWallet, balanceCents: 1000 });
      tick();

      let result: BookWithCreditResponse | null = null;
      service.bookWithCredit(bookRequest).subscribe(r => result = r);

      const req = httpMock.expectOne('/api/wallet/book-with-credit');
      req.flush({
        success: false,
        error: 'Solde insuffisant'
      });

      tick();

      expect(result?.success).toBeFalse();
      expect(service.error()).toBe('Solde insuffisant');
    }));

    it('should handle HTTP error', fakeAsync(() => {
      let error: any;
      service.bookWithCredit(bookRequest).subscribe({
        error: e => error = e
      });

      const req = httpMock.expectOne('/api/wallet/book-with-credit');
      req.flush({ error: 'Service unavailable' }, { status: 503, statusText: 'Service Unavailable' });

      tick();

      expect(error).toBeTruthy();
      expect(service.loading()).toBeFalse();
    }));
  });

  describe('hasEnoughCredit', () => {
    it('should return true when balance is sufficient', fakeAsync(() => {
      service.loadWallet().subscribe();
      httpMock.expectOne('/api/wallet').flush(mockWallet);
      tick();

      expect(service.hasEnoughCredit(3000)).toBeTrue();
      expect(service.hasEnoughCredit(5000)).toBeTrue();
    }));

    it('should return false when balance is insufficient', fakeAsync(() => {
      service.loadWallet().subscribe();
      httpMock.expectOne('/api/wallet').flush(mockWallet);
      tick();

      expect(service.hasEnoughCredit(6000)).toBeFalse();
      expect(service.hasEnoughCredit(10000)).toBeFalse();
    }));

    it('should return false when no wallet', () => {
      expect(service.hasEnoughCredit(100)).toBeFalse();
    });
  });

  describe('formatPrice', () => {
    it('should format cents to euros correctly', () => {
      expect(service.formatPrice(5000)).toBe('50,00 €');
      expect(service.formatPrice(499)).toBe('4,99 €');
      expect(service.formatPrice(100)).toBe('1,00 €');
      expect(service.formatPrice(0)).toBe('0,00 €');
      expect(service.formatPrice(12345)).toBe('123,45 €');
    });
  });

  describe('clearError', () => {
    it('should clear error', fakeAsync(() => {
      // Cause an error first
      service.createTopUpSession(1000).subscribe({ error: () => {} });
      httpMock.expectOne('/api/wallet/topup').flush({}, { status: 500, statusText: 'Error' });
      tick();

      expect(service.error()).toBeTruthy();

      service.clearError();
      expect(service.error()).toBeNull();
    }));
  });

  describe('getTransactionTypeLabel', () => {
    it('should return correct labels for transaction types', () => {
      expect(service.getTransactionTypeLabel('TOPUP')).toBe('Recharge');
      expect(service.getTransactionTypeLabel('LESSON_PAYMENT')).toBe('Cours');
      expect(service.getTransactionTypeLabel('REFUND')).toBe('Remboursement');
      expect(service.getTransactionTypeLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('getTransactionSign', () => {
    it('should return correct signs for transaction types', () => {
      expect(service.getTransactionSign('TOPUP')).toBe('+');
      expect(service.getTransactionSign('LESSON_PAYMENT')).toBe('-');
      expect(service.getTransactionSign('REFUND')).toBe('+');
      expect(service.getTransactionSign('UNKNOWN')).toBe('');
    });
  });

  describe('Computed Signals Reactivity', () => {
    it('should update balance when wallet changes', fakeAsync(() => {
      expect(service.balance()).toBe(0);

      service.loadWallet().subscribe();
      httpMock.expectOne('/api/wallet').flush(mockWallet);
      tick();

      expect(service.balance()).toBe(5000);

      // Update balance
      service.loadBalance().subscribe();
      httpMock.expectOne('/api/wallet/balance').flush({ balanceCents: 8000 });
      tick();

      expect(service.balance()).toBe(8000);
    }));

    it('should update hasCredit when balance changes', fakeAsync(() => {
      expect(service.hasCredit()).toBeFalse();

      service.loadWallet().subscribe();
      httpMock.expectOne('/api/wallet').flush(mockWallet);
      tick();

      expect(service.hasCredit()).toBeTrue();

      // Set balance to 0
      service.loadBalance().subscribe();
      httpMock.expectOne('/api/wallet/balance').flush({ balanceCents: 0 });
      tick();

      expect(service.hasCredit()).toBeFalse();
    }));
  });

  describe('API Endpoint URLs', () => {
    it('should use correct URL for wallet', () => {
      service.loadWallet().subscribe();
      const req = httpMock.expectOne('/api/wallet');
      expect(req.request.url).toBe('/api/wallet');
      req.flush(mockWallet);
    });

    it('should use correct URL for balance', () => {
      service.loadBalance().subscribe();
      const req = httpMock.expectOne('/api/wallet/balance');
      expect(req.request.url).toBe('/api/wallet/balance');
      req.flush({ balanceCents: 0 });
    });

    it('should use correct URL for transactions', () => {
      service.loadTransactions().subscribe();
      const req = httpMock.expectOne('/api/wallet/transactions');
      expect(req.request.url).toBe('/api/wallet/transactions');
      req.flush([]);
    });

    it('should use correct URL for top-up', () => {
      service.createTopUpSession(1000).subscribe({ error: () => {} });
      const req = httpMock.expectOne('/api/wallet/topup');
      expect(req.request.url).toBe('/api/wallet/topup');
      req.flush(mockCheckoutSession);
    });

    it('should use correct URL for book-with-credit', () => {
      service.bookWithCredit({ teacherId: 1, scheduledAt: '2024-01-01', durationMinutes: 60 }).subscribe({ error: () => {} });
      const req = httpMock.expectOne('/api/wallet/book-with-credit');
      expect(req.request.url).toBe('/api/wallet/book-with-credit');
      req.flush({ success: true });
    });
  });
});
