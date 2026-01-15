import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PaymentService } from '../../../core/services/payment.service';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './payment-success.component.html',
  styleUrl: './payment-success.component.scss'
})
export class PaymentSuccessComponent implements OnInit {
  loading = signal(true);
  success = signal(false);
  error = signal<string | null>(null);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    const sessionId = this.route.snapshot.queryParamMap.get('session_id');

    if (!sessionId) {
      this.error.set('Session de paiement invalide');
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
          this.error.set(response.error || 'Erreur lors de la confirmation');
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.error.set(err.error?.error || 'Erreur lors de la confirmation du paiement');
        this.loading.set(false);
      }
    });
  }
}
