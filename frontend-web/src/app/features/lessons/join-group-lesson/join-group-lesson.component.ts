import { Component, OnInit, signal, computed, inject } from '@angular/core';

import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { heroUserGroup, heroCalendarDays, heroClock, heroTicket, heroWallet, heroArrowRightOnRectangle, heroCreditCard } from '@ng-icons/heroicons/outline';
import { GroupLessonService } from '../../../core/services/group-lesson.service';
import { AuthService } from '../../../core/services/auth.service';
import { WalletService } from '../../../core/services/wallet.service';
import { EmbeddedCheckoutComponent } from '../../../shared/embedded-checkout/embedded-checkout.component';
import { GroupInvitationResponse } from '@contracts';

@Component({
    selector: 'app-join-group-lesson',
    imports: [RouterLink, TranslateModule, NgIconComponent, EmbeddedCheckoutComponent],
    viewProviders: [provideIcons({
            heroUserGroup,
            heroCalendarDays,
            heroClock,
            heroTicket,
            heroWallet,
            heroArrowRightOnRectangle,
            heroCreditCard
        })],
    templateUrl: './join-group-lesson.component.html',
    styleUrl: './join-group-lesson.component.scss'
})
export class JoinGroupLessonComponent implements OnInit {
  loading = signal(true);
  joining = signal(false);
  invitation = signal<GroupInvitationResponse | null>(null);
  error = signal<string | null>(null);
  success = signal(false);
  alreadyJoined = signal(false);

  // Embedded checkout
  showCheckout = signal(false);
  checkoutClientSecret = signal<string | null>(null);
  checkoutSessionId = signal<string | null>(null);

  private token = '';

  canPayWithCredit = computed(() => {
    const inv = this.invitation();
    if (!inv) return false;
    return this.walletService.hasEnoughCredit(inv.pricePerPersonCents);
  });

  teacherDisplayName = computed(() => {
    const inv = this.invitation();
    if (!inv) return '';
    return `${inv.teacherFirstName} ${inv.teacherLastInitial}.`;
  });

  teacherInitials = computed(() => {
    const inv = this.invitation();
    if (!inv) return '';
    return `${inv.teacherFirstName.charAt(0)}${inv.teacherLastInitial}`.toUpperCase();
  });

  formattedDate = computed(() => {
    const inv = this.invitation();
    if (!inv) return '';
    const date = new Date(inv.scheduledAt);
    return date.toLocaleDateString('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  });

  formattedTime = computed(() => {
    const inv = this.invitation();
    if (!inv) return '';
    const date = new Date(inv.scheduledAt);
    return date.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit'
    });
  });

  formattedPrice = computed(() => {
    const inv = this.invitation();
    if (!inv) return '';
    return (inv.pricePerPersonCents / 100).toFixed(2).replace('.', ',') + ' \u20ac';
  });

  formattedBalance = computed(() => {
    const balance = this.walletService.balance();
    return (balance / 100).toFixed(2).replace('.', ',') + ' \u20ac';
  });

  formattedDeadline = computed(() => {
    const inv = this.invitation();
    if (!inv) return '';
    const date = new Date(inv.deadline);
    return date.toLocaleDateString('fr-FR', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  });

  private translate = inject(TranslateService);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private groupLessonService: GroupLessonService,
    public authService: AuthService,
    public walletService: WalletService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') || '';

    if (!this.token) {
      this.error.set(this.translate.instant('errors.tokenInvalid'));
      this.loading.set(false);
      return;
    }

    this.loadInvitation();

    if (this.authService.isAuthenticated()) {
      this.walletService.loadBalance().subscribe();
    }
  }

  private loadInvitation(): void {
    this.loading.set(true);
    this.groupLessonService.getInvitationDetails(this.token).subscribe({
      next: (inv) => {
        this.invitation.set(inv);
        this.loading.set(false);
      },
      error: (err) => {
        if (err.status === 409) {
          this.alreadyJoined.set(true);
        } else if (err.status === 410 || err.status === 404) {
          this.error.set('expired');
        } else {
          this.error.set(err.error?.error || this.translate.instant('errors.generic'));
        }
        this.loading.set(false);
      }
    });
  }

  joinWithCredit(): void {
    if (this.joining()) return;
    this.joining.set(true);

    this.groupLessonService.joinWithCredit(this.token).subscribe({
      next: () => {
        this.success.set(true);
        this.joining.set(false);
        setTimeout(() => {
          this.router.navigate(['/lessons']);
        }, 2000);
      },
      error: (err) => {
        if (err.status === 409) {
          this.alreadyJoined.set(true);
        } else {
          this.error.set(err.error?.error || this.translate.instant('errors.joiningLesson'));
        }
        this.joining.set(false);
      }
    });
  }

  joinWithCard(): void {
    if (this.joining()) return;
    this.joining.set(true);

    this.groupLessonService.createJoinCheckout(this.token).subscribe({
      next: (response) => {
        if (response.clientSecret) {
          this.checkoutClientSecret.set(response.clientSecret);
          this.checkoutSessionId.set(response.sessionId);
          this.showCheckout.set(true);
        }
        this.joining.set(false);
      },
      error: (err) => {
        if (err.status === 409) {
          this.alreadyJoined.set(true);
        } else {
          this.error.set(err.error?.error || this.translate.instant('errors.checkoutCreate'));
        }
        this.joining.set(false);
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
      this.joining.set(true);
      this.groupLessonService.confirmJoinPayment(sessionId).subscribe({
        next: () => {
          this.success.set(true);
          this.joining.set(false);
          setTimeout(() => {
            this.router.navigate(['/lessons']);
          }, 2000);
        },
        error: (err) => {
          this.error.set(err.error?.error || this.translate.instant('errors.paymentConfirm'));
          this.joining.set(false);
        }
      });
    }
  }

  goToLogin(): void {
    this.router.navigate(['/login'], {
      queryParams: { returnUrl: `/join/${this.token}` }
    });
  }
}
