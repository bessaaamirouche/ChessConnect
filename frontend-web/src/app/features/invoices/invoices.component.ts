import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroDocumentText,
  heroArrowDownTray,
  heroArrowPath,
  heroArrowUpRight,
  heroArrowDownLeft,
  heroFunnel
} from '@ng-icons/heroicons/outline';
import { InvoiceService, Invoice } from '../../core/services/invoice.service';
import { AuthService } from '../../core/services/auth.service';

type FilterType = 'all' | 'received' | 'issued';

@Component({
  selector: 'app-invoices',
  standalone: true,
  imports: [CommonModule, RouterModule, NgIconComponent],
  providers: [
    provideIcons({
      heroDocumentText,
      heroArrowDownTray,
      heroArrowPath,
      heroArrowUpRight,
      heroArrowDownLeft,
      heroFunnel
    })
  ],
  template: `
    <div class="invoices-container">
      <header class="page-header">
        <div class="header-content">
          <h1>Mes Factures</h1>
          <p class="subtitle">Consultez et telechargez vos factures</p>
        </div>
      </header>

      <!-- Filters -->
      <div class="filters-section">
        <div class="filter-tabs">
          <button
            class="filter-tab"
            [class.active]="activeFilter() === 'all'"
            (click)="setFilter('all')"
          >
            Toutes
          </button>
          <button
            class="filter-tab"
            [class.active]="activeFilter() === 'received'"
            (click)="setFilter('received')"
          >
            <ng-icon name="heroArrowDownLeft" class="tab-icon"></ng-icon>
            Recues
          </button>
          @if (isTeacher()) {
            <button
              class="filter-tab"
              [class.active]="activeFilter() === 'issued'"
              (click)="setFilter('issued')"
            >
              <ng-icon name="heroArrowUpRight" class="tab-icon"></ng-icon>
              Emises
            </button>
          }
        </div>
      </div>

      <!-- Loading State -->
      @if (loading()) {
        <div class="loading-state">
          <ng-icon name="heroArrowPath" class="spin"></ng-icon>
          <span>Chargement des factures...</span>
        </div>
      }

      <!-- Empty State -->
      @if (!loading() && filteredInvoices().length === 0) {
        <div class="empty-state">
          <ng-icon name="heroDocumentText" class="empty-icon"></ng-icon>
          <h3>Aucune facture</h3>
          <p>Vous n'avez pas encore de factures</p>
        </div>
      }

      <!-- Invoices List -->
      @if (!loading() && filteredInvoices().length > 0) {
        <div class="invoices-list">
          @for (invoice of filteredInvoices(); track invoice.id) {
            <div class="invoice-card" [class.commission]="invoice.invoiceType === 'COMMISSION_INVOICE'" [class.subscription]="invoice.invoiceType === 'SUBSCRIPTION'">
              <div class="invoice-header">
                <div class="invoice-type">
                  @if (invoice.isReceived) {
                    <ng-icon name="heroArrowDownLeft" class="type-icon received"></ng-icon>
                    <span class="type-label">Facture recue</span>
                  } @else {
                    <ng-icon name="heroArrowUpRight" class="type-icon issued"></ng-icon>
                    <span class="type-label">Facture emise</span>
                  }
                </div>
                <span class="invoice-number">{{ invoice.invoiceNumber }}</span>
              </div>

              <div class="invoice-body">
                <div class="invoice-parties">
                  <div class="party">
                    <span class="party-label">{{ invoice.isReceived ? 'De' : 'A' }}</span>
                    <span class="party-name">{{ invoice.isReceived ? invoice.issuerName : invoice.customerName }}</span>
                  </div>
                </div>

                <div class="invoice-description">
                  {{ invoice.description }}
                </div>

                <div class="invoice-meta">
                  <span class="invoice-date">{{ invoice.issuedAt }}</span>
                  <span class="invoice-status" [class.paid]="invoice.status === 'PAID'">
                    {{ invoice.status === 'PAID' ? 'Payee' : invoice.status }}
                  </span>
                </div>
              </div>

              <div class="invoice-footer">
                <div class="invoice-amount">
                  <span class="amount-label">Total</span>
                  <span class="amount-value">{{ formatCents(invoice.totalCents) }}</span>
                </div>

                @if (invoice.hasPdf) {
                  <button
                    class="download-btn"
                    (click)="downloadPdf(invoice)"
                    [disabled]="downloadingId() === invoice.id"
                  >
                    @if (downloadingId() === invoice.id) {
                      <ng-icon name="heroArrowPath" class="spin"></ng-icon>
                    } @else {
                      <ng-icon name="heroArrowDownTray"></ng-icon>
                    }
                    <span>Telecharger PDF</span>
                  </button>
                }
              </div>

              @if (invoice.promoApplied) {
                <div class="promo-badge">Code CHESS2026 applique</div>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .invoices-container {
      max-width: 900px;
      margin: 0 auto;
      padding: 2rem;
    }

    .page-header {
      margin-bottom: 2rem;

      h1 {
        font-size: 1.75rem;
        font-weight: 600;
        color: #ffffff;
        margin: 0 0 0.5rem 0;
      }

      .subtitle {
        color: rgba(255, 255, 255, 0.6);
        margin: 0;
      }
    }

    .filters-section {
      margin-bottom: 1.5rem;
    }

    .filter-tabs {
      display: flex;
      gap: 0.5rem;
      background: rgba(255, 255, 255, 0.05);
      padding: 0.25rem;
      border-radius: 12px;
      width: fit-content;
    }

    .filter-tab {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem 1.25rem;
      border: none;
      background: transparent;
      color: rgba(255, 255, 255, 0.6);
      font-size: 0.875rem;
      font-weight: 500;
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        color: rgba(255, 255, 255, 0.9);
        background: rgba(255, 255, 255, 0.05);
      }

      &.active {
        background: rgba(212, 168, 75, 0.2);
        color: #D4A84B;
      }

      .tab-icon {
        font-size: 1rem;
      }
    }

    .loading-state,
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 4rem 2rem;
      color: rgba(255, 255, 255, 0.6);
      text-align: center;

      .spin {
        animation: spin 1s linear infinite;
      }

      .empty-icon {
        font-size: 3rem;
        margin-bottom: 1rem;
        color: rgba(255, 255, 255, 0.3);
      }

      h3 {
        color: #ffffff;
        margin: 0 0 0.5rem 0;
      }

      p {
        margin: 0;
      }
    }

    .invoices-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .invoice-card {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 16px;
      padding: 1.25rem;
      transition: all 0.2s ease;
      position: relative;
      overflow: hidden;

      &:hover {
        background: rgba(255, 255, 255, 0.08);
        border-color: rgba(212, 168, 75, 0.2);
      }

      &.commission {
        border-left: 3px solid #9b59b6;
      }

      &.subscription {
        border-left: 3px solid #D4A84B;
      }
    }

    .invoice-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }

    .invoice-type {
      display: flex;
      align-items: center;
      gap: 0.5rem;

      .type-icon {
        font-size: 1.25rem;

        &.received {
          color: #27ae60;
        }

        &.issued {
          color: #3498db;
        }
      }

      .type-label {
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: rgba(255, 255, 255, 0.6);
      }
    }

    .invoice-number {
      font-family: monospace;
      font-size: 0.875rem;
      color: rgba(255, 255, 255, 0.5);
    }

    .invoice-body {
      margin-bottom: 1rem;
    }

    .invoice-parties {
      margin-bottom: 0.75rem;
    }

    .party {
      display: flex;
      align-items: center;
      gap: 0.5rem;

      .party-label {
        font-size: 0.75rem;
        color: rgba(255, 255, 255, 0.4);
      }

      .party-name {
        font-weight: 500;
        color: #ffffff;
      }
    }

    .invoice-description {
      font-size: 0.875rem;
      color: rgba(255, 255, 255, 0.7);
      margin-bottom: 0.75rem;
    }

    .invoice-meta {
      display: flex;
      gap: 1rem;
      font-size: 0.75rem;
    }

    .invoice-date {
      color: rgba(255, 255, 255, 0.5);
    }

    .invoice-status {
      padding: 0.125rem 0.5rem;
      border-radius: 4px;
      background: rgba(255, 255, 255, 0.1);
      color: rgba(255, 255, 255, 0.7);

      &.paid {
        background: rgba(39, 174, 96, 0.2);
        color: #27ae60;
      }
    }

    .invoice-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 1rem;
      border-top: 1px solid rgba(255, 255, 255, 0.08);
    }

    .invoice-amount {
      .amount-label {
        font-size: 0.75rem;
        color: rgba(255, 255, 255, 0.5);
        display: block;
        margin-bottom: 0.25rem;
      }

      .amount-value {
        font-size: 1.25rem;
        font-weight: 600;
        color: #D4A84B;
      }
    }

    .download-btn {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem 1.25rem;
      border: 1px solid rgba(212, 168, 75, 0.3);
      background: rgba(212, 168, 75, 0.1);
      color: #D4A84B;
      font-size: 0.875rem;
      font-weight: 500;
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover:not(:disabled) {
        background: rgba(212, 168, 75, 0.2);
        border-color: rgba(212, 168, 75, 0.5);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      ng-icon {
        font-size: 1.125rem;
      }
    }

    .promo-badge {
      position: absolute;
      top: 0.75rem;
      right: 0.75rem;
      padding: 0.25rem 0.75rem;
      background: rgba(155, 89, 182, 0.2);
      color: #9b59b6;
      font-size: 0.7rem;
      font-weight: 600;
      border-radius: 6px;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    @media (max-width: 640px) {
      .invoices-container {
        padding: 1rem;
      }

      .filter-tabs {
        width: 100%;
        overflow-x: auto;
      }

      .filter-tab {
        padding: 0.625rem 1rem;
        white-space: nowrap;
      }

      .invoice-footer {
        flex-direction: column;
        gap: 1rem;
        align-items: stretch;
      }

      .download-btn {
        justify-content: center;
      }
    }
  `]
})
export class InvoicesComponent implements OnInit {
  private invoiceService = inject(InvoiceService);
  private authService = inject(AuthService);

  invoices = signal<Invoice[]>([]);
  loading = signal(true);
  activeFilter = signal<FilterType>('all');
  downloadingId = signal<number | null>(null);

  isTeacher = computed(() => this.authService.currentUser()?.role === 'TEACHER');

  filteredInvoices = computed(() => {
    const filter = this.activeFilter();
    const all = this.invoices();

    if (filter === 'all') return all;
    if (filter === 'received') return all.filter(inv => inv.isReceived);
    if (filter === 'issued') return all.filter(inv => !inv.isReceived);

    return all;
  });

  ngOnInit(): void {
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.loading.set(true);
    this.invoiceService.getMyInvoices().subscribe({
      next: (invoices) => {
        this.invoices.set(invoices);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading invoices:', err);
        this.loading.set(false);
      }
    });
  }

  setFilter(filter: FilterType): void {
    this.activeFilter.set(filter);
  }

  downloadPdf(invoice: Invoice): void {
    this.downloadingId.set(invoice.id);
    this.invoiceService.downloadInvoicePdf(invoice.id);
    setTimeout(() => this.downloadingId.set(null), 2000);
  }

  formatCents(cents: number): string {
    return this.invoiceService.formatCents(cents);
  }
}
