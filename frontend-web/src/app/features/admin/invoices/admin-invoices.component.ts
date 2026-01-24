import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroDocumentText,
  heroArrowDownTray,
  heroArrowPath,
  heroMagnifyingGlass,
  heroFunnel
} from '@ng-icons/heroicons/outline';
import { InvoiceService, Invoice } from '../../../core/services/invoice.service';

@Component({
  selector: 'app-admin-invoices',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIconComponent],
  providers: [
    provideIcons({
      heroDocumentText,
      heroArrowDownTray,
      heroArrowPath,
      heroMagnifyingGlass,
      heroFunnel
    })
  ],
  template: `
    <div class="admin-invoices">
      <header class="page-header">
        <div class="header-content">
          <h1>Toutes les Factures</h1>
          <p class="subtitle">Consultez toutes les factures de la plateforme</p>
        </div>
        <button class="btn btn--ghost" (click)="refresh()" [disabled]="loading()">
          <ng-icon name="heroArrowPath" [class.spin]="loading()"></ng-icon>
          Actualiser
        </button>
      </header>

      <!-- Stats -->
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-value">{{ invoices().length }}</span>
          <span class="stat-label">Total factures</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ formatCents(totalRevenue()) }}</span>
          <span class="stat-label">CA Total</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ paidCount() }}</span>
          <span class="stat-label">Payees</span>
        </div>
      </div>

      <!-- Search & Filters -->
      <div class="filters-section">
        <div class="search-box">
          <ng-icon name="heroMagnifyingGlass"></ng-icon>
          <input
            type="text"
            placeholder="Rechercher par numero, client, emetteur..."
            [ngModel]="searchQuery()"
            (ngModelChange)="searchQuery.set($event)"
          >
        </div>
        <select class="filter-select" [ngModel]="statusFilter()" (ngModelChange)="statusFilter.set($event)">
          <option value="">Tous les statuts</option>
          <option value="PAID">Payees</option>
          <option value="PENDING">En attente</option>
        </select>
        <select class="filter-select" [ngModel]="typeFilter()" (ngModelChange)="typeFilter.set($event)">
          <option value="">Tous les types</option>
          <option value="LESSON_INVOICE">Cours</option>
          <option value="COMMISSION_INVOICE">Commission</option>
          <option value="PAYOUT_INVOICE">Virement</option>
        </select>
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
          <p>Aucune facture trouvee</p>
        </div>
      }

      <!-- Invoices Table -->
      @if (!loading() && filteredInvoices().length > 0) {
        <div class="table-container">
          <table class="invoices-table">
            <thead>
              <tr>
                <th>Numero</th>
                <th>Type</th>
                <th>Date</th>
                <th>Emetteur</th>
                <th>Client</th>
                <th>Description</th>
                <th>Montant</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (invoice of filteredInvoices(); track invoice.id) {
                <tr>
                  <td class="invoice-number">{{ invoice.invoiceNumber }}</td>
                  <td>
                    <span class="type-badge" [class]="invoice.invoiceType.toLowerCase()">
                      {{ getInvoiceTypeLabel(invoice.invoiceType) }}
                    </span>
                  </td>
                  <td>{{ invoice.issuedAt }}</td>
                  <td>{{ invoice.issuerName }}</td>
                  <td>{{ invoice.customerName }}</td>
                  <td class="description">{{ invoice.description }}</td>
                  <td class="amount">{{ formatCents(invoice.totalCents) }}</td>
                  <td>
                    <span class="status-badge" [class.paid]="invoice.status === 'PAID'">
                      {{ invoice.status === 'PAID' ? 'Payee' : 'En attente' }}
                    </span>
                  </td>
                  <td>
                    <button
                      class="btn-icon"
                      (click)="downloadPdf(invoice)"
                      [disabled]="downloadingId() === invoice.id"
                      title="Telecharger PDF"
                    >
                      @if (downloadingId() === invoice.id) {
                        <ng-icon name="heroArrowPath" class="spin"></ng-icon>
                      } @else {
                        <ng-icon name="heroDocumentText"></ng-icon>
                      }
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: [`
    .admin-invoices {
      padding: 0;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;

      h1 {
        font-size: 1.5rem;
        font-weight: 600;
        color: var(--text-primary);
        margin: 0;
      }

      .subtitle {
        color: var(--text-muted);
        font-size: 0.875rem;
        margin-top: 4px;
      }
    }

    .stats-row {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
      margin-bottom: 24px;
    }

    .stat-card {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: 12px;
      padding: 20px;
      text-align: center;

      .stat-value {
        display: block;
        font-size: 1.5rem;
        font-weight: 700;
        color: var(--gold-400);
      }

      .stat-label {
        display: block;
        font-size: 0.75rem;
        color: var(--text-muted);
        text-transform: uppercase;
        margin-top: 4px;
      }
    }

    .filters-section {
      display: flex;
      gap: 16px;
      margin-bottom: 24px;
    }

    .search-box {
      flex: 1;
      display: flex;
      align-items: center;
      gap: 8px;
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: 8px;
      padding: 0 12px;

      ng-icon {
        color: var(--text-muted);
      }

      input {
        flex: 1;
        background: none;
        border: none;
        padding: 12px 0;
        color: var(--text-primary);
        font-size: 0.875rem;

        &::placeholder {
          color: var(--text-muted);
        }

        &:focus {
          outline: none;
        }
      }
    }

    .filter-select {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: 8px;
      padding: 12px 16px;
      color: var(--text-primary);
      font-size: 0.875rem;
      min-width: 160px;
    }

    .loading-state, .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 60px 20px;
      color: var(--text-muted);

      ng-icon {
        font-size: 48px;
        margin-bottom: 16px;
      }

      h3 {
        margin: 0 0 8px;
        color: var(--text-primary);
      }
    }

    .spin {
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    .table-container {
      background: var(--bg-secondary);
      border: 1px solid var(--border-subtle);
      border-radius: 12px;
      overflow: hidden;
    }

    .invoices-table {
      width: 100%;
      border-collapse: collapse;

      th, td {
        padding: 12px 16px;
        text-align: left;
        border-bottom: 1px solid var(--border-subtle);
      }

      th {
        background: var(--bg-tertiary);
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        color: var(--text-muted);
      }

      td {
        font-size: 0.875rem;
        color: var(--text-primary);
      }

      tbody tr:hover {
        background: var(--bg-tertiary);
      }

      tbody tr:last-child td {
        border-bottom: none;
      }

      .invoice-number {
        font-weight: 600;
        color: var(--gold-400);
      }

      .description {
        max-width: 200px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .amount {
        font-weight: 600;
      }
    }

    .status-badge {
      display: inline-block;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 500;
      background: var(--bg-tertiary);
      color: var(--text-muted);

      &.paid {
        background: rgba(34, 197, 94, 0.15);
        color: #22c55e;
      }
    }

    .type-badge {
      display: inline-block;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 500;
      background: var(--bg-tertiary);
      color: var(--text-muted);

      &.lesson_invoice {
        background: rgba(59, 130, 246, 0.15);
        color: #3b82f6;
      }

      &.commission_invoice {
        background: rgba(249, 115, 22, 0.15);
        color: #f97316;
      }

      &.payout_invoice {
        background: rgba(34, 197, 94, 0.15);
        color: #22c55e;
      }
    }

    .btn-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border-subtle);
      border-radius: 6px;
      color: var(--text-secondary);
      cursor: pointer;
      transition: all 0.2s;

      &:hover:not(:disabled) {
        background: var(--gold-400);
        color: #000;
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }

    .btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      border-radius: 8px;
      font-size: 0.875rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;

      &--ghost {
        background: transparent;
        border: 1px solid var(--border-subtle);
        color: var(--text-secondary);

        &:hover:not(:disabled) {
          background: var(--bg-secondary);
        }
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }

    @media (max-width: 768px) {
      .stats-row {
        grid-template-columns: 1fr;
      }

      .filters-section {
        flex-direction: column;
      }

      .table-container {
        overflow-x: auto;
      }

      .invoices-table {
        min-width: 800px;
      }
    }
  `]
})
export class AdminInvoicesComponent implements OnInit {
  private invoiceService = inject(InvoiceService);

  invoices = signal<Invoice[]>([]);
  loading = signal(false);
  downloadingId = signal<number | null>(null);
  searchQuery = signal('');
  statusFilter = signal('');
  typeFilter = signal('');

  filteredInvoices = computed(() => {
    let result = this.invoices();
    const query = this.searchQuery().toLowerCase();
    const status = this.statusFilter();
    const type = this.typeFilter();

    if (query) {
      result = result.filter(inv =>
        inv.invoiceNumber.toLowerCase().includes(query) ||
        inv.issuerName.toLowerCase().includes(query) ||
        inv.customerName.toLowerCase().includes(query) ||
        inv.description.toLowerCase().includes(query)
      );
    }

    if (status) {
      result = result.filter(inv => inv.status === status);
    }

    if (type) {
      result = result.filter(inv => inv.invoiceType === type);
    }

    return result;
  });

  totalRevenue = computed(() => {
    return this.invoices().reduce((sum, inv) => sum + inv.totalCents, 0);
  });

  paidCount = computed(() => {
    return this.invoices().filter(inv => inv.status === 'PAID').length;
  });

  ngOnInit(): void {
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.loading.set(true);
    this.invoiceService.getAllInvoices().subscribe({
      next: (invoices) => {
        this.invoices.set(invoices);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  refresh(): void {
    this.loadInvoices();
  }

  formatCents(cents: number): string {
    return (cents / 100).toFixed(2) + ' EUR';
  }

  downloadPdf(invoice: Invoice): void {
    this.downloadingId.set(invoice.id);
    this.invoiceService.downloadInvoicePdf(invoice.id);
    setTimeout(() => this.downloadingId.set(null), 2000);
  }

  getInvoiceTypeLabel(type: string): string {
    switch (type) {
      case 'LESSON_INVOICE': return 'Cours';
      case 'COMMISSION_INVOICE': return 'Commission';
      case 'PAYOUT_INVOICE': return 'Virement';
      default: return type;
    }
  }
}
