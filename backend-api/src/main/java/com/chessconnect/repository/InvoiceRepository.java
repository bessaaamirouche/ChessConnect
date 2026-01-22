package com.chessconnect.repository;

import com.chessconnect.model.Invoice;
import com.chessconnect.model.enums.InvoiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Find invoices where user is the customer (received invoices)
    List<Invoice> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<Invoice> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    // Find invoices where user is the issuer (issued invoices - for teachers)
    List<Invoice> findByIssuerIdOrderByCreatedAtDesc(Long issuerId);

    Page<Invoice> findByIssuerIdOrderByCreatedAtDesc(Long issuerId, Pageable pageable);

    // Find by Stripe payment intent ID
    List<Invoice> findByStripePaymentIntentId(String paymentIntentId);

    // Check if invoices already exist for a payment intent
    boolean existsByStripePaymentIntentId(String paymentIntentId);

    // Find by invoice number
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // Get the latest invoice number for sequential numbering
    @Query("SELECT MAX(i.id) FROM Invoice i")
    Long findMaxId();

    // Find commission invoices for a teacher
    List<Invoice> findByCustomerIdAndInvoiceTypeOrderByCreatedAtDesc(Long customerId, InvoiceType invoiceType);

    // Find lesson invoices issued by a teacher
    List<Invoice> findByIssuerIdAndInvoiceTypeOrderByCreatedAtDesc(Long issuerId, InvoiceType invoiceType);

    // Find all invoices for a specific lesson
    List<Invoice> findByLessonId(Long lessonId);

    // Delete by user (for cascade delete)
    void deleteByCustomerId(Long customerId);
    void deleteByIssuerId(Long issuerId);
}
