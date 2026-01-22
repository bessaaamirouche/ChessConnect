import { Component, OnInit, signal, ChangeDetectionStrategy, computed } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { PaymentService, SubscriptionPlanResponseDto } from '../../core/services/payment.service';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { SubscriptionPlan } from '../../core/models/subscription.model';
import { EmbeddedCheckoutComponent } from '../../shared/embedded-checkout/embedded-checkout.component';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChartBarSquare,
  heroCalendarDays,
  heroTrophy,
  heroCreditCard,
  heroAcademicCap,
  heroUserCircle,
  heroArrowRightOnRectangle,
  heroBanknotes,
  heroArrowTrendingUp,
  heroCheck,
  heroXMark,
  heroInformationCircle
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-subscription',
  standalone: true,
  imports: [RouterLink, DatePipe, NgIconComponent, EmbeddedCheckoutComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroChartBarSquare,
    heroCalendarDays,
    heroTrophy,
    heroCreditCard,
    heroAcademicCap,
    heroUserCircle,
    heroArrowRightOnRectangle,
    heroBanknotes,
    heroArrowTrendingUp,
    heroCheck,
    heroXMark,
    heroInformationCircle
  })],
  templateUrl: './subscription.component.html',
  styleUrl: './subscription.component.scss'
})
export class SubscriptionComponent implements OnInit {
  processingPlan = signal<SubscriptionPlan | null>(null);
  showCancelConfirm = signal(false);
  cancelling = signal(false);

  // Embedded checkout
  showCheckout = signal(false);
  checkoutClientSecret = signal<string | null>(null);
  checkoutSessionId = signal<string | null>(null);
  selectedPlanName = signal<string>('');

  constructor(
    public paymentService: PaymentService,
    public authService: AuthService,
    private router: Router,
    private seoService: SeoService
  ) {
    this.seoService.setSubscriptionPage();
  }

  ngOnInit(): void {
    this.paymentService.loadPlans().subscribe();
    this.paymentService.loadActiveSubscription().subscribe();
  }

  subscribeToPlan(plan: SubscriptionPlan): void {
    this.processingPlan.set(plan);

    // Find plan display name
    const planDetails = this.paymentService.plans().find(p => p.code === plan);
    this.selectedPlanName.set(planDetails?.name || 'Abonnement');

    this.paymentService.createSubscriptionCheckout(plan, true).subscribe({
      next: (response) => {
        if (response.clientSecret) {
          // Use embedded checkout
          this.checkoutClientSecret.set(response.clientSecret);
          this.checkoutSessionId.set(response.sessionId);
          this.showCheckout.set(true);
          this.processingPlan.set(null);
        } else if (response.url) {
          // Fallback to redirect
          window.location.href = response.url;
        }
      },
      error: () => {
        this.processingPlan.set(null);
      }
    });
  }

  closeCheckout(): void {
    this.showCheckout.set(false);
    this.checkoutClientSecret.set(null);
    this.checkoutSessionId.set(null);
    this.selectedPlanName.set('');
  }

  onCheckoutCompleted(): void {
    const sessionId = this.checkoutSessionId();
    this.closeCheckout();

    if (sessionId) {
      // Navigate to success page to confirm payment
      this.router.navigate(['/subscription/success'], {
        queryParams: { session_id: sessionId }
      });
    }
  }

  confirmCancel(): void {
    this.showCancelConfirm.set(true);
  }

  cancelSubscription(): void {
    this.cancelling.set(true);
    this.paymentService.cancelSubscription().subscribe({
      next: () => {
        this.cancelling.set(false);
        this.showCancelConfirm.set(false);
      },
      error: () => {
        this.cancelling.set(false);
      }
    });
  }

  dismissCancelConfirm(): void {
    this.showCancelConfirm.set(false);
  }

  formatPrice(cents: number): string {
    return this.paymentService.formatPrice(cents);
  }

  logout(): void {
    this.authService.logout();
  }
}
