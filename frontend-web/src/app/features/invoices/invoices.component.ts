import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroDocumentText,
  heroArrowDownTray,
  heroArrowPath,
  heroArrowUpRight,
  heroArrowDownLeft,
  heroMagnifyingGlass,
  heroChevronUp,
  heroChevronDown,
  heroChevronUpDown,
  heroXMark
} from '@ng-icons/heroicons/outline';
import { InvoiceService, Invoice } from '../../core/services/invoice.service';
import { AuthService } from '../../core/services/auth.service';

type FilterType = 'all' | 'received' | 'issued';
type SortField = 'date' | 'amount' | 'number';
type SortOrder = 'asc' | 'desc';

@Component({
  selector: 'app-invoices',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NgIconComponent],
  providers: [
    provideIcons({
      heroDocumentText,
      heroArrowDownTray,
      heroArrowPath,
      heroArrowUpRight,
      heroArrowDownLeft,
      heroMagnifyingGlass,
      heroChevronUp,
      heroChevronDown,
      heroChevronUpDown,
      heroXMark
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

      <!-- Filters Row -->
      <div class="filters-row">
        <!-- Search -->
        <div class="search-box">
          <ng-icon name="heroMagnifyingGlass"></ng-icon>
          <input
            type="text"
            placeholder="Rechercher..."
            [ngModel]="searchQuery()"
            (ngModelChange)="searchQuery.set($event)"
          >
          @if (searchQuery()) {
            <button class="clear-btn" (click)="searchQuery.set('')">
              <ng-icon name="heroXMark"></ng-icon>
            </button>
          }
        </div>

        <!-- Date Range -->
        <div class="date-filters">
          <div class="date-input">
            <label>Du</label>
            <input type="date" [value]="dateFrom()" (change)="dateFrom.set($any($event.target).value)">
          </div>
          <div class="date-input">
            <label>Au</label>
            <input type="date" [value]="dateTo()" (change)="dateTo.set($any($event.target).value)">
          </div>
          @if (dateFrom() || dateTo()) {
            <button class="clear-dates-btn" (click)="clearDates()">
              <ng-icon name="heroXMark"></ng-icon>
            </button>
          }
        </div>

        <!-- Type Filter -->
        <div class="type-filters">
          <button
            class="filter-btn"
            [class.active]="activeFilter() === 'all'"
            (click)="setFilter('all')"
          >
            Toutes
          </button>
          <button
            class="filter-btn"
            [class.active]="activeFilter() === 'received'"
            (click)="setFilter('received')"
          >
            Recues
          </button>
          @if (isTeacher()) {
            <button
              class="filter-btn"
              [class.active]="activeFilter() === 'issued'"
              (click)="setFilter('issued')"
            >
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
      @if (!loading() && filteredAndSortedInvoices().length === 0) {
        <div class="empty-state">
          <ng-icon name="heroDocumentText" class="empty-icon"></ng-icon>
          <h3>Aucune facture</h3>
          <p>Aucune facture ne correspond a vos criteres</p>
        </div>
      }

      <!-- Invoices Table -->
      @if (!loading() && filteredAndSortedInvoices().length > 0) {
        <div class="table-container">
          <table class="invoices-table">
            <thead>
              <tr>
                <th class="sortable" (click)="toggleSort('number')">
                  <span>Numero</span>
                  <ng-icon [name]="getSortIcon('number')"></ng-icon>
                </th>
                <th>Type</th>
                <th class="sortable" (click)="toggleSort('date')">
                  <span>Date</span>
                  <ng-icon [name]="getSortIcon('date')"></ng-icon>
                </th>
                <th>Description</th>
                <th class="sortable" (click)="toggleSort('amount')">
                  <span>Montant</span>
                  <ng-icon [name]="getSortIcon('amount')"></ng-icon>
                </th>
                <th>Statut</th>
                <th>PDF</th>
              </tr>
            </thead>
            <tbody>
              @for (invoice of filteredAndSortedInvoices(); track invoice.id) {
                <tr>
                  <td class="invoice-number">{{ invoice.invoiceNumber }}</td>
                  <td>
                    <div class="type-cell">
                      @if (invoice.isReceived) {
                        <ng-icon name="heroArrowDownLeft" class="type-icon received"></ng-icon>
                        <span>Recue</span>
                      } @else {
                        <ng-icon name="heroArrowUpRight" class="type-icon issued"></ng-icon>
                        <span>Emise</span>
                      }
                    </div>
                  </td>
                  <td>{{ formatDate(invoice.issuedAt) }}</td>
                  <td class="description-cell">
                    <span class="description">{{ invoice.description }}</span>
                    <span class="party">{{ invoice.isReceived ? invoice.issuerName : invoice.customerName }}</span>
                  </td>
                  <td class="amount">{{ formatCents(invoice.totalCents) }}</td>
                  <td>
                    <span class="status-badge" [class.paid]="invoice.status === 'PAID'">
                      {{ invoice.status === 'PAID' ? 'Payee' : 'En attente' }}
                    </span>
                  </td>
                  <td>
                    <button
                      class="pdf-btn"
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

        <!-- Results count -->
        <div class="results-info">
          {{ filteredAndSortedInvoices().length }} facture(s) sur {{ invoices().length }}
        </div>
      }
    </div>
  `,
  styles: [`
    .invoices-container {
      max-width: 1100px;
      margin: 0 auto;
      padding: 2rem;
    }

    .page-header {
      margin-bottom: 1.5rem;

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

    .filters-row {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      margin-bottom: 1.5rem;
      align-items: flex-end;
    }

    .search-box {
      flex: 1;
      min-width: 200px;
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      padding: 0 12px;

      ng-icon {
        color: rgba(255, 255, 255, 0.4);
        font-size: 1.125rem;
      }

      input {
        flex: 1;
        background: none;
        border: none;
        padding: 12px 0;
        color: #ffffff;
        font-size: 0.875rem;

        &::placeholder {
          color: rgba(255, 255, 255, 0.4);
        }

        &:focus {
          outline: none;
        }
      }

      .clear-btn {
        background: none;
        border: none;
        color: rgba(255, 255, 255, 0.4);
        cursor: pointer;
        padding: 4px;
        display: flex;

        &:hover {
          color: rgba(255, 255, 255, 0.8);
        }
      }
    }

    .date-filters {
      display: flex;
      align-items: flex-end;
      gap: 0.5rem;
    }

    .date-input {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;

      label {
        font-size: 0.75rem;
        color: rgba(255, 255, 255, 0.5);
      }

      input {
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 8px;
        padding: 10px 12px;
        color: #ffffff;
        font-size: 0.875rem;

        &:focus {
          outline: none;
          border-color: rgba(212, 168, 75, 0.5);
        }
      }
    }

    .clear-dates-btn {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      padding: 10px;
      color: rgba(255, 255, 255, 0.4);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        color: rgba(255, 255, 255, 0.8);
        background: rgba(255, 255, 255, 0.1);
      }
    }

    .type-filters {
      display: flex;
      gap: 0.5rem;
    }

    .filter-btn {
      padding: 10px 16px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      color: rgba(255, 255, 255, 0.6);
      font-size: 0.875rem;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        color: rgba(255, 255, 255, 0.9);
      }

      &.active {
        background: rgba(212, 168, 75, 0.2);
        border-color: rgba(212, 168, 75, 0.3);
        color: #D4A84B;
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

    .spin {
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    .table-container {
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 12px;
      overflow: hidden;
      overflow-x: auto;
    }

    .invoices-table {
      width: 100%;
      border-collapse: collapse;
      min-width: 800px;

      th, td {
        padding: 14px 16px;
        text-align: left;
        border-bottom: 1px solid rgba(255, 255, 255, 0.06);
      }

      th {
        background: rgba(255, 255, 255, 0.03);
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: rgba(255, 255, 255, 0.5);
        white-space: nowrap;

        &.sortable {
          cursor: pointer;
          user-select: none;
          transition: color 0.2s;

          &:hover {
            color: #D4A84B;
          }

          span {
            margin-right: 0.5rem;
          }

          ng-icon {
            font-size: 1rem;
            vertical-align: middle;
          }
        }
      }

      td {
        font-size: 0.875rem;
        color: rgba(255, 255, 255, 0.9);
      }

      tbody tr {
        transition: background 0.2s;

        &:hover {
          background: rgba(255, 255, 255, 0.03);
        }

        &:last-child td {
          border-bottom: none;
        }
      }

      .invoice-number {
        font-family: monospace;
        font-weight: 600;
        color: #D4A84B;
      }

      .type-cell {
        display: flex;
        align-items: center;
        gap: 0.5rem;

        .type-icon {
          font-size: 1.125rem;

          &.received {
            color: #22c55e;
          }

          &.issued {
            color: #3b82f6;
          }
        }

        span {
          font-size: 0.75rem;
          text-transform: uppercase;
          color: rgba(255, 255, 255, 0.5);
        }
      }

      .description-cell {
        .description {
          display: block;
          max-width: 250px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .party {
          display: block;
          font-size: 0.75rem;
          color: rgba(255, 255, 255, 0.4);
          margin-top: 2px;
        }
      }

      .amount {
        font-weight: 600;
        color: #D4A84B;
      }

      .status-badge {
        display: inline-block;
        padding: 4px 10px;
        border-radius: 6px;
        font-size: 0.75rem;
        font-weight: 500;
        background: rgba(255, 255, 255, 0.08);
        color: rgba(255, 255, 255, 0.6);

        &.paid {
          background: rgba(34, 197, 94, 0.15);
          color: #22c55e;
        }
      }

      .pdf-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 36px;
        height: 36px;
        background: rgba(212, 168, 75, 0.1);
        border: 1px solid rgba(212, 168, 75, 0.2);
        border-radius: 8px;
        color: #D4A84B;
        cursor: pointer;
        transition: all 0.2s;

        &:hover:not(:disabled) {
          background: rgba(212, 168, 75, 0.2);
          border-color: rgba(212, 168, 75, 0.4);
        }

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        ng-icon {
          font-size: 1.25rem;
        }
      }
    }

    .results-info {
      margin-top: 1rem;
      text-align: center;
      font-size: 0.875rem;
      color: rgba(255, 255, 255, 0.4);
    }

    @media (max-width: 768px) {
      .invoices-container {
        padding: 1rem;
      }

      .filters-row {
        flex-direction: column;
        align-items: stretch;
      }

      .search-box {
        min-width: 100%;
      }

      .date-filters {
        flex-wrap: wrap;
      }

      .date-input {
        flex: 1;
        min-width: 120px;
      }

      .type-filters {
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

  searchQuery = signal('');
  dateFrom = signal('');
  dateTo = signal('');
  sortField = signal<SortField>('date');
  sortOrder = signal<SortOrder>('desc');

  isTeacher = computed(() => this.authService.currentUser()?.role === 'TEACHER');

  filteredAndSortedInvoices = computed(() => {
    let result = this.invoices();
    const filter = this.activeFilter();
    const query = this.searchQuery().toLowerCase().trim();

    // Filter by type
    if (filter === 'received') {
      result = result.filter(inv => inv.isReceived);
    } else if (filter === 'issued') {
      result = result.filter(inv => !inv.isReceived);
    }

    // Filter by search
    if (query) {
      result = result.filter(inv =>
        inv.invoiceNumber.toLowerCase().includes(query) ||
        inv.description.toLowerCase().includes(query) ||
        inv.issuerName.toLowerCase().includes(query) ||
        inv.customerName.toLowerCase().includes(query)
      );
    }

    // Filter by date range (only when BOTH dates are set)
    if (this.dateFrom() && this.dateTo()) {
      const [fromYear, fromMonth, fromDay] = this.dateFrom().split('-').map(Number);
      const from = new Date(fromYear, fromMonth - 1, fromDay);
      const [toYear, toMonth, toDay] = this.dateTo().split('-').map(Number);
      const to = new Date(toYear, toMonth - 1, toDay, 23, 59, 59, 999);
      result = result.filter(inv => {
        const invDate = this.parseDate(inv.issuedAt);
        return invDate >= from && invDate <= to;
      });
    }

    // Sort
    const field = this.sortField();
    const order = this.sortOrder();
    result = [...result].sort((a, b) => {
      let comparison = 0;

      if (field === 'date') {
        comparison = this.parseDate(a.issuedAt).getTime() - this.parseDate(b.issuedAt).getTime();
      } else if (field === 'amount') {
        comparison = a.totalCents - b.totalCents;
      } else if (field === 'number') {
        comparison = a.invoiceNumber.localeCompare(b.invoiceNumber);
      }

      return order === 'asc' ? comparison : -comparison;
    });

    return result;
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

  toggleSort(field: SortField): void {
    if (this.sortField() === field) {
      this.sortOrder.set(this.sortOrder() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortOrder.set('desc');
    }
  }

  getSortIcon(field: SortField): string {
    if (this.sortField() !== field) {
      return 'heroChevronUpDown';
    }
    return this.sortOrder() === 'asc' ? 'heroChevronUp' : 'heroChevronDown';
  }

  clearDates(): void {
    this.dateFrom.set('');
    this.dateTo.set('');
  }

  downloadPdf(invoice: Invoice): void {
    this.downloadingId.set(invoice.id);
    this.invoiceService.downloadInvoicePdf(invoice.id);
    setTimeout(() => this.downloadingId.set(null), 2000);
  }

  formatCents(cents: number): string {
    return this.invoiceService.formatCents(cents);
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    // Format: "25/01/2026 00:41" -> extract dd/MM/yyyy
    const match = dateString.match(/^(\d{2})\/(\d{2})\/(\d{4})/);
    if (match) {
      const [, day, month, year] = match;
      return `${day}/${month}/${year}`;
    }
    return dateString;
  }

  parseDate(dateString: string): Date {
    if (!dateString) return new Date(0);
    // Format: "25/01/2026 00:41"
    const match = dateString.match(/^(\d{2})\/(\d{2})\/(\d{4})/);
    if (match) {
      const [, day, month, year] = match;
      return new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
    }
    return new Date(0);
  }
}
