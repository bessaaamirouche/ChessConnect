import { Component, OnInit, signal, inject } from '@angular/core';

import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroCheck } from '@ng-icons/heroicons/outline';
import { PaymentService } from '../../../core/services/payment.service';

@Component({
    selector: 'app-payment-success',
    imports: [RouterLink, TranslateModule, NgIconComponent],
    viewProviders: [provideIcons({ heroCheck })],
    templateUrl: './payment-success.component.html',
    styleUrl: './payment-success.component.scss'
})
export class PaymentSuccessComponent implements OnInit {
  loading = signal(true);
  success = signal(false);
  error = signal<string | null>(null);

  private translateService = inject(TranslateService);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    const sessionId = this.route.snapshot.queryParamMap.get('session_id');

    if (!sessionId) {
      this.error.set(this.translateService.instant('errors.invalidSession'));
      this.loading.set(false);
      return;
    }

    this.confirmPayment(sessionId);
  }

  private confirmPayment(sessionId: string): void {
    this.paymentService.confirmLessonPayment(sessionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.success.set(true);
          this.loading.set(false);
          // Redirect to dashboard after 2 seconds
          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 2000);
        } else {
          this.error.set(this.translateService.instant(response.error || 'errors.paymentConfirm'));
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.error.set(this.translateService.instant(err.error?.error || 'errors.paymentConfirm'));
        this.loading.set(false);
      }
    });
  }
}
