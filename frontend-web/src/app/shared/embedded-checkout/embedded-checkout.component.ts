import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, signal, PLATFORM_ID, Inject, ElementRef, ViewChild, ViewEncapsulation } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroXMark, heroCreditCard } from '@ng-icons/heroicons/outline';
import { StripeService } from '../../core/services/stripe.service';
import { StripeEmbeddedCheckout } from '@stripe/stripe-js';

@Component({
  selector: 'app-embedded-checkout',
  standalone: true,
  imports: [NgIconComponent],
  viewProviders: [provideIcons({ heroXMark, heroCreditCard })],
  encapsulation: ViewEncapsulation.None,
  template: `
    <div class="checkout-overlay" (click)="onClose()">
      <div class="checkout-container" (click)="$event.stopPropagation()">
        <div class="checkout-header">
          <div class="checkout-header__info">
            <ng-icon name="heroCreditCard" size="20"></ng-icon>
            <span>{{ title }}</span>
          </div>
          <button class="checkout-header__close" (click)="onClose()">
            <ng-icon name="heroXMark" size="24"></ng-icon>
          </button>
        </div>
        <div class="checkout-content">
          @if (loading()) {
            <div class="checkout-loading">
              <span class="spinner spinner--lg"></span>
              <p>Chargement du formulaire de paiement...</p>
            </div>
          }
          @if (error()) {
            <div class="checkout-error">
              <p>{{ error() }}</p>
              <button class="btn btn--primary" (click)="onClose()">Fermer</button>
            </div>
          }
          <div #checkoutContainer id="checkout-container"></div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    app-embedded-checkout .checkout-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.9);
      z-index: 1000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
      color-scheme: dark;
    }

    app-embedded-checkout .checkout-container {
      width: 100%;
      max-width: 500px;
      background: #1a1a1f;
      border-radius: 12px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      border: 1px solid rgba(255, 255, 255, 0.1);
      color-scheme: dark;
    }

    app-embedded-checkout .checkout-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 1.5rem;
      background: #25252b;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    app-embedded-checkout .checkout-header__info {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      color: #fff;
      font-weight: 600;
    }

    app-embedded-checkout .checkout-header__close {
      background: none;
      border: none;
      color: #888;
      cursor: pointer;
      padding: 0.5rem;
      border-radius: 8px;
      transition: all 0.15s ease;
    }

    app-embedded-checkout .checkout-header__close:hover {
      background: rgba(255, 255, 255, 0.1);
      color: #fff;
    }

    app-embedded-checkout .checkout-content {
      color-scheme: dark;
      background: #1a1a1f;
    }

    app-embedded-checkout .checkout-loading,
    app-embedded-checkout .checkout-error {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 300px;
      text-align: center;
      color: #888;
      padding: 1.5rem;
    }

    app-embedded-checkout .checkout-loading p,
    app-embedded-checkout .checkout-error p {
      margin-top: 1rem;
    }

    app-embedded-checkout .checkout-error {
      color: #f87171;
    }

    app-embedded-checkout .checkout-error button {
      margin-top: 1rem;
    }

    app-embedded-checkout #checkout-container {
      color-scheme: dark;
    }

    /* Apply dark mode filter to Stripe iframe */
    app-embedded-checkout #checkout-container iframe {
      filter: invert(0.88) hue-rotate(180deg);
      border-radius: 8px;
    }
  `]
})
export class EmbeddedCheckoutComponent implements OnInit, OnDestroy {
  @ViewChild('checkoutContainer', { static: true }) checkoutContainer!: ElementRef;

  @Input() clientSecret!: string;
  @Input() title = 'Paiement';
  @Output() closed = new EventEmitter<void>();
  @Output() completed = new EventEmitter<void>();

  loading = signal(true);
  error = signal<string | null>(null);

  private checkout: StripeEmbeddedCheckout | null = null;
  private isBrowser: boolean;

  constructor(
    private stripeService: StripeService,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  async ngOnInit(): Promise<void> {
    if (this.isBrowser && this.clientSecret) {
      await this.initCheckout();
    }
  }

  ngOnDestroy(): void {
    if (this.checkout) {
      this.checkout.destroy();
    }
  }

  private async initCheckout(): Promise<void> {
    try {
      const stripe = await this.stripeService.getStripe();

      if (!stripe) {
        this.error.set('Impossible de charger Stripe');
        this.loading.set(false);
        return;
      }

      this.checkout = await stripe.initEmbeddedCheckout({
        clientSecret: this.clientSecret,
        onComplete: () => {
          this.completed.emit();
        }
      });

      this.checkout.mount('#checkout-container');
      this.loading.set(false);
    } catch (err) {
      console.error('Error initializing checkout:', err);
      this.error.set('Erreur lors du chargement du formulaire de paiement');
      this.loading.set(false);
    }
  }

  onClose(): void {
    if (this.checkout) {
      this.checkout.destroy();
    }
    this.closed.emit();
  }
}
