package com.chessconnect.repository;

import com.chessconnect.model.Invoice;
import com.chessconnect.model.enums.InvoiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // Get the latest invoice number for sequential numbering (legacy, kept for compatibility)
    @Query("SELECT MAX(i.id) FROM Invoice i")
    Long findMaxId();

    // Atomic next value from the invoice_number_seq sequence (race-condition safe)
    @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
    Long getNextInvoiceSequenceValue();

    // Find commission invoices for a teacher
    List<Invoice> findByCustomerIdAndInvoiceTypeOrderByCreatedAtDesc(Long customerId, InvoiceType invoiceType);

    // Find lesson invoices issued by a teacher
    List<Invoice> findByIssuerIdAndInvoiceTypeOrderByCreatedAtDesc(Long issuerId, InvoiceType invoiceType);

    // Find all invoices for a specific lesson
    List<Invoice> findByLessonId(Long lessonId);

    // Nullify customer FK when user is deleted (preserves invoice for legal retention)
    @Modifying
    @Query("UPDATE Invoice i SET i.customer = null WHERE i.customer.id = :customerId")
    void nullifyCustomerId(@Param("customerId") Long customerId);

    // Nullify issuer FK when user is deleted (preserves invoice for legal retention)
    @Modifying
    @Query("UPDATE Invoice i SET i.issuer = null WHERE i.issuer.id = :issuerId")
    void nullifyIssuerId(@Param("issuerId") Long issuerId);

    // Nullify original_invoice FK for credit notes referencing invoices of a deleted user
    @Modifying
    @Query("UPDATE Invoice i SET i.originalInvoice = null WHERE i.originalInvoice.customer.id = :userId OR i.originalInvoice.issuer.id = :userId")
    void nullifyOriginalInvoiceByUserId(@Param("userId") Long userId);

    // Nullify lesson references before deleting lessons (preserves invoices for accounting)
    @Modifying
    @Query("UPDATE Invoice i SET i.lesson = null WHERE i.lesson.student.id = :studentId")
    void nullifyLessonByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Query("UPDATE Invoice i SET i.lesson = null WHERE i.lesson.teacher.id = :teacherId")
    void nullifyLessonByTeacherId(@Param("teacherId") Long teacherId);

    // Find all invoices (admin)
    List<Invoice> findAllByOrderByCreatedAtDesc();
}
