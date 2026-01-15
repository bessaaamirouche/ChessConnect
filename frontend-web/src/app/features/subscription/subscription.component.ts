import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { PaymentService, SubscriptionPlanResponseDto } from '../../core/services/payment.service';
import { AuthService } from '../../core/services/auth.service';
import { SubscriptionPlan } from '../../core/models/subscription.model';
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
  imports: [RouterLink, DatePipe, NgIconComponent],
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

  constructor(
    public paymentService: PaymentService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.paymentService.loadPlans().subscribe();
    this.paymentService.loadActiveSubscription().subscribe();
  }

  subscribeToPlan(plan: SubscriptionPlan): void {
    this.processingPlan.set(plan);

    this.paymentService.createSubscriptionCheckout(plan).subscribe({
      next: (response) => {
        // Redirect to Stripe Checkout
        window.location.href = response.url;
      },
      error: () => {
        this.processingPlan.set(null);
      }
    });
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
