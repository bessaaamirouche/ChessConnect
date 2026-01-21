import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PaymentService } from '../../core/services/payment.service';
import { AuthService } from '../../core/services/auth.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroCheck,
  heroBanknotes,
  heroArrowTrendingUp,
  heroCalendarDays,
  heroXMark,
  heroGift
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [RouterLink, NgIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroCheck,
    heroBanknotes,
    heroArrowTrendingUp,
    heroCalendarDays,
    heroXMark,
    heroGift
  })],
  templateUrl: './pricing.component.html',
  styleUrl: './pricing.component.scss'
})
export class PricingComponent implements OnInit {
  constructor(
    public paymentService: PaymentService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.paymentService.loadPlans().subscribe();
  }

  formatPrice(cents: number): string {
    return this.paymentService.formatPrice(cents);
  }
}
