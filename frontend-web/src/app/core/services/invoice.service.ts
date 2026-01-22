import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Invoice {
  id: number;
  invoiceNumber: string;
  invoiceType: 'LESSON_INVOICE' | 'COMMISSION_INVOICE';
  isReceived: boolean;
  issuerName: string;
  customerName: string;
  description: string;
  subtotalCents: number;
  vatCents: number;
  totalCents: number;
  vatRate: number;
  commissionRate: number;
  promoApplied: boolean;
  status: string;
  hasPdf: boolean;
  issuedAt: string;
  createdAt: string;
  lessonId: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class InvoiceService {
  private http = inject(HttpClient);
  private apiUrl = '/api/invoices';

  /**
   * Get all invoices for the current user
   */
  getMyInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.apiUrl}/me`);
  }

  /**
   * Get invoices received by the user (as customer)
   */
  getReceivedInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.apiUrl}/received`);
  }

  /**
   * Get invoices issued by the user (as teacher)
   */
  getIssuedInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.apiUrl}/issued`);
  }

  /**
   * Get a specific invoice
   */
  getInvoice(invoiceId: number): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.apiUrl}/${invoiceId}`);
  }

  /**
   * Download invoice PDF
   */
  downloadInvoicePdf(invoiceId: number): void {
    this.http.get(`${this.apiUrl}/${invoiceId}/pdf`, {
      responseType: 'blob'
    }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `facture-${invoiceId}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading invoice:', err);
      }
    });
  }

  /**
   * Format cents to EUR string
   */
  formatCents(cents: number): string {
    return (cents / 100).toFixed(2) + ' EUR';
  }
}
