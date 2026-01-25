import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { WalletService, CreditTransaction } from '../../core/services/wallet.service';
import { AuthService } from '../../core/services/auth.service';
import { PaymentService } from '../../core/services/payment.service';
import { EmbeddedCheckoutComponent } from '../../shared/embedded-checkout/embedded-checkout.component';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroWallet,
  heroArrowUpCircle,
  heroArrowDownCircle,
  heroPlusCircle,
  heroCheck,
  heroArrowPath,
  heroBanknotes,
  heroCalendarDays,
  heroReceiptRefund
} from '@ng-icons/heroicons/outline';

interface TopUpOption {
  amountCents: number;
  label: string;
  popular?: boolean;
}

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [RouterLink, DatePipe, NgIconComponent, EmbeddedCheckoutComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroWallet,
    heroArrowUpCircle,
    heroArrowDownCircle,
    heroPlusCircle,
    heroCheck,
    heroArrowPath,
    heroBanknotes,
    heroCalendarDays,
    heroReceiptRefund
  })],
  templateUrl: './wallet.component.html',
  styleUrl: './wallet.component.scss'
})
export class WalletComponent implements OnInit {
  showTopUpModal = signal(false);
  selectedAmount = signal<number | null>(null);
  customAmount = signal<string>('');
  processing = signal(false);

  // Embedded checkout
  showCheckout = signal(false);
  checkoutClientSecret = signal<string | null>(null);
  checkoutSessionId = signal<string | null>(null);

  topUpOptions: TopUpOption[] = [
    { amountCents: 5000, label: '50 €' },
    { amountCents: 10000, label: '100 €', popular: true },
    { amountCents: 20000, label: '200 €' }
  ];

  constructor(
    public walletService: WalletService,
    public authService: AuthService,
    private paymentService: PaymentService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.walletService.loadWallet().subscribe();
    this.walletService.loadTransactions().subscribe();

    // Check for top-up result
    this.route.queryParams.subscribe(params => {
      if (params['topup'] === 'success' && params['session_id']) {
        this.confirmTopUp(params['session_id']);
      }
    });
  }

  openTopUpModal(): void {
    this.showTopUpModal.set(true);
    this.selectedAmount.set(null);
    this.customAmount.set('');
  }

  closeTopUpModal(): void {
    this.showTopUpModal.set(false);
    this.selectedAmount.set(null);
    this.customAmount.set('');
  }

  selectAmount(amountCents: number): void {
    this.selectedAmount.set(amountCents);
    this.customAmount.set('');
  }

  onCustomAmountChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.customAmount.set(input.value);
    this.selectedAmount.set(null);
  }

  getSelectedAmountCents(): number {
    if (this.selectedAmount()) {
      return this.selectedAmount()!;
    }
    const custom = parseFloat(this.customAmount().replace(',', '.'));
    return isNaN(custom) ? 0 : Math.round(custom * 100);
  }

  isValidAmount(): boolean {
    const amount = this.getSelectedAmountCents();
    return amount >= 500; // Minimum 5€
  }

  processTopUp(): void {
    if (!this.isValidAmount()) return;

    this.processing.set(true);
    const amountCents = this.getSelectedAmountCents();

    this.walletService.createTopUpSession(amountCents, true).subscribe({
      next: (response) => {
        if (response.clientSecret) {
          this.closeTopUpModal();
          this.checkoutClientSecret.set(response.clientSecret);
          this.checkoutSessionId.set(response.sessionId);
          this.showCheckout.set(true);
        } else if (response.url) {
          window.location.href = response.url;
        }
        this.processing.set(false);
      },
      error: () => {
        this.processing.set(false);
      }
    });
  }

  closeCheckout(): void {
    this.showCheckout.set(false);
    this.checkoutClientSecret.set(null);
    this.checkoutSessionId.set(null);
  }

  onCheckoutCompleted(): void {
    const sessionId = this.checkoutSessionId();
    this.closeCheckout();

    if (sessionId) {
      this.confirmTopUp(sessionId);
    }
  }

  private confirmTopUp(sessionId: string): void {
    this.walletService.confirmTopUp(sessionId).subscribe({
      next: (response) => {
        if (response.success) {
          // Reload wallet and transactions
          this.walletService.loadWallet().subscribe();
          this.walletService.loadTransactions().subscribe();
        }
        // Clear query params
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {}
        });
      },
      error: () => {
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {}
        });
      }
    });
  }

  formatPrice(cents: number): string {
    return this.walletService.formatPrice(cents);
  }

  getTransactionIcon(type: string): string {
    switch (type) {
      case 'TOPUP': return 'heroArrowUpCircle';
      case 'LESSON_PAYMENT': return 'heroCalendarDays';
      case 'REFUND': return 'heroReceiptRefund';
      default: return 'heroBanknotes';
    }
  }

  getTransactionClass(type: string): string {
    switch (type) {
      case 'TOPUP': return 'transaction--credit';
      case 'LESSON_PAYMENT': return 'transaction--debit';
      case 'REFUND': return 'transaction--credit';
      default: return '';
    }
  }
}
