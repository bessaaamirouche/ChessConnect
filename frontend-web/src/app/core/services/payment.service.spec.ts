import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PaymentService, CheckoutSessionResponse, SubscriptionPlanResponseDto, Payment } from './payment.service';
import { Subscription } from '../models/subscription.model';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  const mockPlans: SubscriptionPlanResponseDto[] = [
    {
      code: 'PREMIUM',
      name: 'Premium',
      priceCents: 499,
      features: ['Revisionnage des cours', 'Badge Premium', 'Notifications prioritaires'],
      popular: true
    }
  ];

  const mockSubscription: Subscription = {
    id: 1,
    userId: 1,
    plan: 'PREMIUM',
    status: 'ACTIVE',
    startDate: '2024-01-01T00:00:00',
    currentPeriodEnd: '2024-02-01T00:00:00'
  };

  const mockCheckoutSession: CheckoutSessionResponse = {
    sessionId: 'sess_test123',
    publishableKey: 'pk_test_123',
    clientSecret: 'cs_test_secret'
  };

  const mockPaymentHistory: Payment[] = [
    {
      id: 1,
      payerId: 1,
      payerName: 'John Doe',
      paymentType: 'SUBSCRIPTION',
      amountCents: 499,
      commissionCents: 0,
      status: 'COMPLETED',
      createdAt: '2024-01-01T00:00:00'
    },
    {
      id: 2,
      payerId: 1,
      payerName: 'John Doe',
      teacherId: 2,
      teacherName: 'Coach Smith',
      lessonId: 5,
      paymentType: 'ONE_TIME_LESSON',
      amountCents: 5000,
      commissionCents: 750,
      teacherPayoutCents: 4250,
      status: 'COMPLETED',
      createdAt: '2024-01-15T00:00:00'
    }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PaymentService]
    });

    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Initial State', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should have empty plans initially', () => {
      expect(service.plans()).toEqual([]);
    });

    it('should have null activeSubscription initially', () => {
      expect(service.activeSubscription()).toBeNull();
    });

    it('should have hasActiveSubscription return false initially', () => {
      expect(service.hasActiveSubscription()).toBeFalse();
    });

    it('should have loading as false initially', () => {
      expect(service.loading()).toBeFalse();
    });

    it('should have error as null initially', () => {
      expect(service.error()).toBeNull();
    });
  });

  describe('loadPlans', () => {
    it('should load subscription plans', fakeAsync(() => {
      let result: SubscriptionPlanResponseDto[] = [];
      service.loadPlans().subscribe(plans => result = plans);

      const req = httpMock.expectOne('/api/payments/plans');
      expect(req.request.method).toBe('GET');
      req.flush(mockPlans);

      tick();

      expect(result).toEqual(mockPlans);
      expect(service.plans()).toEqual(mockPlans);
    }));

    it('should return empty array when no plans', fakeAsync(() => {
      let result: SubscriptionPlanResponseDto[] = [];
      service.loadPlans().subscribe(plans => result = plans);

      httpMock.expectOne('/api/payments/plans').flush([]);

      tick();

      expect(result).toEqual([]);
    }));
  });

  describe('getStripeConfig', () => {
    it('should return Stripe publishable key', fakeAsync(() => {
      let config: any;
      service.getStripeConfig().subscribe(c => config = c);

      const req = httpMock.expectOne('/api/payments/config');
      expect(req.request.method).toBe('GET');
      req.flush({ publishableKey: 'pk_test_123' });

      tick();

      expect(config.publishableKey).toBe('pk_test_123');
    }));
  });

  describe('createSubscriptionCheckout', () => {
    it('should create subscription checkout session', fakeAsync(() => {
      let result: CheckoutSessionResponse | null = null;
      service.createSubscriptionCheckout('PREMIUM').subscribe(r => result = r);

      expect(service.loading()).toBeTrue();

      const req = httpMock.expectOne('/api/payments/checkout/subscription');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ plan: 'PREMIUM', embedded: true });
      req.flush(mockCheckoutSession);

      tick();

      expect(result).toEqual(mockCheckoutSession);
      expect(service.loading()).toBeFalse();
      expect(service.error()).toBeNull();
    }));

    it('should handle error when creating checkout session', fakeAsync(() => {
      let error: any;
      service.createSubscriptionCheckout('PREMIUM').subscribe({
        error: e => error = e
      });

      const req = httpMock.expectOne('/api/payments/checkout/subscription');
      req.flush({}, { status: 500, statusText: 'Server Error' });

      tick();

      expect(error).toBeTruthy();
      expect(service.loading()).toBeFalse();
      expect(service.error()).toBe('Erreur lors de la création de la session de paiement');
    }));
  });

  describe('createLessonCheckout', () => {
    const lessonRequest = {
      teacherId: 1,
      scheduledAt: '2024-02-01T14:00:00',
      durationMinutes: 60,
      notes: 'First lesson'
    };

    it('should create lesson checkout session', fakeAsync(() => {
      let result: CheckoutSessionResponse | null = null;
      service.createLessonCheckout(lessonRequest).subscribe(r => result = r);

      expect(service.loading()).toBeTrue();

      const req = httpMock.expectOne('/api/payments/checkout/lesson');
      expect(req.request.method).toBe('POST');
      req.flush(mockCheckoutSession);

      tick();

      expect(result).toEqual(mockCheckoutSession);
      expect(service.loading()).toBeFalse();
    }));

    it('should handle error when creating lesson checkout', fakeAsync(() => {
      let error: any;
      service.createLessonCheckout(lessonRequest).subscribe({
        error: e => error = e
      });

      const req = httpMock.expectOne('/api/payments/checkout/lesson');
      req.flush({}, { status: 400, statusText: 'Bad Request' });

      tick();

      expect(service.error()).toBe('Erreur lors de la création de la session de paiement');
    }));
  });

  describe('loadActiveSubscription', () => {
    it('should load active subscription', fakeAsync(() => {
      service.loadActiveSubscription().subscribe();

      const req = httpMock.expectOne('/api/payments/subscription');
      expect(req.request.method).toBe('GET');
      req.flush(mockSubscription);

      tick();

      expect(service.activeSubscription()).toEqual(mockSubscription);
      expect(service.hasActiveSubscription()).toBeTrue();
      expect(service.loading()).toBeFalse();
    }));

    it('should handle 204 No Content (no subscription)', fakeAsync(() => {
      service.loadActiveSubscription().subscribe();

      const req = httpMock.expectOne('/api/payments/subscription');
      req.flush(null, { status: 204, statusText: 'No Content' });

      tick();

      expect(service.activeSubscription()).toBeNull();
      expect(service.hasActiveSubscription()).toBeFalse();
    }));
  });

  describe('loadPaymentHistory', () => {
    it('should load payment history', fakeAsync(() => {
      let result: Payment[] = [];
      service.loadPaymentHistory().subscribe(payments => result = payments);

      const req = httpMock.expectOne('/api/payments/history');
      expect(req.request.method).toBe('GET');
      req.flush(mockPaymentHistory);

      tick();

      expect(result).toHaveLength(2);
      expect(service.paymentHistory()).toEqual(mockPaymentHistory);
    }));
  });

  describe('cancelSubscription', () => {
    it('should cancel subscription', fakeAsync(() => {
      const cancelledSubscription = { ...mockSubscription, status: 'CANCELLED' as const };
      let result: Subscription | null = null;

      service.cancelSubscription().subscribe(s => result = s);

      expect(service.loading()).toBeTrue();

      const req = httpMock.expectOne('/api/payments/subscription/cancel');
      expect(req.request.method).toBe('POST');
      req.flush(cancelledSubscription);

      tick();

      expect(result?.status).toBe('CANCELLED');
      expect(service.activeSubscription()?.status).toBe('CANCELLED');
      expect(service.loading()).toBeFalse();
    }));

    it('should handle error when cancelling', fakeAsync(() => {
      let error: any;
      service.cancelSubscription().subscribe({
        error: e => error = e
      });

      const req = httpMock.expectOne('/api/payments/subscription/cancel');
      req.flush({}, { status: 500, statusText: 'Server Error' });

      tick();

      expect(service.error()).toBe("Erreur lors de l'annulation de l'abonnement");
    }));
  });

  describe('verifyCheckoutSession', () => {
    it('should verify checkout session status', fakeAsync(() => {
      let result: any;
      service.verifyCheckoutSession('sess_test123').subscribe(r => result = r);

      const req = httpMock.expectOne('/api/payments/checkout/verify?sessionId=sess_test123');
      expect(req.request.method).toBe('GET');
      req.flush({ status: 'complete', paymentStatus: 'paid' });

      tick();

      expect(result.status).toBe('complete');
      expect(result.paymentStatus).toBe('paid');
    }));
  });

  describe('confirmLessonPayment', () => {
    it('should confirm lesson payment', fakeAsync(() => {
      let result: any;
      service.confirmLessonPayment('sess_test123').subscribe(r => result = r);

      const req = httpMock.expectOne('/api/payments/checkout/lesson/confirm?sessionId=sess_test123');
      expect(req.request.method).toBe('POST');
      req.flush({ success: true, lessonId: 5 });

      tick();

      expect(result.success).toBeTrue();
      expect(result.lessonId).toBe(5);
    }));
  });

  describe('confirmSubscriptionPayment', () => {
    it('should confirm subscription payment and reload subscription', fakeAsync(() => {
      let result: any;
      service.confirmSubscriptionPayment('sess_test123').subscribe(r => result = r);

      const confirmReq = httpMock.expectOne('/api/payments/checkout/subscription/confirm?sessionId=sess_test123');
      expect(confirmReq.request.method).toBe('POST');
      confirmReq.flush({ success: true, subscriptionId: 1, planName: 'Premium' });

      tick();

      // Should trigger loadActiveSubscription
      const subscriptionReq = httpMock.expectOne('/api/payments/subscription');
      subscriptionReq.flush(mockSubscription);

      tick();

      expect(result.success).toBeTrue();
      expect(result.planName).toBe('Premium');
    }));
  });

  describe('formatPrice', () => {
    it('should format cents to euros', () => {
      expect(service.formatPrice(499)).toBe('4,99 €');
      expect(service.formatPrice(5000)).toBe('50,00 €');
      expect(service.formatPrice(100)).toBe('1,00 €');
      expect(service.formatPrice(0)).toBe('0,00 €');
    });
  });

  describe('clearError', () => {
    it('should clear error', fakeAsync(() => {
      // First cause an error
      service.createSubscriptionCheckout('PREMIUM').subscribe({ error: () => {} });
      httpMock.expectOne('/api/payments/checkout/subscription').flush({}, { status: 500, statusText: 'Error' });
      tick();

      expect(service.error()).toBeTruthy();

      // Then clear it
      service.clearError();
      expect(service.error()).toBeNull();
    }));
  });

  describe('API Endpoint URLs', () => {
    it('should use correct URL for plans', () => {
      service.loadPlans().subscribe();
      const req = httpMock.expectOne('/api/payments/plans');
      expect(req.request.url).toBe('/api/payments/plans');
      req.flush([]);
    });

    it('should use correct URL for config', () => {
      service.getStripeConfig().subscribe();
      const req = httpMock.expectOne('/api/payments/config');
      expect(req.request.url).toBe('/api/payments/config');
      req.flush({ publishableKey: 'pk_test' });
    });

    it('should use correct URL for subscription checkout', () => {
      service.createSubscriptionCheckout('PREMIUM').subscribe({ error: () => {} });
      const req = httpMock.expectOne('/api/payments/checkout/subscription');
      expect(req.request.url).toBe('/api/payments/checkout/subscription');
      req.flush(mockCheckoutSession);
    });

    it('should use correct URL for lesson checkout', () => {
      service.createLessonCheckout({ teacherId: 1, scheduledAt: '2024-01-01' }).subscribe({ error: () => {} });
      const req = httpMock.expectOne('/api/payments/checkout/lesson');
      expect(req.request.url).toBe('/api/payments/checkout/lesson');
      req.flush(mockCheckoutSession);
    });
  });
});
