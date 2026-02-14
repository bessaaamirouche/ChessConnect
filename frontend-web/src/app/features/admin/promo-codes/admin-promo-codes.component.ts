import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { AdminPromoService, PromoCodeResponse, CreatePromoCodeRequest, PromoCodeUsageResponse, ReferralEarningResponse } from '../../../core/services/admin-promo.service';

@Component({
    selector: 'app-admin-promo-codes',
    imports: [FormsModule, TranslateModule, DatePipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './admin-promo-codes.component.html',
    styleUrl: './admin-promo-codes.component.scss'
})
export class AdminPromoCodesComponent implements OnInit {
  private promoService = inject(AdminPromoService);
  private translate = inject(TranslateService);

  codes = signal<PromoCodeResponse[]>([]);
  loading = signal(false);
  activeTab = signal<'PROMO' | 'REFERRAL'>('PROMO');

  // Modal state
  showModal = signal(false);
  editingCode = signal<PromoCodeResponse | null>(null);

  // Form fields
  formCode = signal('');
  formCodeType = signal<'PROMO' | 'REFERRAL'>('PROMO');
  formDiscountType = signal<'COMMISSION_REDUCTION' | 'STUDENT_DISCOUNT'>('COMMISSION_REDUCTION');
  formDiscountPercent = signal(100);
  formReferrerName = signal('');
  formReferrerEmail = signal('');
  formPremiumDays = signal(30);
  formRevenueSharePercent = signal(30);
  formMaxUses = signal<number | null>(null);
  formFirstLessonOnly = signal(false);
  formMinAmountCents = signal<number | null>(null);
  formExpiresAt = signal('');

  // Detail view
  showDetail = signal(false);
  detailCode = signal<PromoCodeResponse | null>(null);
  detailUsages = signal<PromoCodeUsageResponse[]>([]);
  detailEarnings = signal<ReferralEarningResponse[]>([]);

  ngOnInit(): void {
    this.loadCodes();
  }

  loadCodes(): void {
    this.loading.set(true);
    this.promoService.getAll().subscribe({
      next: (codes) => {
        this.codes.set(codes);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  get filteredCodes(): PromoCodeResponse[] {
    return this.codes().filter(c => c.codeType === this.activeTab());
  }

  setTab(tab: 'PROMO' | 'REFERRAL'): void {
    this.activeTab.set(tab);
  }

  openCreateModal(): void {
    this.editingCode.set(null);
    this.formCodeType.set(this.activeTab());
    this.formCode.set('');
    this.formDiscountType.set('COMMISSION_REDUCTION');
    this.formDiscountPercent.set(100);
    this.formReferrerName.set('');
    this.formReferrerEmail.set('');
    this.formPremiumDays.set(30);
    this.formRevenueSharePercent.set(30);
    this.formMaxUses.set(null);
    this.formFirstLessonOnly.set(false);
    this.formMinAmountCents.set(null);
    this.formExpiresAt.set('');
    this.showModal.set(true);
  }

  openEditModal(code: PromoCodeResponse): void {
    this.editingCode.set(code);
    this.formCodeType.set(code.codeType);
    this.formCode.set(code.code);
    this.formDiscountType.set(code.discountType || 'COMMISSION_REDUCTION');
    this.formDiscountPercent.set(code.discountPercent || 0);
    this.formReferrerName.set(code.referrerName || '');
    this.formReferrerEmail.set(code.referrerEmail || '');
    this.formPremiumDays.set(code.premiumDays || 0);
    this.formRevenueSharePercent.set(code.revenueSharePercent || 0);
    this.formMaxUses.set(code.maxUses);
    this.formFirstLessonOnly.set(code.firstLessonOnly);
    this.formMinAmountCents.set(code.minAmountCents);
    this.formExpiresAt.set(code.expiresAt ? code.expiresAt.substring(0, 16) : '');
    this.showModal.set(true);
  }

  generateCode(): void {
    this.promoService.generateCode().subscribe({
      next: (res) => this.formCode.set(res.code)
    });
  }

  saveCode(): void {
    const request: CreatePromoCodeRequest = {
      code: this.formCode(),
      codeType: this.formCodeType(),
      discountType: this.formCodeType() === 'PROMO' ? this.formDiscountType() : undefined,
      discountPercent: this.formCodeType() === 'PROMO' ? this.formDiscountPercent() : undefined,
      referrerName: this.formCodeType() === 'REFERRAL' ? this.formReferrerName() : undefined,
      referrerEmail: this.formCodeType() === 'REFERRAL' ? this.formReferrerEmail() : undefined,
      premiumDays: this.formPremiumDays() || 0,
      revenueSharePercent: this.formCodeType() === 'REFERRAL' ? this.formRevenueSharePercent() : undefined,
      maxUses: this.formMaxUses() || undefined,
      firstLessonOnly: this.formFirstLessonOnly(),
      minAmountCents: this.formMinAmountCents() || undefined,
      expiresAt: this.formExpiresAt() || undefined
    };

    const editing = this.editingCode();
    const obs = editing
      ? this.promoService.update(editing.id, request)
      : this.promoService.create(request);

    obs.subscribe({
      next: () => {
        this.showModal.set(false);
        this.loadCodes();
      },
      error: (err) => {
        alert(err.error?.message || err.error?.error || 'Erreur');
      }
    });
  }

  toggleActive(code: PromoCodeResponse): void {
    this.promoService.toggleActive(code.id, !code.isActive).subscribe({
      next: () => this.loadCodes()
    });
  }

  deleteCode(code: PromoCodeResponse): void {
    if (!confirm(`Supprimer le code ${code.code} ?`)) return;
    this.promoService.delete(code.id).subscribe({
      next: () => this.loadCodes()
    });
  }

  openDetail(code: PromoCodeResponse): void {
    this.detailCode.set(code);
    this.showDetail.set(true);
    this.promoService.getUsages(code.id).subscribe({
      next: (usages) => this.detailUsages.set(usages)
    });
    if (code.codeType === 'REFERRAL') {
      this.promoService.getEarnings(code.id).subscribe({
        next: (earnings) => this.detailEarnings.set(earnings)
      });
    }
  }

  closeDetail(): void {
    this.showDetail.set(false);
    this.detailCode.set(null);
  }

  markEarningsPaid(): void {
    const code = this.detailCode();
    if (!code) return;
    const ref = prompt('Reference de paiement :');
    if (!ref) return;
    this.promoService.markEarningsAsPaid(code.id, ref).subscribe({
      next: () => {
        this.loadCodes();
        this.openDetail(code);
      }
    });
  }

  formatCents(cents: number): string {
    return (cents / 100).toFixed(2) + ' EUR';
  }
}
