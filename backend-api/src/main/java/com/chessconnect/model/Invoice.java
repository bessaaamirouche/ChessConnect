package com.chessconnect.model;

import com.chessconnect.model.enums.InvoiceType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_customer_id", columnList = "customer_id"),
    @Index(name = "idx_invoice_issuer_id", columnList = "issuer_id"),
    @Index(name = "idx_invoice_lesson_id", columnList = "lesson_id"),
    @Index(name = "idx_invoice_type", columnList = "invoice_type"),
    @Index(name = "idx_invoice_status", columnList = "status"),
    @Index(name = "idx_invoice_issued_at", columnList = "issued_at"),
    @Index(name = "idx_invoice_number", columnList = "invoice_number")
})
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sequential invoice number for France compliance (e.g., "FAC-2026-000001")
    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false)
    private InvoiceType invoiceType;

    // For LESSON_INVOICE: the student who paid
    // For COMMISSION_INVOICE: the teacher who receives the commission invoice
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    // For LESSON_INVOICE: the teacher who provided the service
    // For COMMISSION_INVOICE: null (platform is the issuer)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id")
    private User issuer;

    // Denormalized fields - preserved when user accounts are deleted (legal: 10 year retention)
    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "issuer_name")
    private String issuerName;

    @Column(name = "issuer_email")
    private String issuerEmail;

    // Related lesson (optional, for context)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    // Related payment intent ID from Stripe
    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    // Amounts in cents
    @Column(name = "subtotal_cents", nullable = false)
    private Integer subtotalCents;

    @Column(name = "vat_cents", nullable = false)
    private Integer vatCents = 0;

    @Column(name = "total_cents", nullable = false)
    private Integer totalCents;

    // VAT rate (e.g., 20 for 20%)
    @Column(name = "vat_rate")
    private Integer vatRate = 0;

    // Description/Label for the invoice line
    @Column(name = "description", nullable = false)
    private String description;

    // Whether promo code was applied (for commission invoices)
    @Column(name = "promo_applied")
    private Boolean promoApplied = false;

    // Commission rate applied (in percent, e.g., 12.5 or 2.5)
    @Column(name = "commission_rate")
    private Double commissionRate;

    // PDF storage path or URL
    @Column(name = "pdf_path")
    private String pdfPath;

    // Stripe Invoice ID (if created via Stripe Invoicing)
    @Column(name = "stripe_invoice_id")
    private String stripeInvoiceId;

    // For CREDIT_NOTE: reference to the original invoice being refunded
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_invoice_id")
    private Invoice originalInvoice;

    // Stripe Refund ID (for credit notes)
    @Column(name = "stripe_refund_id")
    private String stripeRefundId;

    // Refund percentage (for partial refunds)
    @Column(name = "refund_percentage")
    private Integer refundPercentage;

    @Column(name = "status", nullable = false)
    private String status = "PAID"; // PAID, REFUNDED, PARTIALLY_REFUNDED

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public InvoiceType getInvoiceType() { return invoiceType; }
    public void setInvoiceType(InvoiceType invoiceType) { this.invoiceType = invoiceType; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public User getIssuer() { return issuer; }
    public void setIssuer(User issuer) { this.issuer = issuer; }

    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }

    public Integer getSubtotalCents() { return subtotalCents; }
    public void setSubtotalCents(Integer subtotalCents) { this.subtotalCents = subtotalCents; }

    public Integer getVatCents() { return vatCents; }
    public void setVatCents(Integer vatCents) { this.vatCents = vatCents; }

    public Integer getTotalCents() { return totalCents; }
    public void setTotalCents(Integer totalCents) { this.totalCents = totalCents; }

    public Integer getVatRate() { return vatRate; }
    public void setVatRate(Integer vatRate) { this.vatRate = vatRate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getPromoApplied() { return promoApplied; }
    public void setPromoApplied(Boolean promoApplied) { this.promoApplied = promoApplied; }

    public Double getCommissionRate() { return commissionRate; }
    public void setCommissionRate(Double commissionRate) { this.commissionRate = commissionRate; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    public String getStripeInvoiceId() { return stripeInvoiceId; }
    public void setStripeInvoiceId(String stripeInvoiceId) { this.stripeInvoiceId = stripeInvoiceId; }

    public Invoice getOriginalInvoice() { return originalInvoice; }
    public void setOriginalInvoice(Invoice originalInvoice) { this.originalInvoice = originalInvoice; }

    public String getStripeRefundId() { return stripeRefundId; }
    public void setStripeRefundId(String stripeRefundId) { this.stripeRefundId = stripeRefundId; }

    public Integer getRefundPercentage() { return refundPercentage; }
    public void setRefundPercentage(Integer refundPercentage) { this.refundPercentage = refundPercentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getIssuerName() { return issuerName; }
    public void setIssuerName(String issuerName) { this.issuerName = issuerName; }

    public String getIssuerEmail() { return issuerEmail; }
    public void setIssuerEmail(String issuerEmail) { this.issuerEmail = issuerEmail; }

    /**
     * Get display name for customer - uses denormalized field as fallback when User is deleted.
     */
    public String getCustomerDisplayName() {
        if (customer != null) {
            return customer.getFirstName() + " " + customer.getLastName();
        }
        return customerName != null ? customerName : "Compte supprime";
    }

    /**
     * Get display email for customer - uses denormalized field as fallback.
     */
    public String getCustomerDisplayEmail() {
        if (customer != null) {
            return customer.getEmail();
        }
        return customerEmail != null ? customerEmail : "";
    }

    /**
     * Get display name for issuer - uses denormalized field as fallback when User is deleted.
     */
    public String getIssuerDisplayName() {
        if (issuer != null) {
            return issuer.getFirstName() + " " + issuer.getLastName();
        }
        return issuerName != null ? issuerName : "mychess";
    }

    /**
     * Get display email for issuer - uses denormalized field as fallback.
     */
    public String getIssuerDisplayEmail() {
        if (issuer != null) {
            return issuer.getEmail();
        }
        return issuerEmail != null ? issuerEmail : "";
    }
}
